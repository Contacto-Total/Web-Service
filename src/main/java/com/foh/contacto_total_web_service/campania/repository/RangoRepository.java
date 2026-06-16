package com.foh.contacto_total_web_service.campania.repository;
import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;
import com.foh.contacto_total_web_service.campania.dto.RangoRequest;
import com.foh.contacto_total_web_service.campania.util.RangoConditionBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class RangoRepository {
    // Constantes para tipos de contacto
    private static final String CONTACTO_DIRECTO = "RANGO CONTACTO DIRECTO";
    private static final String CONTACTO_INDIRECTO = "RANGO CONTACTO INDIRECTO";
    private static final String PROMESA_ROTA = "RANGO PROMESA ROTA";
    private static final String NO_CONTACTADO = "RANGO NO CONTACTADO";

    // Constantes para columnas de montos
    private static final String SALDO_ACTUAL_CONSUMO = "SLDACTUALCONS";
    private static final String SALDO_CAPITAL_ASIGNADO = "SLDCAPITALASIG";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Busca rangos por tipo de contacto y genera una consulta combinada
     * @param request Parámetros de búsqueda con los diferentes tipos de rangos
     * @return Lista de resultados con documento, teléfono y tipo de contacto
     */
    public List<Object[]> findByRangosAndTipoContacto(
            GetFiltersToGenerateFileRequest request
    ) {
        System.out.println("========== INICIO RANGO REPOSITORY ==========");
        System.out.println("FilterType recibido: " + request.getFilterType());
        System.out.println("Campaign Name: " + request.getCampaignName());

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());
        List<String> subconsultas = construirSubconsultas(request, columnaFiltro);
        String consultaFinal = construirConsultaPrincipal(subconsultas, columnaFiltro, request);

        System.out.println("========== CONSULTA FINAL RANGOS ==========");
        System.out.println(consultaFinal);
        System.out.println("========== FIN CONSULTA RANGOS ==========");

        Query query = entityManager.createNativeQuery(consultaFinal);
        return query.getResultList();
    }

    /**
     * Construye las subconsultas para cada tipo de contacto
     */
    private List<String> construirSubconsultas(
            GetFiltersToGenerateFileRequest request,
            String columnaFiltro
    ) {
        List<String> subconsultas = new ArrayList<>();

        System.out.println("Columna de filtro seleccionada: " + columnaFiltro);

        // Subconsulta para contacto directo
        if (tieneElementos(request.getDirectContactRanges())) {
            String subconsulta = construirSubconsulta(
                    1, // bloque
                    request.getDirectContactRanges(),
                    CONTACTO_DIRECTO,
                    columnaFiltro,
                    "TIPI IN ('CONTACTO CON TITULAR O ENCARGADO')"
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para contacto indirecto
        if (tieneElementos(request.getIndirectContactRanges())) {
            String subconsulta = construirSubconsulta(
                    2, // bloque
                    request.getIndirectContactRanges(),
                    CONTACTO_INDIRECTO,
                    columnaFiltro,
                    "TIPI IN ('CONTACTO CON TERCEROS')"
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para promesas rotas
        if (tieneElementos(request.getBrokenPromisesRanges())) {
            String condicionPagadasHoy = construirCondicionPagadasHoy(request.getExcluirPagadasHoy());
            String condicionesExtra = construirCondicionesPromesasRotas(condicionPagadasHoy);
            String subconsulta = construirSubconsulta(
                    3, // bloque
                    request.getBrokenPromisesRanges(),
                    PROMESA_ROTA,
                    columnaFiltro,
                    condicionesExtra
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para no contactados
        if (tieneElementos(request.getNotContactedRanges())) {
            String condicionesNoContactado =
                    "(TIPI IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', " +
                            "'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE') OR TIPI IS NULL)";
            String subconsulta = construirSubconsulta(
                    4, // bloque
                    request.getNotContactedRanges(),
                    NO_CONTACTADO,
                    columnaFiltro,
                    condicionesNoContactado
            );
            subconsultas.add(subconsulta);
        }

        return subconsultas;
    }


    /**
     * Construye una subconsulta individual para un tipo específico de contacto
     */
    private String construirSubconsulta(
            int numeroBloque,
            List<RangoRequest> rangos,
            String tipoRango,
            String columnaMontos,
            String condicionesAdicionales
    ) {
        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                rangos, tipoRango, columnaMontos);

        return """
            SELECT %d AS BLOQUE, b.*
              FROM (
                   SELECT a.*,
                          %s
                     FROM base_tipi a
              ) b
             WHERE b.rango IS NOT NULL
               AND CAST(%s AS DECIMAL(10, 2)) > 0
               AND %s
            """.formatted(numeroBloque, condicionesRango, columnaMontos, condicionesAdicionales);
    }

    /**
     * Construye la consulta principal que une todas las subconsultas
     */
    private String construirConsultaPrincipal(List<String> subconsultas, String columnaFiltro, GetFiltersToGenerateFileRequest request) {
        String unionSubconsultas = String.join(" UNION ALL ", subconsultas);
        return construirCteBase(request, true) + """
            SELECT DOCUMENTO,
                   COALESCE(TELEFONOCELULAR, telefonodomicilio, telefonolaboral, telfreferencia1, telfreferencia2),
                   TIPI
              FROM (
                   %s
               ) B
             ORDER BY BLOQUE, %s DESC;
            """.formatted(unionSubconsultas, columnaFiltro);
    }

    private String construirCteBase(GetFiltersToGenerateFileRequest request, boolean incluirFiltrosTelefono) {
        return """
            WITH base AS (
                SELECT *
                  FROM TEMP_MERGE
                 %s
            ),
            docs AS (
                SELECT DISTINCT documento
                  FROM base
            ),
            tipi_gh AS (
                SELECT gh.documento,
                       SUBSTRING_INDEX(GROUP_CONCAT(gh.resultado ORDER BY dp.peso DESC SEPARATOR '||'), '||', 1) AS resultado
                  FROM GESTION_HISTORICA gh
                  JOIN diccionario_pesos dp ON gh.resultado = dp.tipificacion
                  JOIN docs d ON d.documento = gh.documento
                 GROUP BY gh.documento
            ),
            tipi_bi AS (
                SELECT bi.documento,
                       SUBSTRING_INDEX(GROUP_CONCAT(bi.resultado ORDER BY dp.peso DESC SEPARATOR '||'), '||', 1) AS resultado
                  FROM GESTION_HISTORICA_BI bi
                  JOIN diccionario_pesos dp ON bi.resultado = dp.tipificacion
                  JOIN docs d ON d.documento = bi.documento
                 WHERE bi.FechaGestion >= DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%%Y-%%m-01')
                   AND bi.FechaGestion < DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
                 GROUP BY bi.documento
            ),
            base_tipi AS (
                SELECT base.*,
                       COALESCE(tipi_gh.resultado, tipi_bi.resultado) AS TIPI
                  FROM base
                  LEFT JOIN tipi_gh ON tipi_gh.documento = base.documento
                  LEFT JOIN tipi_bi ON tipi_bi.documento = base.documento
            )
            """.formatted(construirCondicionesBase(request, incluirFiltrosTelefono));
    }

    private String construirCondicionesBase(GetFiltersToGenerateFileRequest request, boolean incluirFiltrosTelefono) {
        StringBuilder condiciones = new StringBuilder("WHERE 1 = 1");

        if (request.getCampaignName() != null && !request.getCampaignName().trim().isEmpty()) {
            condiciones.append(" AND RANGOMORAPROYAG = '").append(request.getCampaignName().trim()).append("'");
        }

        condiciones.append(construirCondicionFechas(request.getDueDates()));

        String condicionContenido = construirCondicionContenido(request.getCampaignName(), request.getContent());
        if (!condicionContenido.isEmpty()) {
            condiciones.append(" ").append(condicionContenido);
        }

        condiciones.append(" AND DOCUMENTO NOT IN (")
                .append("SELECT DOCUMENTO FROM blacklist ")
                .append("WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN")
                .append(")");
        condiciones.append(" AND DOCUMENTO NOT IN (")
                .append("SELECT DOCUMENTO FROM GESTION_HISTORICA WHERE Resultado = 'CANCELACION TOTAL'")
                .append(")");

        if (incluirFiltrosTelefono) {
            condiciones.append(" AND TELEFONOCELULAR NOT IN (")
                    .append("SELECT DISTINCT Telefono FROM GESTION_HISTORICA_BI ")
                    .append("WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO')")
                    .append(")");
            condiciones.append(" AND TELEFONOCELULAR != ''");
        }

        return condiciones.toString();
    }

    /**
     * Construye las condiciones específicas para promesas rotas
     */
    private String construirCondicionesPromesasRotas(String condicionPagadasHoy) {
        String tiposPromesa = "TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', " +
                "'RECORDATORIO DE PAGO', 'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', " +
                "'CANCELACION TOTAL', 'CANCELACION NO REPORTADAS O APLICADAS')";
        String condicionFinal = tiposPromesa + " AND documento IN (" + construirSubconsultaPromesasCaidasSinColchon() + ")";
        condicionFinal += condicionPagadasHoy;
        return condicionFinal;
    }

    /**
     * Construye la condición para excluir documentos con estado 'Pagada' en PROMESAS_HISTORICO
     */
    private String construirCondicionPagadasHoy(Boolean excluirPagadasHoy) {
        if (excluirPagadasHoy == null || !excluirPagadasHoy) {
            return "";
        }
        return " AND documento NOT IN (SELECT DISTINCT documento FROM PROMESAS_HISTORICO WHERE Estado = 'Pagada')";
    }

    private String construirSubconsultaPromesasCaidasSinColchon() {
        return "SELECT DOCUMENTO FROM COMPROMISOS " +
                "WHERE DAY(CURDATE()) <> 2 " +
                "AND IMPORTE_PAGO_MENSUAL = 0 " +
                "AND ESTADO_COMPROMISO = 'CAIDO' " +
                "AND FECHA_COMPROMISO <= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '%Y-%m-%d') " +
                "AND DOCUMENTO NOT IN (" +
                "SELECT DISTINCT Documento FROM GESTION_HISTORICA " +
                "WHERE Resultado IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO') " +
                "AND (Observacion LIKE '%(CONVENIO)%' OR Observacion LIKE '%(EXCEPCION)%') " +
                "AND FechaGestion <= DATE_FORMAT(CURDATE(), '%Y-%m-03'))";
    }

    /**
     * Construye la condición para las fechas de vencimiento
     * @param dueDates Lista de fechas de vencimiento
     * @return Condición SQL para filtrar por fechas de vencimiento
     */
    private String construirCondicionFechas(List<String> dueDates) {
        if (!tieneElementos(dueDates)) {
            return "";
        }

        // Construir la condición FECVENCIMIENTO IN (lista_de_fechas)
        String fechasFormateadas = dueDates.stream()
                .map(fecha -> "'" + fecha.trim() + "'")
                .collect(Collectors.joining(", "));

        return " AND FECVENCIMIENTO IN (" + fechasFormateadas + ")";
    }

    private String construirCondicionContenido(String campaignName, Boolean content) {
        if(Objects.equals(campaignName, "Tramo 3") && !content) {
            return "AND (DOCUMENTO in (SELECT CASE WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) ELSE A.IDENTITY_CODE END AS DOCUMENTO FROM PAYS_TEMP A WHERE RANGO_MORA_ASIG  IN ('4.[61-90]') AND CONTENCION = 'NO CONTENIDO') OR (SELECT COUNT(*) FROM PAYS_TEMP WHERE RANGO_MORA_ASIG  IN ('4.[61-90]') AND CONTENCION = 'NO CONTENIDO') = 0)";
        }

        if(Objects.equals(campaignName, "Tramo 5") && !content) {
            return "AND (DOCUMENTO in (SELECT CASE WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) ELSE A.IDENTITY_CODE END AS DOCUMENTO FROM PAYS_TEMP A WHERE RANGO_MORA_ASIG  IN ('[121-mas]') AND CONTENCION = 'NO CONTENIDO') OR (SELECT COUNT(*) FROM PAYS_TEMP WHERE RANGO_MORA_ASIG  IN ('[121-mas]') AND CONTENCION = 'NO CONTENIDO') = 0)";
        }

        if(Objects.equals(campaignName, "CONTACTO_TOTAL") && !content) {
            return "AND (DOCUMENTO in (SELECT CASE WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) ELSE A.IDENTITY_CODE END AS DOCUMENTO FROM PAYS_TEMP A WHERE RANGO_MORA_ASIG  IN ('CONTACTO_TOTAL') AND CONTENCION = 'NO CONTENIDO') OR (SELECT COUNT(*) FROM PAYS_TEMP WHERE RANGO_MORA_ASIG  IN ('CONTACTO_TOTAL') AND CONTENCION = 'NO CONTENIDO') = 0)";
        }

        return "";
    }

    /**
     * Verifica si una lista tiene elementos
     */
    private boolean tieneElementos(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }

    /**
     * Obtiene la columna a usar para el filtro según el tipo seleccionado
     * @param filterType Tipo de filtro: "saldoCapital", "baja30", "baja60", "baja90"
     * @return Nombre de la columna en la base de datos
     */
    private String obtenerColumnaFiltro(String filterType) {
        if (filterType == null || filterType.trim().isEmpty()) {
            return SALDO_CAPITAL_ASIGNADO; // Por defecto
        }

        switch (filterType.trim().toLowerCase()) {
            case "saldocapital":
                return SALDO_CAPITAL_ASIGNADO;
            case "baja30":
                return "`2`"; // Columna para baja 30 (escapada con backticks)
            case "baja60":
                return "`3`"; // Columna para baja 60 (escapada con backticks)
            case "baja90":
                return "`4`"; // Columna para baja 90 (escapada con backticks)
            default:
                return SALDO_CAPITAL_ASIGNADO;
        }
    }
}

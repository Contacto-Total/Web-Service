package com.foh.contacto_total_web_service.campania.repository;
import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;
import com.foh.contacto_total_web_service.campania.dto.RangoRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
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
        System.out.println("Columna de filtro seleccionada: " + columnaFiltro);
        String consultaFinal = construirConsultaPrincipal(columnaFiltro, request);

        System.out.println("========== CONSULTA FINAL RANGOS ==========");
        System.out.println(consultaFinal);
        System.out.println("========== FIN CONSULTA RANGOS ==========");

        Query query = entityManager.createNativeQuery(consultaFinal);
        return query.getResultList();
    }

    /**
     * Construye la consulta principal que une todas las subconsultas
     */
    private String construirConsultaPrincipal(String columnaFiltro, GetFiltersToGenerateFileRequest request) {
        String baseTipi = construirBaseTipi(request, true);
        String bloqueCase = construirBloqueCase(request);
        String rangoCase = construirRangoCase(request, columnaFiltro);

        return """
            SELECT DOCUMENTO,
                   COALESCE(TELEFONOCELULAR, telefonodomicilio, telefonolaboral, telfreferencia1, telfreferencia2),
                   TIPI
              FROM (
                   SELECT %s AS BLOQUE,
                          a.*,
                          %s
                     FROM (%s) a
               ) B
             WHERE B.RANGO IS NOT NULL
             ORDER BY BLOQUE, %s DESC;
            """.formatted(bloqueCase, rangoCase, baseTipi, columnaFiltro);
    }

    private String construirBloqueCase(GetFiltersToGenerateFileRequest request) {
        StringBuilder bloqueCase = new StringBuilder("CASE ");
        if (tieneElementos(request.getDirectContactRanges())) {
            bloqueCase.append("WHEN TIPI IN ('CONTACTO CON TITULAR O ENCARGADO') THEN 1 ");
        }
        if (tieneElementos(request.getIndirectContactRanges())) {
            bloqueCase.append("WHEN TIPI IN ('CONTACTO CON TERCEROS') THEN 2 ");
        }
        if (tieneElementos(request.getBrokenPromisesRanges())) {
            bloqueCase.append("WHEN ").append(construirCondicionesPromesasRotas(construirCondicionPagadasHoy(request.getExcluirPagadasHoy()))).append(" THEN 3 ");
        }
        if (tieneElementos(request.getNotContactedRanges())) {
            bloqueCase.append("WHEN (TIPI IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', 'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE') OR TIPI IS NULL) THEN 4 ");
        }
        bloqueCase.append("END");
        return bloqueCase.toString();
    }

    private String construirRangoCase(GetFiltersToGenerateFileRequest request, String columnaFiltro) {
        StringBuilder rangoCase = new StringBuilder("CAST(CASE ");
        agregarRangosAlCase(rangoCase, request.getDirectContactRanges(), CONTACTO_DIRECTO, columnaFiltro,
                "TIPI IN ('CONTACTO CON TITULAR O ENCARGADO')");
        agregarRangosAlCase(rangoCase, request.getIndirectContactRanges(), CONTACTO_INDIRECTO, columnaFiltro,
                "TIPI IN ('CONTACTO CON TERCEROS')");
        agregarRangosAlCase(rangoCase, request.getBrokenPromisesRanges(), PROMESA_ROTA, columnaFiltro,
                construirCondicionesPromesasRotas(construirCondicionPagadasHoy(request.getExcluirPagadasHoy())));
        agregarRangosAlCase(rangoCase, request.getNotContactedRanges(), NO_CONTACTADO, columnaFiltro,
                "(TIPI IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', 'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE') OR TIPI IS NULL)");
        rangoCase.append("END AS CHAR(100)) AS RANGO");
        return rangoCase.toString();
    }

    private void agregarRangosAlCase(StringBuilder rangoCase, List<RangoRequest> rangos, String tipoRango, String columnaFiltro, String condicionTipo) {
        if (!tieneElementos(rangos)) {
            return;
        }

        for (RangoRequest rango : rangos) {
            rangoCase.append("WHEN ").append(condicionTipo)
                    .append(" AND ").append(columnaFiltro).append(" > ").append(rango.getMin());

            if ("+".equals(rango.getMax())) {
                rangoCase.append(" THEN '").append(tipoRango).append(" ").append(rango.getMin()).append(" - +' ");
            } else {
                rangoCase.append(" AND ").append(columnaFiltro).append(" <= ").append(rango.getMax())
                        .append(" THEN '").append(tipoRango).append(" ").append(rango.getMin()).append(" - ").append(rango.getMax()).append("' ");
            }
        }
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

    private String construirBaseTipi(GetFiltersToGenerateFileRequest request, boolean incluirFiltrosTelefono) {
        String condicionesBase = construirCondicionesBase(request, incluirFiltrosTelefono);
        String documentosBase = "SELECT DISTINCT documento FROM TEMP_MERGE " + condicionesBase;

        return """
            SELECT base.*,
                   COALESCE(tipi_gh.resultado, tipi_bi.resultado) AS TIPI
              FROM (
                   SELECT *
                     FROM TEMP_MERGE
                    %s
              ) base
              LEFT JOIN (
                   SELECT gh.documento,
                          SUBSTRING_INDEX(GROUP_CONCAT(gh.resultado ORDER BY dp.peso DESC SEPARATOR '||'), '||', 1) AS resultado
                     FROM GESTION_HISTORICA gh
                     JOIN diccionario_pesos dp ON gh.resultado = dp.tipificacion
                     JOIN (%s) docs ON docs.documento = gh.documento
                    GROUP BY gh.documento
              ) tipi_gh ON tipi_gh.documento = base.documento
              LEFT JOIN (
                   SELECT bi.documento,
                          SUBSTRING_INDEX(GROUP_CONCAT(bi.resultado ORDER BY dp.peso DESC SEPARATOR '||'), '||', 1) AS resultado
                     FROM GESTION_HISTORICA_BI bi
                     JOIN diccionario_pesos dp ON bi.resultado = dp.tipificacion
                     JOIN (%s) docs ON docs.documento = bi.documento
                    WHERE bi.FechaGestion >= DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%%Y-%%m-01')
                      AND bi.FechaGestion < DATE_FORMAT(CURDATE(), '%%Y-%%m-01')
                    GROUP BY bi.documento
              ) tipi_bi ON tipi_bi.documento = base.documento
            """.formatted(condicionesBase, documentosBase, documentosBase);
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

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
     * @param documentosPromesasCaidas Lista de documentos con promesas caídas
     * @return Lista de resultados con documento, teléfono y tipo de contacto
     */
    public List<Object[]> findByRangosAndTipoContacto(
            GetFiltersToGenerateFileRequest request,
            List<String> documentosPromesasCaidas
    ) {
        System.out.println("========== INICIO RANGO REPOSITORY ==========");
        System.out.println("FilterType recibido: " + request.getFilterType());
        System.out.println("Campaign Name: " + request.getCampaignName());

        List<String> subconsultas = construirSubconsultas(request, documentosPromesasCaidas);
        String consultaFinal = construirConsultaPrincipal(subconsultas);

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
            List<String> documentosPromesasCaidas
    ) {
        List<String> subconsultas = new ArrayList<>();
        String rangoMoraProyectado = request.getCampaignName();
        String condicionFechas = construirCondicionFechas(request.getDueDates());
        String condicionContenido = construirCondicionContenido(request.getCampaignName(), request.getContent());

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());
        System.out.println("Columna de filtro seleccionada: " + columnaFiltro);

        // Subconsulta para contacto directo
        if (tieneElementos(request.getDirectContactRanges())) {
            String subconsulta = construirSubconsulta(
                    1, // bloque
                    request.getDirectContactRanges(),
                    CONTACTO_DIRECTO,
                    columnaFiltro,
                    "TIPI IN ('CONTACTO CON TITULAR O ENCARGADO')",
                    rangoMoraProyectado,
                    condicionFechas,
                    condicionContenido
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
                    "TIPI IN ('CONTACTO CON TERCEROS')",
                    rangoMoraProyectado,
                    condicionFechas,
                    condicionContenido
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para promesas rotas
        if (tieneElementos(request.getBrokenPromisesRanges())) {
            String condicionPromesas = construirCondicionPromesasRotas(documentosPromesasCaidas);
            String condicionPagadasHoy = construirCondicionPagadasHoy(request.getExcluirPagadasHoy());
            String condicionesExtra = construirCondicionesPromesasRotas(condicionPromesas, condicionPagadasHoy);
            String subconsulta = construirSubconsulta(
                    3, // bloque
                    request.getBrokenPromisesRanges(),
                    PROMESA_ROTA,
                    columnaFiltro,
                    condicionesExtra,
                    rangoMoraProyectado,
                    condicionFechas,
                    condicionContenido
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
                    condicionesNoContactado,
                    rangoMoraProyectado,
                    condicionFechas,
                    condicionContenido
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
            String condicionesAdicionales,
            String rangoMoraProyectado,
            String condicionFechas,
            String condicionContenido
    ) {
        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                rangos, tipoRango, columnaMontos);
        String condicionRangoMora = construirCondicionRangoMora(rangoMoraProyectado);

        return """
            SELECT %d AS BLOQUE, b.*
              FROM (
                   SELECT BUSCAR_MAYOR_TIP_V3(documento) AS TIPI,
                          a.*,
                          %s
                     FROM TEMP_MERGE a
                    %s
              ) b
             WHERE b.rango IS NOT NULL
               AND CAST(%s AS DECIMAL(10, 2)) > 0
               AND %s%s
            """.formatted(numeroBloque, condicionesRango, condicionRangoMora,
                columnaMontos, condicionesAdicionales, condicionFechas);
    }

    /**
     * Construye la consulta principal que une todas las subconsultas
     */
    private String construirConsultaPrincipal(List<String> subconsultas) {
        String unionSubconsultas = String.join(" UNION ALL ", subconsultas);
        return """
            SELECT DOCUMENTO,
                   COALESCE(TELEFONOCELULAR, telefonodomicilio, telefonolaboral, telfreferencia1, telfreferencia2),
                   TIPI
              FROM (
                   %s
              ) B
             WHERE DOCUMENTO NOT IN (
                   SELECT DOCUMENTO
                     FROM blacklist
                    WHERE DATE_FORMAT(CURDATE(), '%%Y-%%m-%%d') BETWEEN FECHA_INICIO AND FECHA_FIN
             )
               AND DOCUMENTO NOT IN (
                   SELECT DOCUMENTO
                     FROM GESTION_HISTORICA
                    WHERE Resultado = 'CANCELACION TOTAL'
               )
               AND TELEFONOCELULAR NOT IN (
                   SELECT DISTINCT Telefono
                     FROM GESTION_HISTORICA_BI
                    WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO')
               )
               AND TELEFONOCELULAR != ''
             ORDER BY BLOQUE, SLDCAPCONS DESC;
            """.formatted(unionSubconsultas);
    }

    /**
     * Construye las condiciones específicas para promesas rotas
     */
    private String construirCondicionesPromesasRotas(String condicionDocumentos, String condicionPagadasHoy) {
        String tiposPromesa = "TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', " +
                "'RECORDATORIO DE PAGO', 'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', " +
                "'CANCELACION TOTAL', 'CANCELACION NO REPORTADAS O APLICADAS')";
        String condicionFinal = tiposPromesa;
        if (condicionDocumentos.isEmpty()) {
            condicionFinal += " AND documento IN ('')";
        } else {
            condicionFinal += " AND documento IN (" + condicionDocumentos + ")";
        }
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

    /**
     * Construye la condición para documentos de promesas caídas
     */
    private String construirCondicionPromesasRotas(List<String> documentosPromesasCaidas) {
        if (!tieneElementos(documentosPromesasCaidas)) {
            return "";
        }
        return documentosPromesasCaidas.stream()
                .map(documento -> "'" + documento + "'")
                .collect(Collectors.joining(", "));
    }

    /**
     * Construye la condición WHERE para el rango de mora proyectado
     */
    private String construirCondicionRangoMora(String rangoMoraProyectado) {
        if (rangoMoraProyectado == null || rangoMoraProyectado.trim().isEmpty()) {
            return "";
        }
        return "WHERE RANGOMORAPROYAG = '" + rangoMoraProyectado.trim() + "'";
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

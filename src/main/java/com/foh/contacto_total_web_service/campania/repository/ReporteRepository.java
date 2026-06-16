package com.foh.contacto_total_web_service.campania.repository;
import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;
import com.foh.contacto_total_web_service.campania.util.RangoConditionBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class ReporteRepository {
    // Constantes para tipos de rango
    private static final String TIPO_CONTACTO_DIRECTO = "CONTACTO DIRECTO";
    private static final String TIPO_CONTACTO_INDIRECTO = "CONTACTO INDIRECTO";
    private static final String TIPO_PROMESA_ROTA = "PROMESA ROTA";
    private static final String TIPO_NO_CONTACTADO = "NO CONTACTADO";

    // Constantes para prefijos de rangos
    private static final String RANGO_CONTACTO_DIRECTO = "RANGO CONTACTO DIRECTO";
    private static final String RANGO_CONTACTO_INDIRECTO = "RANGO CONTACTO INDIRECTO";
    private static final String RANGO_PROMESA_ROTA = "RANGO PROMESA ROTA";
    private static final String RANGO_NO_CONTACTADO = "RANGO NO CONTACTADO";

    // Constantes para columnas de montos
    private static final String SALDO_ACTUAL_CONSUMO = "SLDACTUALCONS";
    private static final String SALDO_CAPITAL_ASIGNADO = "SLDCAPITALASIG";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Genera un reporte agrupado por rangos de diferentes tipos de contacto
     * @param request Parámetros con los rangos de cada tipo de contacto
     * @return Lista con el conteo de registros por rango y tipo
     */
    public List<Object[]> getReporteByRangos(
            GetFiltersToGenerateFileRequest request
    ) {
        System.out.println("========== INICIO REPORTE REPOSITORY ==========");
        System.out.println("FilterType recibido: " + request.getFilterType());
        System.out.println("Campaign Name: " + request.getCampaignName());

        StringBuilder constructorConsulta = new StringBuilder();
        constructorConsulta.append("SELECT RANGO, COUNT(1) FROM (");

        String condicionFechas = construirCondicionFechas(request.getDueDates());
        String condicionContenido = construirCondicionContenido(request.getCampaignName(), request.getContent());
        String baseTipi = construirBaseTipi(request);
        boolean hayConsultaPrevia = false;

        // Agregar cada tipo de contacto si está presente
        hayConsultaPrevia = agregarConsultaContactoDirecto(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido, baseTipi);
        hayConsultaPrevia = agregarConsultaContactoIndirecto(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido, baseTipi);
        hayConsultaPrevia = agregarConsultaPromesasRotas(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido, baseTipi);
        hayConsultaPrevia = agregarConsultaNoContactados(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido, baseTipi);

        // Finalizar la consulta con GROUP BY y ORDER BY
        finalizarConsulta(constructorConsulta);

        System.out.println("========== CONSULTA FINAL REPORTE ==========");
        String consultaFinal = constructorConsulta.toString();
        System.out.println(consultaFinal);
        System.out.println("========== FIN CONSULTA REPORTE ==========");

        Query query = entityManager.createNativeQuery(consultaFinal);
        return query.getResultList();
    }

    /**
     * Agrega la subconsulta para contactos directos si existen rangos definidos
     */
    private boolean agregarConsultaContactoDirecto(
            GetFiltersToGenerateFileRequest request,
            StringBuilder constructorConsulta,
            boolean hayConsultaPrevia,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        if (!tieneElementos(request.getDirectContactRanges())) {
            return hayConsultaPrevia;
        }

        if (hayConsultaPrevia) {
            constructorConsulta.append(" UNION ALL ");
        }

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());
        System.out.println("Columna filtro CD: " + columnaFiltro);

        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                request.getDirectContactRanges(),
                RANGO_CONTACTO_DIRECTO,
                columnaFiltro
        );

        String subconsulta = construirSubconsultaBase(
                condicionesRango,
                TIPO_CONTACTO_DIRECTO,
                columnaFiltro,
                "TIPI IN ('CONTACTO CON TITULAR O ENCARGADO')",
                "",
                condicionFechas,
                condicionContenido,
                baseTipi
        );

        constructorConsulta.append(subconsulta);
        return true;
    }

    /**
     * Agrega la subconsulta para contactos indirectos si existen rangos definidos
     */
    private boolean agregarConsultaContactoIndirecto(
            GetFiltersToGenerateFileRequest request,
            StringBuilder constructorConsulta,
            boolean hayConsultaPrevia,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        if (!tieneElementos(request.getIndirectContactRanges())) {
            return hayConsultaPrevia;
        }

        if (hayConsultaPrevia) {
            constructorConsulta.append(" UNION ALL ");
        }

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());

        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                request.getIndirectContactRanges(),
                RANGO_CONTACTO_INDIRECTO,
                columnaFiltro
        );

        String subconsulta = construirSubconsultaBase(
                condicionesRango,
                TIPO_CONTACTO_INDIRECTO,
                columnaFiltro,
                "TIPI IN ('CONTACTO CON TERCEROS')",
                "",
                condicionFechas,
                condicionContenido,
                baseTipi
        );

        constructorConsulta.append(subconsulta);
        return true;
    }

    /**
     * Agrega la subconsulta para promesas rotas si existen rangos definidos
     */
    private boolean agregarConsultaPromesasRotas(
            GetFiltersToGenerateFileRequest request,
            StringBuilder constructorConsulta,
            boolean hayConsultaPrevia,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        if (!tieneElementos(request.getBrokenPromisesRanges())) {
            return hayConsultaPrevia;
        }

        if (hayConsultaPrevia) {
            constructorConsulta.append(" UNION ALL ");
        }

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());

        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                request.getBrokenPromisesRanges(),
                RANGO_PROMESA_ROTA,
                columnaFiltro
        );

        String condicionesTipoContacto = construirCondicionesTipoPromesa();
        String condicionDocumentos = construirCondicionDocumentosPromesas();
        String condicionPagadasHoy = construirCondicionPagadasHoy(request.getExcluirPagadasHoy());

        String subconsulta = construirSubconsultaBase(
                condicionesRango,
                TIPO_PROMESA_ROTA,
                columnaFiltro,
                condicionesTipoContacto,
                condicionDocumentos + condicionPagadasHoy,
                condicionFechas,
                condicionContenido,
                baseTipi
        );

        constructorConsulta.append(subconsulta);
        return true;
    }

    /**
     * Agrega la subconsulta para no contactados si existen rangos definidos
     */
    private boolean agregarConsultaNoContactados(
            GetFiltersToGenerateFileRequest request,
            StringBuilder constructorConsulta,
            boolean hayConsultaPrevia,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        if (!tieneElementos(request.getNotContactedRanges())) {
            return hayConsultaPrevia;
        }

        if (hayConsultaPrevia) {
            constructorConsulta.append(" UNION ALL ");
        }

        String columnaFiltro = obtenerColumnaFiltro(request.getFilterType());

        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                request.getNotContactedRanges(),
                RANGO_NO_CONTACTADO,
                columnaFiltro
        );

        String condicionesNoContactado =
                "(COALESCE(TIPI, 'NO CONTESTA') IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', " +
                        "'NO CONTESTA', 'APAGADO', 'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE'))";

        String subconsulta = construirSubconsultaNoContactados(
                condicionesRango,
                columnaFiltro,
                condicionesNoContactado,
                condicionFechas,
                condicionContenido,
                baseTipi
        );

        constructorConsulta.append(subconsulta);
        return true;
    }

    /**
     * Construye la estructura base de subconsulta común a la mayoría de tipos
     */
    private String construirSubconsultaBase(
            String condicionesRango,
            String tipoRango,
            String columnaMontos,
            String condicionesTipo,
            String condicionesAdicionales,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        StringBuilder subconsulta = new StringBuilder();

        subconsulta.append("SELECT *, '").append(tipoRango).append("' AS RANGO_TIPO FROM (")
                .append("SELECT a.*, ")
                .append(condicionesRango)
                .append(" FROM (").append(baseTipi).append(") a ");

        subconsulta.append(") b ")
                .append("WHERE CAST(").append(columnaMontos).append(" AS DECIMAL(10, 2)) > 0 ")
                .append("AND ").append(condicionesTipo).append(" ")
                .append("AND b.rango IS NOT NULL");

        if (!condicionesAdicionales.isEmpty()) {
            subconsulta.append(" ").append(condicionesAdicionales);
        }

        return subconsulta.toString();
    }

    /**
     * Construye la subconsulta específica para no contactados (tiene estructura ligeramente diferente)
     */
    private String construirSubconsultaNoContactados(
            String condicionesRango,
            String columnaFiltro,
            String condicionesNoContactado,
            String condicionFechas,
            String condicionContenido,
            String baseTipi
    ) {
        StringBuilder subconsulta = new StringBuilder();

        subconsulta.append("SELECT *, '").append(TIPO_NO_CONTACTADO).append("' AS RANGO_TIPO FROM (")
                .append("SELECT a.*, ").append(condicionesRango)
                .append(" FROM (").append(baseTipi).append(") a ");

        subconsulta.append(") b ")
                .append("WHERE b.rango IS NOT NULL ")
                .append("AND CAST(").append(columnaFiltro).append(" AS DECIMAL(10, 2)) > 0 ")
                .append("AND ").append(condicionesNoContactado);

        return subconsulta.toString();
    }

    /**
     * Construye las condiciones para tipos de promesas de pago
     */
    private String construirCondicionesTipoPromesa() {
        return "TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', 'RECORDATORIO DE PAGO', " +
                "'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', 'CANCELACION TOTAL', " +
                "'CANCELACION NO REPORTADAS O APLICADAS')";
    }

    /**
     * Construye la condición para filtrar documentos de promesas caídas
     */
    private String construirCondicionDocumentosPromesas() {
        return "AND documento IN (" + construirSubconsultaPromesasCaidasSinColchon() + ")";
    }

    /**
     * Finaliza la construcción de la consulta con GROUP BY y ORDER BY
     */
    private void finalizarConsulta(StringBuilder constructorConsulta) {
        constructorConsulta.append(") E GROUP BY RANGO, RANGO_TIPO ")
                .append("ORDER BY FIELD(RANGO_TIPO, '")
                .append(TIPO_CONTACTO_DIRECTO).append("', '")
                .append(TIPO_CONTACTO_INDIRECTO).append("', '")
                .append(TIPO_PROMESA_ROTA).append("', '")
                .append(TIPO_NO_CONTACTADO).append("')");
    }

    private String construirBaseTipi(GetFiltersToGenerateFileRequest request) {
        String condicionesBase = construirCondicionesBase(request);
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

    private String construirCondicionesBase(GetFiltersToGenerateFileRequest request) {
        StringBuilder condiciones = new StringBuilder("WHERE 1 = 1");

        String condicionRangoMora = construirCondicionRangoMora(request.getCampaignName());
        if (!condicionRangoMora.isEmpty()) {
            condiciones.append(" AND ").append(condicionRangoMora);
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

        return condiciones.toString();
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
     * Construye la condición WHERE para el rango de mora proyectado si está definido
     */
    private String construirCondicionRangoMora(String rangoMoraProyectado) {
        if (rangoMoraProyectado == null || rangoMoraProyectado.trim().isEmpty()) {
            return "";
        }
        return "RANGOMORAPROYAG = '" + rangoMoraProyectado.trim() + "'";
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

    /**
     * Construye la condición para excluir documentos con estado 'Pagada' en PROMESAS_HISTORICO
     */
    private String construirCondicionPagadasHoy(Boolean excluirPagadasHoy) {
        if (excluirPagadasHoy == null || !excluirPagadasHoy) {
            return "";
        }
        return " AND documento NOT IN (SELECT DISTINCT documento FROM PROMESAS_HISTORICO WHERE Estado = 'Pagada')";
    }

    public List<String> getFechasDeVencimientoDisponibles() {
        String sql = "SELECT DISTINCT FECVENCIMIENTO FROM TEMP_MERGE WHERE RANGOMORAPROYAG='Tramo 3' ORDER BY FECVENCIMIENTO";
        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }
}

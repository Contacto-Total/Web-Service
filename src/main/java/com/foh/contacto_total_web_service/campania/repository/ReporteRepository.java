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
     * @param documentosPromesasCaidas Lista de documentos con promesas caídas
     * @return Lista con el conteo de registros por rango y tipo
     */
    public List<Object[]> getReporteByRangos(
            GetFiltersToGenerateFileRequest request,
            List<String> documentosPromesasCaidas
    ) {
        System.out.println("========== INICIO REPORTE REPOSITORY ==========");
        System.out.println("FilterType recibido: " + request.getFilterType());
        System.out.println("Campaign Name: " + request.getCampaignName());

        StringBuilder constructorConsulta = new StringBuilder();
        constructorConsulta.append("SELECT RANGO, COUNT(1) FROM (");

        String condicionFechas = construirCondicionFechas(request.getDueDates());
        String condicionContenido = construirCondicionContenido(request.getCampaignName(), request.getContent());
        boolean hayConsultaPrevia = false;

        // Agregar cada tipo de contacto si está presente
        hayConsultaPrevia = agregarConsultaContactoDirecto(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido);
        hayConsultaPrevia = agregarConsultaContactoIndirecto(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido);
        hayConsultaPrevia = agregarConsultaPromesasRotas(request, constructorConsulta, documentosPromesasCaidas, hayConsultaPrevia, condicionFechas, condicionContenido);
        hayConsultaPrevia = agregarConsultaNoContactados(request, constructorConsulta, hayConsultaPrevia, condicionFechas, condicionContenido);

        // Finalizar la consulta con GROUP BY y ORDER BY
        finalizarConsulta(constructorConsulta);

        System.out.println("========== CONSULTA FINAL REPORTE ==========");
        System.out.println(constructorConsulta.toString());
        System.out.println("========== FIN CONSULTA REPORTE ==========");

        Query query = entityManager.createNativeQuery(constructorConsulta.toString());
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
            String condicionContenido
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
                request.getCampaignName(),
                condicionFechas,
                condicionContenido
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
            String condicionContenido
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
                request.getCampaignName(),
                condicionFechas,
                condicionContenido
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
            List<String> documentosPromesasCaidas,
            boolean hayConsultaPrevia,
            String condicionFechas,
            String condicionContenido
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
        String condicionDocumentos = construirCondicionDocumentosPromesas(documentosPromesasCaidas);
        String condicionPagadasHoy = construirCondicionPagadasHoy(request.getExcluirPagadasHoy());

        String subconsulta = construirSubconsultaBase(
                condicionesRango,
                TIPO_PROMESA_ROTA,
                columnaFiltro,
                condicionesTipoContacto,
                condicionDocumentos + condicionPagadasHoy,
                request.getCampaignName(),
                condicionFechas,
                condicionContenido
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
            String condicionContenido
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
                request.getCampaignName(),
                condicionFechas,
                condicionContenido
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
            String rangoMoraProyectado,
            String condicionFechas,
            String condicionContenido
    ) {
        StringBuilder subconsulta = new StringBuilder();
        String condicionRangoMora = construirCondicionRangoMora(rangoMoraProyectado);

        subconsulta.append("SELECT *, '").append(tipoRango).append("' AS RANGO_TIPO FROM (")
                .append("SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                .append(condicionesRango)
                .append(" FROM TEMP_MERGE a ")
                .append("WHERE DOCUMENTO NOT IN (")
                .append("SELECT DOCUMENTO FROM blacklist ")
                .append("WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN")
                .append(") ");

        // Agregar condición de rango mora si existe
        if (!condicionRangoMora.isEmpty()) {
            subconsulta.append(" AND ").append(condicionRangoMora);
        }

        // Agregar condición de fechas si existe
        if (!condicionFechas.isEmpty()) {
            subconsulta.append(condicionFechas);
        }

        if (!condicionContenido.isEmpty()) {
            subconsulta.append(" ").append(condicionContenido);
        }

        subconsulta.append(" ORDER BY SLDCAPCONS DESC) b ")
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
            String rangoMoraProyectado,
            String condicionFechas,
            String condicionContenido
    ) {
        StringBuilder subconsulta = new StringBuilder();
        String condicionRangoMora = construirCondicionRangoMora(rangoMoraProyectado);

        subconsulta.append("SELECT *, '").append(TIPO_NO_CONTACTADO).append("' AS RANGO_TIPO FROM (")
                .append("SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ").append(condicionesRango)
                .append(" FROM TEMP_MERGE a ")
                .append("WHERE DOCUMENTO NOT IN (")
                .append("SELECT DOCUMENTO FROM blacklist ")
                .append("WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN")
                .append(") ");

        // Agregar condición de rango mora si existe
        if (!condicionRangoMora.isEmpty()) {
            subconsulta.append(" AND ").append(condicionRangoMora);
        }

        // Agregar condición de fechas si existe
        if (!condicionFechas.isEmpty()) {
            subconsulta.append(condicionFechas);
        }

        subconsulta.append(" ORDER BY SLDCAPCONS DESC) b ")
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
    private String construirCondicionDocumentosPromesas(List<String> documentosPromesasCaidas) {
        if (!tieneElementos(documentosPromesasCaidas)) {
            return "AND documento IN ('')";
        }
        String listaDocumentos = construirListaDocumentosPromesas(documentosPromesasCaidas);
        return "AND documento IN (" + listaDocumentos + ")";
    }

    /**
     * Finaliza la construcción de la consulta con GROUP BY y ORDER BY
     */
    private void finalizarConsulta(StringBuilder constructorConsulta) {
        constructorConsulta.append(") E GROUP BY RANGO, RANGO_TIPO ")
                .append("ORDER BY FIELD('RANGO_TIPO', '")
                .append(TIPO_CONTACTO_DIRECTO).append("', '")
                .append(TIPO_CONTACTO_INDIRECTO).append("', '")
                .append(TIPO_PROMESA_ROTA).append("', '")
                .append(TIPO_NO_CONTACTADO).append("')");
    }

    /**
     * Construye una lista de documentos formateada para SQL IN clause
     */
    private String construirListaDocumentosPromesas(List<String> documentosPromesasCaidas) {
        if (!tieneElementos(documentosPromesasCaidas)) {
            return "";
        }
        return documentosPromesasCaidas.stream()
                .map(documento -> "'" + documento + "'")
                .collect(Collectors.joining(", "));
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
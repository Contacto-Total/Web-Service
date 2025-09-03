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
        List<String> subconsultas = construirSubconsultas(request, documentosPromesasCaidas);
        String consultaFinal = construirConsultaPrincipal(subconsultas);
        System.out.println("Consulta Final: " + consultaFinal);
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

        // Subconsulta para contacto directo
        if (tieneElementos(request.getDirectContactRanges())) {
            String subconsulta = construirSubconsulta(
                    1, // bloque
                    request.getDirectContactRanges(),
                    CONTACTO_DIRECTO,
                    SALDO_ACTUAL_CONSUMO,
                    "TIPI IN ('CONTACTO CON TITULAR O ENCARGADO')",
                    rangoMoraProyectado,
                    condicionFechas
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para contacto indirecto
        if (tieneElementos(request.getIndirectContactRanges())) {
            String subconsulta = construirSubconsulta(
                    2, // bloque
                    request.getIndirectContactRanges(),
                    CONTACTO_INDIRECTO,
                    SALDO_ACTUAL_CONSUMO,
                    "TIPI IN ('CONTACTO CON TERCEROS')",
                    rangoMoraProyectado,
                    condicionFechas
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para promesas rotas
        if (tieneElementos(request.getBrokenPromisesRanges())) {
            String condicionPromesas = construirCondicionPromesasRotas(documentosPromesasCaidas);
            String condicionesExtra = construirCondicionesPromesasRotas(condicionPromesas);
            String subconsulta = construirSubconsulta(
                    3, // bloque
                    request.getBrokenPromisesRanges(),
                    PROMESA_ROTA,
                    SALDO_CAPITAL_ASIGNADO,
                    condicionesExtra,
                    rangoMoraProyectado,
                    condicionFechas
            );
            subconsultas.add(subconsulta);
        }

        // Subconsulta para no contactados
        if (tieneElementos(request.getNotContactedRanges())) {
            String condicionesNoContactado =
                    "TIPI IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', " +
                            "'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE') OR TIPI IS NULL";
            String subconsulta = construirSubconsulta(
                    4, // bloque
                    request.getNotContactedRanges(),
                    NO_CONTACTADO,
                    SALDO_CAPITAL_ASIGNADO,
                    condicionesNoContactado,
                    rangoMoraProyectado,
                    condicionFechas
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
            String condicionFechas
    ) {
        String condicionesRango = RangoConditionBuilder.buildRangoConditions(
                rangos, tipoRango, columnaMontos);
        String condicionRangoMora = construirCondicionRangoMora(rangoMoraProyectado);

        return """
            SELECT %d AS BLOQUE, b.*
              FROM (
                   SELECT BUSCAR_MAYOR_TIP(documento) AS TIPI,
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
    private String construirCondicionesPromesasRotas(String condicionDocumentos) {
        String tiposPromesa = "TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', " +
                "'RECORDATORIO DE PAGO', 'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', " +
                "'CANCELACION TOTAL', 'CANCELACION NO REPORTADAS O APLICADAS')";
        String condicionFinal = tiposPromesa;
        if (condicionDocumentos.isEmpty()) {
            condicionFinal += " AND documento IN ('')";
        } else {
            condicionFinal += " AND documento IN (" + condicionDocumentos + ")";
        }
        return condicionFinal;
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

    /**
     * Verifica si una lista tiene elementos
     */
    private boolean tieneElementos(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
package com.foh.contacto_total_web_service.reporte.repository;

import com.foh.contacto_total_web_service.rango.dto.GetRangosByRangesAndGenerateFileRequest;
import com.foh.contacto_total_web_service.rango.util.RangoConditionBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ReporteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getReporteByRangos(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest, List<String> promesasCaidas) {
        String noContactadoRangoConditions = RangoConditionBuilder.buildRangoConditions(
                getRangosByRangesAndGenerateFileRequest.getNoContactadoRangos(),
                "RANGO NO CONTACTADO");

        String contactoIndirectoRangoConditions = RangoConditionBuilder.buildRangoConditions(
                getRangosByRangesAndGenerateFileRequest.getContactoIndirectoRangos(),
                "RANGO CONTACTO INDIRECTO");

        String contactoDirectoRangoConditions = RangoConditionBuilder.buildRangoConditions(
                getRangosByRangesAndGenerateFileRequest.getContactoDirectoRangos(),
                "RANGO CONTACTO DIRECTO");

        String promesasRotasRangoConditions = RangoConditionBuilder.buildRangoConditions(
                getRangosByRangesAndGenerateFileRequest.getPromesasRotasRangos(),
                "RANGO PROMESA ROTA");

        StringBuilder queryStr = new StringBuilder();
        queryStr.append("SELECT RANGO, COUNT(1) FROM (");

        boolean hasPreviousQuery = false;

        if (getRangosByRangesAndGenerateFileRequest.getContactoDirectoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getContactoDirectoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT *, 'CONTACTO DIRECTO' AS RANGO_TIPO FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(contactoDirectoRangoConditions)
                    .append(" FROM TEMP_MERGE a WHERE DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN) ORDER BY SLDCAPCONS DESC) b ")
                    .append(" WHERE CAST(SLDACTUALCONS AS DECIMAL(10, 2)) > 0 ")
                    .append(" AND TIPI IN ('CONTACTO CON TITULAR O ENCARGADO') ")
                    .append(" AND b.rango IS NOT NULL");
            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getContactoIndirectoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getContactoIndirectoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT *, 'CONTACTO INDIRECTO' AS RANGO_TIPO FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(contactoIndirectoRangoConditions)
                    .append(" FROM TEMP_MERGE a WHERE DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN) ORDER BY SLDCAPCONS DESC) b ")
                    .append(" WHERE CAST(SLDACTUALCONS AS DECIMAL(10, 2)) > 0 ")
                    .append(" AND TIPI IN ('CONTACTO CON TERCEROS') ")
                    .append(" AND b.rango IS NOT NULL");
            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getPromesasRotasRangos() != null && !getRangosByRangesAndGenerateFileRequest.getPromesasRotasRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT *, 'PROMESA ROTA' AS RANGO_TIPO FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(promesasRotasRangoConditions)
                    .append(" FROM TEMP_MERGE a WHERE DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN) ORDER BY SLDCAPCONS DESC) b ")
                    .append(" WHERE CAST(SLDCAPITALASIG AS DECIMAL(10, 2)) > 0 ")
                    .append(" AND b.rango IS NOT NULL")
                    .append(" AND TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', 'RECORDATORIO DE PAGO', 'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', 'CANCELACION TOTAL', 'CANCELACION NO REPORTADAS O APLICADAS') ");

            if (promesasCaidas != null && !promesasCaidas.isEmpty()) {
                queryStr.append(" AND documento IN (").append(buildPromesasCaidasList(promesasCaidas)).append(")");
            } else {
                queryStr.append(" AND documento IN ('')");
            }

            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getNoContactadoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getNoContactadoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT *, 'NO CONTACTADO' AS RANGO_TIPO FROM (")
                    .append("SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(noContactadoRangoConditions)
                    .append(" FROM TEMP_MERGE a WHERE DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN) ORDER BY SLDCAPCONS DESC) b ")
                    .append(" WHERE b.rango IS NOT NULL ")
                    .append(" AND CAST(SLDACTUALCONS AS DECIMAL(10, 2)) > 0 AND (COALESCE(TIPI, 'NO CONTESTA') IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', 'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE'))");
            hasPreviousQuery = true;
        }

        queryStr.append(") E GROUP BY RANGO, RANGO_TIPO ORDER BY FIELD('RANGO_TIPO', 'CONTACTO DIRECTO', 'CONTACTO INDIRECTO', 'PROMESA ROTA', 'NO CONTACTADO')");

        Query query = entityManager.createNativeQuery(queryStr.toString());
        return query.getResultList();
    }

    private String buildPromesasCaidasList(List<String> promesasCaidas) {
        if (promesasCaidas == null || promesasCaidas.isEmpty()) {
            return "";
        }
        return promesasCaidas.stream()
                .map(doc -> "'" + doc + "'")
                .collect(Collectors.joining(", "));
    }
}
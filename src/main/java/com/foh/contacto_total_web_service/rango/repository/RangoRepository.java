package com.foh.contacto_total_web_service.rango.repository;

import com.foh.contacto_total_web_service.rango.dto.GetRangosByRangesAndGenerateFileRequest;
import com.foh.contacto_total_web_service.rango.util.RangoConditionBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RangoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> findByRangosAndTipoContacto(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest, List<String> promesasCaidas) {
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
        queryStr.append("SELECT DOCUMENTO, COALESCE(TELEFONOCELULAR, telefonodomicilio, telefonolaboral, telfreferencia1, telfreferencia2), TIPI FROM (");

        boolean hasPreviousQuery = false;

        if (getRangosByRangesAndGenerateFileRequest.getContactoDirectoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getContactoDirectoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT 1 AS BLOQUE, b.* FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(contactoDirectoRangoConditions)
                    .append(" FROM TEMP_MERGE a) b ")
                    .append(" WHERE CAST(SLDACTUALCONS AS DECIMAL(10, 2))  > 0 ")
                    .append(" AND TIPI IN ('CONTACTO CON TITULAR O ENCARGADO') ")
                    .append(" AND b.rango IS NOT NULL ");

            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getContactoIndirectoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getContactoIndirectoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT 2 AS BLOQUE, b.* FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(contactoIndirectoRangoConditions)
                    .append(" FROM TEMP_MERGE a) b ")
                    .append(" WHERE CAST(SLDACTUALCONS AS DECIMAL(10, 2))  > 0 ")
                    .append(" AND TIPI IN ('CONTACTO CON TERCEROS') ")
                    .append(" AND b.rango IS NOT NULL ");

            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getPromesasRotasRangos() != null && !getRangosByRangesAndGenerateFileRequest.getPromesasRotasRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT 3 AS BLOQUE, b.* FROM (SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(promesasRotasRangoConditions)
                    .append(" FROM TEMP_MERGE a) b ")
                    .append(" WHERE CAST(SLDCAPITALASIG AS DECIMAL(10, 2)) > 0 ")
                    .append(" AND b.rango IS NOT NULL ")
                    .append(" AND TIPI IN ('PROMESA DE PAGO', 'OPORTUNIDAD DE PAGO', 'RECORDATORIO DE PAGO', 'CONFIRMACION DE ABONO', 'CANCELACION PARCIAL', 'CANCELACION TOTAL', 'CANCELACION NO REPORTADAS O APLICADAS') ");

            if (promesasCaidas != null && !promesasCaidas.isEmpty()) {
                queryStr.append(" AND documento IN (").append(buildPromesasCaidasList(promesasCaidas)).append(")");
            } else {
                queryStr.append(" AND documento IN ('') ");
            }

            hasPreviousQuery = true;
        }

        if (getRangosByRangesAndGenerateFileRequest.getNoContactadoRangos() != null && !getRangosByRangesAndGenerateFileRequest.getNoContactadoRangos().isEmpty()) {
            if (hasPreviousQuery) {
                queryStr.append(" UNION ALL ");
            }

            queryStr.append("SELECT 4 AS BLOQUE, b.* FROM ( ")
                    .append("SELECT BUSCAR_MAYOR_TIP(documento) TIPI, a.*, ")
                    .append(noContactadoRangoConditions)
                    .append(" FROM TEMP_MERGE a) b ")
                    .append("WHERE b.RANGO IS NOT NULL ")
                    .append("AND CAST(SLDACTUALCONS AS DECIMAL(10, 2)) > 0 ")
                    .append("AND TIPI IN ('MSJ VOZ - SMS - WSP - BAJO PUERTA', 'NO CONTESTA', 'APAGADO', 'EQUIVOCADO', 'FUERA DE SERVICIO - NO EXISTE') OR TIPI IS NULL");

            hasPreviousQuery = true;
        }


        queryStr.append(") B WHERE DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN) ")
                .append("AND TELEFONOCELULAR NOT IN (SELECT DISTINCT Telefono FROM GESTION_HISTORICA_BI WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO')) ")
                .append("AND TELEFONOCELULAR != '' ")
                .append("ORDER BY BLOQUE, SLDCAPCONS DESC;");

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

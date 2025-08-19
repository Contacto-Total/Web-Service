package com.foh.contacto_total_web_service.sms.repository;

import com.foh.contacto_total_web_service.sms.dto.PeopleForSMSResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Repository
public class SMSRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<PeopleForSMSResponse> getPeopleForSMS(List<String> tipiList, List<String> promesas) {
        StringJoiner joiner = new StringJoiner("','", "'", "'");

        for (String tipi : tipiList) {
            joiner.add(tipi);
        }

        StringBuilder sql = new StringBuilder("SELECT " +
                "a.DOCUMENTO AS documento, " +
                "COALESCE(NULLIF(TELEFONOCELULAR,''), NULLIF(TELEFONODOMICILIO,''), NULLIF(TELEFONOLABORAL,''), NULLIF(TELFREFERENCIA1,''), NULLIF(TELFREFERENCIA2,'')) AS telefonoCelular, " +
                "NOMBRE AS nombre, " +
                "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                "CASE " +
                "WHEN LTDESPECIAL > 0 THEN CAST(CEIL(LTDESPECIAL) AS UNSIGNED) " +
                "ELSE CAST(`5` AS UNSIGNED) " +
                "END AS ltd " +
                "FROM TEMP_MERGE a " +
                "JOIN TIPI_RANGE b ON a.DOCUMENTO = b.DOCUMENTO " +
                "WHERE SLDACTUALCONS > 0 " +
                "AND (SLDACTUALCONS > LTDESPECIAL AND SLDACTUALCONS > `5`) " +
                "AND b.TIPI IN (" + joiner.toString() + ") ");

        if (!promesas.isEmpty()) {
            sql.append("AND a.DOCUMENTO NOT IN (").append(buildPromesasList(promesas)).append(") ");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), PeopleForSMSResponse.class);
        return query.getResultList();
    }

    private String buildPromesasList(List<String> promesas) {
        return  promesas.stream()
                .map(doc -> "'" + doc + "'")
                .collect(Collectors.joining(", "));
    }

    public List<PeopleForSMSResponse> getPeopleForSMS2(String templateName) {
        StringBuilder sql = new StringBuilder();

        switch (templateName) {
            case "LOS QUE NO TIENEN LTD NI LTDESPECIAL":
                sql.append("SELECT * FROM ( " +
                        "SELECT " +
                        "DOCUMENTO AS documento, " +
                        "TELEFONOCELULAR AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(`5` AS UNSIGNED) AS ltd " +
                        "FROM TEMP_MERGE " +
                        "WHERE SLDACTUALCONS > 0 " +
                        "AND LTDESPECIAL = '' " +
                        "AND `5` = '' " +
                        "AND DOCUMENTO NOT IN ( " +
                        "SELECT DOCUMENTO " +
                        "FROM COMPROMISOS " +
                        "WHERE ESTADO_COMPROMISO IN ('VIGENTE', 'PAGADO') " +
                        "UNION ALL " +
                        "SELECT DOCUMENTO " +
                        "FROM blacklist " +
                        "WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN " +
                        ") " +
                        "AND IDENTITY_CODE NOT IN ( " +
                        "SELECT IDENTITY_CODE " +
                        "FROM LTD_ART " +
                        "WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m') " +
                        "UNION ALL " +
                        "SELECT IDENTITY_CODE " +
                        "FROM LTDE_ART " +
                        "WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m') " +
                        ") " +
                        ") TAB1 " +
                        "WHERE TELEFONOCELULAR NOT IN ( " +
                        "SELECT DISTINCT Telefono FROM GESTION_HISTORICA_BI " +
                        "WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO') " +
                        ") " +
                        "AND TELEFONOCELULAR != ''; ");
                break;

            case "SOLO LTD ESPECIAL":
                sql.append("SELECT DOCUMENTO AS documento, " +
                        "TELEFONOCELULAR AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(CEIL(LTDESPECIAL) AS UNSIGNED) AS ltd " +
                        "FROM TEMP_MERGE " +
                        "WHERE LTDESPECIAL != '' " +
                        "AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS WHERE ESTADO_COMPROMISO IN ('VIGENTE', 'PAGADO') UNION ALL SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)");
                break;

            case "LOS QUE TIENEN LTD O LTD ESPECIAL":
                sql.append("SELECT * FROM ( " +
                        "SELECT " +
                        "DOCUMENTO AS documento, " +
                        "TELEFONOCELULAR AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CASE " +
                        "WHEN COALESCE(LTDE.LTDE_ART, LTDESPECIAL) > 0 THEN CAST(CEIL(COALESCE(LTDE.LTDE_ART, LTDESPECIAL)) AS UNSIGNED) + 10 " +
                        "WHEN CAST(CEIL(COALESCE(LTD.LTD_ART, `5`)) AS UNSIGNED) = CEIL(SLDACTUALCONS) THEN CAST(CEIL(SLDCAPCONS) AS UNSIGNED) " +
                        "ELSE CAST(CEIL(COALESCE(LTD.LTD_ART, `5`)) AS UNSIGNED) + 10 " +
                        "END AS ltd " +
                        "FROM TEMP_MERGE TM " +
                        "LEFT JOIN LTDE_ART LTDE ON TM.IDENTITY_CODE = LTDE.IDENTITY_CODE " +
                        "AND LTDE.PERIODO = DATE_FORMAT(CURDATE(), '%Y%m') " +
                        "LEFT JOIN LTD_ART LTD ON TM.IDENTITY_CODE = LTD.IDENTITY_CODE " +
                        "AND LTD.PERIODO = DATE_FORMAT(CURDATE(), '%Y%m') " +
                        "WHERE SLDACTUALCONS > 0 " +
                        "AND (COALESCE(LTDE.LTDE_ART, LTDESPECIAL) > 0 OR COALESCE(LTD.LTD_ART, `5`) > 0) " +
                        "AND DOCUMENTO NOT IN ( " +
                        "SELECT DOCUMENTO FROM COMPROMISOS WHERE ESTADO_COMPROMISO IN ('VIGENTE', 'PAGADO') " +
                        "UNION ALL " +
                        "SELECT DISTINCT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN " +
                        ") " +
                        ") TAB2 " +
                        "WHERE TELEFONOCELULAR NOT IN ( " +
                        "SELECT DISTINCT Telefono FROM GESTION_HISTORICA_BI " +
                        "WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO') " +
                        ") " +
                        "AND TELEFONOCELULAR != ''; ");
                break;


            case "PROMESAS DE MAÑANA":
                sql.append("SELECT tm.DOCUMENTO AS documento, " +
                        "tm.TELEFONOCELULAR AS telefonoCelular, " +
                        "tm.NOMBRE AS nombre, " +
                        "CAST(CEIL(tm.SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(CEIL(c.IMPORTE_COMPROMISO) AS UNSIGNED) AS ltd " +
                        "FROM COMPROMISOS c " +
                        "INNER JOIN TEMP_MERGE tm ON c.DOCUMENTO = tm.DOCUMENTO " +
                        "WHERE c.FECHA_COMPROMISO = DATE_FORMAT(SUBDATE(CURDATE(), INTERVAL -1 DAY), '%Y-%m-%d') " +
                        "AND c.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)");
                break;

            case "PROMESAS DE HOY":
                sql.append("SELECT tm.DOCUMENTO AS documento, " +
                        "tm.TELEFONOCELULAR AS telefonoCelular, " +
                        "tm.NOMBRE AS nombre, " +
                        "CAST(CEIL(tm.SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(CEIL(c.IMPORTE_COMPROMISO) AS UNSIGNED) AS ltd " +
                        "FROM COMPROMISOS c " +
                        "INNER JOIN TEMP_MERGE tm ON c.DOCUMENTO = tm.DOCUMENTO " +
                        "WHERE c.FECHA_COMPROMISO = DATE_FORMAT(CURDATE(), '%Y-%m-%d') " +
                        "AND c.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)");
                break;

            case "PROMESAS ROTAS":
                sql.append("SELECT tm.DOCUMENTO AS documento, " +
                        "tm.TELEFONOCELULAR AS telefonoCelular, " +
                        "tm.NOMBRE AS nombre, " +
                        "CAST(CEIL(tm.SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(CEIL(c.IMPORTE_COMPROMISO) AS UNSIGNED) AS ltd " +
                        "FROM COMPROMISOS c " +
                        "INNER JOIN TEMP_MERGE tm ON c.DOCUMENTO = tm.DOCUMENTO " +
                        "WHERE c.FECHA_COMPROMISO < DATE_FORMAT(SUBDATE(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d') " +
                        "AND c.ESTADO_COMPROMISO = 'CAIDO' " +
                        "AND c.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)");
                break;

            case "INICIO MES":
                sql.append("SELECT DOCUMENTO AS documento, " +
                        "TELEFONOCELULAR AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CAST(`5` AS UNSIGNED) AS ltd " +
                        "FROM TEMP_MERGE " +
                        "WHERE SLDACTUALCONS > 0 "+
                        "AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS UNION ALL SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)");
                break;

            case "SIN MONTO":
                sql.append("SELECT DOCUMENTO AS documento, " +
                        "TELEFONOCELULAR AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CASE " +
                        "WHEN LTDESPECIAL > 0 OR NULL THEN CAST(CEIL(LTDESPECIAL) AS UNSIGNED) " +
                        "WHEN CAST(CEIL(`5`) AS UNSIGNED) = CEIL(SLDACTUALCONS) THEN CAST(CEIL(SLDCAPCONS) AS UNSIGNED) " +
                        "ELSE CAST(CEIL(`5`) AS UNSIGNED) " +
                        "END AS ltd " +
                        "FROM TEMP_MERGE " +
                        "WHERE SLDACTUALCONS > 0 " +
                        "AND (LTDESPECIAL > 0 OR 5 > 0) " +
                        "AND DIASMORAASIG >= 180 " +
                        "AND DOCUMENTO NOT IN ( " +
                        "SELECT DISTINCT DOCUMENTO " +
                        "FROM COMPROMISOS " +
                        "WHERE FECHA_COMPROMISO >= SUBDATE(CURDATE(), INTERVAL 2 DAY) " +
                        "OR ESTADO_COMPROMISO IN ('PAGADO') " +
                        "UNION ALL SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN);");
                break;

            case "LTD O LTDE MEDIO MES":
                sql.append("SELECT " +
                        "a.DOCUMENTO AS documento, " +
                        "COALESCE(NULLIF(TELEFONOCELULAR, ''), NULLIF(TELEFONODOMICILIO, ''), " +
                        "NULLIF(TELEFONOLABORAL, ''), NULLIF(TELFREFERENCIA1, ''), NULLIF(TELFREFERENCIA2, '')) AS telefonoCelular, " +
                        "NOMBRE AS nombre, " +
                        "CAST(CEIL(SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CASE " +
                        "WHEN LTDESPECIAL > 0 THEN CAST(CEIL(SLDCAPCONS) AS UNSIGNED) " +
                        "WHEN CEIL(`5`) > 0 AND (CEIL(`5`) + 200) > SLDACTUALCONS THEN CAST(CEIL(`5`) AS UNSIGNED) " +
                        "WHEN CEIL(`5`) > 0 AND (CEIL(`5`) + 200) <= SLDACTUALCONS THEN CAST((CEIL(`5`) + 200) AS UNSIGNED) " +
                        "ELSE CAST(CEIL(SLDCAPCONS) AS UNSIGNED) " +
                        "END AS ltd " +
                        "FROM TEMP_MERGE a " +
                        "JOIN TIPI_RANGE b ON a.DOCUMENTO = b.DOCUMENTO " +
                        "WHERE SLDACTUALCONS > 0 " +
                        "AND SLDCAPCONS > 0 " +
                        "AND (LTDESPECIAL > 0 OR `5` > 0) " +
                        "AND a.DOCUMENTO NOT IN ( " +
                        "SELECT DOCUMENTO FROM COMPROMISOS UNION ALL SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN" +
                        ");");
                break;

            case "RECORDATORIO DE PROMESA":
                sql.append("SELECT " +
                        "PH.Documento AS documento, " +
                        "TM.TELEFONOCELULAR AS telefonoCelular, " +
                        "PH.Cliente AS nombre, " +
                        "CAST(CEIL(TM.SLDACTUALCONS) AS UNSIGNED) AS deudaTotal, " +
                        "CASE " +
                        "WHEN PH.ImporteCompromiso > 0 THEN CAST(CEIL(PH.ImporteCompromiso - PH.ImportePagadoAntesDeFecha) AS UNSIGNED) " +
                        "WHEN PH.ImporteOportunidad > 0 THEN CAST(CEIL(PH.ImporteOportunidad - PH.ImportePagadoAntesDeFecha) AS UNSIGNED) " +
                        "END AS ltd " +
                        "FROM PROMESAS_HISTORICO PH " +
                        "INNER JOIN TEMP_MERGE TM ON PH.Documento = TM.DOCUMENTO " +
                        "WHERE (PH.FechaCompromiso = DATE_FORMAT(CURDATE(), '%Y-%m-%d') " +
                        "OR PH.FechaOportunidad = DATE_FORMAT(CURDATE(), '%Y-%m-%d')) " +
                        "AND PH.Estado = 'Vigente' " +
                        "AND PH.Documento NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN); ");
                break;

            case "COLCHON PARA HOY":
                sql.append("SELECT " +
                        "CAST(documento AS CHAR(300)) documento, " +
                        "CAST(telefonoCelular AS CHAR(300)) telefonoCelular, " +
                        "CAST(nombre AS CHAR(300)) nombre, " +
                        "CAST(deudaTotal AS SIGNED) deudaTotal, " +
                        "CAST(ltd AS SIGNED) ltd " +
                        "FROM (" +
                        "SELECT documento, " +
                        "(SELECT TELEFONOCELULAR FROM TEMP_MERGE_BI WHERE PERIODO = CAST(DATE_FORMAT(CURDATE(), '%Y%m') AS CHAR) " +
                        "AND TEMP_MERGE_BI.DOCUMENTO = TAB1.Documento) telefonoCelular, " +
                        "CONCAT(UPPER(SUBSTRING(SUBSTRING_INDEX(Cliente, ' ', 1), 1, 1)), " +
                        "LOWER(SUBSTRING(SUBSTRING_INDEX(Cliente, ' ', 1), 2))) nombre, " +
                        "(SELECT SLDACTUALCONS FROM TEMP_MERGE_BI WHERE PERIODO = CAST(DATE_FORMAT(CURDATE(), '%Y%m') AS CHAR) " +
                        "AND TEMP_MERGE_BI.DOCUMENTO = TAB1.Documento) deudaTotal, " +
                        "ImporteCompromiso + ImporteOportunidad AS ltd " +
                        "FROM (" +
                        "SELECT * FROM PROMESAS_HISTORICO WHERE " +
                        "(LOWER(Observacion) LIKE '%(convenio)%' OR LOWER(Observacion) LIKE '%(excepcion)%') " +
                        "AND Estado = 'Vigente' " +
                        "AND (FechaCompromiso = DATE_FORMAT(curdate(),'%Y-%m-%d') OR FechaOportunidad = DATE_FORMAT(curdate(),'%Y-%m-%d')) " +
                        "AND Documento NOT IN (SELECT DOCUMENTO FROM blacklist WHERE DATE_FORMAT(CURDATE(), '%Y-%m-%d') BETWEEN FECHA_INICIO AND FECHA_FIN)) TAB1 " +
                        ") TAB2 " +
                        "WHERE telefonoCelular NOT IN (SELECT DISTINCT Telefono FROM GESTION_HISTORICA_BI WHERE Resultado IN ('FUERA DE SERVICIO - NO EXISTE', 'EQUIVOCADO', 'FALLECIDO'));");
                break;    

            default:
                throw new IllegalArgumentException("Template name not recognized: " + templateName);
        }

        Query query = entityManager.createNativeQuery(sql.toString(), PeopleForSMSResponse.class);
        return query.getResultList();
    }
}

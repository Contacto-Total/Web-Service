package com.foh.contacto_total_web_service.sms.repository;

import com.foh.contacto_total_web_service.sms.dto.PeopleForSMSResponse;
import com.foh.contacto_total_web_service.sms.dto.DynamicQueryRequest;
import com.foh.contacto_total_web_service.sms.dto.PeopleForCustomSMSResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
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

    // NUEVA FUNCIONALIDAD

    public List<PeopleForCustomSMSResponse> getPeopleForCustomSMS(boolean onlyLtde, String periodo) {

        String sql;

        if (onlyLtde) {
            sql = """
            SELECT
                IDENTITY_CODE,
                TELEFONOCELULAR,
                CONCAT(
                    UPPER(LEFT(LOWER(NOMBRE), 1)),
                    SUBSTRING(LOWER(NOMBRE), 2)
                ) AS NOMBRE,

                CASE
                    WHEN LTDESPECIAL IS NOT NULL AND LTDESPECIAL > 0 THEN
                        CASE
                            WHEN (CEIL(LTDESPECIAL) + 200) < CEIL(SLDACTUALCONS) 
                                THEN CEIL(LTDESPECIAL) + 200
                            ELSE CEIL(LTDESPECIAL)
                        END
                END AS LTDE_FINAL,
                CEIL(SLDACTUALCONS) AS DEUDA_TOTAL,
                '915168552'
            FROM TEMP_MERGE
            WHERE ((LTDESPECIAL <> '' AND LTDESPECIAL > 0))
              AND SLDCAPITALASIG > 0
              AND TRIM(TELEFONOCELULAR) <> ''
              AND TELEFONOCELULAR IS NOT NULL
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = :periodo)
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS)
              AND RANGOMORAPROYAG = 'Tramo 5'
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist)
              AND SLDACTUALCONS > 0
              AND SLDACTUALCONS > COALESCE(LTDESPECIAL, 0)
            """;
        } else {
            sql = """
            SELECT
                IDENTITY_CODE,
                TELEFONOCELULAR,
                CONCAT(
                    UPPER(LEFT(LOWER(NOMBRE), 1)),
                    SUBSTRING(LOWER(NOMBRE), 2)
                ) AS NOMBRE,

                CASE
                    WHEN LTDESPECIAL IS NOT NULL AND LTDESPECIAL > 0 THEN
                        CASE
                            WHEN (CEIL(LTDESPECIAL) + 200) < CEIL(SLDACTUALCONS) 
                                THEN CEIL(LTDESPECIAL) + 200
                            ELSE CEIL(LTDESPECIAL)
                        END
                    ELSE
                        CASE
                            WHEN (CEIL(`5`) + 200) < CEIL(SLDACTUALCONS) 
                                THEN CEIL(`5`) + 200
                            ELSE CEIL(`5`)
                        END
                END AS LTD_FINAL,
                CEIL(SLDACTUALCONS) AS DEUDA_TOTAL,
                '915168552'
            FROM TEMP_MERGE
            WHERE ((LTDESPECIAL <> '' AND LTDESPECIAL > 0) or (`5` <> '' and `5` > 0))
              AND SLDCAPITALASIG > 0
              AND TRIM(TELEFONOCELULAR) <> ''
              AND TELEFONOCELULAR IS NOT NULL
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = :periodo)
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS)
              AND RANGOMORAPROYAG = 'Tramo 5'
              AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist)
              AND SLDACTUALCONS > 0
              AND SLDACTUALCONS > COALESCE(LTDESPECIAL, 0)
              AND SLDACTUALCONS > COALESCE(`5`, 0)
            """;
        }

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("periodo", periodo);

        List<Object[]> results = query.getResultList();

        List<PeopleForCustomSMSResponse> responseList = new ArrayList<>();

        for (Object[] row : results) {
            String documento = (String) row[0];
            String telefonoCelular = (String) row[1];
            String nombre = (String) row[2];
            BigDecimal ltdeFinal = row[3] != null ? new BigDecimal(row[3].toString()) : null;
            BigDecimal deudaTotal = row[4] != null ? new BigDecimal(row[4].toString()) : null;
            String remitente = (String) row[5];

            responseList.add(new PeopleForCustomSMSResponse(documento, telefonoCelular, nombre, ltdeFinal, deudaTotal, remitente));
        }

        return responseList;

    }


    // acutalizado



    // ===== helper para +monto =====
    private String capPlus(String baseExpr, int amount) {
        // CASE WHEN (base + amount) < deuda_total THEN base+amount ELSE base END
        return "CASE WHEN (" + baseExpr + " + " + amount + ") < CEIL(SLDACTUALCONS) " +
                "THEN (" + baseExpr + " + " + amount + ") ELSE " + baseExpr + " END";
    }


    public List<Map<String,Object>> runDynamicQuery(DynamicQueryRequest req, Integer limit) {
        // 1) map de variables "normales"
        Map<String,String> col = new HashMap<>();
        col.put("nombre",
                "CONCAT(" +
                        "  UPPER(LEFT(LOWER(TRIM(SUBSTRING_INDEX(NOMBRE, ' ', 1))), 1))," +
                        "  SUBSTRING(LOWER(TRIM(SUBSTRING_INDEX(NOMBRE, ' ', 1))), 2)" +
                        ")"
        );
        col.put("capital", "CEIL(SLDCAPCONS)");
        col.put("baja30",     "CEIL(`2`)");
        col.put("saldomora",  "CEIL(SLDMORA)");
        col.put("deudatotal", "CEIL(SLDACTUALCONS)");
        col.put("documento",  "CAST(DOCUMENTO AS CHAR)");
        col.put("celular",    "CAST(TELEFONOCELULAR AS CHAR)");

        List<String> outKeys = new ArrayList<>();

        // 2) SELECT
        StringBuilder sql = new StringBuilder("SELECT CAST(TELEFONOCELULAR AS CHAR) AS CELULAR");
        outKeys.add("celular");
        List<String> vars = (req.getVariables() == null) ? List.of() : req.getVariables();


        // 1) ¿Qué monto sumar?
        int addAmt = 0;
        if (req.getAddAmount() != null) {
            addAmt = Math.max(0, req.getAddAmount());         // clamp: no negativos
        } else if (Boolean.TRUE.equals(req.getAdd200())) {
            addAmt = 200; // compat con el flag antiguo
        }

        // 2) ¿El usuario pidió LTD/LTDE?
        boolean wantsLTD  = vars.stream().anyMatch(v -> "ltd".equalsIgnoreCase(v));
        boolean wantsLTDE = vars.stream().anyMatch(v -> "ltde".equalsIgnoreCase(v));

        // 3) Solo sumamos si hay LTD/LTDE y addAmt > 0
        boolean sumEnabled = addAmt > 0 && (wantsLTD || wantsLTDE);

        // 4) Expresiones
        String ltdExpr  = sumEnabled ? capPlus("CEIL(`5`)", addAmt)           : "CEIL(`5`)";
        String ltdeExpr = sumEnabled ? capPlus("CEIL(LTDESPECIAL)", addAmt)   : "CEIL(LTDESPECIAL)";

        col.put("ltd",  ltdExpr);
        col.put("ltde", ltdeExpr);

        for (String vRaw : vars) {
            String v = vRaw.toLowerCase();
            String expr = col.get(v);
            if (expr != null) {
                sql.append(", ").append(expr).append(" AS ").append(aliasFromExpr(expr));
                outKeys.add(v);
            }
        }

        sql.append(" FROM TEMP_MERGE WHERE 1=1 ");
        sql.append(" AND TRIM(COALESCE(TELEFONOCELULAR,'')) <> '' ");
        sql.append(" AND TELEFONOCELULAR IS NOT NULL ");
        sql.append(" AND SLDCAPITALASIG > 0 ");

        // ===== tramo 3 (baja30 / saldomora) =====
        boolean wantsBaja30     = vars.stream().anyMatch(v -> "baja30".equalsIgnoreCase(v));
        boolean wantsSaldoMora  = vars.stream().anyMatch(v -> "saldomora".equalsIgnoreCase(v));
        boolean tramo3          = req.getTramos()!=null && req.getTramos().contains(3);

        if (wantsBaja30 && tramo3) {
            sql.append(" AND COALESCE(`2`,0) > 0 ");
            sql.append(" AND TRIM(COALESCE(`2`,'')) <> '' ");
            sql.append(" AND DOCUMENTO IN (SELECT CASE " +
                    " WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) " +
                    " WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) " +
                    " ELSE A.IDENTITY_CODE END " +
                    " FROM PAYS_TEMP A WHERE CONTENCION = 'NO CONTENIDO')");
        }
        if (wantsSaldoMora && tramo3) {
            sql.append(" AND COALESCE(`2`,0) > 0 ");
            sql.append(" AND TRIM(COALESCE(`2`,'')) <> '' ");
            sql.append(" AND DOCUMENTO IN (SELECT CASE " +
                    " WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) " +
                    " WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) " +
                    " ELSE A.IDENTITY_CODE END " +
                    " FROM PAYS_TEMP A WHERE CONTENCION = 'NO CONTENIDO')");
        }


        boolean applyAdd200 = (wantsLTD || wantsLTDE) && Boolean.TRUE.equals(req.getAdd200());

        boolean tramo5    = req.getTramos()!=null && req.getTramos().contains(5);

        if (wantsLTD || wantsLTDE || tramo5) {
            sql.append(" AND RANGOMORAPROYAG = 'Tramo 5' ");
            sql.append(" AND SLDACTUALCONS > 0 ");

            if (wantsLTD && wantsLTDE) {
                sql.append(" AND ( (LTDESPECIAL IS NOT NULL AND LTDESPECIAL > 0 AND SLDACTUALCONS > COALESCE(LTDESPECIAL,0)) " +
                        "OR (`5` IS NOT NULL AND `5` > 0 AND SLDACTUALCONS > COALESCE(`5`,0)) ) ");
            } else if (wantsLTDE) {
                sql.append(" AND LTDESPECIAL IS NOT NULL AND LTDESPECIAL > 0 ");
                sql.append(" AND SLDACTUALCONS > COALESCE(LTDESPECIAL,0) ");
            } else { // wantsLTD
                sql.append(" AND `5` IS NOT NULL AND `5` > 0 ");
                sql.append(" AND SLDACTUALCONS > COALESCE(`5`,0) ");
            }
        }

        // Exclusiones
        if (Boolean.TRUE.equals(req.getExcluirPromesas())) {
            sql.append(" AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m')) ");
        }
        if (Boolean.TRUE.equals(req.getExcluirCompromisos())) {
            sql.append(" AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS) ");
        }
        if (Boolean.TRUE.equals(req.getExcluirBlacklist())) {
            sql.append(" AND DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist) ");
        }

        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        System.out.println("===== SQL DINÁMICA GENERADA =====");
        System.out.println(sql.toString());
        System.out.println("=================================");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql.toString()).getResultList();

        // Map rows
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String,Object> m = new LinkedHashMap<>();
            for (int i = 0; i < outKeys.size(); i++) {
                String key = outKeys.get(i);
                m.put(key, r[i]);
            }
            out.add(m);
        }
        return out;
    }

    private String aliasFromExpr(String expr) {
        String e = expr.toUpperCase();
        if (e.contains("LTDESPECIAL")) return "LTDE";       // 1) primero LTDE
        if (e.contains("`5`") || e.matches(".*\\b5\\b.*")) return "LTD"; // 2) luego LTD (por si faltan backticks)
        if (e.contains("SLDACTUALCONS")) return "DEUDA_TOTAL";          // 3) después deuda
        if (e.contains("SLDMORA")) return "SALDO_MORA";
        if (e.contains("`2`") || e.matches(".*\\b2\\b.*")) return "BAJA30";
        if (e.contains("NOMBRE")) return "NOMBRE";
        if (e.contains("SLDCAPCONS")) return "CAPITAL";
        if (e.contains("DOCUMENTO")) return "DOCUMENTO";
        if (e.contains("TELEFONOCELULAR")) return "CELULAR";
        return "COL";
    }


    // QUERY DINAMICO







}

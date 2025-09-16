package com.foh.contacto_total_web_service.sms_template.service;

import com.foh.contacto_total_web_service.sms_template.dto.DynamicQueryRequest1;
import com.foh.contacto_total_web_service.sms_template.dto.Restricciones;
import com.foh.contacto_total_web_service.sms_template.dto.SmsPrecheckDTO;
import com.foh.contacto_total_web_service.sms_template.util.SmsTextUtil;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Service
public class DynamicQueryService {
    private final NamedParameterJdbcTemplate jdbc;
    public DynamicQueryService(NamedParameterJdbcTemplate jdbc){ this.jdbc = jdbc; }

    // arriba, junto a los static final
    private static final String PHONE_EXPR =
            "COALESCE(" +
                    "  CASE WHEN tm.TELEFONOCELULAR    REGEXP '^9[0-9]{8}$' THEN tm.TELEFONOCELULAR    END, " +
                    "  CASE WHEN tm.TELEFONODOMICILIO  REGEXP '^9[0-9]{8}$' THEN tm.TELEFONODOMICILIO  END, " +
                    "  CASE WHEN tm.TELEFONOLABORAL    REGEXP '^9[0-9]{8}$' THEN tm.TELEFONOLABORAL    END, " +
                    "  CASE WHEN tm.TELFREFERENCIA1    REGEXP '^9[0-9]{8}$' THEN tm.TELFREFERENCIA1    END, " +
                    "  CASE WHEN tm.TELFREFERENCIA2    REGEXP '^9[0-9]{8}$' THEN tm.TELFREFERENCIA2    END" +
                    ")";

    private static String capWords(String s) {
        if (s == null) return "";
        String[] parts = s.trim().toLowerCase(java.util.Locale.ROOT).split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                parts[i] = Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
            }
        }
        return String.join(" ", parts);
    }


    // ===== SELECT whitelist =====
    private static final Map<String,String> SELECTS = Map.ofEntries(
            Map.entry("DOCUMENTO", "tm.DOCUMENTO"),
            Map.entry("TELEFONOCELULAR", PHONE_EXPR + " AS TELEFONOCELULAR"),
            Map.entry(
                    "NOMBRE",
                    "CASE " +
                            "  WHEN tm.NOMBRE IS NULL OR TRIM(tm.NOMBRE) = '' THEN '' " +
                            "  ELSE " +
                            "    /* primer token antes del primer espacio, en minúsculas */ " +
                            "    CONCAT(UPPER(LEFT(SUBSTRING_INDEX(LOWER(TRIM(tm.NOMBRE)), ' ', 1), 1)), " +
                            "           SUBSTRING(SUBSTRING_INDEX(LOWER(TRIM(tm.NOMBRE)), ' ', 1), 2)) " +
                            "END AS NOMBRE"
            ),
            Map.entry("BAJA30", "CEIL(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2))) AS BAJA30"),
            Map.entry("SALDO_MORA", "CEIL(tm.SLDMORA) AS SALDO_MORA"),
            // ===== PKM =====
            Map.entry("PKM", "CEIL(CAST(NULLIF(TRIM(pkm.PKM),'') AS DECIMAL(18,2))) AS PKM"),

            Map.entry("CAPITAL", "CEIL(tm.SLDCAPCONS) AS CAPITAL"),
            Map.entry("DEUDA_TOTAL", "CEIL(tm.SLDACTUALCONS) AS DEUDA_TOTAL"),
            Map.entry("NOMBRECOMPLETO", "tm.NOMBRECOMPLETO AS NOMBRECOMPLETO"),
            Map.entry("EMAIL", "tm.EMAIL AS EMAIL"),
            Map.entry("NUMCUENTAPMCP", "tm.NUMCUENTAPMCP AS NUMCUENTAPMCP"),
            Map.entry("DIASMORA", "tm.DIASMORA AS DIASMORA")
            );

    // ===== WHERE fragments =====
    private static final String VALIDACIONES_TODOS = String.join(" AND ", List.of(
            "tm.SLDCAPCONS > 0",
            "tm.SLDACTUALCONS > 0",
            PHONE_EXPR + " IS NOT NULL",
            "TRIM(" + PHONE_EXPR + ") <> ''"
    ));

    private static final String WHERE_LTD =
            "(CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2)) > 0)";

    private static final String WHERE_LTDE =
            "(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)) > 0)";
    private static final String WHERE_BAJA30 = "(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2)) > 0)";
    private static final String WHERE_PKM = "(CAST(NULLIF(TRIM(pkm.PKM),'') AS DECIMAL(18,2)) > 0)";
    // Nuevo :
    private static final String WHERE_BAJA30_SALDOMORA =
            "(" +
                    "  CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2)) > 0 " +
                    "  OR (tm.SLDMORA IS NOT NULL AND tm.SLDMORA > 0)" +
                    ")";

    // ===== Fechas dinámicas para BAJA30 y SALDO_MORA =====
    private Map<String, List<Integer>> calcularFechasVencimiento() {
        List<Integer> fijos = List.of(1, 3, 5, 15, 25);
        int hoy = java.time.LocalDate.now().getDayOfMonth();

        Integer proxima = null;
        for (int f : fijos) {
            if (Math.abs(hoy - f) <= 2) {
                proxima = f;
                break;
            }
        }
        Integer finalProxima = proxima;
        List<Integer> fechasMora = proxima != null
                ? List.of(proxima)
                : List.of();

        List<Integer> fechasBaja30 = proxima != null
                ? fijos.stream()
                .filter(f -> !f.equals(finalProxima)) // usamos la copia final
                .toList()
                : fijos;

        return Map.of(
                "MORA", fechasMora,
                "BAJA30", fechasBaja30
        );
    }


    private static final String DATE_PROM =
            "DATE(COALESCE(\n" +
                    "  STR_TO_DATE(NULLIF(ph.FechaCompromiso,''),'%Y-%m-%d'),\n" +
                    "  STR_TO_DATE(NULLIF(ph.FechaOportunidad,''),'%Y-%m-%d')\n" +
                    "))";

    private static final String EXISTS_PROMESAS =
            "EXISTS (\n" +
                    "  SELECT 1 FROM PROMESAS_HISTORICO ph\n" +
                    "  WHERE ph.DOCUMENTO = tm.DOCUMENTO AND ";


    private static String aliasOf(String selectExpr) {
        // asume " ... AS <ALIAS>" al final
        int i = selectExpr.toUpperCase().lastIndexOf(" AS ");
        return (i >= 0) ? selectExpr.substring(i + 4).trim() : selectExpr.trim();
    }

    private void addSelectOnce(List<String> target, String expr, Set<String> seen) {
        String alias = aliasOf(expr);
        if (!seen.contains(alias)) {
            target.add(expr);
            seen.add(alias);
        }
    }

    public List<Map<String,Object>> run(DynamicQueryRequest1 req){
        Integer importeExtra = Optional.ofNullable(req.importeExtra()).orElse(0);



        List<String> selectList = new ArrayList<>();
        Set<String> seenAliases = new LinkedHashSet<>();

        // base (en este orden)
        addSelectOnce(selectList, SELECTS.get("TELEFONOCELULAR"), seenAliases);
        addSelectOnce(selectList, SELECTS.get("NOMBRE"),           seenAliases);
        addSelectOnce(selectList, SELECTS.get("DOCUMENTO"),        seenAliases);
        addSelectOnce(selectList, SELECTS.get("NOMBRECOMPLETO"),   seenAliases);
        addSelectOnce(selectList, SELECTS.get("EMAIL"),            seenAliases);
        addSelectOnce(selectList, SELECTS.get("NUMCUENTAPMCP"),    seenAliases);


        // 1) este (correcto para la prueba con selectAll)
        List<String> selectsReq = new ArrayList<>(Optional.ofNullable(req.selects()).orElse(List.of()));
        for (String key : selectsReq) {
            String expr = SELECTS.get(key);
            if (expr != null) addSelectOnce(selectList, expr, seenAliases);
        }

        // si selectAll: agrega el resto SIN duplicar
        if (Boolean.TRUE.equals(req.selectAll())) {
            for (var e : SELECTS.entrySet()) {
                addSelectOnce(selectList, e.getValue(), seenAliases);
            }
            // LTD/LTDE “base” (solo si aún no estaban)
            addSelectOnce(selectList, "CEIL(tm.`5`) AS LTD",         seenAliases);
            addSelectOnce(selectList, "CEIL(tm.LTDESPECIAL) AS LTDE", seenAliases);
        }



        //List<String> selectsReq = Optional.ofNullable(req.selects()).orElse(List.of());
        /*if (!selectsReq.isEmpty()) {
            for (String key : selectsReq) {
                String expr = SELECTS.get(key);
                if (expr != null) selectList.add(expr);
            }
        }*/

        // 2) FROM
        String from = " FROM TEMP_MERGE tm ";

        // 3) WHERE base
        List<String> where = new ArrayList<>();
        where.add(VALIDACIONES_TODOS);

        // 3.1) Tramo obligatorio
        String tramo = Optional.ofNullable(req.tramo()).orElse("").trim();
        if (tramo.equals("3")) where.add("tm.RANGOMORAPROYAG = 'Tramo 3'");
        else if (tramo.equals("5")) where.add("tm.RANGOMORAPROYAG = 'Tramo 5'");
        else throw new IllegalArgumentException("Debes indicar tramo '3' o '5'");

        // 3.2) INFERIR condiciones por SELECTS (LTD/LTDE/BAJA30)
        Set<String> selectsSet = new HashSet<>(selectsReq);

        // Fechas dinámicas
        Map<String, List<Integer>> fechas = calcularFechasVencimiento();

        boolean selLTD      = selectsSet.contains("LTD");
        boolean selLTDE     = selectsSet.contains("LTDE");
        boolean selLTD_LTDE = selectsSet.contains("LTD_LTDE");
        boolean selBAJA30   = selectsSet.contains("BAJA30");
        boolean selPKM      = selectsSet.contains("PKM");
        boolean selMORA     = selectsSet.contains("SALDO_MORA");
        boolean selBAJA30_MORA = selectsSet.contains("BAJA30_SALDOMORA");
        boolean wantsPKM = Boolean.TRUE.equals(req.selectAll()) || selectsSet.contains("PKM");

        String fromPKM = " FROM TEMP_MERGE tm ";
        if (wantsPKM) {
            from += " LEFT JOIN FOH_TRAMO3_PKM pkm ON pkm.IDENTITY_CODE = tm.IDENTITY_CODE ";
        }


        // === LTD dinámico con importeExtra ===
        if (selectsSet.contains("LTD")) {
            addSelectOnce(selectList,
                    "CASE WHEN CEIL(tm.`5`) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.`5`) + " + importeExtra +
                            " ELSE CEIL(tm.`5`) END AS LTD",
                    seenAliases
            );
        }

        // === LTDE dinámico con importeExtra ===
        if (selectsSet.contains("LTDE")) {
            addSelectOnce(selectList,
                    "CASE WHEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.LTDESPECIAL) + " + importeExtra +
                            " ELSE CEIL(tm.LTDESPECIAL) END AS LTDE",
                    seenAliases
            );
        }

        // === LTD_LTDE dinámico con importeExtra ===
        if (selectsSet.contains("LTD_LTDE")) {
            addSelectOnce(selectList,
                    "CASE " +
                            "  WHEN tm.LTDESPECIAL IS NOT NULL AND tm.LTDESPECIAL <> '' AND tm.LTDESPECIAL > 0 " +
                            "    THEN CASE WHEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "         THEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " ELSE CEIL(tm.LTDESPECIAL) END " +
                            "  WHEN tm.`5` IS NOT NULL AND tm.`5` <> '' AND tm.`5` > 0 " +
                            "    THEN CASE WHEN CEIL(tm.`5`) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "         THEN CEIL(tm.`5`) + " + importeExtra + " ELSE CEIL(tm.`5`) END " +
                            "END AS LTD_LTDE",
                    seenAliases
            );
        }


        int howMany = (selLTD ? 1 : 0) + (selLTDE ? 1 : 0);
        if (howMany == 2) {
            where.add("(" + WHERE_LTD + " AND " + WHERE_LTDE + ")");
        } else if (howMany == 1) {
            if (selLTD)  where.add(WHERE_LTD);
            if (selLTDE) where.add(WHERE_LTDE);
        } else if (selLTD_LTDE) {
            where.add("(" + WHERE_LTD + " OR " + WHERE_LTDE + ")");
        }

        // ➜ Filtro adicional: asegurar que SLDACTUALCONS sea mayor a LTD y LTDE
        if (selLTD) {
            where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2)), 0)");
        }
        if (selLTDE) {
            where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)), 0)");
        }
        if (selLTD_LTDE) {
            where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2)), 0)");
            where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)), 0)");
        }



        if (selBAJA30) {
            where.add(WHERE_BAJA30);
            List<Integer> fechasBaja = fechas.get("BAJA30");
            if (fechasBaja != null && !fechasBaja.isEmpty()) {
                String dias = String.join(",", fechasBaja.stream().map(String::valueOf).toList());
                where.add("DAY(tm.FECVENCIMIENTO) IN (" + dias + ")");
            }
        }

        if (selPKM) {
            where.add(WHERE_PKM);
        }

        if (selBAJA30_MORA || Boolean.TRUE.equals(req.selectAll())) {
            List<Integer> fechasBaja = fechas.get("BAJA30");
            List<Integer> fechasMora = fechas.get("MORA");

            String diasBaja = (fechasBaja != null && !fechasBaja.isEmpty())
                    ? String.join(",", fechasBaja.stream().map(String::valueOf).toList())
                    : null;
            String diasMora = (fechasMora != null && !fechasMora.isEmpty())
                    ? String.join(",", fechasMora.stream().map(String::valueOf).toList())
                    : null;

            // Regla de “cumple fecha” para cada caso
            String condBajaOk =
                    "(" +
                            "  CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2)) > 0" +
                            (diasBaja != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasBaja + ")" : "") +
                            ")";

            String condMoraOk =
                    "(" +
                            "  tm.SLDMORA IS NOT NULL AND tm.SLDMORA > 0" +
                            (diasMora != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasMora + ")" : "") +
                            ")";

            // ➜ SELECT: prioridad BAJA30; si no, MORA; si ninguno cumple: NULL
            String selectBajaMora =
                    "CASE " +
                            "  WHEN " + condBajaOk + " THEN CEIL(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2))) " +
                            "  WHEN " + condMoraOk + " THEN CEIL(tm.SLDMORA) " +
                            "  ELSE NULL " +
                            "END AS BAJA30_SALDOMORA";

            addSelectOnce(selectList, selectBajaMora, seenAliases);
            where.add("(" + condBajaOk + " OR " + condMoraOk + ")");
        }



        Set<String> selectsSolicitados = new HashSet<>(Optional.ofNullable(req.selects()).orElse(List.of()));

        // SALDO MORA
        if (selMORA) {
            where.add("tm.SLDMORA IS NOT NULL AND tm.SLDMORA > 0");
            List<Integer> fechasMora = fechas.get("MORA");
            if (fechasMora != null && !fechasMora.isEmpty()) {
                String dias = String.join(",", fechasMora.stream().map(String::valueOf).toList());
                where.add("DAY(tm.FECVENCIMIENTO) IN (" + dias + ")");
            }
        }

        // 3.3) Promesas (solo leemos las tres válidas si vienen)
        Set<String> conds = Optional.ofNullable(req.condiciones()).orElse(Set.of());
        List<String> prom = new ArrayList<>();
        if (conds.contains("PROMESAS_HOY")) {
            prom.add("(ph.Estado = 'Vigente' AND " + DATE_PROM + " = CURDATE())");
        }
        if (conds.contains("PROMESAS_MANANA")) {
            prom.add("(ph.Estado = 'Vigente' AND " + DATE_PROM + " = CURDATE() + INTERVAL 1 DAY)");
        }
        if (conds.contains("PROMESAS_MANANA2")) {
            prom.add("(ph.Estado = 'Vigente' AND DATEDIFF(" + DATE_PROM + ", CURDATE()) BETWEEN 0 AND 1)");
        }
        if (conds.contains("PROMESAS_ROTAS")) {
            prom.add("(ph.Estado = 'Caida' AND " + DATE_PROM + " < CURDATE())");
        }
        if (!prom.isEmpty()) {
            where.add(EXISTS_PROMESAS + String.join(" OR ", prom) + ")");
        }

        // 3.4) Restricciones
        Restricciones r = Optional.ofNullable(req.restricciones())
                .orElse(new Restricciones(false,false,false, false));

        if (r.noContenido()) {
            where.add("(" +
                    "tm.DOCUMENTO IN (" +
                    "SELECT CASE " +
                    "  WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) " +
                    "  WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) " +
                    "  ELSE A.IDENTITY_CODE END " +
                    "FROM PAYS_TEMP A " +
                    "WHERE RANGO_MORA_ASIG IN ('4.[61-90]') AND CONTENCION = 'NO CONTENIDO'" +
                    ")" +
                    " OR (SELECT COUNT(*) FROM PAYS_TEMP WHERE CONTENCION = 'NO CONTENIDO') = 0" +
                    ")");
        }
        if (r.excluirPromesasPeriodoActual()) {
            where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m'))");
        }
        if (r.excluirCompromisos()) {
            where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS)");
        }
        if (r.excluirBlacklist()) {
            where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist)");
        }

        // 4) Compose SQL (sin limit si viene null)
        Integer limit = Optional.ofNullable(req.limit()).orElse(null);

        String sql = "SELECT " + String.join(", ", selectList) + from +
                " WHERE " + String.join("\n AND ", where) +
                (limit != null ? " LIMIT :limit" : "");

        Map<String,Object> params = (limit != null)
                ? Map.of("limit", limit)
                : Collections.emptyMap();

        // === DEBUG: arma COUNT(*) con el mismo FROM+WHERE
        String countSql = "SELECT COUNT(*) " + from + " WHERE " + String.join("\n AND ", where);

        try {
            Integer cnt = jdbc.queryForObject(countSql, params, Integer.class);
            System.out.println("\n--- DQ DEBUG ---");
            System.out.println("TRAMO: " + req.tramo());
            System.out.println("SELECTS: " + req.selects());
            System.out.println("CONDICIONES: " + req.condiciones());
            System.out.println("RESTRICCIONES: " + req.restricciones());
            System.out.println("IMPORTE_EXTRA: " + req.importeExtra());
            System.out.println("\nCOUNT SQL:\n" + countSql);
            System.out.println("COUNT = " + cnt);
            System.out.println("\nSELECT SQL:\n" + sql); // <- tu variable 'sql' ya creada arriba
            System.out.println("--- /DQ DEBUG ---\n");
        } catch (Exception e) {
            System.out.println("COUNT DEBUG ERROR: " + e.getMessage());
        }


        // CONTADOR PARA VER ERROR

        try {
            List<String> steps = new ArrayList<>();
            steps.add(VALIDACIONES_TODOS);                                // 0
            steps.add((tramo.equals("3") ? "tm.RANGOMORAPROYAG = 'Tramo 3'" : "tm.RANGOMORAPROYAG = 'Tramo 5'")); // 1

            // réplica EXACTA de lo que metiste en 'where' para este caso:
            if (selLTD && selLTDE) {
                steps.add(WHERE_LTD);                                     // 2
                steps.add(WHERE_LTDE);                                    // 3
            } else if (selLTD) {
                steps.add(WHERE_LTD);
            } else if (selLTDE) {
                steps.add(WHERE_LTDE);
            } else if (selLTD_LTDE) {
                steps.add("(" + WHERE_LTD + " OR " + WHERE_LTDE + ")");   // 2 (cuando solo LTD_LTDE)
                // y las dos comparaciones con SLDACTUALCONS:
                steps.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.`5`), '' ) AS DECIMAL(18,2)),0)");
                steps.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)),0)");
            }

            // restricciones activas (marca cada una si aplica en tu UI)
            if (r.noContenido()) steps.add(
                    "tm.DOCUMENTO in (SELECT CASE WHEN A.IDENTITY_CODE LIKE 'D%' THEN RIGHT(A.IDENTITY_CODE,8) WHEN A.IDENTITY_CODE LIKE 'C%' THEN TRIM(LEADING '0' FROM REPLACE(A.IDENTITY_CODE,'C','0')) ELSE A.IDENTITY_CODE END AS DOCUMENTO FROM PAYS_TEMP A WHERE RANGO_MORA_ASIG  IN ('4.[61-90]') AND CONTENCION = 'NO CONTENIDO') OR (SELECT COUNT(*) FROM PAYS_TEMP WHERE CONTENCION = 'NO CONTENIDO') = 0)"
            );
            if (r.excluirPromesasPeriodoActual()) steps.add(
                    "tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m'))"
            );
            if (r.excluirCompromisos()) steps.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS)");
            if (r.excluirBlacklist()) steps.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist)");

            // ahora ve sumando y contando
            List<String> acc = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                acc.add(steps.get(i));
                String partialCount = "SELECT COUNT(*) " + from + " WHERE " + String.join(" AND ", acc);
                Integer c = jdbc.queryForObject(partialCount, Collections.emptyMap(), Integer.class);
                System.out.printf("COUNT STEP %02d = %d  | %s%n", i, c, steps.get(i));
            }
        } catch (Exception e) {
            System.out.println("INCR COUNT ERROR: " + e.getMessage());
        }


        return jdbc.queryForList(sql, params);
    }

    private String prettyHeader(String col) {
        if (col == null) return "";
        // casos especiales
        if ("_SMS_".equals(col))                return "SMS";
        if ("TELEFONOCELULAR".equalsIgnoreCase(col)) return "Celular";
        if ("DOCUMENTO".equalsIgnoreCase(col))  return "Documento";
        if ("NOMBRE".equalsIgnoreCase(col))     return "Nombre";
        if ("NOMBRECOMPLETO".equalsIgnoreCase(col)) return "Nombre completo";
        if ("EMAIL".equalsIgnoreCase(col))      return "Email";
        if ("NUMCUENTAPMCP".equalsIgnoreCase(col)) return "N° Cuenta PMCP";
        if ("DIASMORA".equalsIgnoreCase(col))   return "Días mora";
        // por defecto muestra el alias tal cual (LTD, LTDE, BAJA30, SALDO_MORA, PKM, DEUDA_TOTAL, BAJA30_SALDOMORA, etc.)
        return col;
    }
    // Supervisores
    private record Supervisors(String phone, String name, String email, String NOMBRECOMPLETO, String NUMCUENTAPMCP) {}
    private static final List<Supervisors> SUPERVISORES = List.of(
            new Supervisors("935374672", "Anthony",  "amarquez@contactototal.com.pe", "Anthony Marquez", "0000000000000000"),
            new Supervisors("987122850", "Romina",   "romina@contactototal.com.pe", "Romina Tapia", "0000000000000000"),
            new Supervisors("965392490", "Jonathan", "jonathan@contactototal.com.pe", "Jonathan Reyes", "0000000000000000")
    );

    // columnas numéricas conocidas para poner 0 como Number (no string)
    private static final Set<String> NUMERIC_COLS = Set.of(
            "BAJA30","SALDO_MORA","PKM","CAPITAL","DEUDA_TOTAL",
            "LTD","LTDE","LTD_LTDE","BAJA30_SALDOMORA","DIASMORA"
    );

    // Crea filas sintéticas con el mismo set de columnas del reporte
    private List<Map<String,Object>> buildSupervisorRows(Collection<String> allColumns) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (Supervisors s : SUPERVISORES) {
            Map<String,Object> row = new LinkedHashMap<>();
            for (String col : allColumns) {
                String k = col.toUpperCase();
                if ("TELEFONOCELULAR".equals(k))      row.put(col, s.phone());
                else if ("NOMBRE".equals(k))           row.put(col, s.name());
                else if ("NOMBRECOMPLETO".equals(k))   row.put(col, s.NOMBRECOMPLETO());
                else if ("NUMCUENTAPMCP".equals(k))   row.put(col, s.NUMCUENTAPMCP());
                else if ("EMAIL".equals(k))            row.put(col, s.email());
                else if (NUMERIC_COLS.contains(k))     row.put(col, 0);     // número → 0
                else                                   row.put(col, "");    // texto vacío
            }
            out.add(row);
        }
        return out;
    }




    private static String findHeaderIgnoreCase(List<String> headers, String key) {
        for (String h : headers) if (h.equalsIgnoreCase(key)) return h;
        return null;
    }



    public void exportToExcel(DynamicQueryRequest1 req, OutputStream out) throws IOException {

        // === Export: solo las columnas seleccionadas por el usuario?
        // Pon en true para ACTIVAR; false para dejar TODO como ahora.
        final boolean EXPORT_ONLY_SELECTED = true;

        // Exportar SIN límite -> enviamos limit = null
        DynamicQueryRequest1 reqAll = new DynamicQueryRequest1(
                req.selects(),
                req.tramo(),
                req.condiciones(),
                req.restricciones(),
                null,
                req.importeExtra(),
                req.selectAll(),
                req.template()
        );
        List<Map<String,Object>> rows = this.run(reqAll);
        if (rows == null) rows = new ArrayList<>();

        // ⇩ Normaliza el nombre completo (y el nombre corto si quieres)
        for (var row : rows) {
            Object nc = row.get("NOMBRECOMPLETO");
            if (nc != null) row.put("NOMBRECOMPLETO", capWords(nc.toString()));

            Object n = row.get("NOMBRE"); // opcional
            if (n != null) row.put("NOMBRE", capWords(n.toString()));
        }

        // 🔎 Une todas las columnas presentes en el resultado (para que los supervisores tengan el mismo esquema)
        LinkedHashSet<String> allCols = new LinkedHashSet<>();
        for (var r : rows) allCols.addAll(r.keySet());
        if (allCols.isEmpty()) {
            // fallback: todas las columnas conocidas por SELECTS + las dinámicas
            for (var v : SELECTS.values()) allCols.add(aliasOf(v));
            allCols.addAll(List.of("LTD","LTDE","LTD_LTDE","BAJA30_SALDOMORA"));
        }

        // ➕ Inserta supervisores al inicio
        List<Map<String,Object>> supRows = buildSupervisorRows(allCols);
        rows.addAll(0, supRows);

        // (opcional) si igual quedara vacío, lanza
        if (rows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay filas para exportar con los filtros seleccionados."
            );
        }

        // Render del SMS una sola vez
        final String SMS_COL = "_SMS_";
        String template = Optional.ofNullable(req.template()).orElse("");
        if (!template.isBlank()) {
            for (var row : rows) {
                String sms = SmsTextUtil.render(template, row);
                row.put(SMS_COL, sms);
            }
        }

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sh = wb.createSheet("Consulta");

            // Headers base desde la primera fila
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            if (headers.isEmpty()) {
                headers = new ArrayList<>(List.of("TELEFONOCELULAR","NOMBRE","DOCUMENTO"));
            }

            // 1) Mover DOCUMENTO, NOMBRECOMPLETO, EMAIL, NUMCUENTAPMCP, DIASMORA al final
            List<String> tailOrder = List.of("DOCUMENTO", "NOMBRECOMPLETO", "EMAIL", "NUMCUENTAPMCP", "DIASMORA");
            List<String> tails = new ArrayList<>();
            for (String key : tailOrder) {
                for (int i = 0; i < headers.size(); i++) {
                    if (key.equalsIgnoreCase(headers.get(i))) {
                        tails.add(headers.remove(i));
                        break;
                    }
                }
            }
            headers.addAll(tails);

            // 2) Mover TELEFONOCELULAR al inicio
            int idxCel = -1;
            for (int i = 0; i < headers.size(); i++) {
                if ("TELEFONOCELULAR".equalsIgnoreCase(headers.get(i))) { idxCel = i; break; }
            }
            if (idxCel > 0) {
                String celKey = headers.remove(idxCel);
                headers.add(0, celKey);
            }

            // 3) Mover SMS inmediatamente después del celular (si existe)
            if (!template.isBlank()) {
                int idxSms = -1;
                for (int i = 0; i < headers.size(); i++) {
                    if (SMS_COL.equals(headers.get(i))) { idxSms = i; break; }
                }
                if (idxSms >= 0) {
                    String smsKey = headers.remove(idxSms);
                    headers.add(1, smsKey); // después de TELEFONOCELULAR
                }
            }

            // 3.1) (NUEVO) Filtrar: Celular + SMS + variables seleccionadas por el usuario (en orden)
            if (EXPORT_ONLY_SELECTED) {
                LinkedHashSet<String> keep = new LinkedHashSet<>();

                // Celular
                String celHeader = headers.stream()
                        .filter(h -> h.equalsIgnoreCase("TELEFONOCELULAR"))
                        .findFirst().orElse(null);
                if (celHeader != null) keep.add(celHeader);

                // SMS (si existe)
                String smsHeader = headers.stream()
                        .filter(h -> h.equals(SMS_COL))
                        .findFirst().orElse(null);
                if (smsHeader != null) keep.add(smsHeader);

                // Variables seleccionadas (alias real)
                List<String> selected = Optional.ofNullable(req.selects()).orElse(List.of());
                for (String k : selected) {
                    final String expr = SELECTS.get(k);
                    final String aliasToFind = (expr != null) ? aliasOf(expr) : k;

                    final String match = headers.stream()
                            .filter(h -> h.equalsIgnoreCase(aliasToFind))
                            .findFirst()
                            .orElse(null);
                    if (match != null) keep.add(match);
                }
                headers = new ArrayList<>(keep);
            }

            // 4) Encabezados visibles (bonitos)
            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);

            List<String> display = new ArrayList<>(headers.size());
            for (String col : headers) display.add(prettyHeader(col));

            var hRow = sh.createRow(0);
            for (int c = 0; c < display.size(); c++) {
                var cell = hRow.createCell(c);
                cell.setCellValue(display.get(c));
                cell.setCellStyle(headerStyle);
            }

            /* para mostrar con Var1...
            List<String> display = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String col = headers.get(i);
                if (i == 0) {
                    display.add("Celular");
                } else if ("DOCUMENTO".equalsIgnoreCase(col)) {
                    display.add("DOCUMENTO");
                } else if (SMS_COL.equals(col)) {
                    display.add("VAR1");                  // <- El SMS se llama VAR1
                } else {
                    display.add("VAR" + i);               // VAR2, VAR3, ...
                }
            }

            var hRow = sh.createRow(0);
            for (int c = 0; c < display.size(); c++) {
                var cell = hRow.createCell(c);
                cell.setCellValue(display.get(c));
                cell.setCellStyle(headerStyle);
            }*/

            // 5) Escribir filas con los headers definidos
            int r = 1;
            for (var row : rows) {
                var x = sh.createRow(r++);
                for (int c = 0; c < headers.size(); c++) {
                    var cell = x.createCell(c);
                    writeCell(cell, row.get(headers.get(c)));
                }
            }

            for (int i = 0; i < headers.size(); i++) sh.autoSizeColumn(i);
            wb.write(out);
            out.flush();
        }
    }


    private void writeCell(org.apache.poi.ss.usermodel.Cell cell, Object val) {
        if (val == null) { cell.setBlank(); return; }
        if (val instanceof Number n) { cell.setCellValue(n.doubleValue()); return; }
        if (val instanceof java.time.LocalDate d) { cell.setCellValue(java.sql.Date.valueOf(d)); return; }
        if (val instanceof java.util.Date d) { cell.setCellValue(d); return; }
        cell.setCellValue(String.valueOf(val));
    }



    /** Revisa todas las filas contra una plantilla y devuelve métricas. */
    public SmsPrecheckDTO.Result precheckRows(List<Map<String,Object>> rows, String template) {
        var res = new SmsPrecheckDTO.Result();
        res.total = rows == null ? 0 : rows.size();
        res.excedidos = 0;
        res.ok = true;
        var ejemplos = new java.util.ArrayList<SmsPrecheckDTO.Item>();
        SmsPrecheckDTO.Item peor = null;

        if (rows != null) {
            for (var row : rows) {
                String rendered = SmsTextUtil.render(template, row);
                var c = SmsTextUtil.countSms(rendered);

                if (c.chars > res.limite) {
                    res.excedidos++;
                    res.ok = false;

                    // 👉 SOLO guardamos los 3 primeros que SÍ exceden
                    if (ejemplos.size() < 3) {
                        var it = new SmsPrecheckDTO.Item();
                        it.documento = String.valueOf(row.getOrDefault("DOCUMENTO",""));
                        it.len = c.chars;
                        it.segments = c.segments;
                        it.text = rendered;
                        ejemplos.add(it);
                    }
                }

                // Peor caso (el más largo), sea o no excedente
                if (peor == null || c.chars > peor.len) {
                    peor = new SmsPrecheckDTO.Item();
                    peor.documento = String.valueOf(row.getOrDefault("DOCUMENTO",""));
                    peor.len = c.chars;
                    peor.segments = c.segments;
                    peor.text = rendered;
                    res.charset = c.charset;
                }
            }
        }
            res.ejemplos = ejemplos;
            res.peor = peor;

            return res;
    }
}

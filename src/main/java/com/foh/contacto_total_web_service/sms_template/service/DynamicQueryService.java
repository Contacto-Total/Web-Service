// =====================================================
// ⚠️  ATENCIÓN: NO TOCAR, FUNCIONA Y NO SÉ POR QUÉ ⚠️
// =====================================================
//
// Este fragmento de código fue escrito entre las 2 y 3 de la mañana,
// bajo los efectos combinados de cafeína, desesperación y un bug que
// solo se manifestaba cuando nadie lo estaba mirando.
//
// No funciona si lo entiendes.
// No lo entiendes si funciona.
//
// Cualquier intento de refactorizar esto ha resultado en la invocación
// de problemas dimensionales, loops infinnitos y un extraño parpadeo en el
// monitor que aún no puedo explicar.
//
// Si necesitas cambiar esto, primero reza, luego haz una copia de seguridad,
// y por último... suerte.



package com.foh.contacto_total_web_service.sms_template.service;

import com.foh.contacto_total_web_service.sms_template.dto.DynamicQueryRequest1;
import com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO;
import com.foh.contacto_total_web_service.sms_template.dto.Restricciones;
import com.foh.contacto_total_web_service.sms_template.dto.SmsPrecheckDTO;
import com.foh.contacto_total_web_service.sms_template.util.SmsTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // NUEVO PARA REEMPLAZO DE VARIABLES

    private static final Logger log = LoggerFactory.getLogger(DynamicQueryService.class);

    private List<Map<String,Object>> runInternal(DynamicQueryRequest1 req, boolean applyMetricFilters) {
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



        // selects solicitados
        List<String> selectsReq = new ArrayList<>(Optional.ofNullable(req.selects()).orElse(List.of()));
        for (String key : selectsReq) {
            String expr = SELECTS.get(key);
            if (expr != null) addSelectOnce(selectList, expr, seenAliases);
        }

        /*
        if (Boolean.TRUE.equals(req.selectAll())) {
            for (var e : SELECTS.entrySet()) addSelectOnce(selectList, e.getValue(), seenAliases);
            addSelectOnce(selectList, "CEIL(tm.`5`) AS LTD",           seenAliases);
            addSelectOnce(selectList, "CEIL(tm.LTDESPECIAL) AS LTDE",  seenAliases);
        }
        */

        // 2) FROM
        String from = " FROM TEMP_MERGE tm ";

        // 3) WHERE base
        List<String> where = new ArrayList<>();
        where.add(VALIDACIONES_TODOS);

        // ⬇️ agrega esta línea aquí (justo después de empezar a armar el WHERE)
        Map<String,Object> params = new LinkedHashMap<>();

        // 3.1) Tramo obligatorio
        String tramo = Optional.ofNullable(req.tramo()).orElse("").trim();
        if (tramo.equals("3")) where.add("tm.RANGOMORAPROYAG = 'Tramo 3'");
        else if (tramo.equals("5")) where.add("tm.RANGOMORAPROYAG = 'Tramo 5'");
        else throw new IllegalArgumentException("Debes indicar tramo '3' o '5'");

        // 3.2) flags por selects
        Set<String> selectsSet = new HashSet<>(selectsReq);

        Map<String, List<Integer>> fechas = calcularFechasVencimiento();

        boolean selLTD          = selectsSet.contains("LTD");
        boolean selLTDE         = selectsSet.contains("LTDE");
        boolean selLTD_LTDE     = selectsSet.contains("LTD_LTDE");
        boolean selBAJA30       = selectsSet.contains("BAJA30");
        boolean selMORA         = selectsSet.contains("SALDO_MORA");
        boolean selBAJA30_MORA  = selectsSet.contains("BAJA30_SALDOMORA");
        boolean wantsPKM = normalizeTemplateVars(Optional.ofNullable(req.template()).orElse("")).contains("{PKM}");
        boolean selPKM = selectsSet.contains("PKM") || wantsPKM || Boolean.TRUE.equals(req.selectAll());

        // ¿Algún filtro de rango menciona PKM?
        boolean filterNeedsPKM = Optional.ofNullable(req.rangos()).orElse(List.of()).stream()
                .anyMatch(rf -> rf != null && "PKM".equalsIgnoreCase(rf.field()));

        boolean needPKMJoin = selPKM || filterNeedsPKM;

        // Join PKM solo si se requiere (por select, template o filtro)
        if (needPKMJoin) {
            from += " LEFT JOIN FOH_TRAMO3_PKM pkm ON pkm.IDENTITY_CODE = tm.IDENTITY_CODE ";
        }


        // === LTD/LTDE dinámicos con importeExtra (en SELECT) ===
        if (selectsSet.contains("LTD")) {
            addSelectOnce(selectList,
                    "CASE WHEN CEIL(tm.`5`) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.`5`) + " + importeExtra +
                            " ELSE CEIL(tm.`5`) END AS LTD",
                    seenAliases
            );
        }
        if (selectsSet.contains("LTDE")) {
            addSelectOnce(selectList,
                    "CASE WHEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.LTDESPECIAL) + " + importeExtra +
                            " ELSE CEIL(tm.LTDESPECIAL) END AS LTDE",
                    seenAliases
            );
        }
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

        // === EXTRA SELECTS (solo para GUIADO) ===
        if (!applyMetricFilters) {
            addSelectOnce(selectList, "tm.FECVENCIMIENTO AS _FECV_", seenAliases);
            addSelectOnce(selectList, "CEIL(tm.SLDACTUALCONS) AS _SLD_", seenAliases);
            addSelectOnce(selectList, "CEIL(CAST(NULLIF(TRIM(tm.`5`),'' ) AS DECIMAL(18,2))) AS _LTD_BASE_", seenAliases);
            addSelectOnce(selectList, "CEIL(CAST(NULLIF(TRIM(tm.LTDESPECIAL),'' ) AS DECIMAL(18,2))) AS _LTDE_BASE_", seenAliases);
            addSelectOnce(selectList, "CEIL(CAST(NULLIF(TRIM(tm.`2`),'' ) AS DECIMAL(18,2))) AS _BAJA30_BASE_", seenAliases);
            addSelectOnce(selectList, "CEIL(tm.SLDMORA) AS _MORA_BASE_", seenAliases);
            if (selPKM) {
                addSelectOnce(selectList, "CEIL(CAST(NULLIF(TRIM(pkm.PKM),'' ) AS DECIMAL(18,2))) AS _PKM_BASE_", seenAliases);
            }
        }


        // === Condiciones de fecha para BAJA30 / MORA (definidas UNA sola vez) ===
        List<Integer> fechasBaja = fechas.get("BAJA30");
        List<Integer> fechasMora = fechas.get("MORA");

        String diasBaja = (fechasBaja != null && !fechasBaja.isEmpty())
                ? String.join(",", fechasBaja.stream().map(String::valueOf).toList())
                : null;

        String diasMora = (fechasMora != null && !fechasMora.isEmpty())
                ? String.join(",", fechasMora.stream().map(String::valueOf).toList())
                : null;

        String condBajaOk =
                "(" +
                        WHERE_BAJA30 +
                        (diasBaja != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasBaja + ")" : "") +
                        ")";

        String condMoraOk =
                "(" +
                        "tm.SLDMORA IS NOT NULL AND tm.SLDMORA > 0" +
                        (diasMora != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasMora + ")" : "") +
                        ")";

        // === WHERE para LTD/LTDE ===
        if (applyMetricFilters) {
            if (selLTD && selLTDE) {
                where.add("(" + WHERE_LTD + " OR " + WHERE_LTDE + ")");
            } else if (selLTD) {
                where.add(WHERE_LTD);
            } else if (selLTDE) {
                where.add(WHERE_LTDE);
            } else if (selLTD_LTDE) {
                where.add("(" + WHERE_LTD + " OR " + WHERE_LTDE + ")");
            }

            // comparativas con SLDACTUALCONS
            if (selLTD)   where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2)), 0)");
            if (selLTDE)  where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)), 0)");
            if (selLTD_LTDE) {
                where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2)), 0)");
                where.add("tm.SLDACTUALCONS > COALESCE(CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2)), 0)");
            }
        }

        // === WHERE para BAJA30 / SALDO_MORA por separado
        if (applyMetricFilters) {
            if (selBAJA30 && selMORA) {
                where.add("(" + condBajaOk + " OR " + condMoraOk + ")");
            } else if (selBAJA30) {
                where.add(condBajaOk);
            } else if (selMORA) {
                where.add(condMoraOk);
            }
        }

        // PKM
        if (applyMetricFilters && selPKM) where.add(WHERE_PKM);

        // === Variable combinada BAJA30_SALDOMORA (SELECT siempre; WHERE solo si filtramos)
        if (selBAJA30_MORA || Boolean.TRUE.equals(req.selectAll())) {
            String selectBajaMora =
                    "CASE " +
                            "  WHEN " + condBajaOk + " THEN CEIL(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2))) " +
                            "  WHEN " + condMoraOk + " THEN CEIL(tm.SLDMORA) " +
                            "  ELSE NULL " +
                            "END AS BAJA30_SALDOMORA";

            addSelectOnce(selectList, selectBajaMora, seenAliases);
            if (applyMetricFilters) {
                where.add("(" + condBajaOk + " OR " + condMoraOk + ")");
            }
        }

        // 3.3) Promesas
        Set<String> conds = Optional.ofNullable(req.condiciones()).orElse(Set.of());
        List<String> prom = new ArrayList<>();
        if (conds.contains("PROMESAS_HOY"))     prom.add("(ph.Estado = 'Vigente' AND " + DATE_PROM + " = CURDATE())");
        if (conds.contains("PROMESAS_MANANA"))  prom.add("(ph.Estado = 'Vigente' AND " + DATE_PROM + " = CURDATE() + INTERVAL 1 DAY)");
        if (conds.contains("PROMESAS_MANANA2")) prom.add("(ph.Estado = 'Vigente' AND DATEDIFF(" + DATE_PROM + ", CURDATE()) BETWEEN 0 AND 1)");
        if (conds.contains("PROMESAS_ROTAS"))   prom.add("(ph.Estado = 'Caida' AND " + DATE_PROM + " < CURDATE())");
        if (!prom.isEmpty()) where.add(EXISTS_PROMESAS + String.join(" OR ", prom) + ")");

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
        if (r.excluirPromesasPeriodoActual()) where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM PROMESAS_HISTORICO WHERE PERIODO = DATE_FORMAT(CURDATE(), '%Y%m'))");
        if (r.excluirCompromisos())           where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM COMPROMISOS)");
        if (r.excluirBlacklist())             where.add("tm.DOCUMENTO NOT IN (SELECT DOCUMENTO FROM blacklist)");

        // === Rangos del frontend (cada tarjeta es [>= min] y/o [<= max]; todo se combina con AND) ===
        List<com.foh.contacto_total_web_service.sms_template.dto.RangeFilter> rangos =
                Optional.ofNullable(req.rangos()).orElse(List.of());

        int rfIdx = 1;
        for (var rf : rangos) {
            if (rf == null) continue;

            Double min = rf.min(), max = rf.max();
            if (min != null && max != null && min > max) {
                throw new IllegalArgumentException("El mínimo no puede ser mayor que el máximo para " + rf.field());
            }

            Pred p = buildRangePredicate(rf.field(), min, max, rf.inclusiveMin(), rf.inclusiveMax(), rfIdx++);
            if (p != null) {
                where.add(p.sql);        // se agrega con AND al resto
                params.putAll(p.params); // añade parámetros nombrados
            }
        }


        // 4) Compose SQL (sin limit si viene null)
        Integer limit = Optional.ofNullable(req.limit()).orElse(null);

        String sql = "SELECT " + String.join(", ", selectList) + from +
                " WHERE " + String.join("\n AND ", where) +
                (limit != null ? " LIMIT :limit" : "");

        if (limit != null) params.put("limit", limit);

        // (logs debug iguales a los tuyos si quieres mantenerlos)
        // ---- LOG de la consulta final ----

        if (!applyMetricFilters) {
            log.info("\n[GUIADO] SQL FINAL:\n{}\n[GUIADO] PARAMS: {}\n" +
                            "[GUIADO] FLAGS -> selLTD={}, selLTDE={}, selLTD_LTDE={}, selBAJA30={}, selMORA={}, selBAJA30_MORA={}, selPKM={}\n" +
                            "[GUIADO] CONTEXTO -> tramo={}, importeExtra={}, selectAll={}, selects={}",
                    sql, params,
                    selLTD, selLTDE, selLTD_LTDE, selBAJA30, selMORA, selBAJA30_MORA, selPKM,
                    tramo, importeExtra, req.selectAll(), req.selects());
        } else if (log.isDebugEnabled()) {
            // Ruta normal (Generar sin guiado)
            log.debug("\n[GENERAR] SQL FINAL:\n{}\n[GENERAR] PARAMS: {}", sql, params);
        }

        return jdbc.queryForList(sql, params);



    }

    //-----------------------------------------------------------------------------------------------

    public List<Map<String,Object>> run(DynamicQueryRequest1 req) {
        return runInternal(req, true);
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

    // === Campos filtrables por rango (expresión base, no alias de SELECT) ===
    private static final Map<String,String> FILTERS = Map.ofEntries(
            Map.entry("DEUDA_TOTAL", "CAST(tm.SLDACTUALCONS AS DECIMAL(18,2))"),
            Map.entry("CAPITAL",     "CAST(tm.SLDCAPCONS   AS DECIMAL(18,2))"),
            Map.entry("SALDO_MORA",  "CAST(tm.SLDMORA      AS DECIMAL(18,2))"),
            Map.entry("BAJA30",      "CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2))"),
            Map.entry("LTD",         "CAST(NULLIF(TRIM(tm.`5`), '') AS DECIMAL(18,2))"),
            Map.entry("LTDE",        "CAST(NULLIF(TRIM(tm.LTDESPECIAL), '') AS DECIMAL(18,2))"),
            Map.entry("PKM",         "CAST(NULLIF(TRIM(pkm.PKM), '') AS DECIMAL(18,2))"),
            Map.entry("DIASMORA",    "tm.DIASMORA")
    );

    private static final class Pred {
        final String sql;
        final Map<String, Object> params;
        Pred(String sql, Map<String, Object> params) { this.sql = sql; this.params = params; }
    }

    private Pred buildRangePredicate(String fieldRaw, Double min, Double max, boolean inclusiveMin, boolean inclusiveMax, int idx) {
        if (fieldRaw == null || fieldRaw.isBlank()) {
            throw new IllegalArgumentException("Campo de rango vacío.");
        }
        String field = fieldRaw.trim().toUpperCase(java.util.Locale.ROOT);
        String expr = FILTERS.get(field);
        if (expr == null) {
            throw new IllegalArgumentException("Campo no filtrable: " + field);
        }

        Map<String,Object> params = new LinkedHashMap<>();
        List<String> parts = new ArrayList<>();

        if (min != null) {
            String p = "rf" + idx + "_min";
            parts.add(expr + (inclusiveMin ? " >= :" : " > :") + p);
            params.put(p, min);
        }
        if (max != null) {
            String p = "rf" + idx + "_max";
            parts.add(expr + (inclusiveMax ? " <= :" : " < :") + p);
            params.put(p, max);
        }

        if (parts.isEmpty()) return null; // no min/max -> no filtra
        return new Pred("(" + String.join(" AND ", parts) + ")", params);
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
        log.info(">>> HEADERS exportResolved: {}");
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
                req.template(),
                req.rangos()
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
        String template = normalizeTemplateVars(Optional.ofNullable(req.template()).orElse(""));

        // ¿qué pide el template?
        boolean wantsLTD   = template.contains("{LTD}");
        boolean wantsLTDE  = template.contains("{LTDE}");
        boolean wantsBM    = template.contains("{BAJA30}");
        boolean wantsMora  = template.contains("{SALDO_MORA}");
        boolean wantsCombL = template.contains("{LTD_LTDE}");
        boolean wantsCombB = template.contains("{BAJA30_SALDOMORA}");

// Espejo de combinadas -> bases
        for (var row : rows) {
            Object combL = row.get("LTD_LTDE");
            if (combL != null) {
                if (wantsLTD  && row.get("LTD")  == null) row.put("LTD",  combL);
                if (wantsLTDE && row.get("LTDE") == null) row.put("LTDE", combL);
            }

            Object combB = row.get("BAJA30_SALDOMORA");
            if (combB != null) {
                if (wantsBM   && row.get("BAJA30")    == null)     row.put("BAJA30",     combB);
                if (wantsMora && row.get("SALDO_MORA")== null)     row.put("SALDO_MORA", combB);
            }

            // (opcional) bases -> combinadas si el template pide la combinada
            if (wantsCombL && row.get("LTD_LTDE") == null) {
                Number ltde = (Number) row.get("LTDE");
                Number ltd  = (Number) row.get("LTD");
                Number chosen = (ltde != null && ltde.doubleValue() > 0) ? ltde : ltd;
                if (chosen != null) row.put("LTD_LTDE", chosen);
            }
            if (wantsCombB && row.get("BAJA30_SALDOMORA") == null) {
                Number b30  = (Number) row.get("BAJA30");
                Number mora = (Number) row.get("SALDO_MORA");
                Number chosen = (b30 != null && b30.doubleValue() > 0) ? b30 : mora;
                if (chosen != null) row.put("BAJA30_SALDOMORA", chosen);
            }
        }

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

            // ==== ENCABEZADOS TIPO: Celular | VAR1 | VAR2 | ... ====
            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);


            // "headers" ya está ordenado con Celular al inicio y SMS en 2do lugar (si existe)
            List<String> headerList = new ArrayList<>(headers);

            // si existe _SMS_, VAR1 = SMS y el contador arranca en 2
            var hRow = sh.createRow(0);
            hRow.createCell(0).setCellValue("Celular");
            for (int i = 1; i <= 15; i++) {
                var cell = hRow.createCell(i);
                cell.setCellValue("VAR" + i);
                cell.setCellStyle(headerStyle);
            }
            // Aplicar estilo a la primera columna también
            hRow.getCell(0).setCellStyle(headerStyle);

            // 5) Escribir filas con los headers definidos
            int r = 1;
            for (var row : rows) {
                var x = sh.createRow(r++);
                // Siempre crear 16 celdas
                for (int c = 0; c < 16; c++) {
                    var cell = x.createCell(c);
                    if (c < headers.size()) {
                        writeCell(cell, row.get(headers.get(c)));
                    } else {
                        cell.setBlank(); // Celda vacía para columnas sin datos
                    }
                }
            }

            for (int i = 0; i < 16; i++) sh.autoSizeColumn(i);
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


    // === INICIO: Soporte de sesiones de preview ===
    private static final List<String> DEFAULT_CANDIDATAS = List.of(
            "BAJA30","SALDO_MORA","PKM","LTD","LTDE","CAPITAL"
    );

    private static final Set<String> HIDDEN_CANDIDATAS =
            Set.of("BAJA30_SALDOMORA", "LTD_LTDE");

    // estado por fila
    private static final class RowState {
        String documento;
        String nombre;
        Map<String,Object> original; // fila original (valores crudos)
        String variableUsada;        // null hasta que se resuelva
        Number valorUsado;           // null hasta que se resuelva
        Map<String,Object> working;  // copia mutable para render (donde podemos sobreescribir BAJA30 si tú así lo decides)
    }

    // sesión completa
    private static final class PreviewSession {
        String sessionId;
        String template;
        List<String> ordenElegido = new ArrayList<>();
        List<String> candidatasPool;
        List<RowState> rows;
        long lastTouched = System.currentTimeMillis();

        // NUEVO
        Set<String> preSelected;   // variables que el usuario marcó antes del guiado

        // Fechas / importe
        List<Integer> fechasBaja;
        List<Integer> fechasMora;
        int importeExtra;


        DynamicQueryRequest1 originalQuery;
    }

    private static Double numOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.valueOf(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }
    private static boolean pos(Object v) {
        Double d = numOrNull(v);
        return d != null && d > 0.0;
    }
    private static Integer dayOfMonth(Object v) {
        try {
            if (v instanceof java.util.Date d) {
                var cal = java.util.Calendar.getInstance();
                cal.setTime(d);
                return cal.get(java.util.Calendar.DAY_OF_MONTH);
            }
            if (v instanceof java.time.LocalDate ld) return ld.getDayOfMonth();
            if (v instanceof java.time.LocalDateTime ldt) return ldt.getDayOfMonth();
            if (v instanceof String s && !s.isBlank()) {
                // intenta YYYY-MM-DD
                try {
                    return java.time.LocalDate.parse(s.substring(0, 10)).getDayOfMonth();
                } catch (Exception ignore) { /* best effort */ }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** Evalúa la MISMA lógica de "Generar" para cada variable */
    private boolean cumpleRegla(
            String var, Map<String,Object> r,
            List<Integer> fechasBaja, List<Integer> fechasMora,
            int /*unused in regla*/ importeExtra
    ) {
        // Valores base
        Double sld     = numOrNull(r.get("_SLD_"));
        Double baja30  = numOrNull(r.get("_BAJA30_BASE_"));
        Double mora    = numOrNull(r.get("_MORA_BASE_"));
        Double ltd     = numOrNull(r.get("_LTD_BASE_"));
        Double ltde    = numOrNull(r.get("_LTDE_BASE_"));
        Double pkm     = numOrNull(r.get("_PKM_BASE_"));
        Double capital = numOrNull(r.get("CAPITAL"));
        Integer diaV   = dayOfMonth(r.get("_FECV_"));

        switch ((var == null ? "" : var).toUpperCase(Locale.ROOT)) {
            case "BAJA30":
                if (baja30 == null || baja30 <= 0) return false;
                if (fechasBaja != null && !fechasBaja.isEmpty()) {
                    if (diaV == null) return false;                    // <- bloquear si no hay fecha
                    if (!fechasBaja.contains(diaV)) return false;
                }
                return true;

            case "SALDO_MORA": {
                if (mora == null || mora <= 0) return false;
                if (fechasMora != null && !fechasMora.isEmpty()) {
                    if (diaV == null) return false;                    // <- idem
                    if (!fechasMora.contains(diaV)) return false;
                }
                return true;
            }
            case "LTD": {
                return ltd != null && ltd > 0 && sld != null && sld > ltd;
            }
            case "LTDE": {
                return ltde != null && ltde > 0 && sld != null && sld > ltde;
            }
            case "PKM": {
                return pkm != null && pkm > 0;
            }
            case "BAJA30_SALDOMORA":{
                // Igual que en SQL: BAJA30 OR MORA con sus respectivas restricciones
                return cumpleRegla("BAJA30", r, fechasBaja, fechasMora, importeExtra)
                        || cumpleRegla("SALDO_MORA", r, fechasBaja, fechasMora, importeExtra);
            }
            case "LTD_LTDE": {
                    boolean okLTD  = (ltd  != null && ltd  > 0 && sld != null && sld > ltd);
                    boolean okLTDE = (ltde != null && ltde > 0 && sld != null && sld > ltde);
                    return okLTD || okLTDE;
            }

            case "CAPITAL": {
                return capital != null && capital > 0;
            }



            default:
                return false;
        }
    }

    // helper: ¿la var del template está vacía/0 en esta fila?
    private static boolean isEmptyOnRow(String var, Map<String,Object> r) {
        switch (var.toUpperCase(Locale.ROOT)) {
            case "BAJA30":     return numOrNull(r.get("_BAJA30_BASE_")) == null || numOrNull(r.get("_BAJA30_BASE_")) <= 0;
            case "SALDO_MORA": return numOrNull(r.get("_MORA_BASE_"))   == null || numOrNull(r.get("_MORA_BASE_"))   <= 0;
            case "LTD":        return numOrNull(r.get("_LTD_BASE_"))    == null || numOrNull(r.get("_LTD_BASE_"))    <= 0;
            case "LTDE":       return numOrNull(r.get("_LTDE_BASE_"))   == null || numOrNull(r.get("_LTDE_BASE_"))   <= 0;
            case "PKM":        return numOrNull(r.get("_PKM_BASE_"))    == null || numOrNull(r.get("_PKM_BASE_"))    <= 0;
            case "CAPITAL": return numOrNull(r.get("CAPITAL")) == null || numOrNull(r.get("CAPITAL")) <= 0;
            default:           return true; // para otras, trátalas como vacías
        }
    }

    private static Set<String> tokensEnTemplate(String tpl) {
        if (tpl == null) return Set.of();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([A-Z0-9_]+)\\}", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(tpl);
        Set<String> out = new java.util.LinkedHashSet<>();
        while (m.find()) out.add(m.group(1).toUpperCase(Locale.ROOT));
        // normaliza MORA
        if (out.contains("MORA")) out.add("SALDO_MORA");
        return out;
    }

    /** Devuelve el valor "base" correspondiente a la variable (para poner en working/VALOR_MENSAJE) */
    private static Number valorBaseDe(String var, Map<String,Object> r) {
        switch ((var == null ? "" : var).toUpperCase(Locale.ROOT)) {
            case "BAJA30":           return numOrNull(r.get("_BAJA30_BASE_"));
            case "SALDO_MORA":       return numOrNull(r.get("_MORA_BASE_"));
            case "LTD":              return numOrNull(r.get("_LTD_BASE_"));
            case "LTDE":             return numOrNull(r.get("_LTDE_BASE_"));
            case "PKM":              return numOrNull(r.get("_PKM_BASE_"));
            case "BAJA30_SALDOMORA": {
                Number v1 = numOrNull(r.get("_BAJA30_BASE_"));
                Number v2 = numOrNull(r.get("_MORA_BASE_"));
                return (v1 != null && v1.doubleValue() > 0) ? v1 : v2;}
            case "LTD_LTDE": {
                Number vLTDE = numOrNull(r.get("_LTDE_BASE_"));
                Number vLTD  = numOrNull(r.get("_LTD_BASE_"));
                // Preferimos LTDE si existe; si no, LTD
                return (vLTDE != null && vLTDE.doubleValue() > 0) ? vLTDE : vLTD;
            }
            default:
                return numOrNull(r.get(var));
        }
    }



    private final java.util.concurrent.ConcurrentHashMap<String, PreviewSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    // TTL simple para memoria (30 min)
    private void touch(PreviewSession s) {
        if (s != null) s.lastTouched = System.currentTimeMillis();
    }
    private void gcSessions() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> (now - s.lastTouched) > 30 * 60_000);
    }

    // Util: lee número positivo
    private static Number num(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.valueOf(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }
    private static boolean isPos(Object v) {
        Number n = num(v);
        return n != null && n.doubleValue() > 0.0;
    }

    // Dedupe por DOCUMENTO
    private static List<Map<String,Object>> dedupeByDocumento(List<Map<String,Object>> rows) {
        Map<String,Map<String,Object>> byDoc = new LinkedHashMap<>();
        for (var r : rows) {
            String doc = String.valueOf(r.getOrDefault("DOCUMENTO",""));
            if (!doc.isBlank() && !byDoc.containsKey(doc)) byDoc.put(doc, r);
        }
        return new ArrayList<>(byDoc.values());
    }

    // Construye RowState desde fila
    private RowState toRowState(Map<String,Object> r) {
        RowState rs = new RowState();
        rs.documento = String.valueOf(r.getOrDefault("DOCUMENTO",""));
        rs.nombre = String.valueOf(r.getOrDefault("NOMBRE",""));
        rs.original = new LinkedHashMap<>(r);
        rs.working  = new LinkedHashMap<>(r);
        return rs;
    }

    // Aplica una variable elegida SOLO a pendientes; resuelve si >0
    private int applyVariable(PreviewSession s, String var) {
        if ("LTD_LTDE".equalsIgnoreCase(var)) {
            int resolvedNow = 0, logged = 0;
            for (RowState rs : s.rows) {
                if (rs.valorUsado != null) continue;
                if (!cumpleRegla(var, rs.original, s.fechasBaja, s.fechasMora, s.importeExtra)) continue;

                Double sld   = numOrNull(rs.original.get("_SLD_"));
                Double baseL = numOrNull(rs.original.get("_LTD_BASE_"));
                Double baseE = numOrNull(rs.original.get("_LTDE_BASE_"));

                // Elige el que cumple; si ambos cumplen, prioriza LTDE
                Double chosen = null;
                if (baseE != null && baseE > 0 && sld != null && sld > baseE)      chosen = baseE;
                else if (baseL != null && baseL > 0 && sld != null && sld > baseL) chosen = baseL;

                // Aplica importeExtra con tope en SLDACTUALCONS
                Double shown = chosen;
                if (chosen != null && sld != null) {
                    double candidate = chosen + s.importeExtra;
                    if (candidate < sld) shown = candidate;
                }

                if (log.isDebugEnabled() && logged < 10) {
                    log.debug("[LTD_LTDE][DECISION] doc={}, sld={}, ltdBase={}, ltdeBase={}, chosenBase={}, importeExtra={}, shown={}",
                            rs.documento, sld, baseL, baseE, chosen, s.importeExtra, shown);
                    logged++;
                }

                rs.variableUsada = var;
                rs.valorUsado = (shown == null) ? 0 : shown;

                // escribe el valor en LTD_LTDE y espeja si el template pide LTD/LTDE
                String tpl = s.template == null ? "" : s.template;
                rs.working.put("LTD_LTDE", rs.valorUsado);
                if (tpl.contains("{LTD}"))  rs.working.put("LTD",  rs.valorUsado);
                if (tpl.contains("{LTDE}")) rs.working.put("LTDE", rs.valorUsado);

                resolvedNow++;
            }
            return resolvedNow;
        }


        int resolvedNow = 0;
        for (RowState rs : s.rows) {
            if (rs.valorUsado != null) continue;
            if (!cumpleRegla(var, rs.original, s.fechasBaja, s.fechasMora, s.importeExtra)) continue;

            rs.variableUsada = var;

            Number baseN = valorBaseDe(var, rs.original);
            Double base = (baseN == null) ? null : baseN.doubleValue();
            Double sld  = numOrNull(rs.original.get("_SLD_"));

            Double shown = base;
            if (("LTD".equalsIgnoreCase(var) || "LTDE".equalsIgnoreCase(var)) && base != null && sld != null) {
                double candidate = base + s.importeExtra;
                shown = (candidate < sld) ? candidate : base;
            }

            rs.valorUsado = (shown == null) ? 0 : shown;
            rs.working.put(var, rs.valorUsado);
            // espejo inteligente según placeholders del template
            String tpl = s.template == null ? "" : s.template;
            if (!var.equalsIgnoreCase("BAJA30") && tpl.contains("{BAJA30}")) {
                rs.working.put("BAJA30", rs.valorUsado);
            }
            if (!var.equalsIgnoreCase("SALDO_MORA") && tpl.contains("{SALDO_MORA}")) {
                rs.working.put("SALDO_MORA", rs.valorUsado);
            }
            if (!var.equalsIgnoreCase("LTD") && tpl.contains("{LTD}")) {
                rs.working.put("LTD", rs.valorUsado);
            }
            if (!var.equalsIgnoreCase("LTDE") && tpl.contains("{LTDE}")) {
                rs.working.put("LTDE", rs.valorUsado);
            }
            if (!var.equalsIgnoreCase("PKM") && tpl.contains("{PKM}")) {
                rs.working.put("PKM", rs.valorUsado);
            }
            resolvedNow++;
        }
        return resolvedNow;
    }


    // Calcula contadores de candidatas sobre PENDIENTES actuales
    private List<com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.CandidateCount> candidateCounts(PreviewSession s) {
        // pendientes = filas sin valorUsado
        Set<String> pendDocs = new HashSet<>();
        for (RowState rs : s.rows) if (rs.valorUsado == null) pendDocs.add(rs.documento);

        List<com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.CandidateCount> out = new ArrayList<>();
        for (String var : s.candidatasPool) {
            // ya elegido en esta sesión
            if (s.ordenElegido != null && s.ordenElegido.contains(var)) continue;

            // NUEVO: también salta si fue preseleccionada antes del guiado
            if (s.preSelected != null && s.preSelected.contains(var.toUpperCase(Locale.ROOT))) continue;

            int can = 0;
            for (RowState rs : s.rows) {
                if (rs.valorUsado != null) continue;               // pendiente solamente
                if (cumpleRegla(var, rs.original, s.fechasBaja, s.fechasMora, s.importeExtra)) {
                    can++;
                }
            }
            if (can > 0) out.add(new PreviewDTO.CandidateCount(var, can));
        }
        out.sort((a,b) -> Integer.compare(b.filasQueResuelve(), a.filasQueResuelve()));
        return out;
    }

    // Render de preview (hasta N filas mezcladas: algunas resueltas + algunas pendientes)
    private List<com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.PreviewItem> buildPreview(PreviewSession s, int max) {
        List<com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.PreviewItem> out = new ArrayList<>();
        for (RowState rs : s.rows) {
            if (out.size() >= max) break;

            String sms = SmsTextUtil.render(s.template == null ? "" : s.template, rs.working);
            out.add(new com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.PreviewItem(
                    rs.documento, rs.nombre, rs.variableUsada, rs.valorUsado, sms
            ));
        }
        return out;
    }

    // === API: INIT ===
    public PreviewDTO.InitResp previewInit(PreviewDTO.InitReq body) {
        gcSessions();

        DynamicQueryRequest1 q = body.query();
        List<String> wanted = new ArrayList<>(Optional.ofNullable(q.selects()).orElse(List.of()));
        Set<String> need = new LinkedHashSet<>(wanted);
        List<String> pool = (body.candidatas() == null || body.candidatas().isEmpty())
                ? DEFAULT_CANDIDATAS
                : body.candidatas();
        for (String v : pool) need.add(v);

        DynamicQueryRequest1 qAll = new DynamicQueryRequest1(
                new ArrayList<>(need),
                q.tramo(),
                q.condiciones(),
                q.restricciones(),
                null,                       // sin límite
                q.importeExtra(),
                q.selectAll(),
                q.template(),
                q.rangos()
        );



        log.info("[GUIADO] previewInit -> tramo={}, selectAll={}, importeExtra={}, selects={}, candidatasSolicitadas={}",
                q.tramo(), q.selectAll(), q.importeExtra(), q.selects(),
                (body.candidatas()==null? List.of() : body.candidatas()));


        // Trae base con columnas _FECV_, _SLD_, _*_BASE_, etc.
        List<Map<String,Object>> base = this.runInternal(qAll, false);
        // ===== DEBUG FOCALIZADO LTD/LTDE =====
        if (log.isDebugEnabled()) {
            long total = base.size();

            long okLTD = base.stream().filter(r ->
                    numOrNull(r.get("_LTD_BASE_")) != null &&
                            numOrNull(r.get("_SLD_"))      != null &&
                            numOrNull(r.get("_LTD_BASE_"))  > 0 &&
                            numOrNull(r.get("_SLD_"))      > numOrNull(r.get("_LTD_BASE_"))
            ).count();

            long okLTDE = base.stream().filter(r ->
                    numOrNull(r.get("_LTDE_BASE_")) != null &&
                            numOrNull(r.get("_SLD_"))       != null &&
                            numOrNull(r.get("_LTDE_BASE_"))  > 0 &&
                            numOrNull(r.get("_SLD_"))       > numOrNull(r.get("_LTDE_BASE_"))
            ).count();

            long okAlguno = base.stream().filter(r -> {
                Double sld  = numOrNull(r.get("_SLD_"));
                Double ltd  = numOrNull(r.get("_LTD_BASE_"));
                Double ltde = numOrNull(r.get("_LTDE_BASE_"));
                boolean a = (ltd  != null && ltd  > 0 && sld != null && sld > ltd);
                boolean b = (ltde != null && ltde > 0 && sld != null && sld > ltde);
                return a || b;
            }).count();

            long okAmbos = base.stream().filter(r -> {
                Double sld  = numOrNull(r.get("_SLD_"));
                Double ltd  = numOrNull(r.get("_LTD_BASE_"));
                Double ltde = numOrNull(r.get("_LTDE_BASE_"));
                boolean a = (ltd  != null && ltd  > 0 && sld != null && sld > ltd);
                boolean b = (ltde != null && ltde > 0 && sld != null && sld > ltde);
                return a && b;
            }).count();

            long okNinguno = total - okAlguno;

            log.debug("[LTD/LTDE][BASE] total={}, okLTD={}, okLTDE={}, okAmbos={}, okNinguno={}",
                    total, okLTD, okLTDE, okAmbos, okNinguno);



            // Muestra de filas que NO califican por ninguna (máx 5)
            int mostrados = 0;
            for (var r : base) {
                Double sld  = numOrNull(r.get("_SLD_"));
                Double ltd  = numOrNull(r.get("_LTD_BASE_"));
                Double ltde = numOrNull(r.get("_LTDE_BASE_"));
                boolean okL = (ltd  != null && ltd  > 0 && sld != null && sld > ltd);
                boolean okE = (ltde != null && ltde > 0 && sld != null && sld > ltde);
                if (!(okL || okE)) {
                    log.debug("[LTD/LTDE][NO-OK] doc={}, sld={}, ltdBase={}, ltdeBase={}",
                            r.get("DOCUMENTO"), sld, ltd, ltde);
                    if (++mostrados >= 5) break;
                }
            }
        }
// ===== FIN DEBUG LTD/LTDE =====


        base = dedupeByDocumento(base);

        // --- Construye sesión
        PreviewSession s = new PreviewSession();
        s.sessionId = java.util.UUID.randomUUID().toString();
        s.template  = Optional.ofNullable(q.template()).orElse("");
        // NUEVO: guarda las que venían desde la UI (antes del guiado)
        s.preSelected = new java.util.HashSet<>();
        for (String v : wanted) s.preSelected.add(v.toUpperCase(Locale.ROOT));

        s.originalQuery = q; // la misma que vino de la UI

        // Construye pool y quita lo preseleccionado
        s.candidatasPool = new ArrayList<>();
        for (String v : pool) {
            String up = v.toUpperCase(Locale.ROOT);
            if (!s.preSelected.contains(up) && !HIDDEN_CANDIDATAS.contains(up)) {
                s.candidatasPool.add(v);  // solo candidatas visibles
            }
        }

        // ⚠️ Primero setea fechas e importe para que cumplaRegla use esto
        Map<String, List<Integer>> fechas = calcularFechasVencimiento();
        s.fechasBaja = fechas.getOrDefault("BAJA30", List.of());
        s.fechasMora = fechas.getOrDefault("MORA",   List.of());
        s.importeExtra = Optional.ofNullable(q.importeExtra()).orElse(0);

        Set<String> targetsTpl = tokensEnTemplate(s.template);

        // Expande combinadas a sus bases para decidir resolvibles
        Set<String> baseTargets = new LinkedHashSet<>();
        if (targetsTpl.contains("BAJA30") || targetsTpl.contains("BAJA30_SALDOMORA")) {
            baseTargets.add("BAJA30");
            baseTargets.add("SALDO_MORA");
        }
        if (targetsTpl.contains("SALDO_MORA")) baseTargets.add("SALDO_MORA");
        if (targetsTpl.contains("LTD") || targetsTpl.contains("LTD_LTDE")) {
            baseTargets.add("LTD");
            baseTargets.add("LTDE");
        }
        if (targetsTpl.contains("PKM")) baseTargets.add("PKM");
        if (targetsTpl.contains("CAPITAL")) baseTargets.add("CAPITAL");

        // Asegura que el pool contenga combinadas si el template las usa
        if (targetsTpl.contains("BAJA30_SALDOMORA") && !s.candidatasPool.contains("BAJA30_SALDOMORA"))
            s.candidatasPool.add("BAJA30_SALDOMORA");
        if (targetsTpl.contains("LTD_LTDE") && !s.candidatasPool.contains("LTD_LTDE"))
            s.candidatasPool.add("LTD_LTDE");

        boolean usaCombLTD = s.candidatasPool.stream()
                .anyMatch(v -> "LTD_LTDE".equalsIgnoreCase(v));
        boolean usaCombBM  = s.candidatasPool.stream()
                .anyMatch(v -> "BAJA30_SALDOMORA".equalsIgnoreCase(v));
        // AND si el usuario marcó ambas LTD y LTDE, o si se usa la combinada
        boolean bothSelectedLTDs =
                s.preSelected != null &&
                        s.preSelected.contains("LTD") &&
                        s.preSelected.contains("LTDE");
        boolean requireAndForLTD = usaCombLTD || bothSelectedLTDs;

        // Solo consideramos como "variables vaciables" estas (ajusta si necesitas más)
        final Set<String> VACIABLES = Set.of("LTD","LTDE","BAJA30","SALDO_MORA","PKM","CAPITAL");

        // Construye la lista de targets a vaciar según lo que marcó el usuario en la UI
        List<String> vacioTargets = new ArrayList<>();
        if (s.preSelected != null) {
            for (String v : s.preSelected) {
                String up = v == null ? "" : v.toUpperCase(Locale.ROOT);
                if (VACIABLES.contains(up)) vacioTargets.add(up);
            }
        }

        // ✅ Filtra universo inicial: solo filas resolvibles por alguna candidata
        List<Map<String,Object>> resolvibles = new ArrayList<>();
        for (var r : base) {

            // --- 1) ¿targets vacíos?
            boolean vacios;

            if (!vacioTargets.isEmpty()) {
                // ✅ Se usa EXACTAMENTE lo que el usuario marcó
                // Si marcó una -> vacíos = isEmptyOnRow(esa)
                // Si marcó varias -> vacíos = AND de todas
                boolean allEmpty = true;
                for (String t : vacioTargets) {
                    if (!isEmptyOnRow(t, r)) { allEmpty = false; break; }
                }
                vacios = allEmpty;
            } else {
                // ⬇️ Comportamiento anterior cuando el usuario no marcó nada para vaciar:
                if (usaCombLTD && baseTargets.contains("LTD") && baseTargets.contains("LTDE")) {
                    // combinada LTD_LTDE -> ambas vacías (AND)
                    vacios = isEmptyOnRow("LTD", r) && isEmptyOnRow("LTDE", r);
                } else if (usaCombBM && baseTargets.contains("BAJA30") && baseTargets.contains("SALDO_MORA")) {
                    // combinada BAJA30_SALDOMORA -> ambas vacías (AND)
                    vacios = isEmptyOnRow("BAJA30", r) && isEmptyOnRow("SALDO_MORA", r);
                } else {
                    // caso normal -> al menos una vacía (OR) según template
                    vacios = baseTargets.isEmpty() ||
                            baseTargets.stream().anyMatch(t -> isEmptyOnRow(t, r));
                }
            }

            // --- 2) ¿alguna candidata aplica?
            boolean candidataAplica = false;
            for (String var : s.candidatasPool) {
                if (cumpleRegla(var, r, s.fechasBaja, s.fechasMora, s.importeExtra)) {
                    candidataAplica = true;
                    break;
                }
            }

            if (vacios && candidataAplica) {
                resolvibles.add(r);
            }
        }

        // ✅ Usa resolvibles, no base
        s.rows = new ArrayList<>();
        for (var r : resolvibles) s.rows.add(toRowState(r));

        log.info("[GUIADO] Filas resolvibles tras filtro inicial (targets vacíos + candidata aplica): {}", resolvibles.size());


        sessions.put(s.sessionId, s);
        touch(s);

        int total = s.rows.size();
        int resueltas = 0;
        int pendientes = total;
        var cand = candidateCounts(s);        // cuenta sobre PENDIENTES
        var preview = buildPreview(s, 10);

        log.info("[GUIADO] Sesión {} -> total={}, resueltas=0, pendientes={}",
                s.sessionId, s.rows.size(), s.rows.size());

        return new PreviewDTO.InitResp(s.sessionId, total, resueltas, pendientes, cand, preview);
    }

    // === API: CHOOSE ===
    public com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.StepResp previewChoose(String sessionId, String var) {
        PreviewSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("Sesión no encontrada");
        touch(s);

        s.ordenElegido.add(var);
        int resolved = applyVariable(s, var);

        int total = s.rows.size();
        int resueltas = 0;
        for (RowState rs : s.rows) if (rs.valorUsado != null) resueltas++;
        int pendientes = total - resueltas;

        var cand = candidateCounts(s);
        var preview = buildPreview(s, 10);

        return new com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.StepResp(
                s.sessionId, total, resueltas, pendientes, cand, preview
        );
    }

    // === API: SKIP (no hace nada, sólo recalcula contadores y preview) ===
    public com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.StepResp previewSkip(String sessionId) {
        PreviewSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("Sesión no encontrada");
        touch(s);

        int total = s.rows.size();
        int resueltas = 0;
        for (RowState rs : s.rows) if (rs.valorUsado != null) resueltas++;
        int pendientes = total - resueltas;

        var cand = candidateCounts(s);
        var preview = buildPreview(s, 10);

        return new com.foh.contacto_total_web_service.sms_template.dto.PreviewDTO.StepResp(
                s.sessionId, total, resueltas, pendientes, cand, preview
        );
    }

    // === API: DOWNLOAD (exporta con VARIABLE_USADA y VALOR_MENSAJE) ===
    public void previewDownload(String sessionId, OutputStream out) throws IOException {
        PreviewSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("Sesión no encontrada");
        touch(s);

        // Construye filas "finales" para export
        List<Map<String,Object>> rows = new ArrayList<>();
        for (RowState rs : s.rows) {
            Map<String,Object> r = new LinkedHashMap<>(rs.working);
            // columnas adicionales
            r.put("VARIABLE_USADA", rs.variableUsada == null ? "" : rs.variableUsada);
            r.put("VALOR_MENSAJE",  rs.valorUsado == null ? 0 : rs.valorUsado);
            // Render del SMS con working (ya contiene sustituciones por variable usada)
            String template = normalizeTemplateVars(s.template == null ? "" : s.template);
            String sms = SmsTextUtil.render(template, r);
            r.put("_SMS_", sms);
            rows.add(r);
        }

        // Exportar SOLO columnas seleccionadas + Celular + SMS + VALOR_MENSAJE + VARIABLE_USADA
        // Reutilizamos tu exportador pero con pequeña variante:
        exportResolved(rows, out);
    }

    // Exportador para lista ya resuelta
    private void exportResolved(List<Map<String,Object>> rows, OutputStream out) throws IOException {
        log.info(">>> HEADERS exportResolved: {}");
        if (rows == null) rows = new ArrayList<>();
        if (rows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay filas para exportar."
            );
        }

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sh = wb.createSheet("Consulta");

            // headers
            LinkedHashSet<String> headers = new LinkedHashSet<>();
            // orden sugerido
            List<String> prefer = List.of("TELEFONOCELULAR","_SMS_","VARIABLE_USADA","VALOR_MENSAJE",
                    "NOMBRE","DOCUMENTO","BAJA30","SALDO_MORA","PKM","CAPITAL","DEUDA_TOTAL");
            for (String p : prefer) headers.add(p);
            // añade los demás presentes
            for (var r : rows) headers.addAll(r.keySet());


            List<String> headerList = new ArrayList<>(headers);


            // estilo
            var hRow = sh.createRow(0);
            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);


            // SIEMPRE crear 16 columnas de cabecera
            hRow.createCell(0).setCellValue("Celular");
            for (int i = 1; i <= 15; i++) {
                var cell = hRow.createCell(i);
                cell.setCellValue("VAR" + i);
                cell.setCellStyle(headerStyle);
            }
            hRow.getCell(0).setCellStyle(headerStyle);

            int rIdx = 1;
            for (var row : rows) {
                var x = sh.createRow(rIdx++);
                // Siempre crear 16 celdas
                for (int ci = 0; ci < 16; ci++) {
                    var cell = x.createCell(ci);
                    if (ci < headerList.size()) {
                        writeCell(cell, row.get(headerList.get(ci)));
                    } else {
                        cell.setBlank();
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) sh.autoSizeColumn(i);
            wb.write(out);
            out.flush();
        }
    }
// === FIN: Soporte de sesiones de preview ===

    // Normaliza tokens para que coincidan con los alias reales del SELECT
    private static String normalizeTemplateVars(String tpl) {
        if (tpl == null) return "";
        String t = tpl;

        // equivalentes y variantes con espacios
        t = t.replaceAll("(?i)\\{\\s*mora\\s*\\}", "{SALDO_MORA}");
        t = t.replaceAll("(?i)\\{\\s*baja\\s*30\\s*\\}", "{BAJA30}");
        t = t.replaceAll("(?i)\\{\\s*deuda\\s*total\\s*\\}", "{DEUDA_TOTAL}");
        t = t.replaceAll("(?i)\\{\\s*nombre\\s*completo\\s*\\}", "{NOMBRECOMPLETO}");
        t = t.replaceAll("(?i)\\{\\s*num(?:ero)?\\s*cuenta\\s*pmcp\\s*\\}", "{NUMCUENTAPMCP}");

        // combinadas (variantes de escritura)
        t = t.replaceAll("(?i)\\{\\s*ltd\\s*[+_/\\-\\s]*\\s*ltde\\s*\\}", "{LTD_LTDE}");
        t = t.replaceAll("(?i)\\{\\s*baja\\s*30\\s*[+_/\\-\\s]*\\s*saldo\\s*mora\\s*\\}", "{BAJA30_SALDOMORA}");

        return t;
    }

    private void exportAppend(List<Map<String,Object>> baseRows,
                              List<Map<String,Object>> extraRows,
                              OutputStream out) throws IOException {
        if (baseRows == null) baseRows = new ArrayList<>();
        if (extraRows == null) extraRows = new ArrayList<>();

        // Si no hay nada, error
        if (baseRows.isEmpty() && extraRows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay filas para exportar."
            );
        }

        // Unión de columnas
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        // preferencia de orden: Celular, SMS, VARIABLE_USADA, VALOR_MENSAJE, luego lo demás
        List<String> prefer = List.of("TELEFONOCELULAR","_SMS_","VARIABLE_USADA","VALOR_MENSAJE",
                "NOMBRE","DOCUMENTO","BAJA30","SALDO_MORA","PKM","CAPITAL","DEUDA_TOTAL",
                "LTD","LTDE","LTD_LTDE","BAJA30_SALDOMORA","NOMBRECOMPLETO","EMAIL","NUMCUENTAPMCP","DIASMORA");
        for (String p : prefer) headers.add(p);
        for (var r : baseRows)  headers.addAll(r.keySet());
        for (var r : extraRows) headers.addAll(r.keySet());

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sh = wb.createSheet("Consulta");

            // Encabezados visibles
            var hRow = sh.createRow(0);
            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);
            int cIdx = 0;

            // Escribir base primero
            int rIdx = 1;
            for (var row : baseRows) {
                var x = sh.createRow(rIdx++);
                int ci = 0;
                for (String h : headers) {
                    var cell = x.createCell(ci++);
                    writeCell(cell, row.get(h));
                }
            }
            // Luego las extras del guiado
            for (var row : extraRows) {
                var x = sh.createRow(rIdx++);
                int ci = 0;
                for (String h : headers) {
                    var cell = x.createCell(ci++);
                    writeCell(cell, row.get(h));
                }
            }

            // auto-size
            int idx = 0;
            for (String ignored : headers) sh.autoSizeColumn(idx++);

            wb.write(out);
            out.flush();
        }
    }


    public void previewDownloadMerged(String sessionId, OutputStream out) throws IOException {
        PreviewSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("Sesión no encontrada");
        touch(s);

        // 1) Base “normal” (tal cual tu export normal)
        DynamicQueryRequest1 reqBase = new DynamicQueryRequest1(
                s.originalQuery.selects(),
                s.originalQuery.tramo(),
                s.originalQuery.condiciones(),
                s.originalQuery.restricciones(),
                null,                        // sin límite
                s.originalQuery.importeExtra(),
                s.originalQuery.selectAll(),
                s.originalQuery.template(),
                s.originalQuery.rangos()
        );
        List<Map<String,Object>> baseRows = this.run(reqBase);
        if (baseRows == null) baseRows = new ArrayList<>();

        // Normaliza nombres bonitos como en el export normal
        for (var row : baseRows) {
            Object nc = row.get("NOMBRECOMPLETO");
            if (nc != null) row.put("NOMBRECOMPLETO", capWords(nc.toString()));
            Object n = row.get("NOMBRE");
            if (n != null) row.put("NOMBRE", capWords(n.toString()));
        }

        // 2) Filas “guiadas” (las añadidas): construir desde la sesión, NO desde baseRows
        final String SMS_COL = "_SMS_";
        String tpl = normalizeTemplateVars(
                Optional.ofNullable(s.template).orElse("")
        );

        // columnas que el template podría necesitar para espejar combinadas <-> bases
        boolean wantsLTD   = tpl.contains("{LTD}");
        boolean wantsLTDE  = tpl.contains("{LTDE}");
        boolean wantsBM    = tpl.contains("{BAJA30}");
        boolean wantsMora  = tpl.contains("{SALDO_MORA}");
        boolean wantsCombL = tpl.contains("{LTD_LTDE}");
        boolean wantsCombB = tpl.contains("{BAJA30_SALDOMORA}");

        List<Map<String,Object>> extraRows = new ArrayList<>();
        for (RowState rs : s.rows) {
            Map<String,Object> r = new LinkedHashMap<>(rs.working);

            // Asegura que la variable elegida tenga su columna con el valor usado
            if (rs.variableUsada != null && rs.valorUsado != null) {
                r.put(rs.variableUsada.toUpperCase(java.util.Locale.ROOT), rs.valorUsado);
            }

            // Espejos combinadas -> bases
            Object combL = r.get("LTD_LTDE");
            if (combL != null) {
                if (wantsLTD  && r.get("LTD")  == null) r.put("LTD",  combL);
                if (wantsLTDE && r.get("LTDE") == null) r.put("LTDE", combL);
            }
            Object combB = r.get("BAJA30_SALDOMORA");
            if (combB != null) {
                if (wantsBM   && r.get("BAJA30")     == null) r.put("BAJA30", combB);
                if (wantsMora && r.get("SALDO_MORA") == null) r.put("SALDO_MORA", combB);
            }
            // bases -> combinadas si el template las pide
            if (wantsCombL && r.get("LTD_LTDE") == null) {
                Number ltde = (Number) r.get("LTDE");
                Number ltd  = (Number) r.get("LTD");
                Number chosen = (ltde != null && ltde.doubleValue() > 0) ? ltde : ltd;
                if (chosen != null) r.put("LTD_LTDE", chosen);
            }
            if (wantsCombB && r.get("BAJA30_SALDOMORA") == null) {
                Number b30  = (Number) r.get("BAJA30");
                Number mo   = (Number) r.get("SALDO_MORA");
                Number chosen = (b30 != null && b30.doubleValue() > 0) ? b30 : mo;
                if (chosen != null) r.put("BAJA30_SALDOMORA", chosen);
            }

            // Render SMS
            String sms = SmsTextUtil.render(tpl, r);
            r.put(SMS_COL, sms);

            extraRows.add(r);
        }

        // 3) Cabeceras con el MISMO formato que el export normal:
        //    Celular, SMS, y SOLO variables seleccionadas + variables nuevas usadas en el guiado
        //    (sin VARIABLE_USADA ni VALOR_MENSAJE).
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        headers.add("TELEFONOCELULAR");
        headers.add(SMS_COL);

        // order: primero las seleccionadas por el usuario (alias reales)
        List<String> selected = new ArrayList<>(Optional.ofNullable(s.originalQuery.selects()).orElse(List.of()));
        for (String k : selected) {
            String expr = SELECTS.get(k);
            String alias = (expr != null) ? aliasOf(expr) : k;
            if (alias != null && !alias.isBlank()) headers.add(alias);
        }

        // luego, variables adicionales realmente usadas en el guiado (p.ej. PKM, SALDO_MORA, etc.)
        // en un orden lógico.
        List<String> pref = List.of("BAJA30","SALDO_MORA","PKM","CAPITAL","DEUDA_TOTAL","LTD","LTDE","LTD_LTDE","BAJA30_SALDOMORA");
        Set<String> usedInGuided = new LinkedHashSet<>();
        for (RowState rs : s.rows) {
            if (rs.variableUsada != null) usedInGuided.add(rs.variableUsada.toUpperCase(java.util.Locale.ROOT));
        }
        for (String p : pref) if (usedInGuided.contains(p)) headers.add(p);
        // por si quedó alguna no listada en pref
        for (String p : usedInGuided) headers.add(p);

        // 4) Escribir Excel (primero base, luego extra) SOLO con esas columnas
        if (baseRows.isEmpty() && extraRows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay filas para exportar."
            );
        }

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sh = wb.createSheet("Consulta");

            // Encabezados: Celular, VAR1 (si hay _SMS_), luego VAR2, VAR3...
            var hRow = sh.createRow(0);
            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);


            // si existe _SMS_, entonces VAR1 = SMS y el contador de VAR arranca en 2
            hRow.createCell(0).setCellValue("Celular");
            for (int i = 1; i <= 15; i++) {
                var cell = hRow.createCell(i);
                cell.setCellValue("VAR" + i);
                cell.setCellStyle(headerStyle);
            }
            hRow.getCell(0).setCellStyle(headerStyle);


            // Asegura que las filas base tengan SMS (como en el normal)
            for (var row : baseRows) {
                if (!row.containsKey(SMS_COL)) {
                    String sms = SmsTextUtil.render(tpl, row);
                    row.put(SMS_COL, sms);
                }
            }

            int rIdx = 1;
            // base primero
            List<String> headerList = new ArrayList<>(headers);

            // base primero
            for (var row : baseRows) {
                var x = sh.createRow(rIdx++);
                for (int ci = 0; ci < 16; ci++) {
                    var cell = x.createCell(ci);
                    if (ci < headerList.size()) {
                        writeCell(cell, row.get(headerList.get(ci)));
                    } else {
                        cell.setBlank();
                    }
                }
            }
            // luego las añadidas
            for (var row : extraRows) {
                var x = sh.createRow(rIdx++);
                for (int ci = 0; ci < 16; ci++) {
                    var cell = x.createCell(ci);
                    if (ci < headerList.size()) {
                        writeCell(cell, row.get(headerList.get(ci)));
                    } else {
                        cell.setBlank();
                    }
                }
            }

            // auto-size
            int idx = 0;
            for (String ignored : headers) sh.autoSizeColumn(idx++);

            wb.write(out);
            out.flush();
        }
    }

    public void previewDownloadBase(String sessionId, OutputStream out) throws IOException {
        PreviewSession s = sessions.get(sessionId);
        if (s == null) throw new IllegalStateException("Sesión no encontrada");
        touch(s);

        DynamicQueryRequest1 reqBase = new DynamicQueryRequest1(
                s.originalQuery.selects(),
                s.originalQuery.tramo(),
                s.originalQuery.condiciones(),
                s.originalQuery.restricciones(),
                null,                        // sin límite
                s.originalQuery.importeExtra(),
                s.originalQuery.selectAll(),
                s.originalQuery.template(),
                s.originalQuery.rangos()
        );

        exportToExcel(reqBase, out);  // ← tu export normal
    }






}

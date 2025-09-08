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

    // ===== SELECT whitelist =====
    private static final Map<String,String> SELECTS = Map.ofEntries(
            Map.entry("DOCUMENTO", "tm.DOCUMENTO"),
            Map.entry("TELEFONOCELULAR", "tm.TELEFONOCELULAR"),
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
            Map.entry("PKM", "CEIL(CAST(NULLIF(TRIM(tm.`9`), '') AS DECIMAL(18,2))) AS PKM"),
            // ===== Columna combinada: BAJA30 si existe; si no, MORA =====
            Map.entry(
                    "BAJA30_SALDOMORA",
                    "CEIL(" +
                            "  IFNULL(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2)), 0) +" +
                            "  IFNULL(tm.SLDMORA, 0)" +
                            ") AS BAJA30_SALDOMORA"
            ),

            Map.entry("CAPITAL", "CEIL(tm.SLDCAPCONS) AS CAPITAL"),
            Map.entry("DEUDA_TOTAL", "CEIL(tm.SLDACTUALCONS) AS DEUDA_TOTAL")
    );

    // ===== WHERE fragments =====
    private static final String VALIDACIONES_TODOS = String.join(" AND ", List.of(
            "tm.SLDCAPCONS > 0",
            "tm.SLDACTUALCONS > 0",
            "tm.TELEFONOCELULAR IS NOT NULL",
            "TRIM(tm.TELEFONOCELULAR) <> ''"
    ));

    private static final String WHERE_LTD   = "(tm.`5` <> '' AND tm.`5` > 0)";
    private static final String WHERE_LTDE  = "(tm.LTDESPECIAL IS NOT NULL AND tm.LTDESPECIAL <> '' AND tm.LTDESPECIAL > 0)";
    private static final String WHERE_BAJA30 = "(CAST(NULLIF(TRIM(tm.`2`), '') AS DECIMAL(18,2)) > 0)";
    private static final String WHERE_PKM = "(CAST(NULLIF(TRIM(tm.`9`), '') AS DECIMAL(18,2)) > 0)";
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

    public List<Map<String,Object>> run(DynamicQueryRequest1 req){
        Integer importeExtra = Optional.ofNullable(req.importeExtra()).orElse(0);

        // 1) SELECT
        List<String> selectList = new ArrayList<>();
        // base
        selectList.add(SELECTS.get("DOCUMENTO"));
        selectList.add(SELECTS.get("TELEFONOCELULAR"));
        selectList.add(SELECTS.get("NOMBRE"));


        // 1) este (correcto para la prueba con selectAll)
        List<String> selectsReq = new ArrayList<>(Optional.ofNullable(req.selects()).orElse(List.of()));
        for (String key : selectsReq) {
            String expr = SELECTS.get(key);
            if (expr != null) selectList.add(expr);
        }
        if (Boolean.TRUE.equals(req.selectAll())) {
            for (String key : SELECTS.keySet()) {
                if (!selectsReq.contains(key)) {
                    String expr = SELECTS.get(key);
                    if (expr != null) selectList.add(expr);
                }
            }
            selectList.add("CEIL(tm.`5`) AS LTD");
            selectList.add("CEIL(tm.LTDESPECIAL) AS LTDE");
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


        // === LTD dinámico con importeExtra ===
        if (selectsSet.contains("LTD")) {
            selectList.add(
                    "CASE WHEN CEIL(tm.`5`) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.`5`) + " + importeExtra +
                            " ELSE CEIL(tm.`5`) END AS LTD"
            );
        }

        // === LTDE dinámico con importeExtra ===
        if (selectsSet.contains("LTDE")) {
            selectList.add(
                    "CASE WHEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "THEN CEIL(tm.LTDESPECIAL) + " + importeExtra +
                            " ELSE CEIL(tm.LTDESPECIAL) END AS LTDE"
            );
        }

        // === LTD_LTDE dinámico con importeExtra ===
        if (selectsSet.contains("LTD_LTDE")) {
            selectList.add(
                    "CASE " +
                            "  WHEN tm.LTDESPECIAL IS NOT NULL AND tm.LTDESPECIAL <> '' AND tm.LTDESPECIAL > 0 " +
                            "    THEN CASE WHEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "         THEN CEIL(tm.LTDESPECIAL) + " + importeExtra + " ELSE CEIL(tm.LTDESPECIAL) END " +
                            "  WHEN tm.`5` IS NOT NULL AND tm.`5` <> '' AND tm.`5` > 0 " +
                            "    THEN CASE WHEN CEIL(tm.`5`) + " + importeExtra + " < CEIL(tm.SLDACTUALCONS) " +
                            "         THEN CEIL(tm.`5`) + " + importeExtra + " ELSE CEIL(tm.`5`) END " +
                            "END AS LTD_LTDE"
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

        // Combinada BAJA30 + MORA
        if (selBAJA30_MORA) {
            List<Integer> fechasBaja = fechas.get("BAJA30");
            List<Integer> fechasMora = fechas.get("MORA");

            String diasBaja = (fechasBaja != null && !fechasBaja.isEmpty())
                    ? String.join(",", fechasBaja.stream().map(String::valueOf).toList())
                    : null;
            String diasMora = (fechasMora != null && !fechasMora.isEmpty())
                    ? String.join(",", fechasMora.stream().map(String::valueOf).toList())
                    : null;

            String condBaja =
                    WHERE_BAJA30 +
                            (diasBaja != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasBaja + ")" : "");

            String condMora =
                    "(tm.SLDMORA IS NOT NULL AND tm.SLDMORA > 0)" +
                            (diasMora != null ? " AND DAY(tm.FECVENCIMIENTO) IN (" + diasMora + ")" : "");

            // ✅ OR entre bloques, no intersección imposible
            where.add("(" + condBaja + " OR " + condMora + ")");
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
        if (conds.contains("PROMESAS_ROTAS")) {
            prom.add("(ph.Estado = 'Caida' AND " + DATE_PROM + " < CURDATE())");
        }
        if (!prom.isEmpty()) {
            where.add(EXISTS_PROMESAS + String.join(" OR ", prom) + ")");
        }

        // 3.4) Restricciones
        Restricciones r = Optional.ofNullable(req.restricciones()).orElse(new Restricciones(false,false,false));
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

        return jdbc.queryForList(sql, params);
    }

    public void exportToExcel(DynamicQueryRequest1 req, OutputStream out) throws IOException {
        // Exportar SIN límite -> enviamos limit = null
        DynamicQueryRequest1 reqAll = new DynamicQueryRequest1(
                req.selects(),
                req.tramo(),
                req.condiciones(),
                req.restricciones(),
                null,
                req.importeExtra(),
                req.selectAll()
        );
        List<Map<String,Object>> rows = this.run(reqAll);

        // ⛔ Si no hay filas, corta con 422
        if (rows == null || rows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "No hay filas para exportar con los filtros seleccionados."
            );
        }

        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sh = wb.createSheet("Consulta");

            List<String> headers = new ArrayList<>();
            if (!rows.isEmpty()) {
                headers.addAll(rows.get(0).keySet());
            }
            // Si por alguna razón vinieran vacíos, define un orden básico
            if (headers.isEmpty()) {
                headers = new ArrayList<>(List.of("TELEFONOCELULAR","NOMBRE","DOCUMENTO"));
            }

            // ➜ Forzar DOCUMENTO al final (case-insensitive)
            boolean hadDocumento = headers.stream().anyMatch(h -> "DOCUMENTO".equalsIgnoreCase(h));
            if (hadDocumento) {
                headers.removeIf(h -> "DOCUMENTO".equalsIgnoreCase(h));
                headers.add("DOCUMENTO");
            }

            var headerStyle = wb.createCellStyle();
            var font = wb.createFont(); font.setBold(true);
            headerStyle.setFont(font);

            var h = sh.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                var cell = h.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (var row : rows) {
                var xrow = sh.createRow(r++);
                int c = 0;
                for (String col : headers) {
                    Object val = row.get(col);
                    var cell = xrow.createCell(c++);
                    writeCell(cell, val);
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

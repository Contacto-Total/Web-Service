package com.foh.contacto_total_web_service.plantillaSMS.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foh.contacto_total_web_service.sms.dto.*;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.plantillaSMS.model.PlantillaSMS;
import com.foh.contacto_total_web_service.compromiso.repository.CompromisoRepository;
import com.foh.contacto_total_web_service.plantillaSMS.repository.PlantillaSMSRepository;
import com.foh.contacto_total_web_service.sms.repository.SMSRepository;
import com.foh.contacto_total_web_service.plantillaSMS.service.PlantillaSMSService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlantillaSMSServiceImpl implements PlantillaSMSService {

    @Autowired
    private PlantillaSMSRepository plantillaSMSRepository;

    @Autowired
    private SMSRepository smsRepository;

    @Autowired
    private CompromisoRepository compromisoRepository;

    @Override
    public PlantillaSMS createPlantillaSMS(PlantillaSMSRequest plantillaSMSRequest) {
        PlantillaSMS plantillaSMS = new PlantillaSMS();
        plantillaSMS.setName(plantillaSMSRequest.getName());
        plantillaSMS.setTemplate(plantillaSMSRequest.getTemplate());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String tipisJson = mapper.writeValueAsString(plantillaSMSRequest.getTipis());
            plantillaSMS.setTipis(tipisJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir los campos a JSON", e);
        }

        return plantillaSMSRepository.save(plantillaSMS);
    }

    @Override
    public List<PlantillaSMS> getPlantillasSMS() {
        return plantillaSMSRepository.findAll();
    }

    @Override
    public PlantillaSMS getPlantillaById(Integer id) {
        return plantillaSMSRepository.findById(id).orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
    }

    @Override
    public PlantillaSMS getPlantillaByNombre(String name) {
        return plantillaSMSRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con el nombre: " + name));
    }

    @Override
    public File getFileByPlantillaWithData(GenerateMessagesRequest generateMessagesRequest) {
        PlantillaSMS plantilla = getPlantillaByNombre(generateMessagesRequest.getName());
        List<String> tipis = generateMessagesRequest.getTipis();
        List<String> promesas;

        if (tipis.contains("PROMESA DE PAGO")) {
            promesas = compromisoRepository.findAllPromesasExceptVigentes();
        } else {
            promesas = compromisoRepository.findAllPromesasExceptCaidasToSMS();
        }

        List<PeopleForSMSResponse> peopleData = smsRepository.getPeopleForSMS(tipis, promesas);

        LocalDate today = LocalDate.now();
        int dia = today.getDayOfMonth();

        if (today.getMonth() != Month.FEBRUARY && dia == today.lengthOfMonth()) {
        } else if (dia < today.lengthOfMonth()) {
            dia += 1;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mensajes SMS");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CELULAR");
        headerRow.createCell(1).setCellValue("var1");
        headerRow.createCell(2).setCellValue("var2");

        for (int i = 3; i <= 15; i++) {
            headerRow.createCell(i).setCellValue("var" + (i));
        }

        int rowNum = 1;
        for (PeopleForSMSResponse person : peopleData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(person.getTelefonoCelular());

            String nombreCompleto = person.getNombre();
            String primerNombre = nombreCompleto.split(" ")[0];

            String mensaje = plantilla.getTemplate()
                    .replace("{documento}", person.getDocumento())
                    .replace("{nombre}", primerNombre)
                    .replace("{telefonoCelular}", person.getTelefonoCelular())
                    .replace("{deudaTotal}", String.valueOf(person.getDeudaTotal()))
                    .replace("{ltd}", String.valueOf(person.getLtd()))
                    .replace("{dia}", String.valueOf(dia));

            if (mensaje.length() > 160) {
                for (int i = primerNombre.length(); i > 0; i--) {
                    String nombreCortado = primerNombre.substring(0, i);
                    String mensajeAjustado = mensaje.replace(primerNombre, nombreCortado);
                    if (mensajeAjustado.length() <= 160) {
                        mensaje = mensajeAjustado;
                        break;
                    }
                }
            }

            row.createCell(1).setCellValue(mensaje);

            row.createCell(2).setCellValue(mensaje.length());

            for (int i = 3; i <= 15; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        File file = new File("Mensajes_SMS.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Error al crear el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @Override
    public File getFileByPlantillaWithData2(GenerateMessagesRequest generateMessagesRequest) {
        PlantillaSMS plantilla = getPlantillaByNombre(generateMessagesRequest.getName());

        List<PeopleForSMSResponse> peopleData = smsRepository.getPeopleForSMS2(generateMessagesRequest.getName());

        LocalDate today = LocalDate.now();
        int dia = today.getDayOfMonth();

        if (today.getMonth() != Month.FEBRUARY && dia == today.lengthOfMonth()) {
        } else if (dia < today.lengthOfMonth()) {
            dia += 1;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mensajes SMS");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CELULAR");
        headerRow.createCell(1).setCellValue("var1");
        headerRow.createCell(2).setCellValue("var2");

        for (int i = 3; i <= 15; i++) {
            headerRow.createCell(i).setCellValue("var" + (i));
        }

        int rowNum = 1;
        for (PeopleForSMSResponse person : peopleData) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(person.getTelefonoCelular());

            String nombreCompleto = person.getNombre();
            String primerNombre = nombreCompleto.split(" ")[0];

            String mensaje = plantilla.getTemplate()
                    .replace("{documento}", person.getDocumento())
                    .replace("{nombre}", primerNombre)
                    .replace("{telefonoCelular}", person.getTelefonoCelular())
                    .replace("{deudaTotal}", String.valueOf(person.getDeudaTotal()))
                    .replace("{ltd}", String.valueOf(person.getLtd()))
                    .replace("{dia}", String.valueOf(dia));

            if (mensaje.length() > 160) {
                for (int i = primerNombre.length(); i > 0; i--) {
                    String nombreCortado = primerNombre.substring(0, i);
                    String mensajeAjustado = mensaje.replace(primerNombre, nombreCortado);
                    if (mensajeAjustado.length() <= 160) {
                        mensaje = mensajeAjustado;
                        break;
                    }
                }
            }

            row.createCell(1).setCellValue(mensaje);

            row.createCell(2).setCellValue(mensaje.length());

            for (int i = 3; i <= 15; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        File file = new File("Mensajes_SMS.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Error al crear el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @Override
    public PlantillaSMS updatePlantilla(PlantillaSMSToUpdateRequest plantillaSMSToUpdateRequest) {
        PlantillaSMS plantilla = getPlantillaById(plantillaSMSToUpdateRequest.getId());
        plantilla.setName(plantillaSMSToUpdateRequest.getName());
        plantilla.setTemplate(plantillaSMSToUpdateRequest.getTemplate());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String tipisJson = mapper.writeValueAsString(plantillaSMSToUpdateRequest.getTipis());
            plantilla.setTipis(tipisJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir los campos a JSON", e);
        }

        return plantillaSMSRepository.save(plantilla);
    }

    @Override
    public void deletePlantilla(Integer id) {
        PlantillaSMS plantilla = getPlantillaById(id);
        plantillaSMSRepository.delete(plantilla);
    }

    // NUEVO

    @Override
    public File getFileByCustomSMS(boolean onlyLtde, String periodo) {
        // si no necesitás plantilla, podés armar el Excel directo
        List<PeopleForCustomSMSResponse> peopleData = smsRepository.getPeopleForCustomSMS(onlyLtde, periodo);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mensajes SMS");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CELULAR");
        headerRow.createCell(1).setCellValue("var1");
        headerRow.createCell(2).setCellValue("var2");

        for (int i = 3; i <= 15; i++) {
            headerRow.createCell(i).setCellValue("var" + (i));
        }

        int rowNum = 1;
        for (PeopleForCustomSMSResponse person : peopleData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(person.getTelefonoCelular());
            row.createCell(1).setCellValue(person.getNombre());

            BigDecimal deudaTotal = person.getDeudaTotal();
            if (deudaTotal != null) {
                row.createCell(2).setCellValue(deudaTotal.doubleValue());
            } else {
                row.createCell(2).setCellValue(0.0);
            }
        }

        File file = new File("Mensajes_SMS_Custom.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Error al crear el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    // ACTUALIZADO

    // === Helpers para normalizar y detectar placeholders usados por la plantilla ===
    private String normalizeVar(String v) {
        if (v == null) return "";
        String x = v.toLowerCase().replaceAll("\\s+", "");
        if (x.equals("baja_30")) x = "baja30";
        if (x.equals("saldo_mora")) x = "saldomora";
        if (x.equals("deuda_total")) x = "deudatotal";
        return x;
    }

    private Set<String> extractVarsFromTemplateText(String tpl) {
        // Devuelve claves normalizadas: nombre, baja30, saldomora, deudatotal, ltd, ltde, dia
        Set<String> out = new java.util.LinkedHashSet<>();
        if (tpl == null || tpl.isBlank()) return out;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\{([^}]+)\\}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(tpl);
        while (m.find()) {
            String raw = m.group(1).trim();
            // caso {ltd/ltde}
            if (raw.equalsIgnoreCase("ltd/ltde")) {
                out.add("ltd");
                out.add("ltde");
            } else {
                out.add(normalizeVar(raw));
            }
        }
        return out;
    }

    /** Render completo (case-insensitive), con sinónimos y {ltd/ltde}. */
    private String renderWithValuesCaseInsensitive(String tpl, Map<String,Object> values) {
        if (tpl == null || tpl.isBlank()) return "";

        // Construir un map de valores con claves “normalizadas” y sinónimos
        Map<String,String> vals = new java.util.HashMap<>();
        java.util.function.Function<String,String> get = k ->
                String.valueOf(values.getOrDefault(k, "") == null ? "" : values.getOrDefault(k, ""));

        // originales tal como vienen desde runDynamicQuery (usas claves en minúscula)
        vals.put("nombre",      get.apply("nombre"));
        vals.put("baja30",      get.apply("baja30"));
        vals.put("saldomora",   values.containsKey("saldomora") ? get.apply("saldomora") : get.apply("saldo_mora"));
        vals.put("deudatotal",  values.containsKey("deudatotal") ? get.apply("deudatotal") : get.apply("deuda_total"));
        vals.put("ltd",         get.apply("ltd"));
        vals.put("ltde",        get.apply("ltde"));
        vals.put("ltd_final",   get.apply("ltd_final"));
        vals.put("celular",     values.containsKey("celular") ? get.apply("celular") : get.apply("CELULAR"));
        vals.put("documento",   get.apply("documento"));

        // 1) reemplazos directos por clave exacta (case-insensitive)
        String out = tpl;
        for (var e : vals.entrySet()) {
            String key = e.getKey();
            String val = e.getValue() == null ? "" : e.getValue();
            out = out.replaceAll("(?i)\\{\\s*" + java.util.regex.Pattern.quote(key) + "\\s*\\}",
                    java.util.regex.Matcher.quoteReplacement(val));
        }

        // 2) sinónimos con espacios / camel
        String[][] synonyms = new String[][] {
                {"saldo mora","saldomora"},
                {"deuda total","deudatotal"}
        };
        for (String[] s : synonyms) {
            String pat = "\\{\\s*" + java.util.regex.Pattern.quote(s[0]) + "\\s*\\}";
            out = out.replaceAll("(?i)" + pat,
                    java.util.regex.Matcher.quoteReplacement(vals.getOrDefault(s[1], "")));
        }

        // 3) {ltd/ltde}: prioriza ltde si hay valor, luego ltd, luego ltd_final
        String pick = !vals.getOrDefault("ltde","").isEmpty() ? vals.get("ltde")
                : !vals.getOrDefault("ltd","").isEmpty()   ? vals.get("ltd")
                : vals.getOrDefault("ltd_final", "");
        out = out.replaceAll("(?i)\\{\\s*ltd\\s*/\\s*ltde\\s*\\}",
                java.util.regex.Matcher.quoteReplacement(pick == null ? "" : pick));

        // 4) {dia}
        out = out.replaceAll("(?i)\\{\\s*dia\\s*\\}",
                String.valueOf(java.time.LocalDate.now().getDayOfMonth()));

        return out;
    }




    private Optional<String> findTemplateByVariables(List<String> vars) {
        // Normaliza variables pedidas
        Set<String> need = vars.stream()
                .map(v -> v == null ? "" : v.toLowerCase().replaceAll("\\s+",""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        return plantillaSMSRepository.findAll().stream()
                .map(PlantillaSMS::getTemplate)
                .filter(tpl -> {
                    Set<String> inTpl = extractVarsFromTemplateText(tpl); // ya normaliza y expande {ltd/ltde} -> ltd, ltde
                    // Para compatibles, todas las 'need' deben estar presentes en 'inTpl'
                    for (String n : need) {
                        if (!inTpl.contains(n)) return false;
                    }
                    return true;
                })
                .findFirst();
    }


    @Override
    public DynamicPreviewResponse previewDynamic(DynamicQueryRequest req) {
        int limit = (req.getLimitPreview()==null || req.getLimitPreview()<1) ? 1 : req.getLimitPreview();
        var rows = smsRepository.runDynamicQuery(req, limit);

        var resp = new DynamicPreviewResponse();
        if (!rows.isEmpty()) {
            var values = rows.get(0);
            resp.setValues(values);

            String tpl = null;
            if (req.getTemplateName()!=null && !req.getTemplateName().isBlank()) {
                tpl = plantillaSMSRepository.findByName(req.getTemplateName())
                        .map(PlantillaSMS::getTemplate).orElse(null);
            } else if (req.getVariables()!=null && !req.getVariables().isEmpty()) {
                tpl = findTemplateByVariables(req.getVariables()).orElse(null);
            }

            if (tpl != null) resp.setPreviewText(renderWithValues(tpl, values));
        }
        return resp;
    }

    private String renderWithValues(String tpl, Map<String,Object> values) {
        String out = tpl;
        for (var e : values.entrySet()) {
            String key = e.getKey();
            String val = String.valueOf(e.getValue()==null ? "" : e.getValue());
            out = out.replaceAll("(?i)\\{" + java.util.regex.Pattern.quote(key) + "\\}",
                    java.util.regex.Matcher.quoteReplacement(val));
        }
        out = out.replaceAll("(?i)\\{dia\\}",
                String.valueOf(java.time.LocalDate.now().getDayOfMonth()));
        return out;
    }

    @Override
    public File exportDynamic(DynamicQueryRequest req) {

        // 0) Resolver la plantilla a usar (por nombre o inferida por variables como en preview)
        String plantillaTexto = null;
        if (req.getTemplateName() != null && !req.getTemplateName().isBlank()) {
            plantillaTexto = plantillaSMSRepository.findByName(req.getTemplateName())
                    .map(PlantillaSMS::getTemplate).orElse(null);
        } else if (req.getVariables() != null && !req.getVariables().isEmpty()) {
            plantillaTexto = findTemplateByVariables(req.getVariables()).orElse(null);
        }
        boolean canRender = plantillaTexto != null && !plantillaTexto.isBlank();

        // 1) Unir variables solicitadas con variables requeridas por la plantilla
        List<String> reqVars = (req.getVariables() == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(req.getVariables());
        // incluir variables que requiere la plantilla...
        if (canRender) {
            Set<String> needed = extractVarsFromTemplateText(plantillaTexto);
            needed.remove("dia");
            for (String v : needed) {
                if (!reqVars.stream().anyMatch(x -> x.equalsIgnoreCase(v))) {
                    reqVars.add(v);
                }
            }
        }

        // SIEMPRE agregar 'capital'
        if (reqVars.stream().noneMatch(v -> "capital".equalsIgnoreCase(v))) {
            reqVars.add("capital");
        }

        // Clonar request con variables extendidas si hiciera falta
        DynamicQueryRequest req2 = new DynamicQueryRequest();
        java.util.List<String> baseVars = (req.getVariables()==null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(req.getVariables());
        if (baseVars.stream().noneMatch(v -> "capital".equalsIgnoreCase(v))) baseVars.add("capital");
        req2.setVariables(reqVars);
        req2.setVariables(baseVars);
        req2.setTramos(req.getTramos());
        req2.setDiasVenc(req.getDiasVenc());
        req2.setExcluirPromesas(req.getExcluirPromesas());
        req2.setExcluirCompromisos(req.getExcluirCompromisos());
        req2.setExcluirBlacklist(req.getExcluirBlacklist());
        req2.setTemplateName(req.getTemplateName()); // por consistencia

        req2.setAddAmount(req.getAddAmount());       // usa monto variable

        var rows = smsRepository.runDynamicQuery(req2, null); // SIN LÍMITE

        var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        var sh = wb.createSheet("Mensajes SMS");





        // Fila 1 (VARs)
        Row h2 = sh.createRow(0);
        int c = 0;
        // 1) CELULAR
        h2.createCell(c++).setCellValue("CELULAR");

        // 2) si el usuario pidió NOMBRE, primero NOMBRE
        boolean userWantsNombre = req.getVariables()!=null && req.getVariables().stream().anyMatch(v -> "nombre".equalsIgnoreCase(v));
        if (userWantsNombre) h2.createCell(c++).setCellValue("Nombre");

        // 3) CAPITAL (siempre)
        h2.createCell(c++).setCellValue("CAPITAL");

        // 4) Resto de variables del usuario (sin repetir nombre ni capital)
        for (String v : req.getVariables()) {
            String k = v.toLowerCase();
            if ("nombre".equals(k) || "capital".equals(k)) continue;
            h2.createCell(c++).setCellValue(headerFor(v));
        }

        // 5) MENSAJE (si hay plantilla)
        if (canRender) {
            h2.createCell(c++).setCellValue("MENSAJE");
        }



        // Filas de datos
        int r = 1;
        for (var rowValues : rows) {
            Row row = sh.createRow(r++);
            int j = 0;

            Object cel = rowValues.getOrDefault("celular", rowValues.getOrDefault("CELULAR", ""));
            row.createCell(j++).setCellValue(cel == null ? "" : String.valueOf(cel));

            // 2) NOMBRE si el usuario lo pidió
            if (userWantsNombre) {
                Object nombre = rowValues.get("nombre");
                row.createCell(j++).setCellValue(nombre == null ? "" : String.valueOf(nombre));
            }

            // 3) CAPITAL (siempre)
            // CAPITAL (siempre)
            Object capital = rowValues.get("capital");
            if (capital instanceof Number) {
                row.createCell(j++).setCellValue(((Number) capital).doubleValue());
            } else {
                row.createCell(j++).setCellValue(capital == null ? "" : String.valueOf(capital));
            }


            // 4) Resto de variables del usuario (sin repetir nombre/capital)
            for (String v : req.getVariables()) {
                String k = v.toLowerCase();
                if ("nombre".equals(k) || "capital".equals(k)) continue;
                Object val = rowValues.get(v);
                if (val instanceof Number) row.createCell(j++).setCellValue(((Number) val).doubleValue());
                else row.createCell(j++).setCellValue(val == null ? "" : String.valueOf(val));
            }

            // 5) MENSAJE
            if (canRender) {
                String mensaje = renderWithValuesCaseInsensitive(plantillaTexto, rowValues);
                row.createCell(j++).setCellValue(mensaje);
            }
        }

        File out = new File("Mensajes_SMS.xlsx");
        try (var fos = new java.io.FileOutputStream(out)) { wb.write(fos); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
        finally { try { wb.close(); } catch (java.io.IOException ignored) {} }
        return out;
    }

    private String headerFor(String var) {
        switch (var.toLowerCase()) {
            case "nombre": return "Nombre";
            case "capital": return "CAPITAL";
            case "baja30": return "BAJA 30";
            case "saldomora": case "saldo_mora": return "SALDO MORA";
            case "deudatotal": case "deuda_total": return "DEUDA TOTAL";
            case "ltd": return "LTD";
            case "ltde": return "LTDE";
            default: return var.toUpperCase();
        }
    }

    // QUERY DINAMICO

        private final NamedParameterJdbcTemplate jdbc;

        public PlantillaSMSServiceImpl(NamedParameterJdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public Optional<String> getTextoById(Integer id) {
            String sql = "SELECT template FROM TEST_SMS_TEMPLATE WHERE id = :id"; // <--- ajusta nombres
            try {
                String txt = jdbc.queryForObject(sql, Map.of("id", id), String.class);
                return Optional.ofNullable(txt);
            } catch (EmptyResultDataAccessException e) {
                return Optional.empty();
            }
        }




}

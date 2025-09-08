package com.foh.contacto_total_web_service.sms_template.controller;

import com.foh.contacto_total_web_service.sms_template.dto.SmsPrecheckDTO;
import com.foh.contacto_total_web_service.sms_template.service.ComboService;
import com.foh.contacto_total_web_service.sms_template.dto.CombosDTO;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "**", maxAge = 3600)
@RestController
@RequestMapping("/api/combos")
public class ComboController {

    private final ComboService service;

    public ComboController(ComboService service) {
        this.service = service;
    }

    // ---- CRUD ----
    @GetMapping
    public List<CombosDTO.Response> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<CombosDTO.Response> get(@PathVariable Integer id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Integer> create(@RequestBody CombosDTO.CreateRequest req) {
        Integer id = service.create(req);
        return ResponseEntity.ok(id);
    }

    @PutMapping
    public ResponseEntity<Integer> update(@RequestBody CombosDTO.UpdateRequest req) {
        return ResponseEntity.ok(service.update(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(service.delete(id));
    }

    // ---- Preview desde combo ----
    @PostMapping("/{id}/preview")
    public ResponseEntity<List<java.util.Map<String,Object>>> preview(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.previewFromCombo(id, limit));
    }

    // ---- Export desde combo ----
    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> export(@PathVariable Integer id) {
        // 1) valida siempre antes de exportar
        SmsPrecheckDTO.Result chk = service.precheckFromCombo(id, null);
        if (!chk.ok) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Hay " + chk.excedidos + " mensajes que superan " + chk.limite + " caracteres"
            );
        }

        String filename = "combo_" + id + "_" + java.time.LocalDate.now() + ".xlsx";

        StreamingResponseBody body = out -> {
            try {
                service.exportFromCombo(id, out);
            } catch (Exception e) {
                throw new RuntimeException("Error exportando combo " + id, e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    // ---- Precheck desde combo ----
    @PostMapping("/{id}/precheck")
    public ResponseEntity<SmsPrecheckDTO.Result> precheck(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer limit
    ) {
        SmsPrecheckDTO.Result r = service.precheckFromCombo(id, limit);
        return ResponseEntity.ok(r);
    }


}

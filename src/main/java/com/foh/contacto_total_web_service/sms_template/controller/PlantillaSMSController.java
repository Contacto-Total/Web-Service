package com.foh.contacto_total_web_service.sms_template.controller;

import com.foh.contacto_total_web_service.sms.dto.DynamicPreviewResponse;
import com.foh.contacto_total_web_service.sms.dto.DynamicQueryRequest;
import com.foh.contacto_total_web_service.sms_template.service.DynamicQueryService;
import com.foh.contacto_total_web_service.sms.dto.GenerateMessagesRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.plantillaSMS.model.PlantillaSMS;
import com.foh.contacto_total_web_service.plantillaSMS.service.PlantillaSMSService;
import com.foh.contacto_total_web_service.sms_template.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/plantillas/sms")
public class PlantillaSMSController {

    @Autowired
    private PlantillaSMSService plantillaSMSService;
    @Autowired
    private DynamicQueryService dynamicQueryService;

    @GetMapping
    public ResponseEntity<List<PlantillaSMS>> getPlantillasSMS() {
        List<PlantillaSMS> allPlantillasSMS = plantillaSMSService.getPlantillasSMS();

        return new ResponseEntity<>(allPlantillasSMS, HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<PlantillaSMS> createTemplate(@RequestBody PlantillaSMSRequest plantillaSMSRequest) {
        PlantillaSMS newTemplate = plantillaSMSService.createPlantillaSMS(plantillaSMSRequest);
        return new ResponseEntity<>(newTemplate, HttpStatus.CREATED);
    }

    @PostMapping("/generate/messages")
    public ResponseEntity<Resource> generateMessages(@RequestBody GenerateMessagesRequest generateMessagesRequest) {
        File file = plantillaSMSService.getFileByPlantillaWithData2(generateMessagesRequest);

        if (file != null && file.exists()) {
            Resource resource = new FileSystemResource(file);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    @PutMapping("/update")
    public ResponseEntity<PlantillaSMS> updateTemplate(@RequestBody PlantillaSMSToUpdateRequest plantillaSMSToUpdateRequest) {
        PlantillaSMS updatedTemplate = plantillaSMSService.updatePlantilla(plantillaSMSToUpdateRequest);
        return new ResponseEntity<>(updatedTemplate, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        plantillaSMSService.deletePlantilla(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // NUEVO

    @GetMapping("/custom-sms")
    public ResponseEntity<Resource> generateCustomSMS(
            @RequestParam boolean onlyLtde) {

        System.out.println("Inicio método getFileByCustomSMS");
        String periodo = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        File file = plantillaSMSService.getFileByCustomSMS(onlyLtde, periodo);

        if (file != null && file.exists()) {
            Resource resource = new FileSystemResource(file);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE); // 👈 obligatorio

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    // ACTRUALIZADO

    @PostMapping("/dynamic/preview")
    public ResponseEntity<DynamicPreviewResponse> preview(@RequestBody DynamicQueryRequest req) {
        var resp = plantillaSMSService.previewDynamic(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/dynamic/export")
    public ResponseEntity<Resource> export(@RequestBody DynamicQueryRequest req) {
        File file = plantillaSMSService.exportDynamic(req);
        if (file != null && file.exists()) {
            var resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return ResponseEntity.ok().headers(headers).body(resource);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    // DINAMICO


    @PostMapping("/dynamic-query")
    public List<Map<String, Object>> runDynamicQuery(@RequestBody DynamicQueryRequest1 req) {
        return dynamicQueryService.run(req);
    }

    /** Precheck para la consulta dinámica con plantilla ad-hoc. */
    @PostMapping("/precheck")
    public ResponseEntity<SmsPrecheckDTO.Result> precheck(@RequestBody SmsPrecheckDTO.DynamicRequest body) {
        if (body == null || body.query == null || body.template == null) {
            return ResponseEntity.badRequest().build();
        }

        // Correr la misma consulta que usas para export (sin límite)
        DynamicQueryRequest1 q = new DynamicQueryRequest1(
                body.query.selects(),
                body.query.tramo(),
                body.query.condiciones(),
                body.query.restricciones(),
                null,                 // limit = null para evaluar todo
                body.query.importeExtra(),
                body.query.selectAll(),
                body.query.template()
        );

        List<java.util.Map<String,Object>> rows = dynamicQueryService.run(q);
        SmsPrecheckDTO.Result res = dynamicQueryService.precheckRows(rows, body.template);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value="/export")
    public ResponseEntity<StreamingResponseBody> export(@RequestBody DynamicQueryRequest1 req) {
        String filename = "resultado_" + java.time.LocalDate.now() + ".xlsx";
        StreamingResponseBody stream = out -> dynamicQueryService.exportToExcel(req, out);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }



}

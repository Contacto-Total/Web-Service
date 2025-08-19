package com.foh.contacto_total_web_service.plantillaSMS.controller;

import com.foh.contacto_total_web_service.sms.dto.GenerateMessagesRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSRequest;
import com.foh.contacto_total_web_service.plantillaSMS.dto.PlantillaSMSToUpdateRequest;
import com.foh.contacto_total_web_service.plantillaSMS.model.PlantillaSMS;
import com.foh.contacto_total_web_service.plantillaSMS.service.PlantillaSMSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/plantillas/sms")
public class PlantillaSMSController {

    @Autowired
    private PlantillaSMSService plantillaSMSService;

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
}

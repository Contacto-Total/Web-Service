package com.foh.contacto_total_web_service.controller;

import com.foh.contacto_total_web_service.dto.CreateAudioEvaluacionFileRequest;
import com.foh.contacto_total_web_service.service.AudioEvaluacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/audio/evaluation")
public class AudioEvaluacionController {

    @Autowired
    private AudioEvaluacionService audioEvaluationService;

    @PostMapping("/create")
    public ResponseEntity<Resource> createAudioEvaluation(@RequestBody CreateAudioEvaluacionFileRequest createAudioEvaluationFileRequest) {
        File file = audioEvaluationService.createAudioEvaluationFile(createAudioEvaluationFileRequest);

        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers(headers)
                .body(resource);

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                file.delete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return response;
    }

    @PostMapping("/create/zip")
    public ResponseEntity<Resource> createAudioEvaluationsZip(@RequestBody List<CreateAudioEvaluacionFileRequest> createAudioEvaluations) {
        // Crear el archivo ZIP en el servidor
        File zipFile = audioEvaluationService.createAudioEvaluationsZip(createAudioEvaluations);

        if (zipFile == null || !zipFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Crear un recurso para el archivo ZIP
        Resource resource = new FileSystemResource(zipFile);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers(headers)
                .body(resource);

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                zipFile.delete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return response;
    }
}

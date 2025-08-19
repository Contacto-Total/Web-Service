package com.foh.contacto_total_web_service.controller;

import com.foh.contacto_total_web_service.dto.CreateAudioEvaluacionFileRequest;
import com.foh.contacto_total_web_service.dto.DownloadHistoricoAudioRequest;
import com.foh.contacto_total_web_service.dto.DownloadRecordingRequest;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.service.FtpService;
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
@RequestMapping("/api/recording")
public class FtpController {

    @Autowired
    private FtpService ftpService;

    @GetMapping
    public ResponseEntity<Resource> getRecording() {
        File file = ftpService.downloadFile();

        Resource recordingResource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ResponseEntity<Resource> recordingResponse = ResponseEntity.ok()
                .headers(headers)
                .body(recordingResource);

        return recordingResponse;
    }

    @PostMapping("/download/{name}")
    public ResponseEntity<Resource> downloadFileByName(@RequestBody DownloadRecordingRequest downloadRecordingRequest) {
        File file = ftpService.downloadFileByName(downloadRecordingRequest.getName());

        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource recordingResource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers(headers)
                .body(recordingResource);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                file.delete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return response;
    }

    @PostMapping("/range/date")
    public ResponseEntity<List<String>> getRecordingNamesPerRangeDate(@RequestBody RecordingDateRequest recordingDateRequest) {
        List<String> recordingNames = ftpService.getRecordingNamesFromDateRange(recordingDateRequest);
        return ResponseEntity.ok(recordingNames);
    }

    @PostMapping("/download/historico/audio")
    public ResponseEntity<Resource> downloadGestionHistoricaAudioFileByName(@RequestBody DownloadHistoricoAudioRequest downloadHistoricoAudioRequest) {
        File file = ftpService.downloadGestionHistoricaAudioFileByName(downloadHistoricoAudioRequest);

        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource recordingResource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers(headers)
                .body(recordingResource);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                file.delete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return response;
    }

    @PostMapping("/download/historico/audio/zip")
    public ResponseEntity<Resource> downloadGestionHistoricaAudiosZip(@RequestBody List<DownloadHistoricoAudioRequest> downloadHistoricoAudioRequests) {
        // Crear el archivo ZIP en el servidor
        File zipFile = ftpService.downloadGestionHistoricaAudiosZip(downloadHistoricoAudioRequests);

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

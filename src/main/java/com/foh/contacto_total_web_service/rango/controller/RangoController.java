package com.foh.contacto_total_web_service.rango.controller;

import com.foh.contacto_total_web_service.rango.dto.GetRangosByRangesAndGenerateFileRequest;
import com.foh.contacto_total_web_service.rango.service.RangoService;
import com.foh.contacto_total_web_service.reporte.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/rangos")
public class RangoController {

    @Autowired
    private RangoService rangoService;

    @Autowired
    private ReporteService reporteService;

    @PostMapping("/consulta")
    public ResponseEntity<Resource> getRangosByRangesAndGenerateFile(@RequestBody GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest) {
        File file1 = rangoService.getRangosByRangesAndGenerateFile(getRangosByRangesAndGenerateFileRequest);
        File file2 = reporteService.getReporteByRangesAndGenerateFile(getRangosByRangesAndGenerateFileRequest);

        if (file1 != null && file1.exists() && file2 != null && file2.exists()) {
            File zipFile = new File("rangos_reportes.zip");
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                addToZipFile(file1, zipOut);
                addToZipFile(file2, zipOut);
            } catch (IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            Resource resource = new FileSystemResource(zipFile);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            ResponseEntity<Resource> response = ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    file1.delete();
                    file2.delete();
                    zipFile.delete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            return response;
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void addToZipFile(File file, ZipOutputStream zipOut) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.closeEntry();
        }
    }
}

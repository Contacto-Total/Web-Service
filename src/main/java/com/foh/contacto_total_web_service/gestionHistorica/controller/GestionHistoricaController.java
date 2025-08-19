package com.foh.contacto_total_web_service.gestionHistorica.controller;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.gestionHistorica.service.GestionHistoricaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/gestion/historica")
public class GestionHistoricaController {

    @Autowired
    private GestionHistoricaService gestionHistoricaService;

    @PostMapping("/date/range")
    public ResponseEntity<List<GestionHistoricaResponse>> getGestionHistoricaByDateRange(@RequestBody RecordingDateRequest recordingDateRequest) {
        List<GestionHistoricaResponse> gestionHistoricaResponse = gestionHistoricaService.getGestionHistoricaByDateRange(recordingDateRequest);

        return new ResponseEntity<>(gestionHistoricaResponse, HttpStatus.OK);
    }
}

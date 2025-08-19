package com.foh.contacto_total_web_service.controller;

import com.foh.contacto_total_web_service.dto.GestionHistoricaAudiosDocumentoRequest;
import com.foh.contacto_total_web_service.dto.GestionHistoricaAudiosResponse;
import com.foh.contacto_total_web_service.dto.GestionHistoricaAudiosTelefonoRequest;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.service.GestionHistoricaAudiosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/gestion/historica/audios")
public class GestionHistoricaAudiosController {

    @Autowired
    GestionHistoricaAudiosService gestionHistoricaAudiosService;

    @PostMapping("/date/range")
    public ResponseEntity<List<GestionHistoricaAudiosResponse>> getGestionHistoricaByDateRange(@RequestBody RecordingDateRequest recordingDateRequest) {
        List<GestionHistoricaAudiosResponse> gestionHistoricaAudiosResponse = gestionHistoricaAudiosService.getGestionHistoricaAudiosByDateRange(recordingDateRequest);

        return new ResponseEntity<>(gestionHistoricaAudiosResponse, HttpStatus.OK);
    }

    @PostMapping("/documento")
    public ResponseEntity<List<GestionHistoricaAudiosResponse>> getGestionHistoricaByDocumento(@RequestBody GestionHistoricaAudiosDocumentoRequest gestionHistoricaAudiosDocumentoRequest) {
        List<GestionHistoricaAudiosResponse> gestionHistoricaAudiosResponse = gestionHistoricaAudiosService.getGestionHistoricaAudiosByDocumento(gestionHistoricaAudiosDocumentoRequest.getDocumento());

        return new ResponseEntity<>(gestionHistoricaAudiosResponse, HttpStatus.OK);
    }

    @PostMapping("/telefono")
    public ResponseEntity<List<GestionHistoricaAudiosResponse>> getGestionHistoricaByTelefono(@RequestBody GestionHistoricaAudiosTelefonoRequest gestionHistoricaAudiosTelefonoRequest) {
        List<GestionHistoricaAudiosResponse> gestionHistoricaAudiosResponse = gestionHistoricaAudiosService.getGestionHistoricaAudiosByTelefono(gestionHistoricaAudiosTelefonoRequest.getTelefono());

        return new ResponseEntity<>(gestionHistoricaAudiosResponse, HttpStatus.OK);
    }
}

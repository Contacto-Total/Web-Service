package com.foh.contacto_total_web_service.gestionHistorica.controller;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaClienteResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.PageResponse;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.gestionHistorica.service.GestionHistoricaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/gestion/historica")
@Tag(name = "Gestión Histórica", description = "Endpoints para consultar gestiones históricas")
public class GestionHistoricaController {

    @Autowired
    private GestionHistoricaService gestionHistoricaService;

    @PostMapping("/date/range")
    public ResponseEntity<List<GestionHistoricaResponse>> getGestionHistoricaByDateRange(@RequestBody RecordingDateRequest recordingDateRequest) {
        List<GestionHistoricaResponse> gestionHistoricaResponse = gestionHistoricaService.getGestionHistoricaByDateRange(recordingDateRequest);

        return new ResponseEntity<>(gestionHistoricaResponse, HttpStatus.OK);
    }

    @Operation(summary = "Obtener gestiones históricas de un cliente",
            description = "Retorna las gestiones históricas de un cliente paginadas")
    @GetMapping("/cliente/{documento}")
    public ResponseEntity<PageResponse<GestionHistoricaClienteResponse>> getGestionesByDocumento(
            @Parameter(description = "Documento del cliente") @PathVariable String documento,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size) {

        PageResponse<GestionHistoricaClienteResponse> response = gestionHistoricaService.getGestionesByDocumento(documento, page, size);

        return ResponseEntity.ok(response);
    }
}

package com.foh.contacto_total_web_service.gestionHistorica.service;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaClienteResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.PageResponse;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;

import java.util.List;

public interface GestionHistoricaService {
    List<GestionHistoricaResponse> getGestionHistoricaByDateRange(RecordingDateRequest recordingDateRequest);

    PageResponse<GestionHistoricaClienteResponse> getGestionesByDocumento(String documento, int page, int size);
}

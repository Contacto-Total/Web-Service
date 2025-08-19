package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;

import java.util.List;

public interface GestionHistoricaService {
    public abstract List<GestionHistoricaResponse> getGestionHistoricaByDateRange(RecordingDateRequest recordingDateRequest);
}

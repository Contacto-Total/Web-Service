package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.GestionHistoricaAudiosResponse;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;

import java.util.List;

public interface GestionHistoricaAudiosService {
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(RecordingDateRequest recordingDateRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(String documento);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(String telefono);
}

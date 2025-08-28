package com.foh.contacto_total_web_service.gestionHistoricaAudios.service;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.GestionHistoricaAudiosResponse;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.GestionHistoricaAudiosTramoRequest;

import java.util.List;

public interface GestionHistoricaAudiosService {
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTramo(GestionHistoricaAudiosTramoRequest gestionHistoricaAudiosTramoRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(RecordingDateRequest recordingDateRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(String documento);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(String telefono);
}

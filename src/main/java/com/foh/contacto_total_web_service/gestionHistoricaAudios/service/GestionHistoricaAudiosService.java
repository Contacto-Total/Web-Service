package com.foh.contacto_total_web_service.gestionHistoricaAudios.service;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.*;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;

import java.util.List;

public interface GestionHistoricaAudiosService {
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTramo(GestionHistoricaAudiosTramoRequest gestionHistoricaAudiosTramoRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(GestionHistoricaAudiosDateRangeRequest recordingDateRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(GestionHistoricaAudiosDocumentoRequest documentoRequest);
    public abstract List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(GestionHistoricaAudiosTelefonoRequest telefono);
}

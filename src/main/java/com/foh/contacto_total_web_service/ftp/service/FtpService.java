package com.foh.contacto_total_web_service.ftp.service;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.DownloadHistoricoAudioRequest;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

public interface FtpService {
    public abstract File downloadFile();
    @Transactional
    public abstract List<String> getRecordingNamesFromDateRange(RecordingDateRequest recordingDateRequest);
    public abstract File downloadFileByName(String name);
    public abstract File downloadGestionHistoricaAudioFileByName(DownloadHistoricoAudioRequest downloadHistoricoAudioRequest);
    public abstract File downloadGestionHistoricaAudiosZip(List<DownloadHistoricoAudioRequest> downloadHistoricoAudioRequests);
}
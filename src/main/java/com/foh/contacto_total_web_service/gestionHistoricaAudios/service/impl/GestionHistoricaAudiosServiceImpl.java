package com.foh.contacto_total_web_service.gestionHistoricaAudios.service.impl;

import com.foh.contacto_total_web_service.gestionHistoricaAudios.dto.*;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.gestionHistoricaAudios.repository.GestionHistoricaAudiosRepository;
import com.foh.contacto_total_web_service.gestionHistoricaAudios.service.GestionHistoricaAudiosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GestionHistoricaAudiosServiceImpl implements GestionHistoricaAudiosService {

    @Autowired
    GestionHistoricaAudiosRepository gestionHistoricaAudiosRepository;

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTramo(GestionHistoricaAudiosTramoRequest gestionHistoricaAudiosTramoRequest) {
        String tramo = gestionHistoricaAudiosTramoRequest.getTramo();
        if (tramo == null) {
            throw new IllegalArgumentException("El tramo no puede ser nulo");
        }
        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByTramo(tramo);
    }

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(GestionHistoricaAudiosDateRangeRequest recordingDateRequest) {
        LocalDate startDate = recordingDateRequest.getStartDate();
        LocalDate endDate = recordingDateRequest.getEndDate();
        String tramo = recordingDateRequest.getTramo();

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin no pueden ser nulas");
        }

        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByDateRange(recordingDateRequest);
    }

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(GestionHistoricaAudiosDocumentoRequest documentoRequest) {
        if (documentoRequest.getDocumento() == null) {
            throw new IllegalArgumentException("El documento no puede ser nulo");
        }

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByDocumento(documentoRequest);
    }

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(GestionHistoricaAudiosTelefonoRequest telefono) {
        if (telefono == null) {
            throw new IllegalArgumentException("El telefono no puede ser nulo");
        }

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByTelefono(telefono);
    }
}

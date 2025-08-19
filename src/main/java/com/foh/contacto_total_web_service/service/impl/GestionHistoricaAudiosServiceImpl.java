package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.GestionHistoricaAudiosResponse;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.repository.GestionHistoricaAudiosRepository;
import com.foh.contacto_total_web_service.service.GestionHistoricaAudiosService;
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
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDateRange(RecordingDateRequest recordingDateRequest) {
        LocalDate startDate = recordingDateRequest.getStartDate();
        LocalDate endDate = recordingDateRequest.getEndDate();

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin no pueden ser nulas");
        }

        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByDateRange(startDateStr, endDateStr);
    }

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByDocumento(String documento) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento no puede ser nulo");
        }

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByDocumento(documento);
    }

    @Override
    public List<GestionHistoricaAudiosResponse> getGestionHistoricaAudiosByTelefono(String telefono) {
        if (telefono == null) {
            throw new IllegalArgumentException("El telefono no puede ser nulo");
        }

        return gestionHistoricaAudiosRepository.getGestionHistoricaAudiosByTelefono(telefono);
    }
}

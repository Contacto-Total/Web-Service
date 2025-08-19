package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.repository.GestionHistoricaRepository;
import com.foh.contacto_total_web_service.service.GestionHistoricaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GestionHistoricaServiceImpl implements GestionHistoricaService {

    @Autowired
    GestionHistoricaRepository gestionHistoricaRepository;

    @Override
    public List<GestionHistoricaResponse> getGestionHistoricaByDateRange(RecordingDateRequest recordingDateRequest) {
        LocalDate startDate = recordingDateRequest.getStartDate();
        LocalDate endDate = recordingDateRequest.getEndDate();

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin no pueden ser nulas");
        }

        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return gestionHistoricaRepository.getGestionHistoricaByDateRange(startDateStr, endDateStr);
    }
}

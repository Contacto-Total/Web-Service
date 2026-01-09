package com.foh.contacto_total_web_service.gestionHistorica.service.impl;

import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaClienteResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.GestionHistoricaResponse;
import com.foh.contacto_total_web_service.gestionHistorica.dto.PageResponse;
import com.foh.contacto_total_web_service.ftp.dto.RecordingDateRequest;
import com.foh.contacto_total_web_service.gestionHistorica.repository.GestionHistoricaRepository;
import com.foh.contacto_total_web_service.gestionHistorica.service.GestionHistoricaService;
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

    @Override
    public PageResponse<GestionHistoricaClienteResponse> getGestionesByDocumento(String documento, int page, int size) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("El documento no puede ser nulo o vacío");
        }

        List<GestionHistoricaClienteResponse> content = gestionHistoricaRepository.getGestionesByDocumento(documento, page, size);
        long totalElements = gestionHistoricaRepository.countGestionesByDocumento(documento);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<GestionHistoricaClienteResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}

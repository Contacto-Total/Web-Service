package com.foh.contacto_total_web_service.ftp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordingDateRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}

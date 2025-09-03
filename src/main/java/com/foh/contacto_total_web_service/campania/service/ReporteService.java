package com.foh.contacto_total_web_service.campania.service;

import com.foh.contacto_total_web_service.campania.dto.GetFiltersToGenerateFileRequest;

import java.io.File;

public interface ReporteService {
    public abstract File getReporteByRangesAndGenerateFile(GetFiltersToGenerateFileRequest getRangosByRangesAndGenerateFileRequest);
}

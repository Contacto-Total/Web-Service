package com.foh.contacto_total_web_service.reporte.service;

import com.foh.contacto_total_web_service.rango.dto.GetRangosByRangesAndGenerateFileRequest;

import java.io.File;

public interface ReporteService {
    public abstract File getReporteByRangesAndGenerateFile(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest);
}

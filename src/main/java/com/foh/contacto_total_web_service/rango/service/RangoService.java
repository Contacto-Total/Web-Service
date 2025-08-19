package com.foh.contacto_total_web_service.rango.service;

import com.foh.contacto_total_web_service.rango.dto.GetRangosByRangesAndGenerateFileRequest;

import java.io.File;

public interface RangoService {
    public abstract File getRangosByRangesAndGenerateFile(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest);
}

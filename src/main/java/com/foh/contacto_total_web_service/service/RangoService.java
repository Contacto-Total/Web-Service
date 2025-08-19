package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.GetRangosByRangesAndGenerateFileRequest;
import com.foh.contacto_total_web_service.dto.RangoRequest;

import java.io.File;
import java.util.List;

public interface RangoService {
    public abstract File getRangosByRangesAndGenerateFile(GetRangosByRangesAndGenerateFileRequest getRangosByRangesAndGenerateFileRequest);
}

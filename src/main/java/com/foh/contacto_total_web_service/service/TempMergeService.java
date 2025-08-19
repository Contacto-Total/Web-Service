package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.TempMergeResponse;

public interface TempMergeService {
    public abstract TempMergeResponse getEmailAndTelefonoByDocumentoInTempMerge(String entidad, String documento);
}

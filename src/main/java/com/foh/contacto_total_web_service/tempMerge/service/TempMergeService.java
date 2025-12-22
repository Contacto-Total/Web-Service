package com.foh.contacto_total_web_service.tempMerge.service;

import com.foh.contacto_total_web_service.tempMerge.dto.TempMergeResponse;

public interface TempMergeService {
    public abstract TempMergeResponse getEmailAndTelefonoByDocumentoInTempMerge(String entidad, String documento);
    public abstract String getEntidadByDocumento(String documento);
}

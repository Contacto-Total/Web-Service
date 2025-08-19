package com.foh.contacto_total_web_service.tempMerge.service.impl;

import com.foh.contacto_total_web_service.tempMerge.dto.TempMergeResponse;
import com.foh.contacto_total_web_service.tempMerge.repository.TempMergeRepository;
import com.foh.contacto_total_web_service.tempMerge.service.TempMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TempMergeServiceImpl implements TempMergeService {

    @Autowired
    TempMergeRepository tempMergeRepository;

    @Override
    public TempMergeResponse getEmailAndTelefonoByDocumentoInTempMerge(String entidad, String documento) {
        return tempMergeRepository.getEmailAndTelefonoByDocumentoInTempMerge(entidad, documento);
    }
}

package com.foh.contacto_total_web_service.service;

import com.foh.contacto_total_web_service.dto.CreateAudioEvaluacionFileRequest;

import java.io.File;
import java.util.List;

public interface AudioEvaluacionService {
    public abstract File createAudioEvaluationFile(CreateAudioEvaluacionFileRequest createAudioEvaluationFileRequest);
    public abstract File createAudioEvaluationsZip(List<CreateAudioEvaluacionFileRequest> createAudioEvaluations);
}

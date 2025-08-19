package com.foh.contacto_total_web_service.audioEvaluation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class UltimoAudioProcesado {
    @Id
    private Long id = 1L;
    private String anio;
    private String mes;
    private String dia;
    private String nombreAudio;
}

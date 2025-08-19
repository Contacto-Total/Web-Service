package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAudioEvaluacionFileRequest {
    private String dni;
    private String cliente;
    private String telefono;
    private String fecha;
    private String asesor;
    private String resultado;
    private Integer gestionHistoricaAudioIdx;
}

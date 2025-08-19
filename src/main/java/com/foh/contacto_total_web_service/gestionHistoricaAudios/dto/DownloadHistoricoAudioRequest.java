package com.foh.contacto_total_web_service.gestionHistoricaAudios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadHistoricoAudioRequest {
    private String anio;
    private String mes;
    private String dia;
    private String nombre;
    private String fecha;
    private String resultado;
    private String telefono;
    private String documento;
    private String cliente;
    private String asesor;
}

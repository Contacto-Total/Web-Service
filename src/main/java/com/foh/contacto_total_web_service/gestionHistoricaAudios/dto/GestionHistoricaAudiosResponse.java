package com.foh.contacto_total_web_service.gestionHistoricaAudios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GestionHistoricaAudiosResponse {
    private Integer idx;
    private String DOCUMENTO;
    private String CARTERA;
    private String CLIENTE;
    private String FECHAGESTION;
    private String HORAGESTION;
    private String TELEFONO;
    private String RESULTADO;
    private String SOLUCION;
    private String USUARIOREGISTRA;
    private String ANIO;
    private String MES;
    private String DIA;
    private String NOMBRE;
}

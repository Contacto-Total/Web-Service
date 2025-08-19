package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompromisoResponse {
    private String Item;
    private String Documento;
    private String EstadoCompromiso;
    private String TipoResultado;
    private Double ImporteCompromiso;
    private Double ImportePagoMensual;
}

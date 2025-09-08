package com.foh.contacto_total_web_service.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class DynamicPreviewRow {
    private String documento;
    private String telefonoCelular;
    private String nombre;
    private Long   baja30;
    private Long   deudaTotal;
    private Long   ltde;
    private String mensaje;
}
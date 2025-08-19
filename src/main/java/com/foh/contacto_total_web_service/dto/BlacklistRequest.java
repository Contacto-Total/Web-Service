package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistRequest {
    private String empresa;
    private String cartera;
    private String subcartera;
    private String documento;
    private String fechaFin;
    private String entidad;
}

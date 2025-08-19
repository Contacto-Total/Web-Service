package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistResponse {
    String empresa;
    String cartera;
    String subcartera;
    String documento;
    String email;
    String telefono;
    String FechaInicio;
    String fechaFin;
}

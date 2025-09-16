package com.foh.contacto_total_web_service.campania.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RangoRequest {
    private String min;
    private String max;
}

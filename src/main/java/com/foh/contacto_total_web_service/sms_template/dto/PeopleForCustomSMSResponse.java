package com.foh.contacto_total_web_service.sms_template.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleForCustomSMSResponse {

    private String documento;
    private String telefonoCelular;
    private String nombre;
    private BigDecimal ltdeFinal;
    private BigDecimal deudaTotal;
    private String remitente;
}

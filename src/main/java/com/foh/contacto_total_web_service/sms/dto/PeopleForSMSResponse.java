package com.foh.contacto_total_web_service.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleForSMSResponse {
    private String documento;
    private String telefonoCelular;
    private String nombre;
    private Long deudaTotal;
    private Long ltd;
}

package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioEvaluacionResponse {
    private Integer id;
    private String filename;
    private Integer presAsert;
    private Integer presNomApe;
    private Integer origLlamada;
    private Integer motivLlamada;
    private Integer motivNoPago;
    private Integer manejoObjec;
    private Integer opcionesPago;
    private Integer beneficioPago;
    private Integer fechaHoraPago;
    private Integer datosAdicionales;
    private Integer confirmMonto;
    private Integer confirmFecha;
    private Integer confirmCanal;
    private Integer consecPago;
    private String summary;
    private Integer gestionHistoricaAudiosId;
}

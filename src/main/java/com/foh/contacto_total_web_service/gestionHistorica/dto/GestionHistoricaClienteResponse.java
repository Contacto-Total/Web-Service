package com.foh.contacto_total_web_service.gestionHistorica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestionHistoricaClienteResponse {
    private String fechaGestion;
    private String horaGestion;
    private String agente;
    private String resultado;       // ruta_nivel_2 (normalizado: CANCELACION TOTAL → CANCELACION)
    private String solucion;        // ruta_nivel_3
    private String telefono;
    private String observacion;
    private String canal;           // LLAMADA_SALIENTE, LLAMADA_ENTRANTE, WHATSAPP
    private String metodo;          // GESTION_MANUAL, GESTION_PROGRESIVO
    private BigDecimal montoPromesa;
    private String estadoPromesa;   // PAGADA, VENCIDA, PENDIENTE
}

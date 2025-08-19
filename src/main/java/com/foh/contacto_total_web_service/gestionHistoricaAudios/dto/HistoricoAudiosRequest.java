package com.foh.contacto_total_web_service.gestionHistoricaAudios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoAudiosRequest {
    String anio;
    String mes;
    String dia;
    String nombre;
    String duracion;
    String peso;
}

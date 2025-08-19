package com.foh.contacto_total_web_service.gestionHistorica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GestionHistoricaResponse {
    String Documento;
    String Cliente;
    String Telefono;
    String FechaGestion;
    String HoraUbicacion;
    String Resultado;
    String TipoGestion;
    String Solucion;
}
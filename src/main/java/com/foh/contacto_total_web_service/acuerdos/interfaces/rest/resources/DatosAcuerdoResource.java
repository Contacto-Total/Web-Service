package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources;

public record DatosAcuerdoResource(
    String fechaActual,
    String nombreDelTitular,
    String cuentaTarjeta,
    String fechaCompromiso,
    String deudaTotal,
    String saldoCapitalAsig,
    String ltd,
    String ltde,
    String asesor,
    String observacion,
    String tramo
) {
}

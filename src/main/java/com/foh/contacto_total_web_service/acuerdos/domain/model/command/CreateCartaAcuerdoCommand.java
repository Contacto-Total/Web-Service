package com.foh.contacto_total_web_service.acuerdos.domain.model.command;

import java.util.List;

public record CreateCartaAcuerdoCommand(
    String entidad,
    String fechaActual,
    String nombreTitular,
    String dni,
    String cuentaTarjeta,
    String fechaCompromiso,
    Double deudaTotal,
    Double descuento,
    Double montoAprobado,
    List<String> formasDePago
) {
}

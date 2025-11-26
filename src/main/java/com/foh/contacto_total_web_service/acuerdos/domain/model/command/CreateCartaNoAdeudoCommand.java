package com.foh.contacto_total_web_service.acuerdos.domain.model.command;

public record CreateCartaNoAdeudoCommand(
        String entidad,
        String nombreCompleto,
        String dni,
        String numeroCuenta,
        String fechaActual,
        String fechaCancelacion,
        String rucFinanciera,
        String rucNsoluciones
) {
}

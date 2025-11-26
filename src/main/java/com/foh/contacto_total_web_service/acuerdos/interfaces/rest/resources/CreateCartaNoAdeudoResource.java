package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateCartaNoAdeudoResource(
        @NotBlank(message = "La entidad es requerida")
        String entidad,

        @NotBlank(message = "El nombre completo es requerido")
        String nombreCompleto,

        @NotBlank(message = "El DNI es requerido")
        String dni,

        @NotBlank(message = "El número de cuenta es requerido")
        String numeroCuenta,

        @NotBlank(message = "La fecha actual es requerida")
        String fechaActual,

        @NotBlank(message = "La fecha de cancelación es requerida")
        String fechaCancelacion,

        @NotBlank(message = "El RUC de Financiera es requerido")
        String rucFinanciera,

        @NotBlank(message = "El RUC de NSoluciones es requerido")
        String rucNsoluciones
) {
}

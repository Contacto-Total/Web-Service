package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.transform;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaNoAdeudoCommand;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.CreateCartaNoAdeudoResource;

public class CreateCartaNoAdeudoCommandFromResourceAssembler {
    public static CreateCartaNoAdeudoCommand toCommandFromResource(CreateCartaNoAdeudoResource resource) {
        return new CreateCartaNoAdeudoCommand(
                resource.entidad(),
                resource.nombreCompleto(),
                resource.dni(),
                resource.numeroCuenta(),
                resource.fechaActual(),
                resource.fechaCancelacion(),
                resource.rucFinanciera(),
                resource.rucNsoluciones()
        );
    }
}

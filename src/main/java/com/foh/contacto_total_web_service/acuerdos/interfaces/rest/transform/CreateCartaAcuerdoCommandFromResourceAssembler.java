package com.foh.contacto_total_web_service.acuerdos.interfaces.rest.transform;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaAcuerdoCommand;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.CreateCartaAcuerdoResource;

public class CreateCartaAcuerdoCommandFromResourceAssembler {
    public static CreateCartaAcuerdoCommand toCommandFromResource(CreateCartaAcuerdoResource resource) {
        return new CreateCartaAcuerdoCommand(
                resource.entidad(),
                resource.fechaActual(),
                resource.nombreTitular(),
                resource.dni(),
                resource.cuentaTarjeta(),
                resource.fechaCompromiso(),
                resource.deudaTotal(),
                resource.descuento(),
                resource.montoAprobado(),
                resource.formasDePago()
        );
    }
}

package com.foh.contacto_total_web_service.acuerdos.domain.services;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaNoAdeudoCommand;

import java.io.IOException;

public interface CartaNoAdeudoCommandService {
    byte[] handle(CreateCartaNoAdeudoCommand command) throws IOException;
}

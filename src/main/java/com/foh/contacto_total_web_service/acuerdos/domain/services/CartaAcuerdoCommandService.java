package com.foh.contacto_total_web_service.acuerdos.domain.services;

import com.foh.contacto_total_web_service.acuerdos.domain.model.command.CreateCartaAcuerdoCommand;

import java.io.IOException;

public interface CartaAcuerdoCommandService {
    byte[] handle(CreateCartaAcuerdoCommand command) throws IOException;
}

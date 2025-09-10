package com.foh.contacto_total_web_service.acuerdos.domain.services;

import com.foh.contacto_total_web_service.acuerdos.domain.model.queries.GetDatosByClienteQuery;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.DatosAcuerdoResource;

import java.util.Optional;

public interface CartaAcuerdoQueryService {
    Optional<DatosAcuerdoResource> handle(GetDatosByClienteQuery query);
}

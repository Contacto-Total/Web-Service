package com.foh.contacto_total_web_service.cartasCesion.domain.services;

import com.foh.contacto_total_web_service.cartasCesion.domain.model.queries.GetCartaCesionByDniQuery;
import com.foh.contacto_total_web_service.cartasCesion.interfaces.rest.resources.CartaCesionResource;

import java.util.Optional;

public interface CartaCesionQueryService {
    Optional<CartaCesionResource> handle(GetCartaCesionByDniQuery query);
}

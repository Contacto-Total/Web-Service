package com.foh.contacto_total_web_service.cartasCesion.application.internal.queryservices;

import com.foh.contacto_total_web_service.cartasCesion.domain.model.queries.GetCartaCesionByDniQuery;
import com.foh.contacto_total_web_service.cartasCesion.domain.services.CartaCesionQueryService;
import com.foh.contacto_total_web_service.cartasCesion.infrastructure.filesystem.CartaCesionFileService;
import com.foh.contacto_total_web_service.cartasCesion.interfaces.rest.resources.CartaCesionResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartaCesionQueryServiceImpl implements CartaCesionQueryService {

    @Autowired
    private CartaCesionFileService fileService;

    @Override
    public Optional<CartaCesionResource> handle(GetCartaCesionByDniQuery query) {
        Optional<String> filename = fileService.findPdfByDni(query.dni());

        if (filename.isPresent()) {
            return Optional.of(new CartaCesionResource(
                    query.dni(),
                    filename.get(),
                    "Carta de cesión encontrada"
            ));
        }

        return Optional.empty();
    }
}

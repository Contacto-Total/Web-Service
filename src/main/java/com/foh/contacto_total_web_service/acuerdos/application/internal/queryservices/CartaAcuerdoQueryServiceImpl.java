package com.foh.contacto_total_web_service.acuerdos.application.internal.queryservices;

import com.foh.contacto_total_web_service.acuerdos.domain.model.queries.GetDatosByClienteQuery;
import com.foh.contacto_total_web_service.acuerdos.domain.services.CartaAcuerdoQueryService;
import com.foh.contacto_total_web_service.acuerdos.infrastructure.persistence.jpa.repositories.CartaAcuerdoRepository;
import com.foh.contacto_total_web_service.acuerdos.interfaces.rest.resources.DatosAcuerdoResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartaAcuerdoQueryServiceImpl implements CartaAcuerdoQueryService {

    @Autowired
    private CartaAcuerdoRepository cartaAcuerdoRepository;

    @Override
    public Optional<DatosAcuerdoResource> handle(GetDatosByClienteQuery query) {
        return cartaAcuerdoRepository.findByDniAndTramo(query.dni(), query.tramo());
    }
}

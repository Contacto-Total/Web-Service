package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Token;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.TokenResource;

public class TokenResourceFromEntityAssembler {
    public static TokenResource toResourceFromEntity (Token entity) {
        return new TokenResource(entity.getToken(), entity.getUsername(), entity.getIssuedAt(), entity.getExpiresAt());
    }
}

package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntity(User user, String accessToken, String refreshToken, String tokenType) {
        return new AuthenticatedUserResource(user.getId(), user.getUsername(), accessToken, refreshToken, tokenType);
    }
}

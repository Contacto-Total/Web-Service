package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.commands.RefreshTokenCommand;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.RefreshTokenResource;

public class RefreshTokenCommandFromResourceAssembler {
    public static RefreshTokenCommand toCommandFromResource(RefreshTokenResource resource) {
        return new RefreshTokenCommand(resource.refreshToken());
    }
}

package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.commands.ValidateTokenCommand;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.ValidateTokenResource;

public class ValidateTokenCommandFromResourceAssembler {
    public static ValidateTokenCommand toCommandFromResource(ValidateTokenResource resource) {
        return new ValidateTokenCommand(resource.token());
    }
}

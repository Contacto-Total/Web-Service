package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.commands.SignUpCommand;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(resource.username(), resource.email(), resource.password(), resource.roles());
    }

}

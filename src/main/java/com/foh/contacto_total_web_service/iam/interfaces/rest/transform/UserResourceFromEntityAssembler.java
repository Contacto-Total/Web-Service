package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(user.getId(), user.getUsername());
    }
}

package com.foh.contacto_total_web_service.iam.interfaces.rest.transform;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Role;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.RoleResource;

public class RoleResourceFromEntityAssembler {
    public static RoleResource toResourceFromEntity(Role role) {
        return new RoleResource(role.getId(), role.getStringName());
    }
}

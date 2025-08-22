package com.foh.contacto_total_web_service.iam.domain.services;

import com.foh.contacto_total_web_service.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}

package com.foh.contacto_total_web_service.iam.application.internal.commandservices;

import com.foh.contacto_total_web_service.iam.domain.model.commands.SeedRolesCommand;
import com.foh.contacto_total_web_service.iam.domain.model.entities.Role;
import com.foh.contacto_total_web_service.iam.domain.model.valueobjects.Roles;
import com.foh.contacto_total_web_service.iam.domain.services.RoleCommandService;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class RoleCommandServiceImpl implements RoleCommandService {
    private final RoleRepository roleRepository;

    public RoleCommandServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void handle(SeedRolesCommand command) {
        Arrays.stream(Roles.values()).forEach(role -> {
            if(!roleRepository.existsByName(role)) {
                roleRepository.save(new Role(Roles.valueOf(role.name())));
            }
        });
    }
}
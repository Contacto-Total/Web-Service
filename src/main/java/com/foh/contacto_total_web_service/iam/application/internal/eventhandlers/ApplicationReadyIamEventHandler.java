package com.foh.contacto_total_web_service.iam.application.internal.eventhandlers;

import com.foh.contacto_total_web_service.iam.domain.model.commands.SeedRolesCommand;
import com.foh.contacto_total_web_service.iam.domain.services.RoleCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class ApplicationReadyIamEventHandler {
    private final RoleCommandService roleCommandService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReadyIamEventHandler.class);


    public ApplicationReadyIamEventHandler(RoleCommandService roleCommandService) {
        this.roleCommandService = roleCommandService;
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        var applicationName = event.getApplicationContext().getId();
        LOGGER.info("Starting to verify if roles seeding is needed for {} at {}", applicationName, currentTimestamp());
        var seedRolesCommand = new SeedRolesCommand();
        roleCommandService.handle(seedRolesCommand);
        LOGGER.info("Roles seeding verification finished for {} at {}", applicationName, currentTimestamp());
    }

    private Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}

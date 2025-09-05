package com.foh.contacto_total_web_service.shared.interfaces.rest.resources;

public record ErrorResponseResource(
        String message,
        String code
) {}
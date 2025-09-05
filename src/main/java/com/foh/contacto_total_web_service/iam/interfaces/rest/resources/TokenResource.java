package com.foh.contacto_total_web_service.iam.interfaces.rest.resources;

import java.time.Instant;

public record TokenResource(String token, String username, Instant issuedAt, Instant expiresAt) {
}

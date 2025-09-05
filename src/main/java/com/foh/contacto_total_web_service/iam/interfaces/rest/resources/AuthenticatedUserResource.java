package com.foh.contacto_total_web_service.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long id, String username, String accessToken, String refreshToken, String tokenType) {

}

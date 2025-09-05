package com.foh.contacto_total_web_service.iam.interfaces.rest.resources;

public record RefreshedTokenResource(String accessToken, String refreshToken, String tokenType) {
}

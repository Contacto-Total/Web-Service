package com.foh.contacto_total_web_service.iam.interfaces.rest.resources;

import java.util.List;

public record SignUpResource(String username, String email, String password, List<String> roles) {
}

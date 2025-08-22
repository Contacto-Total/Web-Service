package com.foh.contacto_total_web_service.iam.domain.model.commands;

import java.util.List;

public record SignUpCommand(String username, String email, String password, List<String> roles) {
}

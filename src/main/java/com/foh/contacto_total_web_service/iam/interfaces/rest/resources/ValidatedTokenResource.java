package com.foh.contacto_total_web_service.iam.interfaces.rest.resources;

import java.util.Map;

public record ValidatedTokenResource(boolean valid, Map<String, Object> tokenInfo) {
}

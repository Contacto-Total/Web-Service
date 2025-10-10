package com.foh.contacto_total_web_service.sms_template.dto;

public record RangeFilter(
        String field,          // "DEUDA_TOTAL", "LTD", "PKM", etc.
        Double min,            // puede ser null
        Double max,            // puede ser null
        boolean inclusiveMin,  // true => >=, false => >
        boolean inclusiveMax   // true => <=, false => <
) {}
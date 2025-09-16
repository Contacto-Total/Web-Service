package com.foh.contacto_total_web_service.sms_template.dto;

public record Restricciones(
        boolean noContenido,
        boolean excluirPromesasPeriodoActual,
        boolean excluirCompromisos,
        boolean excluirBlacklist
) {}
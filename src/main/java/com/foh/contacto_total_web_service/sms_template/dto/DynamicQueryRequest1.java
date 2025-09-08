package com.foh.contacto_total_web_service.sms_template.dto;

import java.util.List; import java.util.Set;

public record DynamicQueryRequest1(
        List<String> selects,
        String tramo,              // "3" o "5"
        Set<String> condiciones,   // e.g., LTD_Y_LTDE, LTD_O_LTDE, BAJA30, PROMESAS_HOY, PROMESAS_MANANA, PROMESAS_ROTAS
        Restricciones restricciones,
        Integer limit,
        Integer importeExtra,
        Boolean selectAll   //PARA PRUEBAS
) {}
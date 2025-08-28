package com.foh.contacto_total_web_service.sms_template.dto;

import lombok.Data;
import java.util.List;

@Data
public class DynamicPreviewRequest {
    private List<String> variables;

    // filtros opcionales
    private Integer tramo;
    private List<Integer> vencimientoDias;
    private Boolean onlyLtde;
    private Integer limit;
    private String templateName;
}
package com.foh.contacto_total_web_service.sms_template.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DynamicPreviewResponse {
    private Map<String,Object> values;  // { nombre: "Romina", baja30: 300, deudaTotal: 7000 }
    private String previewText;         // texto renderizado con la plantilla (si se envía templateName)
}

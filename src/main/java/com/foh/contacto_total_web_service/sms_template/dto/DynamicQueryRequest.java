package com.foh.contacto_total_web_service.sms_template.dto;

import lombok.Data;
import java.util.List;

@Data
public class DynamicQueryRequest {
    private List<String> variables;     // ["nombre","baja30","deudaTotal",...]
    private List<Integer> tramos;       // ej [3] o [3,5]
    private List<Integer> diasVenc;     // ej [1,3,5,15]
    private Boolean add200;
    private Boolean onlyLtde;           // true => solo LTD especial
    private Boolean excluirPromesas;    // true => excluye PROMESAS_HISTORICO del mes actual
    private Boolean excluirCompromisos; // true => excluye COMPROMISOS
    private Boolean excluirBlacklist;   // true => excluye blacklist

    // preview
    private String templateName;        // opcional: nombre de plantilla para renderizar 1 ejemplo
    private Integer limitPreview;       // opcional: por defecto 1
}

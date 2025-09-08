package com.foh.contacto_total_web_service.sms_template.dto;

import java.util.List;

public class SmsPrecheckDTO {

    /** Para consulta dinámica: trae la query y la plantilla ad-hoc. */
    public static class DynamicRequest {
        public DynamicQueryRequest1 query;
        public String template; // texto del SMS con {VARIABLES}
    }

    /** Item de ejemplo/peor caso. */
    public static class Item {
        public String documento;
        public int len;
        public int segments;
        public String text;
    }

    /** Respuesta del precheck. */
    public static class Result {
        public boolean ok;
        public int total;
        public int excedidos;
        public int limite = 160;
        public String charset; // GSM7/UCS2 del peor caso
        public Item peor;
        public List<Item> ejemplos;
    }
}

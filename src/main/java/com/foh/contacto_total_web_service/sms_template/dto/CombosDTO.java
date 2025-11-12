package com.foh.contacto_total_web_service.sms_template.dto;

import java.util.List;
import java.util.Set;

public class CombosDTO {

    public record RangeFilter(
            String field,
            Double min,
            Double max,
            Boolean inclusiveMin,
            Boolean inclusiveMax
    ) {}

    public static class CreateRequest {
        public String name;
        public String descripcion;
        public Integer plantillaSmsId;
        public String plantillaName;   // NUEVO
        public String plantillaTexto;  // NUEVO
        public List<String> selects;
        public String tramo;
        public Set<String> condiciones;
        public Restricciones restricciones;
        public List<RangeFilter> rangos;
        public Integer importeExtra;
    }


    public static class UpdateRequest {
        public Integer id;
        public String name;
        public String descripcion;
        public Integer plantillaSmsId;
        public String  plantillaTexto;   // NUEVO: texto a guardar
        public String  plantillaName;    // NUEVO: opcional, renombrar plantilla
        public List<String> selects;
        public String tramo;
        public Set<String> condiciones;
        public Restricciones restricciones;
        public Boolean isActive;
        public List<RangeFilter> rangos;
        public Integer importeExtra;
    }

    public static class Response {
        public Integer id;
        public String name;
        public String descripcion;
        public Integer plantillaSmsId;
        public String plantillaTexto;
        public List<String> selects;
        public String tramo;
        public Set<String> condiciones;
        public Restricciones restricciones;
        public Boolean isActive;
        public java.sql.Timestamp createdAt;
        public java.sql.Timestamp updatedAt;
        public List<RangeFilter> rangos;
        public Integer importeExtra;
    }
}

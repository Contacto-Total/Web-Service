package com.foh.contacto_total_web_service.util;

import com.foh.contacto_total_web_service.dto.RangoRequest;

import java.util.List;
import java.util.stream.Collectors;

public class RangoConditionBuilder {
    public static String buildRangoConditions(List<RangoRequest> rangos, String tipoRango) {
        String variableMonto = "";

        if (tipoRango.equals("RANGO NO CONTACTADO")) {
            variableMonto = "SLDCAPITALASIG";
        } else {
            variableMonto = "SLDCAPITALASIG";
        }

        String finalVariableMonto = variableMonto;
        return rangos.stream()
                .map(rango -> {
                    String min = rango.getMin();
                    String max = rango.getMax();
                    if (max.equals("+")) {
                        return "WHEN " + finalVariableMonto +" > " + min + " THEN '" + tipoRango + " " + min + " - +'";
                    } else {
                        return "WHEN " + finalVariableMonto + " > " + min + " AND " + finalVariableMonto + " <= " + max + " THEN '" + tipoRango + " " + min + " - " + max + "'";
                    }
                })
                .collect(Collectors.joining(" ", "CASE ", " END AS RANGO"));
    }
}

package com.foh.contacto_total_web_service.campania.util;

import com.foh.contacto_total_web_service.campania.dto.RangoRequest;

import java.util.List;

/**
 * Utilidad para construir condiciones SQL de rangos monetarios
 * Genera sentencias CASE WHEN para clasificar registros por rangos de montos
 */
public class RangoConditionBuilder {

    private static final String RANGO_INFINITO = "+";

    /**
     * Construye las condiciones SQL CASE WHEN para clasificar registros por rangos
     *
     * @param rangos Lista de rangos con valores mínimos y máximos
     * @param tipoRango Prefijo para el nombre del rango (ej: "RANGO CONTACTO DIRECTO")
     * @param columnaMontos Nombre de la columna que contiene los montos a evaluar
     * @return String con la sentencia CASE WHEN completa
     *
     * Ejemplo de salida:
     * CASE
     *   WHEN SLDACTUALCONS > 1000 AND SLDACTUALCONS <= 5000 THEN 'RANGO CONTACTO DIRECTO 1000 - 5000'
     *   WHEN SLDACTUALCONS > 5000 THEN 'RANGO CONTACTO DIRECTO 5000 - +'
     * END AS RANGO
     */
    public static String buildRangoConditions(
            List<RangoRequest> rangos,
            String tipoRango,
            String columnaMontos
    ) {
        if (rangos == null || rangos.isEmpty()) {
            return "NULL AS RANGO";
        }

        StringBuilder constructorCase = new StringBuilder("CAST(CASE ");

        for (RangoRequest rango : rangos) {
            String valorMinimo = rango.getMin();
            String valorMaximo = rango.getMax();

            constructorCase.append(construirCondicionRango(
                    columnaMontos,
                    valorMinimo,
                    valorMaximo,
                    tipoRango
            ));
        }

        constructorCase.append("END AS CHAR(100)) AS RANGO");
        return constructorCase.toString();
    }

    /**
     * Construye una condición individual WHEN para un rango específico
     */
    private static String construirCondicionRango(
            String columnaMontos,
            String valorMinimo,
            String valorMaximo,
            String tipoRango
    ) {
        StringBuilder condicion = new StringBuilder();

        condicion.append("WHEN ").append(columnaMontos).append(" > ").append(valorMinimo);

        // Si el máximo es "+", es un rango abierto (sin límite superior)
        if (esRangoInfinito(valorMaximo)) {
            condicion.append(" THEN '")
                    .append(tipoRango)
                    .append(" ")
                    .append(valorMinimo)
                    .append(" - +' ");
        } else {
            // Rango cerrado con límite superior definido
            condicion.append(" AND ")
                    .append(columnaMontos)
                    .append(" <= ")
                    .append(valorMaximo)
                    .append(" THEN '")
                    .append(tipoRango)
                    .append(" ")
                    .append(valorMinimo)
                    .append(" - ")
                    .append(valorMaximo)
                    .append("' ");
        }

        return condicion.toString();
    }

    /**
     * Verifica si el valor máximo representa un rango infinito
     */
    private static boolean esRangoInfinito(String valorMaximo) {
        return RANGO_INFINITO.equals(valorMaximo);
    }
}
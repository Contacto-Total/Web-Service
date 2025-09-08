package com.foh.contacto_total_web_service.sms_template.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

public final class SmsTextUtil {
    private SmsTextUtil(){}

    // Conjunto GSM 03.38 básico + extensiones comunes (sin emojis).
    private static final Set<Integer> GSM7 =
            Set.of(
                    // Letras y números ASCII básicos
                    // Nota: para brevedad no listamos todos uno por uno; usamos un chequeo simple:
                    // 0x0A,0x0D, espacio 0x20..0x7E excepto algunos; y extensiones comunes como ^ { } \ [ ] ~ |
                    // Implementamos un validador "loose" adecuado para textos típicos en ES/LA.
            );

    private static final DateTimeFormatter D_M = DateTimeFormatter.ofPattern("d/LL");

    /** Reemplaza {CLAVES} en template con valores de row (y HOY/MANANA). */
    public static String render(String template, Map<String,Object> row) {
        if (template == null) return "";
        String t = template;

        // Fechas relativas
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);

        t = t.replace("{HOY}", hoy.format(D_M));
        t = t.replace("{MANANA}", manana.format(D_M));

        // Valores numéricos/comunes (si falta, reemplaza por vacío)
        t = replaceOne(t, "NOMBRE", row.get("NOMBRE"));
        t = replaceOne(t, "DOCUMENTO", row.get("DOCUMENTO"));
        t = replaceOne(t, "TELEFONOCELULAR", row.get("TELEFONOCELULAR"));
        t = replaceOne(t, "BAJA30", row.get("BAJA30"));
        t = replaceOne(t, "SALDO_MORA", row.get("SALDO_MORA"));
        t = replaceOne(t, "BAJA30_SALDOMORA", row.get("BAJA30_SALDOMORA"));
        t = replaceOne(t, "PKM", row.get("PKM"));
        t = replaceOne(t, "CAPITAL", row.get("CAPITAL"));
        t = replaceOne(t, "DEUDA_TOTAL", row.get("DEUDA_TOTAL"));
        t = replaceOne(t, "LTD", row.get("LTD"));
        t = replaceOne(t, "LTDE", row.get("LTDE"));
        t = replaceOne(t, "LTD_LTDE", row.get("LTD_LTDE"));

        return t;
    }

    private static String replaceOne(String text, String key, Object val) {
        String rep = "";
        if (val != null) {
            if (val instanceof Number n) rep = String.valueOf(n.longValue());
            else rep = String.valueOf(val);
        }
        return text.replace("{" + key + "}", rep);
    }

    /** Resultado de conteo de SMS. */
    public static final class Count {
        public final String charset;  // "GSM7" | "UCS2"
        public final int chars;
        public final int segments;
        public Count(String charset, int chars, int segments){
            this.charset = charset; this.chars = chars; this.segments = segments;
        }
    }

    /** Cuenta longitud y segmentos (GSM7: 160/153; UCS2: 70/67). */
    public static Count countSms(String text) {
        if (text == null) text = "";
        final boolean gsm7 = isLikelyGsm7(text);
        final int len = text.length();
        if (gsm7) {
            if (len <= 160) return new Count("GSM7", len, 1);
            int segs = (int)Math.ceil((len - 160) / 153.0) + 1;
            return new Count("GSM7", len, segs);
        } else {
            if (len <= 70) return new Count("UCS2", len, 1);
            int segs = (int)Math.ceil((len - 70) / 67.0) + 1;
            return new Count("UCS2", len, segs);
        }
    }

    /** Chequeo sencillo para GSM7 (permite ASCII común y algunos signos). */
    private static boolean isLikelyGsm7(String s) {
        for (int i = 0; i < s.length(); i++) {
            int cp = s.charAt(i);
            // Acepta: CR/LF, espacio, ASCII imprimible, y signos comunes usados en ES
            if (cp == 10 || cp == 13) continue; // \n \r
            if (cp >= 32 && cp <= 126) continue; // ASCII básico
            // Extensiones “toleradas”: € ^ { } \ [ ] ~ |
            if ("€^{}\\[]~|".indexOf(cp) >= 0) continue;
            // Cualquier otro → tratamos como UCS2
            return false;
        }
        return true;
    }
}

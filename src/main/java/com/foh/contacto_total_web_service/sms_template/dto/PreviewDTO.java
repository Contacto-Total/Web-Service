package com.foh.contacto_total_web_service.sms_template.dto;

import java.util.List;

public class PreviewDTO {

    public static record InitReq(
            DynamicQueryRequest1 query,
            List<String> candidatas // opcional; si null uso default: ["BAJA30","SALDO_MORA","PKM","CAPITAL","LTD","LTDE"]
    ) {}

    public static record CandidateCount(String variable, int filasQueResuelve) {}

    public static record PreviewItem(
            String documento,
            String nombre,
            String variableUsada,   // p.ej. "BAJA30" o "SALDO_MORA"
            Number valorUsado,      // numérico
            String sms              // render con template actual
    ) {}

    public static record InitResp(
            String sessionId,
            int total,
            int resueltas,
            int pendientes,
            List<CandidateCount> candidatas,
            List<PreviewItem> muestraPreview // hasta 10
    ) {}

    public static record ChooseReq(String sessionId, String variableElegida) {}

    public static record SkipReq(String sessionId) {}

    public static record StepResp(
            String sessionId,
            int total,
            int resueltas,
            int pendientes,
            List<CandidateCount> candidatas,
            List<PreviewItem> muestraPreview
    ) {}

    public static record DownloadReq(String sessionId) {}
}

package com.foh.contacto_total_web_service.campania.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetFiltersToGenerateFileRequest {
    String campaignName;
    Boolean content;
    String filterType; // "saldoCapital", "baja30", "baja60", "baja90"
    Boolean excluirPagadasHoy; // Excluir documentos con Estado='Pagada' en PROMESAS_HISTORICO
    List<String> dueDates;
    List<RangoRequest> directContactRanges;
    List<RangoRequest> indirectContactRanges;
    List<RangoRequest> brokenPromisesRanges;
    List<RangoRequest> notContactedRanges;
}

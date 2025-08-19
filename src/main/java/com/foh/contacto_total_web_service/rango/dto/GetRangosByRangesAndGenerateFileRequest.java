package com.foh.contacto_total_web_service.rango.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRangosByRangesAndGenerateFileRequest {
    List<RangoRequest> contactoDirectoRangos;
    List<RangoRequest> contactoIndirectoRangos;
    List<RangoRequest> promesasRotasRangos;
    List<RangoRequest> noContactadoRangos;
}

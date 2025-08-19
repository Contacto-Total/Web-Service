package com.foh.contacto_total_web_service.plantillaSMS.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaSMSToUpdateRequest {
    private Integer id;
    private String name;
    private String template;
    private List<String> tipis;
}

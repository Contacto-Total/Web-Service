package com.foh.contacto_total_web_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMessagesRequest {
    String name;
    List<String> tipis;
}

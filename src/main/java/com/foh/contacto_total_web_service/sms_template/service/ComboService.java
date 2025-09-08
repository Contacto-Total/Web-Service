package com.foh.contacto_total_web_service.sms_template.service;

import com.foh.contacto_total_web_service.plantillaSMS.service.PlantillaSMSService;
import com.foh.contacto_total_web_service.sms_template.dto.SmsPrecheckDTO;
import com.foh.contacto_total_web_service.sms_template.repository.ComboRepository;
import com.foh.contacto_total_web_service.sms_template.dto.CombosDTO;
import com.foh.contacto_total_web_service.sms_template.dto.DynamicQueryRequest1;
import com.foh.contacto_total_web_service.sms_template.dto.Restricciones;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.OutputStream;


import java.util.*;
import java.util.*;

@Service
public class ComboService {

    private final ComboRepository repo;
    private final DynamicQueryService dynamicService;
    private final PlantillaSMSService plantillaSMSService;

    public ComboService(ComboRepository repo, DynamicQueryService dynamicService,
                        PlantillaSMSService plantillaSMSService) {
        this.repo = repo;
        this.dynamicService = dynamicService;
        this.plantillaSMSService = plantillaSMSService;
    }

    // --------- CRUD ----------
    public List<CombosDTO.Response> list() {
        return repo.findAll();
    }

    public Optional<CombosDTO.Response> get(Integer id) {
        return repo.findById(id);
    }

    public Integer create(CombosDTO.CreateRequest req) {
        // defaults
        if (req.restricciones == null) {
            req.restricciones = new Restricciones(false, false, false);
        }

        // El repository se encarga de crear la plantilla si hace falta
        return repo.insert(req);
    }



    public int update(CombosDTO.UpdateRequest req) {
        // defaults
        if (req.restricciones == null) req.restricciones = new Restricciones(false,false,false);
        if (req.isActive == null) req.isActive = Boolean.TRUE;

        // 1) si trae texto, persiste en plantillasms
        if (req.plantillaTexto != null && !req.plantillaTexto.isBlank()) {
            // si no hay plantilla, crea una y úsala
            if (req.plantillaSmsId == null) {
                String nombrePlantilla = (req.plantillaName != null && !req.plantillaName.isBlank())
                        ? req.plantillaName
                        : (req.name != null && !req.name.isBlank() ? req.name : "Plantilla combo " + req.id);
                Integer nuevaId = repo.insertPlantilla(nombrePlantilla, req.plantillaTexto);
                req.plantillaSmsId = nuevaId;
            } else {
                // actualiza la existente (renombra si mandas plantillaName)
                repo.updatePlantilla(req.plantillaSmsId,
                        (req.plantillaName != null && !req.plantillaName.isBlank()) ? req.plantillaName : null,
                        req.plantillaTexto);
            }
        }

        // 2) si name viene vacío pero ya tenemos plantilla, puedes tomar el nombre de la plantilla
        if ((req.name == null || req.name.isBlank()) && req.plantillaSmsId != null) {
            repo.getPlantillaNameById(req.plantillaSmsId).ifPresent(n -> req.name = n);
        }

        // 3) actualiza combo
        return repo.update(req);
    }

    public int delete(Integer id) {
        return repo.delete(id);
    }

    // --------- Preview / Export construidos desde un combo ----------
    public List<Map<String, Object>> previewFromCombo(Integer comboId, Integer limit) {
        var combo = repo.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Combo id=" + comboId + " no existe"));

        var req = toDynReq(combo, (limit == null ? 1000 : limit));  // default 1000
        return dynamicService.run(req);
    }

    public void exportFromCombo(Integer comboId, OutputStream out) throws Exception {
        var combo = repo.findById(comboId)
                .orElseThrow(() -> new IllegalArgumentException("Combo id=" + comboId + " no existe"));

        var req = toDynReq(combo, null); // null => sin límite
        dynamicService.exportToExcel(req, out);
    }

    // --------- helper para construir la consulta dinámica ----------
    private DynamicQueryRequest1 toDynReq(CombosDTO.Response combo, Integer limit) {
        return new DynamicQueryRequest1(
                combo.selects,
                combo.tramo,
                combo.condiciones,
                combo.restricciones,
                limit,
                null,
                null
        );
    }

    // Obtén el texto de plantilla: si el combo trae plantillaTexto úsalo; si no, busca por plantillaSmsId.
    private String resolveTemplateText(CombosDTO.Response combo) {
        if (combo.plantillaTexto != null && !combo.plantillaTexto.isBlank()) return combo.plantillaTexto;
        if (combo.plantillaSmsId != null) {
            // TODO: reemplaza por tu forma real de leer el texto de plantilla
            return plantillaSMSService.getTextoById(combo.plantillaSmsId)  // <-- implementa en tu repo si no existe
                    .orElse("");
        }
        return "";
    }

    /** Precheck para combos existentes. */
    public SmsPrecheckDTO.Result precheckFromCombo(Integer id, Integer limit) {
        var comboOpt = this.get(id); // ya lo tienes en tu servicio
        var combo = comboOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo no encontrado"));

        String template = resolveTemplateText(combo);
        if (template.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El combo no tiene plantilla de SMS");
        }

        var rows = this.previewFromCombo(id, limit); // ya existe
        return dynamicService.precheckRows(rows, template);
    }

}

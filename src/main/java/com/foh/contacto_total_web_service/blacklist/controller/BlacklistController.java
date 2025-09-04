package com.foh.contacto_total_web_service.blacklist.controller;

import com.foh.contacto_total_web_service.blacklist.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.blacklist.dto.BlacklistResponse;
import com.foh.contacto_total_web_service.tempMerge.dto.TempMergeResponse;
import com.foh.contacto_total_web_service.blacklist.service.BlacklistService;
import com.foh.contacto_total_web_service.tempMerge.service.TempMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/blacklist", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlacklistController {

    @Autowired
    BlacklistService blacklistService;

    @Autowired
    TempMergeService tempMergeService;

    @GetMapping("/all")
    public List<BlacklistResponse> getBlacklist() {
        return blacklistService.getBlacklist();
    }

    @PostMapping
    public ResponseEntity<String> insertBlacklist(@RequestBody BlacklistRequest blacklist) {
        TempMergeResponse tempMergeTemp = tempMergeService.getEmailAndTelefonoByDocumentoInTempMerge(blacklist.getEntidad(), blacklist.getDocumento());

        if (tempMergeTemp != null) {
            blacklistService.insertBlacklist(blacklist, tempMergeTemp.getEMAIL(), tempMergeTemp.getTELEFONOCELULAR());
            return ResponseEntity.ok("Registro insertado correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El documento no existe en la cartera seleccionada");
        }
    }
}

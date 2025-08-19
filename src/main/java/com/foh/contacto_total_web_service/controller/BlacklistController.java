package com.foh.contacto_total_web_service.controller;

import com.foh.contacto_total_web_service.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.dto.BlacklistResponse;
import com.foh.contacto_total_web_service.dto.TempMergeResponse;
import com.foh.contacto_total_web_service.service.BlacklistService;
import com.foh.contacto_total_web_service.service.TempMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "**" , maxAge = 3600)
@RestController
@RequestMapping("/api/blacklist")
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

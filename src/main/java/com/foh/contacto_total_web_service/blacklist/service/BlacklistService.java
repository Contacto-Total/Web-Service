package com.foh.contacto_total_web_service.blacklist.service;

import com.foh.contacto_total_web_service.blacklist.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.blacklist.dto.BlacklistResponse;

import java.util.List;

public interface BlacklistService {
    public abstract List<BlacklistResponse> getBlacklist();
    public abstract void insertBlacklist(BlacklistRequest blacklist, String email, String telefono);
}

package com.foh.contacto_total_web_service.blacklist.service.impl;

import com.foh.contacto_total_web_service.blacklist.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.blacklist.dto.BlacklistResponse;
import com.foh.contacto_total_web_service.blacklist.repository.BlacklistRepository;
import com.foh.contacto_total_web_service.blacklist.service.BlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BlacklistServiceImpl implements BlacklistService {

    @Autowired
    BlacklistRepository blacklistRepository;

    @Override
    public List<BlacklistResponse> getBlacklist() {
        return blacklistRepository.getAll();
    }

    @Override
    @Transactional
    public void insertBlacklist(BlacklistRequest blacklist, String email, String telefono) {
        blacklistRepository.insert(blacklist, email, telefono);
    }
}

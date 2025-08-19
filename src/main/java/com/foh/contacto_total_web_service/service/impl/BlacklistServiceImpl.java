package com.foh.contacto_total_web_service.service.impl;

import com.foh.contacto_total_web_service.dto.BlacklistRequest;
import com.foh.contacto_total_web_service.dto.BlacklistResponse;
import com.foh.contacto_total_web_service.repository.BlacklistRepository;
import com.foh.contacto_total_web_service.service.BlacklistService;
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

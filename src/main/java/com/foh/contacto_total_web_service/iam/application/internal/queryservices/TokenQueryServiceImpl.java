package com.foh.contacto_total_web_service.iam.application.internal.queryservices;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Token;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetAllTokensByUsernameQuery;
import com.foh.contacto_total_web_service.iam.domain.services.TokenQueryService;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.TokenRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenQueryServiceImpl implements TokenQueryService {

    private final TokenRepository tokenRepository;

    public TokenQueryServiceImpl(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public List<Token> handle(GetAllTokensByUsernameQuery query) {
        return tokenRepository.findAll().stream()
                .filter(t -> t.getUsername().equals(query.username()) && !t.isRevoked())
                .toList();
    }
}

package com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {
    Optional<Token> findByToken(String token);

}
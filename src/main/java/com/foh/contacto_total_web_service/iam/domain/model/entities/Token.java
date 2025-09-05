package com.foh.contacto_total_web_service.iam.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "TOKENS")
@Getter
@Setter
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private String username;

    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked = false;

    public Token() {}

    public Token(String token, String username, Instant issuedAt, Instant expiresAt) {
        this.token = token;
        this.username = username;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

}
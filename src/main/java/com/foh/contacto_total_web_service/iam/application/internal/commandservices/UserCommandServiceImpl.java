package com.foh.contacto_total_web_service.iam.application.internal.commandservices;

import com.foh.contacto_total_web_service.iam.application.internal.outboundservices.hashing.HashingService;
import com.foh.contacto_total_web_service.iam.application.internal.outboundservices.tokens.TokenService;
import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.domain.model.commands.SignInCommand;
import com.foh.contacto_total_web_service.iam.domain.model.commands.SignUpCommand;
import com.foh.contacto_total_web_service.iam.domain.services.UserCommandService;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public UserCommandServiceImpl(UserRepository userRepository, HashingService hashingService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            throw new RuntimeException("Username already exists");
        if (userRepository.existsByEmail(command.email()))
            throw new RuntimeException("Email already exists");

        var user = new User(command.username(), command.email(), hashingService.encode(command.password()));
        userRepository.save(user);
        return userRepository.findByEmail(command.email());
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty()) throw new RuntimeException("User not found");
        if (!hashingService.matches(command.password(), user.get().getPassword()))
            throw new RuntimeException("Invalid password");
        var token = tokenService.generateToken(user.get().getUsername());
        return Optional.of(ImmutablePair.of(user.get(), token));
    }
}

package com.foh.contacto_total_web_service.iam.domain.services;

import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.domain.model.commands.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Map;
import java.util.Optional;

public interface UserCommandService {
    Optional<User> handle(SignUpCommand command);
    Optional<ImmutablePair<User, Map<String, String>>>  handle(SignInCommand command);
    Optional<ImmutablePair<String, Map<String, String>>> handle(RefreshTokenCommand command);
    Optional<Map<String, Object>> handle(ValidateTokenCommand command);
    void handle(SignOutCommand command);

}

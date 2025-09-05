package com.foh.contacto_total_web_service.iam.application.internal.commandservices;

import com.foh.contacto_total_web_service.iam.application.internal.outboundservices.hashing.HashingService;
import com.foh.contacto_total_web_service.iam.application.internal.outboundservices.tokens.TokenService;
import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.domain.model.commands.*;
import com.foh.contacto_total_web_service.iam.domain.model.entities.Role;
import com.foh.contacto_total_web_service.iam.domain.model.valueobjects.Roles;
import com.foh.contacto_total_web_service.iam.domain.services.UserCommandService;
import com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.foh.contacto_total_web_service.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(UserCommandServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HashingService hashingService;

    @Autowired
    private TokenServiceImpl jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public Optional<User> handle(SignUpCommand command) {
        logger.info("Processing sign up for user: {} ", command.email());

        // Validaciones de negocio
        if (userRepository.existsByUsername(command.username())) {
            logger.warn("Username already exists: {}", command.username());
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(command.email())) {
            logger.warn("Email already exists: {}", command.email());
            throw new RuntimeException("Email already exists");
        }

        // Validar formato de email
        if (!isValidEmail(command.email())) {
            logger.warn("Invalid email format: {}", command.email());
            throw new RuntimeException("Invalid email format");
        }

        // Validar fortaleza de contraseña
        if (!isValidPassword(command.password())) {
            logger.warn("Password does not meet requirements for user: {}", command.email());
            throw new RuntimeException("Password must be at least 8 characters long and contain uppercase, lowercase, number and special character");
        }

        try {
            // Procesar roles
            List<Role> userRoles = processRoles(command.roles());

            // Crear usuario
            var user = new User(
                    command.username(),
                    command.email(),
                    passwordEncoder.encode(command.password()),
                    userRoles
            );

            User savedUser = userRepository.save(user);
            logger.info("User created successfully with ID: {}", savedUser.getId());

            return Optional.of(savedUser);

        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    @Override
    public Optional<ImmutablePair<User, Map<String, String>>> handle(SignInCommand command) {
        logger.info("Processing sign in for email: {}", command.email());

        try {
            User user = userRepository.findByEmail(command.email())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),  // o user.getEmail() si tu UserDetails así lo espera
                            command.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("tokenType", "Bearer");

            return Optional.of(ImmutablePair.of(user, tokens));

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user: {}", command.email());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during authentication: {}", e.getMessage(), e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    @Override
    public Optional<ImmutablePair<String, Map<String, String>>> handle(RefreshTokenCommand command) {
        if (!jwtService.validateToken(command.token()) || !jwtService.isRefreshToken(command.token())) {
            return Optional.empty();
        }

        String username = jwtService.extractUsername(command.token());

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username,
                "",
                Collections.emptyList()
        );

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", command.token());
        tokens.put("tokenType", "Bearer");

        return Optional.of(ImmutablePair.of(username, tokens));
    }

    public Optional<Map<String, Object>> handle(ValidateTokenCommand command) {
        if (!jwtService.validateToken(command.token())) {
            return Optional.empty();
        }
        return Optional.of(jwtService.getTokenInfo(command.token()));
    }


    @Override
    public void handle(SignOutCommand command) {
        if (command.rawToken() != null && command.rawToken().startsWith("Bearer ")) {
            String token = command.rawToken().substring(7);
            jwtService.revokeToken(token);
        }

        SecurityContextHolder.clearContext();
    }

    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public void updateUserPassword(Long userId, String currentPassword, String newPassword) {
        logger.info("Updating password for user ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!isValidPassword(newPassword)) {
            throw new RuntimeException("New password does not meet requirements");
        }

        // Usar reflection para actualizar la contraseña (ya que User no tiene setter público)
        try {
            var passwordField = User.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(user, passwordEncoder.encode(newPassword));

            userRepository.save(user);
            logger.info("Password updated successfully for user ID: {}", userId);

        } catch (Exception e) {
            logger.error("Error updating password for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update password");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void assignRoleToUser(Long userId, String roleName) {
        logger.info("Assigning role {} to user ID: {}", roleName, userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var role = roleRepository.findByName(Roles.valueOf(roleName))
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.addRole(role);
        userRepository.save(user);

        logger.info("Role {} assigned successfully to user ID: {}", roleName, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public void deactivateUser(Long userId) {
        logger.info("Deactivating user ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Implementar lógica de desactivación
        // Por ejemplo, agregar un campo 'active' al User entity

        logger.info("User ID: {} deactivated successfully", userId);
    }

    private List<Role> processRoles(List<String> roleNames) {
        List<Role> roles = new ArrayList<>();

        if (roleNames == null || roleNames.isEmpty()) {
            // Asignar rol por defecto
            roles.add(roleRepository.findByName(Roles.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found")));
        } else {
            for (String roleName : roleNames) {
                try {
                    Roles roleEnum = Roles.valueOf(roleName);
                    Role role = roleRepository.findByName(roleEnum)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid role name: {}", roleName);
                    throw new RuntimeException("Invalid role: " + roleName);
                }
            }
        }

        return roles;
    }

    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }

        // Verificar que contenga al menos: 1 mayúscula, 1 minúscula, 1 número, 1 carácter especial
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$");
    }

    public String getCurrentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    public Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }
}
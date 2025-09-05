package com.foh.contacto_total_web_service.iam.infrastructure.authorization.utils;

import com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class SecurityUtils {

    /**
     * Obtiene el usuario autenticado actual
     */
    public static Optional<UserDetailsImpl> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return Optional.of(userDetails);
        }

        return Optional.empty();
    }

    /**
     * Obtiene el ID del usuario autenticado actual
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(UserDetailsImpl::getId);
    }

    /**
     * Obtiene el email del usuario autenticado actual
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUser().map(UserDetailsImpl::getEmail);
    }

    /**
     * Obtiene el username del usuario autenticado actual
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(UserDetailsImpl::getUsername);
    }

    /**
     * Verifica si el usuario actual tiene un rol específico
     */
    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(UserDetailsImpl::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority -> authority.equals("ROLE_" + role) || authority.equals(role)))
                .orElse(false);
    }

    /**
     * Verifica si el usuario actual tiene alguno de los roles especificados
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Verifica si el usuario actual es el propietario del recurso o es administrador
     */
    public static boolean isOwnerOrAdmin(Long resourceOwnerId) {
        return getCurrentUserId()
                .map(currentUserId -> currentUserId.equals(resourceOwnerId) || isAdmin())
                .orElse(false);
    }

    /**
     * Verifica si hay un usuario autenticado
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String);
    }

    /**
     * Obtiene todas las autoridades del usuario actual
     */
    public static Optional<Collection<? extends GrantedAuthority>> getCurrentUserAuthorities() {
        return getCurrentUser().map(UserDetailsImpl::getAuthorities);
    }

    /**
     * Verifica si el usuario actual tiene una autoridad específica
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentUserAuthorities()
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(auth -> auth.equals(authority)))
                .orElse(false);
    }

    /**
     * Obtiene el contexto de seguridad actual
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Limpia el contexto de seguridad
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifica si el token actual es válido y no ha expirado
     */
    public static boolean isValidAuthentication() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getCredentials() != null;
    }
}
package com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.pipeline;

import com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.foh.contacto_total_web_service.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerAuthorizationRequestFilter.class);
    private final BearerTokenService tokenService;

    @Qualifier("defaultUserDetailsService")
    private final UserDetailsService userDetailsService;

    // Lista de rutas a excluir del filtro
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/authentication/sign-in",
            "/api/authentication/sign-up"
    );

    public BearerAuthorizationRequestFilter(BearerTokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Verifica si el path actual está en la lista excluida
        if (EXCLUDED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = tokenService.getBearerTokenFrom(request);
            if (token != null && tokenService.validateToken(token)) {
                String username = tokenService.getUsernameFromToken(token);
                var userDetails = userDetailsService.loadUserByUsername(username);
                SecurityContextHolder.getContext()
                        .setAuthentication(UsernamePasswordAuthenticationTokenBuilder
                                .build(userDetails, request));
            } else {
                LOGGER.info("Token is not valid or missing");
            }
        } catch (Exception e) {
            LOGGER.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

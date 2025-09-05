package com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.pipeline;

import com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.services.UserDetailsServiceImpl;
import com.foh.contacto_total_web_service.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BearerAuthorizationRequestFilter.class);

    @Autowired
    private TokenServiceImpl jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            logger.debug("No JWT token found in request headers for URI: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authorities for user {}: {}", username, userDetails.getAuthorities());

                    logger.debug("User '{}' authenticated successfully for URI: {}", username, requestURI);
                } else {
                    logger.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired for URI: {} - {}", requestURI, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token expired\", \"message\": \"JWT token has expired\"}");
            response.setContentType("application/json");
            return;
        } catch (Exception e) {
            logger.error("Cannot set user authentication for URI: {} - {}", requestURI, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/v1/authentication/") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/swagger-resources") ||
                requestURI.startsWith("/webjars") ||
                requestURI.equals("/swagger-ui.html") ||
                requestURI.startsWith("/actuator/health");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Additional logic to skip filtering for specific conditions
        String path = request.getServletPath();
        return path.equals("/error") ||
                path.startsWith("/static/") ||
                path.startsWith("/public/");
    }
}
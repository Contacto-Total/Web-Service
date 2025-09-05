package com.foh.contacto_total_web_service.shared.infrastructure.persistence.jpa.configuration;

import com.foh.contacto_total_web_service.iam.infrastructure.authorization.utils.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SecurityAuditorAware();
    }

    public static class SecurityAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            return SecurityUtils.getCurrentUserEmail()
                    .or(() -> Optional.of("system"));
        }
    }
}
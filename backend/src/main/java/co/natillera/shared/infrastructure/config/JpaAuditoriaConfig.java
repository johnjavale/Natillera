package co.natillera.shared.infrastructure.config;

import co.natillera.auth.application.UsuarioActual;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditor", dateTimeProviderRef = "proveedorFechaAuditoria")
public class JpaAuditoriaConfig {

    @Bean
    public AuditorAware<String> auditor() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UsuarioActual u) {
                return Optional.of(u.login());
            }
            return Optional.of("sistema");
        };
    }

    @Bean
    public DateTimeProvider proveedorFechaAuditoria(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}

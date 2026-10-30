package co.natillera.shared.infrastructure.config;

import co.natillera.shared.application.FechaNegocio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }

    @Bean
    public FechaNegocio fechaNegocio(Clock clock, @Value("${app.fecha-simulada:}") String fechaSimulada) {
        if (fechaSimulada != null && !fechaSimulada.isBlank()) {
            LocalDate fija = LocalDate.parse(fechaSimulada.trim());
            return () -> fija;
        }
        return () -> LocalDate.now(clock);
    }
}

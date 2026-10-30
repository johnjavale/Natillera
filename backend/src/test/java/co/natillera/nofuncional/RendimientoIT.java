package co.natillera.nofuncional;

import co.natillera.soporte.IntegracionBase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** p95 de reportes < 500 ms con datos semilla y detección de N+1 (las sentencias no crecen con los datos). */
@DisplayName("No funcionales: latencia de reportes y N+1")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class RendimientoIT extends IntegracionBase {

    static final List<String> URLS = List.of("/api/v1/reportes/consolidado", "/api/v1/reportes/deudores",
            "/api/v1/reportes/estado-cuenta/3", "/api/v1/reportes/tesoreria", "/api/v1/resumen",
            "/api/v1/reparticion", "/api/v1/integrantes/aportes", "/api/v1/prestamos", "/api/v1/actividades");

    @Autowired
    private EntityManagerFactory emf;

    @ParameterizedTest(name = "p95 {0}")
    @ValueSource(strings = {"/api/v1/reportes/consolidado", "/api/v1/reportes/deudores",
            "/api/v1/reportes/estado-cuenta/3", "/api/v1/reportes/tesoreria", "/api/v1/resumen",
            "/api/v1/reparticion", "/api/v1/integrantes/aportes", "/api/v1/prestamos"})
    void p95MenorA500ms(String url) throws Exception {
        for (int i = 0; i < 5; i++) {
            getComo(ADMIN, url).andExpect(status().isOk());
        }
        List<Long> tiempos = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long inicio = System.nanoTime();
            getComo(ADMIN, url).andExpect(status().isOk());
            tiempos.add((System.nanoTime() - inicio) / 1_000_000);
        }
        Collections.sort(tiempos);
        long p95 = tiempos.get((int) Math.ceil(tiempos.size() * 0.95) - 1);
        System.out.printf("[rendimiento] %s p50=%dms p95=%dms%n", url, tiempos.get(tiempos.size() / 2), p95);
        assertThat(p95).as("p95 de %s", url).isLessThan(500);
    }

    @Test
    @Transactional
    @DisplayName("N+1: el número de sentencias por petición no crece al agregar préstamos, pagos y movimientos")
    void sinNmas1() throws Exception {
        Map<String, Long> antes = contarSentencias();
        long ultimoPrestamo = 0;
        for (int i = 0; i < 3; i++) {
            ultimoPrestamo = leer(postComo(ADMIN, "/api/v1/prestamos", Map.of("integranteId", 4 + i,
                    "cajitaOrigenId", 3, "fechaDesembolso", "2027-03-20", "monto", 10000, "numeroCuotas", 2))
                    .andExpect(status().isCreated())).get("id").asLong();
            postComo(ADMIN, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 1 + i, "fecha", "2027-03-20",
                    "tipo", "OTRO_INGRESO", "valor", 1000)).andExpect(status().isCreated());
        }
        postComo(ADMIN, "/api/v1/prestamos/" + ultimoPrestamo + "/pagos", Map.of("fecha", "2027-04-10"))
                .andExpect(status().isCreated());
        Map<String, Long> despues = contarSentencias();
        antes.forEach((url, n) -> {
            System.out.printf("[n+1] %s sentencias=%d -> %d%n", url, n, despues.get(url));
            assertThat(despues.get(url)).as("sentencias de %s", url).isEqualTo(n);
            assertThat(n).as("sentencias de %s", url).isLessThanOrEqualTo(40);
        });
    }

    private Map<String, Long> contarSentencias() throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (String url : URLS) {
            getComo(ADMIN, url).andExpect(status().isOk());
            stats.clear();
            getComo(ADMIN, url).andExpect(status().isOk());
            conteo.put(url, stats.getPrepareStatementCount());
        }
        return conteo;
    }
}

package co.natillera.seguridad;

import co.natillera.auth.application.UsuarioActual;
import co.natillera.auth.domain.Rol;
import co.natillera.auth.infrastructure.security.JwtTokenProvider;
import co.natillera.soporte.IntegracionBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Seguridad y autorización")
@Transactional
class SeguridadIT extends IntegracionBase {

    static final Map<String, Object> CICLO = Map.of("nombre", "Natillera Familiar", "fechaInicio", "2027-01-01",
            "numeroMeses", 12, "tasaInteresMensualPrestamos", 0.03, "gastosAdministrativos", 0,
            "maxCuotasPrestamo", 12, "tasaEACajitasNu", 0.09, "maxActividades", 2,
            "permitirPrestamoDespuesDelCierre", false);
    static final Map<String, Object> PRESTAMO = Map.of("integranteId", 1, "cajitaOrigenId", 3,
            "fechaDesembolso", "2027-03-20", "monto", 100000, "numeroCuotas", 3);
    static final Map<String, Object> ACTIVIDAD = Map.of("nombre", "Rifa extra", "fechaSorteo", "2027-08-01",
            "valorBoleta", 5000, "boletasPorIntegrante", 10, "costoPremio", 100000, "otrosGastos", 0);

    /** Matriz rol × endpoint: estado esperado para INTEGRANTE (integrante 3), TESORERO (tesorero 1) y ADMIN. */
    static Stream<Arguments> matriz() {
        return Stream.of(
                fila("GET", "/api/v1/ciclo", null, 200, 200, 200),
                fila("PUT", "/api/v1/ciclo", CICLO, 403, 403, 200),
                fila("PUT", "/api/v1/ciclo/estado", Map.of("estado", "ACTIVO"), 403, 403, 200),
                fila("GET", "/api/v1/usuarios", null, 403, 403, 200),
                fila("GET", "/api/v1/integrantes", null, 200, 200, 200),
                fila("POST", "/api/v1/integrantes", Map.of("nombre", "Nuevo", "cuotaMensual", 10000, "tesoreroId", 1),
                        403, 403, 201),
                fila("PUT", "/api/v1/integrantes/me/cuota", Map.of("cuotaMensual", 90000), 422, 422, 422),
                fila("GET", "/api/v1/integrantes/aportes", null, 200, 200, 200),
                fila("GET", "/api/v1/tesoreria/cajitas", null, 200, 200, 200),
                fila("GET", "/api/v1/tesoreria/resumen", null, 200, 200, 200),
                fila("POST", "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 1, "fecha", "2027-03-20",
                        "tipo", "AHORRO", "valor", 10000), 403, 201, 201),
                fila("POST", "/api/v1/tesoreria/aportes-lote", Map.of("fecha", "2027-03-20",
                        "aportes", List.of(Map.of("integranteId", 1, "monto", 100000))), 403, 201, 201),
                fila("POST", "/api/v1/tesoreria/cajitas/1/saldo-reportado", Map.of("fecha", "2027-03-20",
                        "saldoApp", 500720), 403, 204, 204),
                fila("GET", "/api/v1/prestamos", null, 200, 200, 200),
                fila("GET", "/api/v1/prestamos/proyeccion-intereses", null, 200, 200, 200),
                fila("POST", "/api/v1/prestamos/simular", PRESTAMO, 403, 200, 200),
                fila("POST", "/api/v1/prestamos", PRESTAMO, 403, 201, 201),
                fila("POST", "/api/v1/prestamos/1/pagos", Map.of("fecha", "2027-03-20", "cajitaDestinoId", 3),
                        403, 201, 201),
                fila("GET", "/api/v1/actividades", null, 200, 200, 200),
                fila("POST", "/api/v1/actividades", ACTIVIDAD, 403, 403, 422),
                fila("PUT", "/api/v1/actividades/1/boletas/1", Map.of("boletas", 25), 403, 403, 200),
                fila("POST", "/api/v1/actividades/1/pagos", Map.of("integranteId", 1, "fecha", "2027-03-20",
                        "monto", 1000), 403, 201, 201),
                fila("GET", "/api/v1/reparticion", null, 200, 200, 200),
                fila("GET", "/api/v1/resumen", null, 403, 403, 200),
                fila("GET", "/api/v1/reportes/consolidado", null, 403, 200, 200),
                fila("GET", "/api/v1/reportes/deudores", null, 403, 200, 200),
                fila("GET", "/api/v1/reportes/estado-cuenta/3", null, 200, 200, 200),
                fila("GET", "/api/v1/reportes/tesoreria", null, 403, 200, 200),
                fila("GET", "/api/v1/reportes/resumen", null, 403, 403, 200),
                fila("GET", "/api/v1/auditoria", null, 403, 403, 200));
    }

    static Arguments fila(String metodo, String url, Object cuerpo, int integrante, int tesorero, int admin) {
        return Arguments.of(metodo, url, cuerpo, integrante, tesorero, admin);
    }

    @ParameterizedTest(name = "{0} {1} → integrante {3}, tesorero {4}, admin {5}")
    @MethodSource("matriz")
    void matrizRolPorEndpoint(String metodo, String url, Object cuerpo, int integrante, int tesorero, int admin)
            throws Exception {
        assertThat(ejecutar(metodo, url, cuerpo, INTEGRANTE3)).as("INTEGRANTE").isEqualTo(integrante);
        assertThat(ejecutar(metodo, url, cuerpo, TESORERO1)).as("TESORERO").isEqualTo(tesorero);
        assertThat(ejecutar(metodo, url, cuerpo, ADMIN)).as("ADMIN").isEqualTo(admin);
    }

    @ParameterizedTest(name = "sin token: {0} {1} → 401")
    @MethodSource("matriz")
    void sinTokenEs401(String metodo, String url, Object cuerpo) throws Exception {
        var builder = request(org.springframework.http.HttpMethod.valueOf(metodo), url)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo == null ? "" : json.writeValueAsString(cuerpo));
        mvc.perform(builder).andExpect(status().isUnauthorized());
    }

    private int ejecutar(String metodo, String url, Object cuerpo, String login) throws Exception {
        return switch (metodo) {
            case "GET" -> getComo(login, url).andReturn().getResponse().getStatus();
            case "POST" -> postComo(login, url, cuerpo).andReturn().getResponse().getStatus();
            case "PUT" -> putComo(login, url, cuerpo).andReturn().getResponse().getStatus();
            default -> throw new IllegalArgumentException(metodo);
        };
    }

    @Nested
    @DisplayName("Prevención de IDOR (404 fuera de alcance)")
    class Idor {

        @Test
        void integranteNoVeDatosDeOtro() throws Exception {
            getComo(INTEGRANTE3, "/api/v1/integrantes/3").andExpect(status().isOk());
            getComo(INTEGRANTE3, "/api/v1/integrantes/7").andExpect(status().isNotFound());
            getComo(INTEGRANTE3, "/api/v1/integrantes/7/aportes").andExpect(status().isNotFound());
            getComo(INTEGRANTE3, "/api/v1/reportes/estado-cuenta/7").andExpect(status().isNotFound());
            getComo(INTEGRANTE3, "/api/v1/integrantes/9999").andExpect(status().isNotFound());
        }

        @Test
        void integranteSoloVeSusPrestamos() throws Exception {
            getComo(INTEGRANTE3, "/api/v1/prestamos/1").andExpect(status().isOk());
            getComo(INTEGRANTE7, "/api/v1/prestamos/1").andExpect(status().isNotFound());
            JsonNode lista = leer(getComo(INTEGRANTE7, "/api/v1/prestamos"));
            assertThat(lista).isEmpty();
        }

        @Test
        void listadosFiltradosPorAlcance() throws Exception {
            JsonNode propios = leer(getComo(INTEGRANTE3, "/api/v1/integrantes"));
            assertThat(propios).hasSize(1);
            assertThat(propios.get(0).get("id").asLong()).isEqualTo(3);

            JsonNode cajitasT1 = leer(getComo(TESORERO1, "/api/v1/tesoreria/cajitas"));
            assertThat(cajitasT1).extracting(n -> n.get("integranteId").asLong()).containsExactly(1L, 2L, 3L, 4L, 5L);

            JsonNode reparto = leer(getComo(INTEGRANTE3, "/api/v1/reparticion"));
            assertThat(reparto.get("filas")).hasSize(1);
            assertThat(reparto.get("proyectado").isNull()).isTrue();
        }

        @Test
        void tesoreroNoVeIntegrantesNoAsignados() throws Exception {
            getComo(TESORERO1, "/api/v1/integrantes/7").andExpect(status().isNotFound());
            getComo(TESORERO1, "/api/v1/tesoreria/movimientos?cajitaId=7").andExpect(status().isNotFound());
            postComo(TESORERO1, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 7, "fecha", "2027-03-20",
                    "tipo", "AHORRO", "valor", 1000)).andExpect(status().isNotFound());
            postComo(TESORERO1, "/api/v1/tesoreria/cajitas/7/saldo-reportado", Map.of("fecha", "2027-03-20",
                    "saldoApp", 1)).andExpect(status().isNotFound());
        }

        @Test
        void tesoreroVeTodosLosPrestamosPeroNoCobraEnCajitasAjenas() throws Exception {
            getComo(TESORERO2, "/api/v1/prestamos/1").andExpect(status().isOk());
            // el pago entra a la cajita 3 (tesorero 1): el tesorero 2 no la administra
            postComo(TESORERO2, "/api/v1/prestamos/1/pagos", Map.of("fecha", "2027-03-20", "cajitaDestinoId", 3))
                    .andExpect(status().isNotFound());
        }

        @Test
        void tesoreroQueTambienEsIntegranteVeSuPropiaCuentaPeroNoLaAdministraSiNoEsSuya() throws Exception {
            // Pascuala (tesorero 1) es el integrante 2, asignado a ella misma: la ve y la administra
            getComo(TESORERO1, "/api/v1/integrantes/2").andExpect(status().isOk());
            // Arley (tesorero 2) es el integrante 6, asignado a sí mismo
            getComo(TESORERO2, "/api/v1/integrantes/6").andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("JWT y refresh tokens")
    class Tokens {

        @Test
        void tokenExpiradoResponde401ConTipoTokenExpirado() throws Exception {
            JwtTokenProvider viejo = new JwtTokenProvider("secreto-de-pruebas-0123456789abcdef0123456789abcdef", 15,
                    Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC));
            String token = viejo.emitirAccessToken(new UsuarioActual(1L, ADMIN, "Admin", Set.of(Rol.ADMIN), 1L, null,
                    false));
            mvc.perform(get("/api/v1/ciclo").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("https://natillera.app/problemas/token-expirado"));
        }

        @Test
        void tokenAlteradoOFirmadoConOtraClaveEs401() throws Exception {
            JwtTokenProvider otro = new JwtTokenProvider("otra-clave-distinta-0123456789abcdef0123456789abcdef", 15,
                    Clock.systemUTC());
            String falso = otro.emitirAccessToken(new UsuarioActual(1L, ADMIN, "Admin", Set.of(Rol.ADMIN), 1L, null,
                    false));
            mvc.perform(get("/api/v1/ciclo").header("Authorization", "Bearer " + falso))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/ciclo").header("Authorization", "Bearer abc.def.ghi"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshRotaYLaReutilizacionRevocaLaFamilia() throws Exception {
            String r1 = login(INTEGRANTE3, CLAVE).get("refreshToken").asText();
            JsonNode s2 = refresh(r1, 200);
            String r2 = s2.get("refreshToken").asText();
            assertThat(r2).isNotEqualTo(r1);
            refresh(r1, 401);          // reutilización detectada
            refresh(r2, 401);          // toda la familia quedó revocada
        }

        @Test
        void logoutRevocaElRefreshToken() throws Exception {
            String r = login(INTEGRANTE3, CLAVE).get("refreshToken").asText();
            mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("refreshToken", r)))).andExpect(status().isNoContent());
            refresh(r, 401);
        }

        @Test
        void loginIncorrectoNoRevelaSiElUsuarioExiste() throws Exception {
            for (String login : List.of(ADMIN, "noexiste@natillera.local")) {
                mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(Map.of("login", login, "password", "Incorrecta123"))))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
            }
        }

        @Test
        void loginConCelular() throws Exception {
            assertThat(login("3000000003", CLAVE).get("usuario").get("integranteId").asLong()).isEqualTo(3);
        }

        private JsonNode refresh(String token, int esperado) throws Exception {
            var r = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("refreshToken", token))))
                    .andExpect(status().is(esperado));
            return leer(r);
        }
    }

    @Nested
    @DisplayName("Rate limit, CORS, headers y contraseñas")
    class Protecciones {

        @Test
        void rateLimitDeLoginPorIpYUsuario() throws Exception {
            for (int i = 0; i < 5; i++) {
                intentoLogin("10.9.9.9", INTEGRANTE7, "Incorrecta123").andExpect(status().isUnauthorized());
            }
            intentoLogin("10.9.9.9", INTEGRANTE7, CLAVE)
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"));
            // el bloqueo por usuario aplica también desde otra IP
            intentoLogin("10.8.8.8", INTEGRANTE7, CLAVE).andExpect(status().isTooManyRequests());
        }

        @Test
        void corsSoloAceptaElOrigenDelFrontend() throws Exception {
            mvc.perform(options("/api/v1/ciclo").header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
            mvc.perform(options("/api/v1/ciclo").header("Origin", "https://malicioso.example")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void headersDeSeguridad() throws Exception {
            mvc.perform(get("/actuator/health").secure(true))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("Strict-Transport-Security"))
                    .andExpect(header().string("Content-Security-Policy", "frame-ancestors 'none'"));
        }

        @Test
        void cambioDePasswordObligatorioEnPrimerIngresoYPoliticaDeContrasena() throws Exception {
            postComo(ADMIN, "/api/v1/usuarios", Map.of("nombre", "Prima Nueva", "correo", "prima@natillera.local",
                    "roles", List.of("INTEGRANTE"), "contrasenaTemporal", "Temporal2027x"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.debeCambiarPassword").value(true));

            JsonNode sesion = login("prima@natillera.local", "Temporal2027x");
            String token = sesion.get("accessToken").asText();
            assertThat(sesion.get("usuario").get("debeCambiarPassword").asBoolean()).isTrue();
            mvc.perform(get("/api/v1/integrantes").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("https://natillera.app/problemas/cambio-password-obligatorio"));

            cambiar(token, "Temporal2027x", "corta1").andExpect(status().isUnprocessableEntity());
            cambiar(token, "Temporal2027x", "solamenteletras").andExpect(status().isUnprocessableEntity());
            cambiar(token, "Temporal2027x", "prima2027segura").andExpect(status().isUnprocessableEntity());
            JsonNode nueva = leer(cambiar(token, "Temporal2027x", "Familia2027segura").andExpect(status().isOk()));
            assertThat(nueva.get("usuario").get("debeCambiarPassword").asBoolean()).isFalse();
            mvc.perform(get("/api/v1/integrantes").header("Authorization", "Bearer " + nueva.get("accessToken").asText()))
                    .andExpect(status().isOk());
        }

        private org.springframework.test.web.servlet.ResultActions cambiar(String token, String actual, String nueva)
                throws Exception {
            return mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("actual", actual, "nueva", nueva))));
        }

        private org.springframework.test.web.servlet.ResultActions intentoLogin(String ip, String login, String clave)
                throws Exception {
            return mvc.perform(post("/api/v1/auth/login").with(r -> {
                r.setRemoteAddr(ip);
                return r;
            }).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("login", login,
                    "password", clave))));
        }
    }
}

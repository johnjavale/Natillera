package co.natillera.soporte;

import co.natillera.auth.application.LimitadorIntentosLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: API completa (MockMvc) + MySQL real con Flyway (esquema + seed dev).
 * Las clases que modifican datos se anotan con @Transactional para revertir al final de cada prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracionBase {

    public static final String CLAVE = "Natillera2027*";
    public static final String ADMIN = "admin@natillera.local";
    public static final String TESORERO1 = "tesorero1@natillera.local";
    public static final String TESORERO2 = "tesorero2@natillera.local";
    /** Samay Moreno, integrante 3, asignado al tesorero 1. */
    public static final String INTEGRANTE3 = "integrante3@natillera.local";
    /** Haddy Moreno, integrante 7, asignado al tesorero 2. */
    public static final String INTEGRANTE7 = "integrante7@natillera.local";

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected LimitadorIntentosLogin limitador;

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registry) {
        MysqlDePrueba.Conexion c = MysqlDePrueba.obtener();
        registry.add("spring.datasource.url", c::url);
        registry.add("spring.datasource.username", c::usuario);
        registry.add("spring.datasource.password", c::clave);
    }

    @BeforeEach
    void reiniciarLimitador() {
        limitador.reiniciar();
    }

    /** Access token de un usuario del seed (cacheado: el login con BCrypt 12 es costoso). */
    protected String token(String login) {
        return TOKENS.computeIfAbsent(login, l -> login(l, CLAVE).get("accessToken").asText());
    }

    protected JsonNode login(String login, String clave) {
        try {
            String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("login", login, "password", clave))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return json.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Login fallido para " + login, e);
        }
    }

    protected ResultActions getComo(String login, String url) throws Exception {
        return mvc.perform(conToken(get(url), login));
    }

    protected ResultActions postComo(String login, String url, Object cuerpo) throws Exception {
        return mvc.perform(conToken(post(url), login).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    protected ResultActions putComo(String login, String url, Object cuerpo) throws Exception {
        return mvc.perform(conToken(put(url), login).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    protected JsonNode leer(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder b, String login) {
        return login == null ? b : b.header("Authorization", "Bearer " + token(login));
    }
}

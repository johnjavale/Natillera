package co.natillera.seguridad;

import co.natillera.auth.application.RecuperacionContrasenaService;
import co.natillera.auth.application.port.out.EnviadorCorreoPort;
import co.natillera.auth.domain.TokenRecuperacion;
import co.natillera.soporte.IntegracionBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Recuperación de contraseña por correo")
@Transactional
class RecuperacionPasswordIT extends IntegracionBase {

    private static final Pattern ENLACE = Pattern.compile("http://localhost:3000/restablecer-password\\?token=([A-Za-z0-9_-]{43})");
    private static final String NUEVA = "Ahorro2028casa";

    @MockitoBean
    EnviadorCorreoPort correo;

    @Autowired
    RecuperacionContrasenaService recuperacion;

    @BeforeEach
    void reiniciarLimite() {
        recuperacion.reiniciarLimite();
    }

    @Test
    void elEnlaceDelCorreoPermiteCambiarLaContrasenaUnaSolaVez() throws Exception {
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", "Integrante3@Natillera.local"))
                .andExpect(status().isAccepted());
        String token = tokenDelCorreo(INTEGRANTE3);

        restablecer(token, NUEVA).andExpect(status().isNoContent());

        assertThat(login(INTEGRANTE3, NUEVA).get("usuario").get("debeCambiarPassword").asBoolean()).isFalse();
        intentarLogin(INTEGRANTE3, CLAVE).andExpect(status().isUnauthorized());
        restablecer(token, "OtraClave2029x").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El enlace no es válido o ya expiró. Solicite uno nuevo."));
    }

    @Test
    void pedirUnEnlaceNuevoInvalidaElAnterior() throws Exception {
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", INTEGRANTE7)).andExpect(status().isAccepted());
        String primero = tokenDelCorreo(INTEGRANTE7);
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", INTEGRANTE7)).andExpect(status().isAccepted());

        restablecer(primero, NUEVA).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void correoInexistenteRespondeIgualYNoEnviaNada() throws Exception {
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", "nadie@natillera.local"))
                .andExpect(status().isAccepted());
        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void tokenInventadoYContrasenaDebilSeRechazan() throws Exception {
        restablecer("token-que-no-existe-0123456789abcdefghijklmn", NUEVA).andExpect(status().isUnprocessableEntity());

        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", INTEGRANTE3)).andExpect(status().isAccepted());
        restablecer(tokenDelCorreo(INTEGRANTE3), "corta1").andExpect(status().isUnprocessableEntity());
        intentarLogin(INTEGRANTE3, CLAVE).andExpect(status().isOk());
    }

    @Test
    void demasiadasSolicitudesResponden429() throws Exception {
        for (int i = 0; i < 5; i++) {
            postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", "nadie@natillera.local"))
                    .andExpect(status().isAccepted());
        }
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", "nadie@natillera.local"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void correoMalFormadoEs400() throws Exception {
        postComo(null, "/api/v1/auth/recuperar-password", Map.of("correo", "3000000003"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elTokenVenceYNoSirveDosVeces() {
        Instant ahora = Instant.parse("2027-03-20T12:00:00Z");
        assertThat(new TokenRecuperacion(1L, 1L, "h", ahora.plusSeconds(60), false).vigente(ahora)).isTrue();
        assertThat(new TokenRecuperacion(1L, 1L, "h", ahora, false).vigente(ahora)).isFalse();
        assertThat(new TokenRecuperacion(1L, 1L, "h", ahora.plusSeconds(60), true).vigente(ahora)).isFalse();
    }

    private String tokenDelCorreo(String para) {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo, atLeastOnce()).enviar(eq(para), anyString(), texto.capture());
        Matcher m = ENLACE.matcher(texto.getValue());
        assertThat(m.find()).as("el correo trae el enlace de recuperación").isTrue();
        return m.group(1);
    }

    private ResultActions restablecer(String token, String nueva) throws Exception {
        return postComo(null, "/api/v1/auth/restablecer-password", Map.of("token", token, "nueva", nueva));
    }

    private ResultActions intentarLogin(String login, String clave) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "password", clave))));
    }
}

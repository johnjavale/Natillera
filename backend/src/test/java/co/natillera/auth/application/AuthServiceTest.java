package co.natillera.auth.application;

import co.natillera.auth.application.port.out.PasswordHasherPort;
import co.natillera.auth.application.port.out.RefreshTokenRepositoryPort;
import co.natillera.auth.application.port.out.TokenEmisorPort;
import co.natillera.auth.application.port.out.UsuarioRepositoryPort;
import co.natillera.auth.domain.RefreshToken;
import co.natillera.auth.domain.Rol;
import co.natillera.auth.domain.Usuario;
import co.natillera.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    UsuarioRepositoryPort usuarios;
    @Mock
    RefreshTokenRepositoryPort refresh;
    @Mock
    TokenEmisorPort tokens;
    @Mock
    PasswordHasherPort hasher;

    final Clock clock = Clock.fixed(Instant.parse("2027-03-20T12:00:00Z"), ZoneOffset.UTC);
    final Usuario usuario = new Usuario(5L, "Samay", "s@n.co", null, "HASH", Set.of(Rol.INTEGRANTE), true, false, 3L, null);
    AuthService auth;
    LimitadorIntentosLogin limitador;

    @BeforeEach
    void setUp() {
        limitador = new LimitadorIntentosLogin(5, 15, clock);
        auth = new AuthService(usuarios, refresh, tokens, hasher, limitador, clock, 7);
        when(usuarios.buscarPorLogin("s@n.co")).thenReturn(Optional.of(usuario));
        when(usuarios.buscarPorId(5L)).thenReturn(Optional.of(usuario));
        when(hasher.coincide("Clave12345", "HASH")).thenReturn(true);
        when(tokens.emitirAccessToken(any())).thenReturn("jwt");
        when(tokens.duracionAccessTokenSegundos()).thenReturn(900L);
        when(refresh.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void loginGuardaSoloElHashDelRefreshToken() {
        AuthService.Sesion s = auth.login("s@n.co", "Clave12345", "1.1.1.1");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refresh).guardar(captor.capture());
        assertThat(captor.getValue().tokenHash()).isEqualTo(AuthService.hashToken(s.refreshToken()))
                .isNotEqualTo(s.refreshToken()).hasSize(64);
        assertThat(captor.getValue().expiraEn()).isEqualTo(clock.instant().plusSeconds(7 * 86400));
        assertThat(s.usuario().integranteId()).isEqualTo(3L);
    }

    @Test
    void refreshReutilizadoRevocaLaFamilia() {
        UUID familia = UUID.randomUUID();
        when(refresh.buscarPorHash(AuthService.hashToken("viejo"))).thenReturn(Optional.of(
                new RefreshToken(1L, 5L, "h", familia, clock.instant().plusSeconds(60), true, false)));
        assertThatThrownBy(() -> auth.refrescar("viejo")).isInstanceOf(CredencialesInvalidasException.class);
        verify(refresh).revocarFamilia(familia);
    }

    @Test
    void refreshValidoRotaDentroDeLaMismaFamilia() {
        UUID familia = UUID.randomUUID();
        when(refresh.buscarPorHash(AuthService.hashToken("vigente"))).thenReturn(Optional.of(
                new RefreshToken(1L, 5L, "h", familia, clock.instant().plusSeconds(60), false, false)));
        AuthService.Sesion s = auth.refrescar("vigente");
        verify(refresh).marcarUsado(1L);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refresh).guardar(captor.capture());
        assertThat(captor.getValue().familia()).isEqualTo(familia);
        assertThat(s.refreshToken()).isNotEqualTo("vigente");
    }

    @Test
    void refreshExpiradoSeRechaza() {
        when(refresh.buscarPorHash(anyString())).thenReturn(Optional.of(
                new RefreshToken(1L, 5L, "h", UUID.randomUUID(), clock.instant(), false, false)));
        assertThatThrownBy(() -> auth.refrescar("x")).hasMessageContaining("expiró");
        verify(refresh, never()).marcarUsado(any());
    }

    @Test
    void cambioDeContrasenaValidaLaPoliticaYRevocaSesiones() {
        assertThatThrownBy(() -> auth.cambiarContrasena(5L, "Clave12345", "corta"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> auth.cambiarContrasena(5L, "otra", "NuevaClave2027"))
                .hasMessage("La contraseña actual no es correcta");
        when(hasher.hash("NuevaClave2027")).thenReturn("NUEVO");
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));
        auth.cambiarContrasena(5L, "Clave12345", "NuevaClave2027");
        verify(refresh).revocarTodosDelUsuario(5L);
    }

    @Test
    void rateLimitTrasCincoFallos() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> auth.login("s@n.co", "mala", "2.2.2.2"))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> auth.login("s@n.co", "Clave12345", "2.2.2.2"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void ventanaDelRateLimitExpira() {
        Clock[] reloj = {clock};
        LimitadorIntentosLogin l = new LimitadorIntentosLogin(2, 15, new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return reloj[0].instant();
            }
        });
        l.registrarFallo("ip", "u");
        l.registrarFallo("ip", "u");
        assertThatThrownBy(() -> l.verificar("ip", "u")).isInstanceOf(DemasiadosIntentosException.class);
        reloj[0] = Clock.offset(clock, java.time.Duration.ofMinutes(16));
        l.verificar("ip", "u");
    }
}

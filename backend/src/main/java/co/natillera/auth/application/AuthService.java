package co.natillera.auth.application;

import co.natillera.auth.application.port.out.PasswordHasherPort;
import co.natillera.auth.application.port.out.RefreshTokenRepositoryPort;
import co.natillera.auth.application.port.out.TokenEmisorPort;
import co.natillera.auth.application.port.out.UsuarioRepositoryPort;
import co.natillera.auth.domain.PoliticaContrasena;
import co.natillera.auth.domain.RefreshToken;
import co.natillera.auth.domain.Usuario;
import co.natillera.shared.domain.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Casos de uso de autenticación: login, refresh con rotación, logout y cambio de contraseña. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos";

    public record Sesion(String accessToken, String refreshToken, long expiraEnSegundos, UsuarioActual usuario) {
    }

    private final UsuarioRepositoryPort usuarios;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final TokenEmisorPort tokenEmisor;
    private final PasswordHasherPort hasher;
    private final LimitadorIntentosLogin limitador;
    private final Clock clock;
    private final Duration duracionRefresh;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UsuarioRepositoryPort usuarios, RefreshTokenRepositoryPort refreshTokens,
                       TokenEmisorPort tokenEmisor, PasswordHasherPort hasher, LimitadorIntentosLogin limitador,
                       Clock clock, @Value("${app.jwt.refresh-dias:7}") int diasRefresh) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.tokenEmisor = tokenEmisor;
        this.hasher = hasher;
        this.limitador = limitador;
        this.clock = clock;
        this.duracionRefresh = Duration.ofDays(diasRefresh);
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public Sesion login(String login, String contrasena, String ip) {
        limitador.verificar(ip, login);
        Usuario usuario = usuarios.buscarPorLogin(login.trim())
                .filter(Usuario::activo)
                .filter(u -> hasher.coincide(contrasena, u.passwordHash()))
                .orElse(null);
        if (usuario == null) {
            limitador.registrarFallo(ip, login);
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }
        limitador.registrarExito(login);
        return emitirSesion(usuario, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public Sesion refrescar(String refreshTokenPlano) {
        RefreshToken actual = refreshTokens.buscarPorHash(hashToken(refreshTokenPlano))
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
        if (actual.reutilizado()) {
            log.warn("Refresh token reutilizado: se revoca la familia completa del usuario {}", actual.usuarioId());
            refreshTokens.revocarFamilia(actual.familia());
            throw new CredencialesInvalidasException("Sesión inválida");
        }
        if (actual.expirado(clock.instant())) {
            throw new CredencialesInvalidasException("La sesión expiró, ingrese de nuevo");
        }
        Usuario usuario = usuarios.buscarPorId(actual.usuarioId())
                .filter(Usuario::activo)
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
        refreshTokens.marcarUsado(actual.id());
        return emitirSesion(usuario, actual.familia());
    }

    @Transactional
    public void logout(String refreshTokenPlano) {
        if (refreshTokenPlano == null || refreshTokenPlano.isBlank()) {
            return;
        }
        refreshTokens.buscarPorHash(hashToken(refreshTokenPlano))
                .ifPresent(t -> refreshTokens.revocarFamilia(t.familia()));
    }

    @Transactional
    public Sesion cambiarContrasena(Long usuarioId, String actual, String nueva) {
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
        if (!hasher.coincide(actual, usuario.passwordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente a la actual");
        }
        PoliticaContrasena.validar(nueva, usuario.login());
        Usuario actualizado = usuarios.guardar(usuario.conPassword(hasher.hash(nueva), false));
        refreshTokens.revocarTodosDelUsuario(usuarioId);
        return emitirSesion(actualizado, UUID.randomUUID());
    }

    public UsuarioActual aUsuarioActual(Usuario u) {
        return new UsuarioActual(u.id(), u.login(), u.nombre(), u.roles(), u.integranteId(), u.tesoreroId(),
                u.debeCambiarPassword());
    }

    private Sesion emitirSesion(Usuario usuario, UUID familia) {
        String refreshPlano = generarTokenOpaco();
        refreshTokens.guardar(new RefreshToken(null, usuario.id(), hashToken(refreshPlano), familia,
                clock.instant().plus(duracionRefresh), false, false));
        UsuarioActual actual = aUsuarioActual(usuario);
        return new Sesion(tokenEmisor.emitirAccessToken(actual), refreshPlano,
                tokenEmisor.duracionAccessTokenSegundos(), actual);
    }

    private String generarTokenOpaco() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

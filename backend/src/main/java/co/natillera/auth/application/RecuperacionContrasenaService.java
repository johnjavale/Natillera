package co.natillera.auth.application;

import co.natillera.auth.application.port.out.EnviadorCorreoPort;
import co.natillera.auth.application.port.out.PasswordHasherPort;
import co.natillera.auth.application.port.out.RefreshTokenRepositoryPort;
import co.natillera.auth.application.port.out.TokenRecuperacionRepositoryPort;
import co.natillera.auth.application.port.out.UsuarioRepositoryPort;
import co.natillera.auth.domain.PoliticaContrasena;
import co.natillera.auth.domain.TokenRecuperacion;
import co.natillera.auth.domain.Usuario;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * "¿Olvidó su contraseña?": envía por correo un enlace de un solo uso y con expiración para fijar una clave nueva.
 * La solicitud responde igual exista o no el correo, para no revelar qué cuentas hay.
 */
@Service
public class RecuperacionContrasenaService {

    static final String ENLACE_INVALIDO = "El enlace no es válido o ya expiró. Solicite uno nuevo.";

    private final UsuarioRepositoryPort usuarios;
    private final TokenRecuperacionRepositoryPort tokens;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final PasswordHasherPort hasher;
    private final EnviadorCorreoPort correo;
    private final AuditoriaPort auditoria;
    private final LimitadorIntentosLogin limitadorLogin;
    private final LimitadorIntentosLogin limitadorSolicitudes;
    private final Clock clock;
    private final Duration vigencia;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public RecuperacionContrasenaService(UsuarioRepositoryPort usuarios, TokenRecuperacionRepositoryPort tokens,
                                         RefreshTokenRepositoryPort refreshTokens, PasswordHasherPort hasher,
                                         EnviadorCorreoPort correo, AuditoriaPort auditoria,
                                         LimitadorIntentosLogin limitadorLogin, Clock clock,
                                         @Value("${app.recuperacion.minutos:30}") int minutos,
                                         @Value("${app.recuperacion.max-solicitudes:5}") int maxSolicitudes,
                                         @Value("${app.frontend-url}") String frontendUrl) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.hasher = hasher;
        this.correo = correo;
        this.auditoria = auditoria;
        this.limitadorLogin = limitadorLogin;
        this.limitadorSolicitudes = new LimitadorIntentosLogin(maxSolicitudes, 15, clock);
        this.clock = clock;
        this.vigencia = Duration.ofMinutes(minutos);
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void solicitar(String correoUsuario, String ip) {
        String login = correoUsuario.trim();
        limitadorSolicitudes.verificar(ip, login);
        limitadorSolicitudes.registrarFallo(ip, login);
        usuarios.buscarPorLogin(login)
                .filter(Usuario::activo)
                .filter(u -> u.correo() != null && u.correo().equalsIgnoreCase(login))
                .ifPresent(this::enviarEnlace);
    }

    @Transactional
    public void restablecer(String tokenPlano, String nueva) {
        TokenRecuperacion token = tokens.buscarPorHash(AuthService.hashToken(tokenPlano))
                .filter(t -> t.vigente(clock.instant()))
                .orElseThrow(() -> new ReglaNegocioException(ENLACE_INVALIDO));
        Usuario usuario = usuarios.buscarPorId(token.usuarioId())
                .filter(Usuario::activo)
                .orElseThrow(() -> new ReglaNegocioException(ENLACE_INVALIDO));
        PoliticaContrasena.validar(nueva, usuario.login());
        usuarios.guardar(usuario.conPassword(hasher.hash(nueva), false));
        tokens.invalidarDelUsuario(usuario.id());
        refreshTokens.revocarTodosDelUsuario(usuario.id());
        limitadorLogin.registrarExito(usuario.login());
        auditoria.registrar("usuario", usuario.id(), "RECUPERAR_PASSWORD", "Contraseña cambiada con enlace por correo");
    }

    /** Solo para pruebas: limpia el rate limit de solicitudes. */
    public void reiniciarLimite() {
        limitadorSolicitudes.reiniciar();
    }

    private void enviarEnlace(Usuario usuario) {
        tokens.invalidarDelUsuario(usuario.id());
        String plano = generarToken();
        tokens.guardar(new TokenRecuperacion(null, usuario.id(), AuthService.hashToken(plano),
                clock.instant().plus(vigencia), false));
        String enlace = frontendUrl + "/restablecer-password?token=" + plano;
        correo.enviar(usuario.correo(), "Natillera Familiar: cambie su contraseña", """
                Hola, %s:

                Recibimos una solicitud para cambiar la contraseña de su cuenta en Natillera Familiar.
                Para elegir una nueva, abra este enlace (vale %d minutos y se puede usar una sola vez):

                %s

                Si usted no lo pidió, ignore este correo: su contraseña actual sigue funcionando.
                """.formatted(usuario.nombre(), vigencia.toMinutes(), enlace));
        auditoria.registrar("usuario", usuario.id(), "SOLICITAR_RECUPERACION", null);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

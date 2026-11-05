package co.natillera.auth.infrastructure.web;

import co.natillera.auth.application.AuthService;
import co.natillera.auth.application.RecuperacionContrasenaService;
import co.natillera.auth.application.SesionActual;
import co.natillera.auth.application.UsuarioActual;
import co.natillera.auth.domain.Rol;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record LoginRequest(@NotBlank(message = "Ingrese su correo o celular") @Size(max = 160) String login,
                               @NotBlank(message = "Ingrese su contraseña") @Size(max = 72) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record CambiarPasswordRequest(@NotBlank(message = "Ingrese la contraseña actual") String actual,
                                         @NotBlank(message = "Ingrese la nueva contraseña") @Size(max = 72) String nueva) {
    }

    public record RecuperarPasswordRequest(@NotBlank(message = "Ingrese su correo")
                                           @Email(message = "Correo inválido") @Size(max = 160) String correo) {
    }

    public record RestablecerPasswordRequest(@NotBlank @Size(max = 100) String token,
                                             @NotBlank(message = "Ingrese la nueva contraseña") @Size(max = 72) String nueva) {
    }

    public record UsuarioSesionDto(Long id, String login, String nombre, Set<Rol> roles, Long integranteId,
                                   Long tesoreroId, boolean debeCambiarPassword) {
        static UsuarioSesionDto de(UsuarioActual u) {
            return new UsuarioSesionDto(u.usuarioId(), u.login(), u.nombre(), u.roles(), u.integranteId(),
                    u.tesoreroId(), u.debeCambiarPassword());
        }
    }

    public record SesionDto(String accessToken, String refreshToken, long expiresIn, UsuarioSesionDto usuario) {
        static SesionDto de(AuthService.Sesion s) {
            return new SesionDto(s.accessToken(), s.refreshToken(), s.expiraEnSegundos(),
                    UsuarioSesionDto.de(s.usuario()));
        }
    }

    private final AuthService auth;
    private final RecuperacionContrasenaService recuperacion;
    private final SesionActual sesion;

    public AuthController(AuthService auth, RecuperacionContrasenaService recuperacion, SesionActual sesion) {
        this.auth = auth;
        this.recuperacion = recuperacion;
        this.sesion = sesion;
    }

    @PostMapping("/login")
    public SesionDto login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return SesionDto.de(auth.login(req.login(), req.password(), http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public SesionDto refresh(@Valid @RequestBody RefreshRequest req) {
        return SesionDto.de(auth.refrescar(req.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest req) {
        auth.logout(req == null ? null : req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cambiar-password")
    public SesionDto cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req) {
        return SesionDto.de(auth.cambiarContrasena(sesion.usuario().usuarioId(), req.actual(), req.nueva()));
    }

    /** Siempre 202, exista o no el correo (no revela qué cuentas hay). */
    @PostMapping("/recuperar-password")
    public ResponseEntity<Void> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest req,
                                                  HttpServletRequest http) {
        recuperacion.solicitar(req.correo(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest req) {
        recuperacion.restablecer(req.token(), req.nueva());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioSesionDto yo() {
        return UsuarioSesionDto.de(sesion.usuario());
    }
}

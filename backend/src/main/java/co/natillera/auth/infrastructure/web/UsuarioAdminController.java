package co.natillera.auth.infrastructure.web;

import co.natillera.auth.application.UsuarioAdminService;
import co.natillera.auth.domain.Rol;
import co.natillera.auth.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    public record UsuarioDto(Long id, String nombre, String correo, String celular, Set<Rol> roles, boolean activo,
                             boolean debeCambiarPassword, Long integranteId, Long tesoreroId) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.id(), u.nombre(), u.correo(), u.celular(), u.roles(), u.activo(),
                    u.debeCambiarPassword(), u.integranteId(), u.tesoreroId());
        }
    }

    public record CrearUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @Email(message = "Correo inválido") @Size(max = 160) String correo,
            @Pattern(regexp = "^$|^[0-9+ ]{7,20}$", message = "Celular inválido") String celular,
            @NotEmpty(message = "Asigne al menos un rol") Set<Rol> roles,
            @NotBlank(message = "La contraseña temporal es obligatoria") @Size(max = 72) String contrasenaTemporal,
            Long integranteId) {
    }

    public record RolesRequest(@NotEmpty(message = "Asigne al menos un rol") Set<Rol> roles, Long integranteId) {
    }

    public record RestablecerRequest(@NotBlank @Size(max = 72) String contrasenaTemporal) {
    }

    public record ActivoRequest(@NotNull Boolean activo) {
    }

    private final UsuarioAdminService service;

    public UsuarioAdminController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return service.listar().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody CrearUsuarioRequest r) {
        return UsuarioDto.de(service.crear(new UsuarioAdminService.NuevoUsuario(r.nombre(), r.correo(), r.celular(),
                r.roles(), r.contrasenaTemporal(), r.integranteId())));
    }

    @PutMapping("/{id}/roles")
    public UsuarioDto roles(@PathVariable Long id, @Valid @RequestBody RolesRequest r) {
        return UsuarioDto.de(service.actualizarRoles(id, r.roles(), r.integranteId()));
    }

    @PostMapping("/{id}/restablecer-password")
    public ResponseEntity<Void> restablecer(@PathVariable Long id, @Valid @RequestBody RestablecerRequest r) {
        service.restablecerContrasena(id, r.contrasenaTemporal());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activo")
    public UsuarioDto activo(@PathVariable Long id, @Valid @RequestBody ActivoRequest r) {
        return UsuarioDto.de(service.cambiarActivo(id, r.activo()));
    }
}

package co.natillera.auth.application;

import co.natillera.auth.application.port.out.PasswordHasherPort;
import co.natillera.auth.application.port.out.RefreshTokenRepositoryPort;
import co.natillera.auth.application.port.out.UsuarioRepositoryPort;
import co.natillera.auth.application.port.out.VinculosUsuarioPort;
import co.natillera.auth.domain.PoliticaContrasena;
import co.natillera.auth.domain.Rol;
import co.natillera.auth.domain.Usuario;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Administración de usuarios, roles y vínculos (solo ADMIN). */
@Service
public class UsuarioAdminService {

    public record NuevoUsuario(String nombre, String correo, String celular, Set<Rol> roles,
                               String contrasenaTemporal, Long integranteId) {
    }

    private final UsuarioRepositoryPort usuarios;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final PasswordHasherPort hasher;
    private final VinculosUsuarioPort vinculos;
    private final AuditoriaPort auditoria;
    private final SesionActual sesion;

    public UsuarioAdminService(UsuarioRepositoryPort usuarios, RefreshTokenRepositoryPort refreshTokens,
                               PasswordHasherPort hasher, VinculosUsuarioPort vinculos, AuditoriaPort auditoria,
                               SesionActual sesion) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.hasher = hasher;
        this.vinculos = vinculos;
        this.auditoria = auditoria;
        this.sesion = sesion;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listar();
    }

    @Transactional
    public Usuario crear(NuevoUsuario datos) {
        String correo = normalizarCorreo(datos.correo());
        String celular = datos.celular() == null || datos.celular().isBlank() ? null : datos.celular().trim();
        if (correo == null && celular == null) {
            throw new ReglaNegocioException("Indique correo o celular para el ingreso");
        }
        if (datos.roles() == null || datos.roles().isEmpty()) {
            throw new ReglaNegocioException("El usuario debe tener al menos un rol");
        }
        if (usuarios.existeLogin(correo, celular)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo o celular");
        }
        PoliticaContrasena.validar(datos.contrasenaTemporal(), correo != null ? correo : celular);
        Usuario nuevo = usuarios.guardar(new Usuario(null, datos.nombre().trim(), correo, celular,
                hasher.hash(datos.contrasenaTemporal()), datos.roles(), true, true, null, null));
        aplicarVinculos(nuevo, datos.roles(), datos.integranteId());
        auditoria.registrar("usuario", nuevo.id(), "CREAR", "roles=" + datos.roles());
        return usuarios.buscarPorId(nuevo.id()).orElseThrow();
    }

    @Transactional
    public Usuario actualizarRoles(Long usuarioId, Set<Rol> roles, Long integranteId) {
        if (roles == null || roles.isEmpty()) {
            throw new ReglaNegocioException("El usuario debe tener al menos un rol");
        }
        Usuario usuario = obtener(usuarioId);
        if (usuarioId.equals(sesion.usuario().usuarioId()) && !roles.contains(Rol.ADMIN)) {
            throw new ReglaNegocioException("No puede quitarse a sí mismo el rol de administrador");
        }
        Usuario actualizado = usuarios.guardar(usuario.conRoles(roles));
        aplicarVinculos(actualizado, roles, integranteId);
        refreshTokens.revocarTodosDelUsuario(usuarioId);
        auditoria.registrar("usuario", usuarioId, "CAMBIAR_ROLES", usuario.roles() + " -> " + roles);
        return usuarios.buscarPorId(usuarioId).orElseThrow();
    }

    @Transactional
    public void restablecerContrasena(Long usuarioId, String contrasenaTemporal) {
        Usuario usuario = obtener(usuarioId);
        PoliticaContrasena.validar(contrasenaTemporal, usuario.login());
        usuarios.guardar(usuario.conPassword(hasher.hash(contrasenaTemporal), true));
        refreshTokens.revocarTodosDelUsuario(usuarioId);
        auditoria.registrar("usuario", usuarioId, "RESTABLECER_PASSWORD", null);
    }

    @Transactional
    public Usuario cambiarActivo(Long usuarioId, boolean activo) {
        if (usuarioId.equals(sesion.usuario().usuarioId()) && !activo) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario");
        }
        Usuario usuario = obtener(usuarioId);
        Usuario actualizado = usuarios.guardar(usuario.conActivo(activo));
        if (!activo) {
            refreshTokens.revocarTodosDelUsuario(usuarioId);
        }
        auditoria.registrar("usuario", usuarioId, activo ? "ACTIVAR" : "DESACTIVAR", null);
        return actualizado;
    }

    private void aplicarVinculos(Usuario usuario, Set<Rol> roles, Long integranteId) {
        if (roles.contains(Rol.TESORERO)) {
            vinculos.asegurarTesorero(usuario.id(), usuario.nombre());
        }
        if (integranteId != null) {
            vinculos.vincularIntegrante(integranteId, usuario.id());
        }
    }

    private Usuario obtener(Long usuarioId) {
        return usuarios.buscarPorId(usuarioId).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
    }

    private static String normalizarCorreo(String correo) {
        return correo == null || correo.isBlank() ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}

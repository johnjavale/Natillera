package co.natillera.auth.application;

import co.natillera.auth.application.port.out.AsignacionesPort;
import co.natillera.auth.domain.Alcance;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve el alcance (OWN, ASSIGNED, ALL) del usuario autenticado y lo aplica en cada caso de uso.
 * Disponible en SpEL como {@code @access} (p. ej. {@code @PreAuthorize("@access.puedeVerIntegrante(#id)")}).
 * Un recurso fuera de alcance se reporta como 404 para no revelar que existe (prevención de IDOR).
 */
@Component("access")
public class AccessScopeService {

    private final SesionActual sesion;
    private final AsignacionesPort asignaciones;

    public AccessScopeService(SesionActual sesion, AsignacionesPort asignaciones) {
        this.sesion = sesion;
        this.asignaciones = asignaciones;
    }

    public Alcance alcance() {
        return sesion.usuario().alcance();
    }

    public boolean puedeVerIntegrante(Long integranteId) {
        if (integranteId == null) {
            return false;
        }
        UsuarioActual u = sesion.usuario();
        if (u.esAdmin()) {
            return asignaciones.asignacionDe(integranteId).isPresent();
        }
        if (integranteId.equals(u.integranteId())) {
            return true;
        }
        return u.esTesorero() && esDelTesorero(integranteId, u.tesoreroId());
    }

    /** Lectura: lanza 404 si el integrante no existe o está fuera del alcance. */
    public void exigirVerIntegrante(Long integranteId) {
        if (!puedeVerIntegrante(integranteId)) {
            throw new RecursoNoEncontradoException("Integrante", integranteId);
        }
    }

    /** ADMIN, o TESORERO que administra la cajita del integrante. */
    public boolean puedeGestionarIntegrante(Long integranteId) {
        UsuarioActual u = sesion.usuario();
        if (u.esAdmin()) {
            return true;
        }
        return u.esTesorero() && esDelTesorero(integranteId, u.tesoreroId());
    }

    /** Escritura sobre la cajita de un integrante: 404 si no lo ve, 403 si lo ve pero no lo administra. */
    public void exigirGestionarIntegrante(Long integranteId) {
        exigirVerIntegrante(integranteId);
        if (!puedeGestionarIntegrante(integranteId)) {
            throw new AccessDeniedException("Solo el tesorero de la cajita o el administrador pueden registrar movimientos");
        }
    }

    /** @return vacío si el usuario ve a todos (ADMIN); si no, los ids de integrantes visibles. */
    public Optional<Set<Long>> integrantesVisibles() {
        UsuarioActual u = sesion.usuario();
        if (u.esAdmin()) {
            return Optional.empty();
        }
        Set<Long> ids = new HashSet<>();
        if (u.integranteId() != null) {
            ids.add(u.integranteId());
        }
        if (u.esTesorero()) {
            ids.addAll(asignaciones.integrantesDeTesorero(u.tesoreroId()));
        }
        return Optional.of(ids);
    }

    public boolean esAdmin() {
        return sesion.usuario().esAdmin();
    }

    private boolean esDelTesorero(Long integranteId, Long tesoreroId) {
        return asignaciones.asignacionDe(integranteId)
                .map(a -> Objects.equals(a.tesoreroId(), tesoreroId))
                .orElse(false);
    }
}

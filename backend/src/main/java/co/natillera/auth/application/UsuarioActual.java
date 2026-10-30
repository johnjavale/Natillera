package co.natillera.auth.application;

import co.natillera.auth.domain.Alcance;
import co.natillera.auth.domain.Rol;

import java.util.Set;

/** Usuario autenticado, reconstruido desde los claims del access token. */
public record UsuarioActual(Long usuarioId, String login, String nombre, Set<Rol> roles, Long integranteId,
                            Long tesoreroId, boolean debeCambiarPassword) {

    public UsuarioActual {
        roles = Set.copyOf(roles);
    }

    public boolean esAdmin() {
        return roles.contains(Rol.ADMIN);
    }

    public boolean esTesorero() {
        return roles.contains(Rol.TESORERO) && tesoreroId != null;
    }

    public Alcance alcance() {
        if (esAdmin()) {
            return Alcance.ALL;
        }
        return esTesorero() ? Alcance.ASSIGNED : Alcance.OWN;
    }
}

package co.natillera.auth.domain;

import java.util.Set;

/** Usuario del sistema. Puede tener varios roles y estar vinculado a un integrante y/o un tesorero. */
public record Usuario(
        Long id,
        String nombre,
        String correo,
        String celular,
        String passwordHash,
        Set<Rol> roles,
        boolean activo,
        boolean debeCambiarPassword,
        Long integranteId,
        Long tesoreroId) {

    public Usuario {
        roles = Set.copyOf(roles);
    }

    public String login() {
        return correo != null ? correo : celular;
    }

    public Usuario conPassword(String nuevoHash, boolean debeCambiar) {
        return new Usuario(id, nombre, correo, celular, nuevoHash, roles, activo, debeCambiar, integranteId, tesoreroId);
    }

    public Usuario conRoles(Set<Rol> nuevosRoles) {
        return new Usuario(id, nombre, correo, celular, passwordHash, nuevosRoles, activo, debeCambiarPassword,
                integranteId, tesoreroId);
    }

    public Usuario conActivo(boolean nuevoActivo) {
        return new Usuario(id, nombre, correo, celular, passwordHash, roles, nuevoActivo, debeCambiarPassword,
                integranteId, tesoreroId);
    }
}

package co.natillera.auth.domain;

import java.time.Instant;

/** Enlace de recuperación de contraseña enviado por correo. Solo se guarda su hash; es de un solo uso. */
public record TokenRecuperacion(Long id, Long usuarioId, String tokenHash, Instant expiraEn, boolean usado) {

    public boolean vigente(Instant ahora) {
        return !usado && ahora.isBefore(expiraEn);
    }
}

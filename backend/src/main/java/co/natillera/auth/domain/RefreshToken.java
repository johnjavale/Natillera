package co.natillera.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token opaco. Solo se guarda su hash. Cada uso lo marca como usado y emite uno nuevo de la misma familia;
 * si llega uno ya usado o revocado, se considera robado y se revoca toda la familia.
 */
public record RefreshToken(Long id, Long usuarioId, String tokenHash, UUID familia, Instant expiraEn, boolean usado,
                           boolean revocado) {

    public boolean expirado(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }

    public boolean reutilizado() {
        return usado || revocado;
    }
}

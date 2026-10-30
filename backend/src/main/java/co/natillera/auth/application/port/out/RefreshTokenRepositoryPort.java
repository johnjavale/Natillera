package co.natillera.auth.application.port.out;

import co.natillera.auth.domain.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    RefreshToken guardar(RefreshToken token);

    void marcarUsado(Long id);

    void revocarFamilia(UUID familia);

    void revocarTodosDelUsuario(Long usuarioId);
}

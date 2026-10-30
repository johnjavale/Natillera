package co.natillera.auth.infrastructure.persistence;

import co.natillera.auth.application.port.out.RefreshTokenRepositoryPort;
import co.natillera.auth.domain.RefreshToken;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;
    private final Clock clock;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(e -> new RefreshToken(e.getId(), e.getUsuarioId(),
                e.getTokenHash(), e.getFamilia(), e.getExpiraEn(), e.isUsado(), e.isRevocado()));
    }

    @Override
    public RefreshToken guardar(RefreshToken t) {
        RefreshTokenJpaEntity e = repository.save(new RefreshTokenJpaEntity(t.usuarioId(), t.tokenHash(), t.familia(),
                t.expiraEn(), clock.instant()));
        return new RefreshToken(e.getId(), t.usuarioId(), t.tokenHash(), t.familia(), t.expiraEn(), false, false);
    }

    @Override
    public void marcarUsado(Long id) {
        repository.marcarUsado(id);
    }

    @Override
    public void revocarFamilia(UUID familia) {
        repository.revocarFamilia(familia);
    }

    @Override
    public void revocarTodosDelUsuario(Long usuarioId) {
        repository.revocarDelUsuario(usuarioId);
    }
}

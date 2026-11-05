package co.natillera.auth.infrastructure.persistence;

import co.natillera.auth.application.port.out.TokenRecuperacionRepositoryPort;
import co.natillera.auth.domain.TokenRecuperacion;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class TokenRecuperacionPersistenceAdapter implements TokenRecuperacionRepositoryPort {

    private final TokenRecuperacionJpaRepository repository;
    private final Clock clock;

    public TokenRecuperacionPersistenceAdapter(TokenRecuperacionJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<TokenRecuperacion> buscarPorHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(e -> new TokenRecuperacion(e.getId(), e.getUsuarioId(),
                e.getTokenHash(), e.getExpiraEn(), e.isUsado()));
    }

    @Override
    public void guardar(TokenRecuperacion t) {
        repository.save(new TokenRecuperacionJpaEntity(t.usuarioId(), t.tokenHash(), t.expiraEn(), clock.instant()));
    }

    @Override
    public void invalidarDelUsuario(Long usuarioId) {
        repository.invalidarDelUsuario(usuarioId);
    }
}

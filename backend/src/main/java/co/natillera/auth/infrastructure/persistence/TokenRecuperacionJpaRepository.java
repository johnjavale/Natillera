package co.natillera.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionJpaRepository extends JpaRepository<TokenRecuperacionJpaEntity, Long> {

    Optional<TokenRecuperacionJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRecuperacionJpaEntity t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    void invalidarDelUsuario(@Param("usuarioId") Long usuarioId);
}

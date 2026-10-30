package co.natillera.prestamos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositorios Spring Data del módulo de préstamos. */
public final class PrestamoJpaRepositories {

    private PrestamoJpaRepositories() {
    }

    public interface PrestamoJpaRepository extends JpaRepository<PrestamoJpaEntity, Long> {

        List<PrestamoJpaEntity> findAllByOrderByIdAsc();
    }

    public interface PagoCuotaJpaRepository extends JpaRepository<PagoCuotaJpaEntity, Long> {

        List<PagoCuotaJpaEntity> findByAnuladoFalse();

        List<PagoCuotaJpaEntity> findByPrestamoIdOrderByNumeroCuotaAsc(Long prestamoId);
    }
}

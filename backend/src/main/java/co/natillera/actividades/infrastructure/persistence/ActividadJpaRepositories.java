package co.natillera.actividades.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositorios Spring Data del módulo de actividades. */
public final class ActividadJpaRepositories {

    private ActividadJpaRepositories() {
    }

    public interface ActividadJpaRepository extends JpaRepository<ActividadJpaEntity, Long> {

        List<ActividadJpaEntity> findAllByOrderByFechaSorteoAscIdAsc();
    }

    public interface AsignacionBoletasJpaRepository
            extends JpaRepository<AsignacionBoletasJpaEntity, AsignacionBoletasJpaEntity.Clave> {
    }
}

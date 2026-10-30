package co.natillera.tesoreria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorios Spring Data del módulo de tesorería. */
public final class TesoreriaJpaRepositories {

    private TesoreriaJpaRepositories() {
    }

    public interface MovimientoJpaRepository extends JpaRepository<MovimientoJpaEntity, Long> {

        Optional<MovimientoJpaEntity> findFirstByPagoCuotaIdAndAnulaAIsNull(Long pagoCuotaId);

        boolean existsByAnulaA(Long anulaA);
    }

    public interface CajitaJpaRepository extends JpaRepository<CajitaJpaEntity, Long> {

        Optional<CajitaJpaEntity> findByIntegranteId(Long integranteId);

        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("update CajitaJpaEntity c set c.nombre = :nombre where c.integranteId = :integranteId")
        void renombrar(@Param("integranteId") Long integranteId, @Param("nombre") String nombre);
    }

    public interface TesoreroJpaRepository extends JpaRepository<TesoreroJpaEntity, Long> {

        Optional<TesoreroJpaEntity> findByUsuarioId(Long usuarioId);

        List<TesoreroJpaEntity> findAllByOrderByIdAsc();
    }

    public interface SaldoReportadoNuJpaRepository extends JpaRepository<SaldoReportadoNuJpaEntity, Long> {
    }
}

package co.natillera.integrantes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IntegranteJpaRepository extends JpaRepository<IntegranteJpaEntity, Long> {

    List<IntegranteJpaEntity> findAllByOrderByIdAsc();

    int countByActivoTrue();

    @Query("select i.id from IntegranteJpaEntity i where i.tesoreroId = :tesoreroId")
    List<Long> idsDeTesorero(@Param("tesoreroId") Long tesoreroId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update IntegranteJpaEntity i set i.usuarioId = null where i.usuarioId = :usuarioId")
    void desvincularUsuario(@Param("usuarioId") Long usuarioId);
}

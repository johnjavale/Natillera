package co.natillera.ciclo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CicloJpaRepository extends JpaRepository<CicloJpaEntity, Long> {

    Optional<CicloJpaEntity> findFirstByOrderByIdDesc();
}

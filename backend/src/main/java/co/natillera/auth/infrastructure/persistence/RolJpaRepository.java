package co.natillera.auth.infrastructure.persistence;

import co.natillera.auth.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolJpaRepository extends JpaRepository<RolJpaEntity, Short> {

    List<RolJpaEntity> findByNombreIn(Collection<Rol> nombres);
}

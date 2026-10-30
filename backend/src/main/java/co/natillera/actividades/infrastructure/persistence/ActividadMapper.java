package co.natillera.actividades.infrastructure.persistence;

import co.natillera.actividades.domain.Actividad;
import co.natillera.shared.infrastructure.persistence.DineroMapeo;
import org.mapstruct.Mapper;

@Mapper(uses = DineroMapeo.class)
public interface ActividadMapper {

    Actividad aDominio(ActividadJpaEntity entidad);

    ActividadJpaEntity aEntidad(Actividad actividad);
}

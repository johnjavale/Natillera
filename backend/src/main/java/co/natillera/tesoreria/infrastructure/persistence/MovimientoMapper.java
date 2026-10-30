package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.DineroMapeo;
import co.natillera.tesoreria.domain.Movimiento;
import org.mapstruct.Mapper;

@Mapper(uses = DineroMapeo.class)
public interface MovimientoMapper {

    Movimiento aDominio(MovimientoJpaEntity entidad);

    MovimientoJpaEntity aEntidad(Movimiento movimiento);
}

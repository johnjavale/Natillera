package co.natillera.prestamos.infrastructure.persistence;

import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.shared.infrastructure.persistence.DineroMapeo;
import org.mapstruct.Mapper;

@Mapper(uses = DineroMapeo.class)
public interface PrestamoMapper {

    Prestamo aDominio(PrestamoJpaEntity entidad);

    PrestamoJpaEntity aEntidad(Prestamo prestamo);

    PagoCuota aDominio(PagoCuotaJpaEntity entidad);

    PagoCuotaJpaEntity aEntidad(PagoCuota pago);
}

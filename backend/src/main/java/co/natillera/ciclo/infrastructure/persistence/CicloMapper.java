package co.natillera.ciclo.infrastructure.persistence;

import co.natillera.ciclo.domain.Ciclo;
import co.natillera.shared.infrastructure.persistence.DineroMapeo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = DineroMapeo.class)
public interface CicloMapper {

    Ciclo aDominio(CicloJpaEntity entidad);

    @Mapping(target = "id", ignore = true)
    void copiar(Ciclo ciclo, @MappingTarget CicloJpaEntity entidad);
}

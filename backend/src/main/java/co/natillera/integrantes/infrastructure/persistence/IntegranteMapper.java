package co.natillera.integrantes.infrastructure.persistence;

import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.infrastructure.persistence.DineroMapeo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = DineroMapeo.class)
public interface IntegranteMapper {

    Integrante aDominio(IntegranteJpaEntity entidad);

    @Mapping(target = "id", ignore = true)
    void copiar(Integrante integrante, @MappingTarget IntegranteJpaEntity entidad);
}

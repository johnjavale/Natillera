package co.natillera.ciclo.infrastructure.persistence;

import co.natillera.ciclo.application.port.out.CicloRepositoryPort;
import co.natillera.ciclo.domain.Ciclo;
import org.springframework.stereotype.Component;

@Component
public class CicloPersistenceAdapter implements CicloRepositoryPort {

    private final CicloJpaRepository repository;
    private final CicloMapper mapper;

    public CicloPersistenceAdapter(CicloJpaRepository repository, CicloMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Ciclo actual() {
        return repository.findFirstByOrderByIdDesc().map(mapper::aDominio)
                .orElseThrow(() -> new IllegalStateException("No hay un ciclo configurado"));
    }

    @Override
    public Ciclo guardar(Ciclo ciclo) {
        CicloJpaEntity entidad = ciclo.id() == null ? new CicloJpaEntity()
                : repository.findById(ciclo.id()).orElseThrow();
        mapper.copiar(ciclo, entidad);
        return mapper.aDominio(repository.save(entidad));
    }
}

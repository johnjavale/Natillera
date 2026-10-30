package co.natillera.shared.infrastructure.persistence;

import co.natillera.shared.application.AuditoriaPort;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class AuditoriaAdapter implements AuditoriaPort {

    private static final int MAX_DETALLE = 1000;

    private final AuditoriaJpaRepository repository;
    private final AuditorAware<String> auditor;
    private final Clock clock;

    public AuditoriaAdapter(AuditoriaJpaRepository repository, AuditorAware<String> auditor, Clock clock) {
        this.repository = repository;
        this.auditor = auditor;
        this.clock = clock;
    }

    @Override
    public void registrar(String entidad, Long entidadId, String accion, String detalle) {
        String recortado = detalle != null && detalle.length() > MAX_DETALLE ? detalle.substring(0, MAX_DETALLE) : detalle;
        repository.save(new AuditoriaJpaEntity(entidad, entidadId, accion, recortado,
                auditor.getCurrentAuditor().orElse("sistema"), OffsetDateTime.now(clock)));
    }

    public Page<AuditoriaJpaEntity> listar(int pagina, int tamano) {
        return repository.findAll(PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fecha", "id")));
    }
}

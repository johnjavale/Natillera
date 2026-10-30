package co.natillera.integrantes.infrastructure.persistence;

import co.natillera.auth.application.port.out.AsignacionesPort;
import co.natillera.auth.application.port.out.VinculosUsuarioPort;
import co.natillera.integrantes.application.port.out.IntegranteRepositoryPort;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA de integrantes. También implementa los puertos que el módulo auth necesita
 * (alcance de tesoreros y vínculo usuario ↔ integrante/tesorero).
 */
@Component
public class IntegrantePersistenceAdapter implements IntegranteRepositoryPort, AsignacionesPort, VinculosUsuarioPort {

    private final IntegranteJpaRepository repository;
    private final IntegranteMapper mapper;
    private final TesoreriaRepositoryPort tesoreria;
    private final EntityManager em;

    public IntegrantePersistenceAdapter(IntegranteJpaRepository repository, IntegranteMapper mapper,
                                        TesoreriaRepositoryPort tesoreria, EntityManager em) {
        this.repository = repository;
        this.mapper = mapper;
        this.tesoreria = tesoreria;
        this.em = em;
    }

    @Override
    public List<Integrante> listar() {
        return repository.findAllByOrderByIdAsc().stream().map(mapper::aDominio).toList();
    }

    @Override
    public Optional<Integrante> buscar(Long id) {
        return repository.findById(id).map(mapper::aDominio);
    }

    @Override
    public Integrante guardar(Integrante integrante) {
        IntegranteJpaEntity e = integrante.id() == null ? new IntegranteJpaEntity()
                : repository.findById(integrante.id()).orElseThrow();
        mapper.copiar(integrante, e);
        IntegranteJpaEntity guardado = repository.saveAndFlush(e);
        Integrante resultado = mapper.aDominio(guardado);
        // El nombre de la cajita se calcula con @Formula; antes de crearla se conserva el solicitado
        return new Integrante(resultado.id(), resultado.usuarioId(), resultado.nombre(), resultado.telefono(),
                resultado.cuotaMensual(), resultado.tesoreroId(), integrante.nombreCajitaNu(), resultado.activo());
    }

    @Override
    public int contarActivos() {
        return repository.countByActivoTrue();
    }

    @Override
    public Optional<Asignacion> asignacionDe(Long integranteId) {
        return repository.findById(integranteId).map(i -> new Asignacion(i.getId(), i.getTesoreroId()));
    }

    @Override
    public List<Long> integrantesDeTesorero(Long tesoreroId) {
        return repository.idsDeTesorero(tesoreroId);
    }

    @Override
    public Long asegurarTesorero(Long usuarioId, String nombre) {
        return tesoreria.asegurarTesorero(usuarioId, nombre).id();
    }

    @Override
    public void vincularIntegrante(Long integranteId, Long usuarioId) {
        IntegranteJpaEntity integrante = repository.findById(integranteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Integrante", integranteId));
        repository.desvincularUsuario(usuarioId);
        em.flush();
        integrante.setUsuarioId(usuarioId);
        repository.save(integrante);
    }
}

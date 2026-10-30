package co.natillera.actividades.infrastructure.persistence;

import co.natillera.actividades.application.port.out.ActividadRepositoryPort;
import co.natillera.actividades.domain.Actividad;
import co.natillera.actividades.infrastructure.persistence.ActividadJpaRepositories.ActividadJpaRepository;
import co.natillera.actividades.infrastructure.persistence.ActividadJpaRepositories.AsignacionBoletasJpaRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ActividadPersistenceAdapter implements ActividadRepositoryPort {

    private final ActividadJpaRepository actividades;
    private final AsignacionBoletasJpaRepository asignaciones;
    private final ActividadMapper mapper;

    public ActividadPersistenceAdapter(ActividadJpaRepository actividades, AsignacionBoletasJpaRepository asignaciones,
                                       ActividadMapper mapper) {
        this.actividades = actividades;
        this.asignaciones = asignaciones;
        this.mapper = mapper;
    }

    @Override
    public List<Actividad> listar() {
        return actividades.findAllByOrderByFechaSorteoAscIdAsc().stream().map(mapper::aDominio).toList();
    }

    @Override
    public Optional<Actividad> buscar(Long id) {
        return actividades.findById(id).map(mapper::aDominio);
    }

    @Override
    public Actividad guardar(Actividad actividad) {
        return mapper.aDominio(actividades.save(mapper.aEntidad(actividad)));
    }

    @Override
    public int contar() {
        return (int) actividades.count();
    }

    @Override
    public Map<Long, Map<Long, Integer>> asignaciones() {
        Map<Long, Map<Long, Integer>> r = new HashMap<>();
        asignaciones.findAll().forEach(a -> r.computeIfAbsent(a.getActividadId(), k -> new HashMap<>())
                .put(a.getIntegranteId(), a.getBoletas()));
        return r;
    }

    @Override
    public void asignarBoletas(Long actividadId, Long integranteId, int boletas) {
        AsignacionBoletasJpaEntity e = asignaciones.findById(new AsignacionBoletasJpaEntity.Clave(actividadId, integranteId))
                .orElseGet(() -> new AsignacionBoletasJpaEntity(actividadId, integranteId, boletas));
        e.setBoletas(boletas);
        asignaciones.save(e);
    }
}

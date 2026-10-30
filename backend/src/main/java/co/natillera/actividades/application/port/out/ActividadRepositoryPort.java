package co.natillera.actividades.application.port.out;

import co.natillera.actividades.domain.Actividad;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ActividadRepositoryPort {

    List<Actividad> listar();

    Optional<Actividad> buscar(Long id);

    Actividad guardar(Actividad actividad);

    int contar();

    /** actividadId → (integranteId → boletas) para los integrantes con un número distinto al de la actividad. */
    Map<Long, Map<Long, Integer>> asignaciones();

    void asignarBoletas(Long actividadId, Long integranteId, int boletas);
}

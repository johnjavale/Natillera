package co.natillera.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/** Puerto hacia el módulo de integrantes para resolver el alcance de un tesorero. */
public interface AsignacionesPort {

    record Asignacion(Long integranteId, Long tesoreroId) {
    }

    /** @return vacío si el integrante no existe */
    Optional<Asignacion> asignacionDe(Long integranteId);

    List<Long> integrantesDeTesorero(Long tesoreroId);
}

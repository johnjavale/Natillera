package co.natillera.prestamos.domain.regla;

import co.natillera.shared.domain.ReglaNegocioException;

import java.util.Optional;

/**
 * Patrón Specification/Policy: una regla de validación de préstamos.
 * Las reglas se encadenan con {@link #y(ReglaPrestamo)}; la primera que falle detiene la evaluación.
 */
@FunctionalInterface
public interface ReglaPrestamo {

    /** @return el mensaje de error en español, o vacío si la solicitud cumple la regla */
    Optional<String> evaluar(SolicitudPrestamo solicitud);

    default ReglaPrestamo y(ReglaPrestamo siguiente) {
        return solicitud -> {
            Optional<String> error = evaluar(solicitud);
            return error.isPresent() ? error : siguiente.evaluar(solicitud);
        };
    }

    default void validar(SolicitudPrestamo solicitud) {
        evaluar(solicitud).ifPresent(mensaje -> {
            throw new ReglaNegocioException(mensaje);
        });
    }
}

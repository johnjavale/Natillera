package co.natillera.actividades.domain;

import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

import java.time.LocalDate;
import java.util.Objects;

/** Actividad (rifa) del ciclo (hoja "Actividades"). */
public record Actividad(
        Long id,
        String nombre,
        LocalDate fechaSorteo,
        Dinero valorBoleta,
        int boletasPorIntegrante,
        Dinero costoPremio,
        Dinero otrosGastos,
        String ganador) {

    public Actividad {
        Objects.requireNonNull(fechaSorteo, "fechaSorteo");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre de la actividad es obligatorio");
        }
        if (valorBoleta == null || valorBoleta.esNegativo()) {
            throw new ReglaNegocioException("El valor de la boleta no puede ser negativo");
        }
        if (boletasPorIntegrante < 0) {
            throw new ReglaNegocioException("Las boletas por integrante no pueden ser negativas");
        }
        costoPremio = costoPremio == null ? Dinero.CERO : costoPremio;
        otrosGastos = otrosGastos == null ? Dinero.CERO : otrosGastos;
        if (costoPremio.esNegativo() || otrosGastos.esNegativo()) {
            throw new ReglaNegocioException("Los costos no pueden ser negativos");
        }
    }

    /** costos = costoPremio + otrosGastos */
    public Dinero costos() {
        return costoPremio.sumar(otrosGastos);
    }
}

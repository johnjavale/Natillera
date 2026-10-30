package co.natillera.reparticion.domain;

import co.natillera.shared.domain.Dinero;

import java.util.List;
import java.util.Map;

/** Patrón Strategy: cómo se reparte un monto entre los integrantes. */
public interface EstrategiaReparto {

    record Participante(Long integranteId, Dinero ahorro) {
    }

    /** @return monto asignado a cada integrante (por id) */
    Map<Long, Dinero> repartir(Dinero monto, List<Participante> participantes);
}

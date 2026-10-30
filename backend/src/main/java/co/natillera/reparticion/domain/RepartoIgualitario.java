package co.natillera.reparticion.domain;

import co.natillera.shared.domain.Dinero;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Utilidad de actividades (rifas): partes iguales entre los integrantes. */
public class RepartoIgualitario implements EstrategiaReparto {

    @Override
    public Map<Long, Dinero> repartir(Dinero monto, List<Participante> participantes) {
        Map<Long, Dinero> resultado = new LinkedHashMap<>();
        if (participantes.isEmpty()) {
            return resultado;
        }
        Dinero parte = monto.dividir(participantes.size());
        for (Participante p : participantes) {
            resultado.put(p.integranteId(), parte);
        }
        return resultado;
    }
}

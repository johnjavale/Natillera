package co.natillera.reparticion.domain;

import co.natillera.shared.domain.Dinero;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Intereses de préstamos: quien más ahorra, más recibe (% participación = ahorro / Σ ahorro). */
public class RepartoProporcionalAlAhorro implements EstrategiaReparto {

    @Override
    public Map<Long, Dinero> repartir(Dinero monto, List<Participante> participantes) {
        Dinero totalAhorro = Dinero.sumarTodos(participantes.stream().map(Participante::ahorro).toList());
        Map<Long, Dinero> resultado = new LinkedHashMap<>();
        for (Participante p : participantes) {
            resultado.put(p.integranteId(), monto.multiplicar(p.ahorro().proporcionDe(totalAhorro)));
        }
        return resultado;
    }
}

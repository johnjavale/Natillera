package co.natillera.reparticion.domain;

import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.Ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Repartición al cierre, proyectada y real (sección 3.7). */
public class CalculadoraReparticion {

    private final EstrategiaReparto estrategiaIntereses;
    private final EstrategiaReparto estrategiaActividades;

    public CalculadoraReparticion(EstrategiaReparto estrategiaIntereses, EstrategiaReparto estrategiaActividades) {
        this.estrategiaIntereses = estrategiaIntereses;
        this.estrategiaActividades = estrategiaActividades;
    }

    /** Reglas de la natillera: intereses proporcionales al ahorro, actividades en partes iguales. */
    public static CalculadoraReparticion porDefecto() {
        return new CalculadoraReparticion(new RepartoProporcionalAlAhorro(), new RepartoIgualitario());
    }

    public record Integrante(Long integranteId, String nombre, Dinero ahorroEsperado, Dinero totalAportado,
                             Dinero rendimientoNuProyectado, Dinero rendimientoNuReal) {
    }

    public record Entrada(List<Integrante> integrantes, Dinero interesesProyectados, Dinero interesesRecibidos,
                          Dinero gastosAdministrativos, Dinero utilidadActividadesEsperada,
                          Dinero utilidadActividadesReal) {
    }

    public Reparticion calcular(Entrada e) {
        int n = e.integrantes().size();
        Reparticion.Conceptos proy = conceptos(e.interesesProyectados(), e.gastosAdministrativos(),
                e.utilidadActividadesEsperada(), n);
        Reparticion.Conceptos real = conceptos(e.interesesRecibidos(), e.gastosAdministrativos(),
                e.utilidadActividadesReal(), n);

        List<EstrategiaReparto.Participante> porEsperado = e.integrantes().stream()
                .map(i -> new EstrategiaReparto.Participante(i.integranteId(), i.ahorroEsperado())).toList();
        List<EstrategiaReparto.Participante> porReal = e.integrantes().stream()
                .map(i -> new EstrategiaReparto.Participante(i.integranteId(), i.totalAportado())).toList();

        Map<Long, Dinero> interesesProy = estrategiaIntereses.repartir(proy.interesesNetos(), porEsperado);
        Map<Long, Dinero> interesesReal = estrategiaIntereses.repartir(real.interesesNetos(), porReal);
        Map<Long, Dinero> actividadesProy = estrategiaActividades.repartir(proy.utilidadActividades(), porEsperado);
        Map<Long, Dinero> actividadesReal = estrategiaActividades.repartir(real.utilidadActividades(), porReal);

        Dinero totalEsperado = Dinero.sumarTodos(porEsperado.stream().map(EstrategiaReparto.Participante::ahorro).toList());
        Dinero totalReal = Dinero.sumarTodos(porReal.stream().map(EstrategiaReparto.Participante::ahorro).toList());

        List<Reparticion.Fila> filas = new ArrayList<>(n);
        for (Integrante i : e.integrantes()) {
            Dinero intP = interesesProy.getOrDefault(i.integranteId(), Dinero.CERO);
            Dinero actP = actividadesProy.getOrDefault(i.integranteId(), Dinero.CERO);
            Dinero intR = interesesReal.getOrDefault(i.integranteId(), Dinero.CERO);
            Dinero actR = actividadesReal.getOrDefault(i.integranteId(), Dinero.CERO);
            filas.add(new Reparticion.Fila(
                    i.integranteId(), i.nombre(),
                    i.ahorroEsperado(), i.ahorroEsperado().proporcionDe(totalEsperado), intP, actP,
                    i.rendimientoNuProyectado(),
                    i.ahorroEsperado().sumar(intP).sumar(actP).sumar(i.rendimientoNuProyectado()),
                    i.totalAportado(), i.totalAportado().proporcionDe(totalReal), intR, actR, i.rendimientoNuReal(),
                    i.totalAportado().sumar(intR).sumar(actR).sumar(i.rendimientoNuReal()),
                    Ranking.puesto(i, e.integrantes(), Integrante::ahorroEsperado),
                    Ranking.puesto(i, e.integrantes(), Integrante::totalAportado)));
        }
        return new Reparticion(proy, real, filas);
    }

    private static Reparticion.Conceptos conceptos(Dinero intereses, Dinero gastos, Dinero utilidadActividades,
                                                   int numeroIntegrantes) {
        Dinero netos = intereses.restar(gastos).max(Dinero.CERO);
        Dinero porIntegrante = numeroIntegrantes == 0 ? Dinero.CERO : utilidadActividades.dividir(numeroIntegrantes);
        return new Reparticion.Conceptos(intereses, gastos, netos, utilidadActividades, porIntegrante);
    }
}

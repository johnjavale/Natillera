package co.natillera.integrantes.domain;

import co.natillera.shared.domain.Dinero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aportes derivados del libro de movimientos (sección 3.3).
 * aporteReal(integrante, mes) llega ya agregado desde los movimientos AHORRO.
 */
public final class CalculadoraAportes {

    private CalculadoraAportes() {
    }

    public static EstadoAportes estado(Dinero cuotaMensual, int numeroMeses, int mesesTranscurridos,
                                       Map<Integer, Dinero> aportesPorMes) {
        List<Dinero> porMes = new ArrayList<>(numeroMeses);
        Dinero total = Dinero.CERO;
        for (int mes = 1; mes <= numeroMeses; mes++) {
            Dinero aporte = aportesPorMes.getOrDefault(mes, Dinero.CERO);
            porMes.add(aporte);
            total = total.sumar(aporte);
        }
        Dinero esperado = CalculadoraAhorro.ahorroEsperado(cuotaMensual, numeroMeses);
        Dinero esperadoALaFecha = cuotaMensual.multiplicar(mesesTranscurridos);
        Dinero atraso = esperadoALaFecha.restar(total).max(Dinero.CERO);
        return new EstadoAportes(porMes, total, esperado, total.restar(esperado), total.proporcionDe(esperado),
                esperadoALaFecha, atraso);
    }

    /** Totales por mes: real, esperado (Σ cuotas) y sus acumulados. */
    public static List<TotalMes> totalesPorMes(int numeroMeses, Dinero sumaCuotas, List<EstadoAportes> filas) {
        List<TotalMes> totales = new ArrayList<>(numeroMeses);
        Dinero acumReal = Dinero.CERO;
        Dinero acumEsperado = Dinero.CERO;
        for (int i = 0; i < numeroMeses; i++) {
            Dinero real = Dinero.CERO;
            for (EstadoAportes f : filas) {
                real = real.sumar(f.aportesPorMes().get(i));
            }
            acumReal = acumReal.sumar(real);
            acumEsperado = acumEsperado.sumar(sumaCuotas);
            totales.add(new TotalMes(i + 1, real, sumaCuotas, acumReal, acumEsperado));
        }
        return totales;
    }

    public record TotalMes(int mes, Dinero totalReal, Dinero totalEsperado, Dinero acumuladoReal,
                           Dinero acumuladoEsperado) {
    }
}

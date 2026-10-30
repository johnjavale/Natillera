package co.natillera.resumen.domain;

import co.natillera.prestamos.domain.CalculadoraPrestamo;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Flujo de caja proyectado mes a mes (sección 3.8). */
public final class CalculadoraResumen {

    private CalculadoraResumen() {
    }

    /** Utilidad esperada de una actividad ubicada en su mes del ciclo. */
    public record ActividadMes(int mes, Dinero utilidadEsperada) {
    }

    /**
     * @param sumaCuotas             Σ cuotas mensuales de los integrantes activos
     * @param ahorroRealPorMes       Σ aportes de cada mes (clave = mes del ciclo)
     * @param capitalRealPorMes      capital efectivamente recuperado por pagos (clave = mes del ciclo)
     */
    public static List<FilaResumenMensual> calcular(LocalDate inicio, int numeroMeses, Dinero sumaCuotas,
                                                    Map<Integer, Dinero> ahorroRealPorMes,
                                                    List<Prestamo> prestamos,
                                                    Map<Integer, Dinero> capitalRealPorMes,
                                                    List<ActividadMes> actividades) {
        List<FilaResumenMensual> filas = new ArrayList<>(numeroMeses);
        Dinero caja = Dinero.CERO;
        Dinero fondo = Dinero.CERO;
        for (int m = 1; m <= numeroMeses; m++) {
            Dinero entregados = Dinero.CERO;
            Dinero capital = Dinero.CERO;
            Dinero intereses = Dinero.CERO;
            for (Prestamo p : prestamos) {
                if (p.mesDelPrestamo() == m) {
                    entregados = entregados.sumar(p.monto());
                }
                capital = capital.sumar(CalculadoraPrestamo.capitalDelMes(p, m));
                intereses = intereses.sumar(CalculadoraPrestamo.interesDelMes(p, m));
            }
            Dinero utilidad = Dinero.CERO;
            for (ActividadMes a : actividades) {
                if (a.mes() == m) {
                    utilidad = utilidad.sumar(a.utilidadEsperada());
                }
            }
            Dinero flujo = sumaCuotas.restar(entregados).sumar(capital).sumar(intereses).sumar(utilidad);
            caja = caja.sumar(flujo);
            fondo = fondo.sumar(sumaCuotas).sumar(intereses).sumar(utilidad);
            filas.add(new FilaResumenMensual(m, CalendarioCiclo.fechaDelMes(inicio, m), sumaCuotas,
                    ahorroRealPorMes.getOrDefault(m, Dinero.CERO), entregados, capital,
                    capitalRealPorMes.getOrDefault(m, Dinero.CERO), intereses, utilidad, flujo, caja, fondo));
        }
        return filas;
    }
}

package co.natillera.prestamos.domain;

import co.natillera.shared.domain.Dinero;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Fórmulas de préstamos con interés simple (sección 3.4). */
public final class CalculadoraPrestamo {

    private CalculadoraPrestamo() {
    }

    /** interesPorCuota = monto × tasaMensual */
    public static Dinero interesPorCuota(Prestamo p) {
        return p.monto().multiplicar(p.tasaMensual());
    }

    /** abonoCapitalPorCuota = monto / numeroCuotas (valor exacto, como en el Excel) */
    public static Dinero abonoCapitalPorCuota(Prestamo p) {
        return p.monto().dividir(p.numeroCuotas());
    }

    /** valorCuota = interesPorCuota + abonoCapitalPorCuota */
    public static Dinero valorCuota(Prestamo p) {
        return interesPorCuota(p).sumar(abonoCapitalPorCuota(p));
    }

    /** totalIntereses = interesPorCuota × numeroCuotas */
    public static Dinero totalIntereses(Prestamo p) {
        return interesPorCuota(p).multiplicar(p.numeroCuotas());
    }

    /** totalAPagar = monto + totalIntereses */
    public static Dinero totalAPagar(Prestamo p) {
        return p.monto().sumar(totalIntereses(p));
    }

    /**
     * Plan de pagos. Mejora sobre el Excel: cada cuota se redondea al peso y el residuo del capital se ajusta
     * en la última cuota, de modo que Σ capital = monto exacto. La primera cuota vence el mes siguiente a la entrega.
     */
    public static List<CuotaPlan> planDePagos(Prestamo p) {
        int n = p.numeroCuotas();
        Dinero capitalRedondeado = abonoCapitalPorCuota(p).redondearAlPeso();
        Dinero interes = interesPorCuota(p).redondearAlPeso();
        List<CuotaPlan> plan = new ArrayList<>(n);
        Dinero capitalAcumulado = Dinero.CERO;
        for (int k = 1; k <= n; k++) {
            Dinero capital = k < n ? capitalRedondeado : p.monto().restar(capitalAcumulado);
            capitalAcumulado = capitalAcumulado.sumar(capital);
            plan.add(new CuotaPlan(k, p.mesDelPrestamo() + k, capital, interes, capital.sumar(interes)));
        }
        return plan;
    }

    /**
     * Resumen con el estado del préstamo.
     *
     * @param cuotasPagadasNumeros números de cuota (1..n) con pago registrado
     * @param mesesTranscurridos   meses transcurridos del ciclo a la fecha
     */
    public static ResumenPrestamo resumir(Prestamo p, Collection<Integer> cuotasPagadasNumeros,
                                          int mesesTranscurridos) {
        int n = p.numeroCuotas();
        Set<Integer> pagadas = new HashSet<>(cuotasPagadasNumeros);
        int cuotasPagadas = pagadas.size();
        int pagadasEfectivas = Math.min(cuotasPagadas, n);

        Dinero interesPorCuota = interesPorCuota(p);
        Dinero valorCuota = valorCuota(p);
        Dinero totalAPagar = totalAPagar(p);
        Dinero interesesRecibidos = interesPorCuota.multiplicar(pagadasEfectivas);
        Dinero saldoPendiente = totalAPagar.restar(valorCuota.multiplicar(pagadasEfectivas)).max(Dinero.CERO);

        List<CuotaPlan> plan = planDePagos(p);
        Set<Integer> vencidas = new TreeSet<>();
        Dinero saldoVencido = Dinero.CERO;
        for (CuotaPlan c : plan) {
            if (c.mesVencimiento() <= mesesTranscurridos && !pagadas.contains(c.numero())) {
                vencidas.add(c.numero());
                saldoVencido = saldoVencido.sumar(c.valor());
            }
        }
        int cuotasVencidas = vencidas.size();

        EstadoPrestamo estado;
        if (cuotasPagadas >= n) {
            estado = EstadoPrestamo.PAGADO;
        } else if (cuotasVencidas > 0) {
            estado = EstadoPrestamo.EN_MORA;
        } else {
            estado = EstadoPrestamo.VIGENTE;
        }
        return new ResumenPrestamo(p, interesPorCuota, abonoCapitalPorCuota(p), valorCuota, totalIntereses(p),
                totalAPagar, cuotasPagadas, interesesRecibidos, saldoPendiente, cuotasVencidas, saldoVencido, estado,
                plan, Set.copyOf(pagadas), Set.copyOf(vencidas));
    }

    /** interés(m) = interesPorCuota si mesDelPrestamo < m ≤ mesDelPrestamo + n; si no, 0. */
    public static Dinero interesDelMes(Prestamo p, int mes) {
        if (p.monto().esPositivo() && mes > p.mesDelPrestamo() && mes <= p.mesDelPrestamo() + p.numeroCuotas()) {
            return interesPorCuota(p);
        }
        return Dinero.CERO;
    }

    /** capitalRecuperado proyectado del mes: abonoCapitalPorCuota en los mismos meses que el interés. */
    public static Dinero capitalDelMes(Prestamo p, int mes) {
        if (mes > p.mesDelPrestamo() && mes <= p.mesDelPrestamo() + p.numeroCuotas()) {
            return abonoCapitalPorCuota(p);
        }
        return Dinero.CERO;
    }

    /** Fila de la grilla "Proyección de intereses por mes". */
    public static List<Dinero> proyeccionIntereses(Prestamo p, int numeroMeses) {
        List<Dinero> fila = new ArrayList<>(numeroMeses);
        for (int m = 1; m <= numeroMeses; m++) {
            fila.add(interesDelMes(p, m));
        }
        return fila;
    }
}

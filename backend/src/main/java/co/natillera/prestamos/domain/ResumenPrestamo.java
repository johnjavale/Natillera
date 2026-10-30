package co.natillera.prestamos.domain;

import co.natillera.shared.domain.Dinero;

import java.util.List;
import java.util.Set;

/** Columnas calculadas de una fila de la hoja "Préstamos", más el plan de pagos. */
public record ResumenPrestamo(
        Prestamo prestamo,
        Dinero interesPorCuota,
        Dinero abonoCapitalPorCuota,
        Dinero valorCuota,
        Dinero totalIntereses,
        Dinero totalAPagar,
        int cuotasPagadas,
        Dinero interesesRecibidos,
        Dinero saldoPendiente,
        int cuotasVencidas,
        Dinero saldoVencido,
        EstadoPrestamo estado,
        List<CuotaPlan> plan,
        Set<Integer> numerosPagados,
        Set<Integer> numerosVencidos) {
}

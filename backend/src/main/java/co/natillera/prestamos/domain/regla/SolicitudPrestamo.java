package co.natillera.prestamos.domain.regla;

import co.natillera.shared.domain.Dinero;

/**
 * Datos que las reglas necesitan para validar un préstamo nuevo.
 *
 * @param saldoCajitaOrigen saldo disponible actual de la cajita de la que sale el dinero
 */
public record SolicitudPrestamo(
        Dinero monto,
        int mesDelPrestamo,
        int numeroCuotas,
        boolean integranteActivo,
        Dinero saldoCajitaOrigen,
        int maxCuotasPrestamo,
        int numeroMesesCiclo,
        boolean permitirPrestamoDespuesDelCierre) {
}

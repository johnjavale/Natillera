package co.natillera.prestamos.domain;

import co.natillera.shared.domain.Dinero;

/** Cuota k del plan de pagos: vence en el mes mesDelPrestamo + k. */
public record CuotaPlan(int numero, int mesVencimiento, Dinero capital, Dinero interes, Dinero valor) {
}

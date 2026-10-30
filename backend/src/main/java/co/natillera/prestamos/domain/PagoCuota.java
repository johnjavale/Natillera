package co.natillera.prestamos.domain;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Pago de una cuota. Al registrarse genera un movimiento PAGO_CUOTA_PRESTAMO en la cajita destino. */
public record PagoCuota(Long id, Long prestamoId, int numeroCuota, LocalDate fecha, Dinero monto,
                        Long cajitaDestinoId, boolean anulado, String motivoAnulacion) {
}

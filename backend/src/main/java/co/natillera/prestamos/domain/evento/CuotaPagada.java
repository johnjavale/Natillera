package co.natillera.prestamos.domain.evento;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Evento de dominio: se pagó una cuota. Tesorería registra el PAGO_CUOTA_PRESTAMO. */
public record CuotaPagada(Long prestamoId, Long pagoCuotaId, int numeroCuota, Long cajitaDestinoId,
                          LocalDate fecha, Dinero monto) {
}

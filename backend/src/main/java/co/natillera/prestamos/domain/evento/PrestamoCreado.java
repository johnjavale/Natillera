package co.natillera.prestamos.domain.evento;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Evento de dominio: se creó un préstamo. Tesorería registra el DESEMBOLSO_PRESTAMO. */
public record PrestamoCreado(Long prestamoId, Long cajitaOrigenId, LocalDate fecha, Dinero monto,
                             String nombreIntegrante) {
}

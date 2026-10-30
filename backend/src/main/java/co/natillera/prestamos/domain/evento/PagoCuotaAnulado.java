package co.natillera.prestamos.domain.evento;

import java.time.LocalDate;

/** Evento de dominio: se anuló el pago de una cuota. Tesorería registra el contramovimiento. */
public record PagoCuotaAnulado(Long pagoCuotaId, LocalDate fecha, String motivo) {
}

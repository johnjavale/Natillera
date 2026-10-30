package co.natillera.actividades.domain.evento;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Evento de dominio: un integrante pagó boletas. Tesorería registra el PAGO_BOLETAS_RIFA en su cajita. */
public record PagoBoletasRegistrado(Long actividadId, String nombreActividad, Long cajitaId, LocalDate fecha,
                                    Dinero monto) {
}

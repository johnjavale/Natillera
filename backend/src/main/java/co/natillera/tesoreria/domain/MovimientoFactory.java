package co.natillera.tesoreria.domain;

import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

import java.time.LocalDate;

/**
 * Patrón Factory: crea los movimientos del libro, en especial los derivados de otros módulos
 * (desembolso de préstamo, pago de cuota, pago de boletas) y los contramovimientos de anulación.
 */
public final class MovimientoFactory {

    private MovimientoFactory() {
    }

    /** Movimiento digitado por un tesorero o el admin. */
    public static Movimiento manual(Long cajitaId, LocalDate fecha, TipoMovimiento tipo, String descripcion,
                                    Dinero monto) {
        if (tipo.esDerivado()) {
            throw new ReglaNegocioException("El tipo '" + tipo.etiqueta()
                    + "' se genera automáticamente desde su módulo (préstamos o rifas)");
        }
        return nuevo(cajitaId, fecha, tipo, descripcion, monto, null, null, null);
    }

    public static Movimiento ahorro(Long cajitaId, LocalDate fecha, String descripcion, Dinero monto) {
        return nuevo(cajitaId, fecha, TipoMovimiento.AHORRO, descripcion, monto, null, null, null);
    }

    public static Movimiento desembolsoPrestamo(Long cajitaOrigenId, LocalDate fecha, Long prestamoId,
                                                String nombreIntegrante, Dinero monto) {
        return nuevo(cajitaOrigenId, fecha, TipoMovimiento.DESEMBOLSO_PRESTAMO,
                "Desembolso préstamo #" + prestamoId + " a " + nombreIntegrante, monto, prestamoId, null, null);
    }

    public static Movimiento pagoCuota(Long cajitaDestinoId, LocalDate fecha, Long prestamoId, Long pagoCuotaId,
                                       int numeroCuota, Dinero monto) {
        return nuevo(cajitaDestinoId, fecha, TipoMovimiento.PAGO_CUOTA_PRESTAMO,
                "Cuota " + numeroCuota + " préstamo #" + prestamoId, monto, prestamoId, pagoCuotaId, null);
    }

    public static Movimiento pagoBoletas(Long cajitaId, LocalDate fecha, Long actividadId, String nombreActividad,
                                         Dinero monto) {
        return nuevo(cajitaId, fecha, TipoMovimiento.PAGO_BOLETAS_RIFA, "Boletas " + nombreActividad, monto,
                null, null, actividadId);
    }

    /** Contramovimiento: mismo tipo y monto, lado invertido. Los movimientos nunca se borran. */
    public static Movimiento contramovimiento(Movimiento original, LocalDate fecha, String motivo) {
        if (original.esContramovimiento()) {
            throw new ReglaNegocioException("No se puede anular una anulación");
        }
        return new Movimiento(null, original.cajitaId(), fecha, original.tipo(),
                "Anulación de movimiento #" + original.id() + ": " + motivo,
                original.salida(), original.entrada(), original.prestamoId(), original.pagoCuotaId(),
                original.actividadId(), original.id(), motivo);
    }

    private static Movimiento nuevo(Long cajitaId, LocalDate fecha, TipoMovimiento tipo, String descripcion,
                                    Dinero monto, Long prestamoId, Long pagoCuotaId, Long actividadId) {
        if (monto == null || !monto.esPositivo()) {
            throw new ReglaNegocioException("El valor del movimiento debe ser mayor que cero");
        }
        Dinero valor = monto.redondearCentavos();
        Dinero entrada = tipo.esEntrada() ? valor : Dinero.CERO;
        Dinero salida = tipo.esEntrada() ? Dinero.CERO : valor;
        return new Movimiento(null, cajitaId, fecha, tipo, descripcion, entrada, salida, prestamoId, pagoCuotaId,
                actividadId, null, null);
    }
}

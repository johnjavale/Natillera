package co.natillera.tesoreria.domain;

import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Movimiento del libro de tesorería: la fuente única de verdad del dinero.
 * Exactamente uno de {@code entrada} o {@code salida} es mayor que cero.
 * Un contramovimiento ({@code anulaA != null}) invierte el lado del movimiento original.
 */
public record Movimiento(
        Long id,
        Long cajitaId,
        LocalDate fecha,
        TipoMovimiento tipo,
        String descripcion,
        Dinero entrada,
        Dinero salida,
        Long prestamoId,
        Long pagoCuotaId,
        Long actividadId,
        Long anulaA,
        String motivoAnulacion) {

    public Movimiento {
        Objects.requireNonNull(cajitaId, "cajitaId");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(tipo, "tipo");
        entrada = entrada == null ? Dinero.CERO : entrada;
        salida = salida == null ? Dinero.CERO : salida;
        if (entrada.esNegativo() || salida.esNegativo()) {
            throw new ReglaNegocioException("Los valores de entrada y salida no pueden ser negativos");
        }
        if (entrada.esPositivo() == salida.esPositivo()) {
            throw new ReglaNegocioException("El movimiento debe tener una entrada o una salida, no ambas ni ninguna");
        }
        boolean esContramovimiento = anulaA != null;
        boolean ladoEsperadoEsEntrada = tipo.esEntrada() != esContramovimiento;
        if (ladoEsperadoEsEntrada != entrada.esPositivo()) {
            throw new ReglaNegocioException(
                    "El tipo '" + tipo.etiqueta() + "' es de " + (tipo.esEntrada() ? "entrada" : "salida"));
        }
        if (esContramovimiento && (motivoAnulacion == null || motivoAnulacion.isBlank())) {
            throw new ReglaNegocioException("La anulación requiere un motivo");
        }
    }

    /** Efecto neto en el saldo de la cajita. */
    public Dinero neto() {
        return entrada.restar(salida);
    }

    public Dinero monto() {
        return entrada.esPositivo() ? entrada : salida;
    }

    public boolean esContramovimiento() {
        return anulaA != null;
    }
}

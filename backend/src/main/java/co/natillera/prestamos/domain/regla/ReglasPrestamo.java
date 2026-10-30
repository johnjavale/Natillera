package co.natillera.prestamos.domain.regla;

import java.util.Optional;

/** Catálogo de reglas de préstamo (sección 3.4, validaciones) y su cadena por defecto. */
public final class ReglasPrestamo {

    private ReglasPrestamo() {
    }

    public static final ReglaPrestamo MONTO_POSITIVO = s -> s.monto() == null || !s.monto().esPositivo()
            ? Optional.of("El monto del préstamo debe ser mayor que cero")
            : Optional.empty();

    public static final ReglaPrestamo INTEGRANTE_ACTIVO = s -> s.integranteActivo()
            ? Optional.empty()
            : Optional.of("El integrante no está activo");

    public static final ReglaPrestamo CUOTAS_POSITIVAS = s -> s.numeroCuotas() < 1
            ? Optional.of("El préstamo debe tener al menos una cuota")
            : Optional.empty();

    public static final ReglaPrestamo MAXIMO_CUOTAS = s -> s.numeroCuotas() > s.maxCuotasPrestamo()
            ? Optional.of("Los préstamos no pueden ser a más de " + s.maxCuotasPrestamo() + " cuotas (un año)")
            : Optional.empty();

    public static final ReglaPrestamo MES_VALIDO = s -> s.mesDelPrestamo() < 1 || s.mesDelPrestamo() > s.numeroMesesCiclo()
            ? Optional.of("El mes del préstamo debe estar entre 1 y " + s.numeroMesesCiclo())
            : Optional.empty();

    public static final ReglaPrestamo FIN_DEL_CICLO = s ->
            !s.permitirPrestamoDespuesDelCierre() && s.mesDelPrestamo() + s.numeroCuotas() > s.numeroMesesCiclo()
                    ? Optional.of("El préstamo termina después del cierre de la natillera")
                    : Optional.empty();

    public static final ReglaPrestamo SALDO_CAJITA = s -> s.monto().esMayorQue(s.saldoCajitaOrigen())
            ? Optional.of("El monto supera el saldo disponible de la cajita origen ($"
            + s.saldoCajitaOrigen().redondearAlPeso().valor().toPlainString() + ")")
            : Optional.empty();

    /** Cadena completa en el orden en que se reportan los errores. */
    public static ReglaPrestamo cadenaPorDefecto() {
        return MONTO_POSITIVO
                .y(INTEGRANTE_ACTIVO)
                .y(CUOTAS_POSITIVAS)
                .y(MAXIMO_CUOTAS)
                .y(MES_VALIDO)
                .y(FIN_DEL_CICLO)
                .y(SALDO_CAJITA);
    }
}

package co.natillera.prestamos.domain;

import co.natillera.shared.domain.Dinero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Préstamo a un integrante, con interés simple sobre el monto prestado (hoja "Préstamos"). */
public record Prestamo(
        Long id,
        Long integranteId,
        Long cajitaOrigenId,
        LocalDate fechaDesembolso,
        Dinero monto,
        int mesDelPrestamo,
        int numeroCuotas,
        BigDecimal tasaMensual) {
}

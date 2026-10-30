package co.natillera.tesoreria.domain;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Saldo de una cajita según el libro y según la app de Nu. */
public record SaldoCajita(
        Long cajitaId,
        String nombreCajita,
        Long integranteId,
        String nombreIntegrante,
        Long tesoreroId,
        String nombreTesorero,
        Dinero entradas,
        Dinero salidas,
        Dinero saldoApp,
        LocalDate fechaSaldoApp) {

    /** saldoCajita = Σ entradas − Σ salidas */
    public Dinero saldo() {
        return entradas.restar(salidas);
    }

    public Dinero diferencia() {
        return Conciliacion.diferencia(saldoApp, saldo());
    }
}

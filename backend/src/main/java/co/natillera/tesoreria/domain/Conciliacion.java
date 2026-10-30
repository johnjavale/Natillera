package co.natillera.tesoreria.domain;

import co.natillera.shared.domain.Dinero;

/** Conciliación contra el saldo que muestra la app de Nu (sección 3.6). */
public final class Conciliacion {

    private Conciliacion() {
    }

    /** diferencia = saldoApp − saldoSegúnRegistro; 0 si no hay saldo reportado. */
    public static Dinero diferencia(Dinero saldoApp, Dinero saldoRegistro) {
        if (saldoApp == null) {
            return Dinero.CERO;
        }
        return saldoApp.restar(saldoRegistro);
    }

    public static boolean cuadra(Dinero diferencia) {
        return diferencia.esCero();
    }
}

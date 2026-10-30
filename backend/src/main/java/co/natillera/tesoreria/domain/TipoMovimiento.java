package co.natillera.tesoreria.domain;

/** Tipos de movimiento del libro de tesorería, con su naturaleza (entrada o salida). */
public enum TipoMovimiento {
    AHORRO("Ahorro", Naturaleza.ENTRADA),
    PAGO_CUOTA_PRESTAMO("Pago cuota préstamo", Naturaleza.ENTRADA),
    PAGO_BOLETAS_RIFA("Pago boletas rifa", Naturaleza.ENTRADA),
    RENDIMIENTO_NU("Rendimiento Nu", Naturaleza.ENTRADA),
    OTRO_INGRESO("Otro ingreso", Naturaleza.ENTRADA),
    DESEMBOLSO_PRESTAMO("Desembolso préstamo", Naturaleza.SALIDA),
    COMPRA_PREMIO("Compra premio", Naturaleza.SALIDA),
    GASTO("Gasto", Naturaleza.SALIDA),
    RETIRO_ENTREGA_FINAL("Retiro / entrega final", Naturaleza.SALIDA);

    public enum Naturaleza { ENTRADA, SALIDA }

    private final String etiqueta;
    private final Naturaleza naturaleza;

    TipoMovimiento(String etiqueta, Naturaleza naturaleza) {
        this.etiqueta = etiqueta;
        this.naturaleza = naturaleza;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public Naturaleza naturaleza() {
        return naturaleza;
    }

    public boolean esEntrada() {
        return naturaleza == Naturaleza.ENTRADA;
    }

    /** Tipos que nacen de otro módulo y no se registran a mano. */
    public boolean esDerivado() {
        return this == PAGO_CUOTA_PRESTAMO || this == DESEMBOLSO_PRESTAMO || this == PAGO_BOLETAS_RIFA;
    }
}

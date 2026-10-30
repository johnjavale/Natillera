package co.natillera.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object para montos en pesos colombianos.
 * Internamente conserva toda la precisión; el redondeo HALF_UP se aplica
 * explícitamente al presentar o al persistir ({@link #redondearCentavos()}, {@link #redondearAlPeso()}).
 */
public final class Dinero implements Comparable<Dinero> {

    private static final int ESCALA_DIVISION = 10;

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    private final BigDecimal valor;

    private Dinero(BigDecimal valor) {
        this.valor = Objects.requireNonNull(valor, "valor");
    }

    public static Dinero de(BigDecimal valor) {
        return valor == null ? CERO : new Dinero(valor);
    }

    public static Dinero de(long pesos) {
        return new Dinero(BigDecimal.valueOf(pesos));
    }

    public static Dinero de(String valor) {
        return new Dinero(new BigDecimal(valor));
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(valor.add(otro.valor));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(valor.subtract(otro.valor));
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(valor.multiply(factor));
    }

    public Dinero multiplicar(long factor) {
        return new Dinero(valor.multiply(BigDecimal.valueOf(factor)));
    }

    public Dinero dividir(long divisor) {
        return dividir(BigDecimal.valueOf(divisor));
    }

    public Dinero dividir(BigDecimal divisor) {
        return new Dinero(valor.divide(divisor, ESCALA_DIVISION, RoundingMode.HALF_UP));
    }

    /** Proporción de este monto respecto a un total; 0 si el total es 0. */
    public BigDecimal proporcionDe(Dinero total) {
        if (total.esCero()) {
            return BigDecimal.ZERO;
        }
        return valor.divide(total.valor, ESCALA_DIVISION, RoundingMode.HALF_UP);
    }

    public Dinero redondearAlPeso() {
        return new Dinero(valor.setScale(0, RoundingMode.HALF_UP));
    }

    public Dinero redondearCentavos() {
        return new Dinero(valor.setScale(2, RoundingMode.HALF_UP));
    }

    public Dinero max(Dinero otro) {
        return compareTo(otro) >= 0 ? this : otro;
    }

    public Dinero min(Dinero otro) {
        return compareTo(otro) <= 0 ? this : otro;
    }

    public Dinero negar() {
        return new Dinero(valor.negate());
    }

    public boolean esCero() {
        return valor.signum() == 0;
    }

    public boolean esPositivo() {
        return valor.signum() > 0;
    }

    public boolean esNegativo() {
        return valor.signum() < 0;
    }

    public boolean esMayorQue(Dinero otro) {
        return compareTo(otro) > 0;
    }

    public BigDecimal valor() {
        return valor;
    }

    /** Valor con 2 decimales, listo para persistir en NUMERIC(14,2) o serializar. */
    public BigDecimal aCentavos() {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinero sumarTodos(Iterable<Dinero> montos) {
        Dinero total = CERO;
        for (Dinero m : montos) {
            total = total.sumar(m);
        }
        return total;
    }

    @Override
    public int compareTo(Dinero otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dinero d && valor.compareTo(d.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}

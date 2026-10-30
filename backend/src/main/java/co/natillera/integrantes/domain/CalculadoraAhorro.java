package co.natillera.integrantes.domain;

import co.natillera.shared.domain.Dinero;

import java.math.BigDecimal;

/** Fórmulas de la hoja "Integrantes" (sección 3.2). */
public final class CalculadoraAhorro {

    private CalculadoraAhorro() {
    }

    /** ahorroEsperado = cuotaMensual × numeroMeses */
    public static Dinero ahorroEsperado(Dinero cuotaMensual, int numeroMeses) {
        return cuotaMensual.multiplicar(numeroMeses);
    }

    /** % del total = ahorroEsperado / Σ ahorroEsperado (0 si el total es 0) */
    public static BigDecimal porcentajeDelTotal(Dinero ahorro, Dinero totalAhorro) {
        return ahorro.proporcionDe(totalAhorro);
    }

    /**
     * rendimientoProyectadoCajita = cuotaMensual × meses × (meses − 1) / 2 × tasaMensualNu.
     * Cada cuota gana interés los meses que le quedan al ciclo.
     */
    public static Dinero rendimientoProyectadoCajita(Dinero cuotaMensual, int meses, BigDecimal tasaMensualNu) {
        return cuotaMensual.multiplicar((long) meses * (meses - 1)).dividir(2).multiplicar(tasaMensualNu);
    }
}

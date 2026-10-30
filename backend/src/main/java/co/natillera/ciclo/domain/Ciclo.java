package co.natillera.ciclo.domain;

import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Objects;

/** Configuración del ciclo de la natillera (hoja "Configuración"). */
public record Ciclo(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        int numeroMeses,
        BigDecimal tasaInteresMensualPrestamos,
        Dinero gastosAdministrativos,
        int maxCuotasPrestamo,
        BigDecimal tasaEACajitasNu,
        int maxActividades,
        boolean permitirPrestamoDespuesDelCierre,
        EstadoCiclo estado) {

    public static final int MAX_MESES = 12;

    public Ciclo {
        Objects.requireNonNull(fechaInicio, "fechaInicio");
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre de la natillera es obligatorio");
        }
        if (numeroMeses < 1 || numeroMeses > MAX_MESES) {
            throw new ReglaNegocioException("El número de meses debe estar entre 1 y 12");
        }
        if (maxCuotasPrestamo < 1 || maxCuotasPrestamo > MAX_MESES) {
            throw new ReglaNegocioException("El máximo de cuotas por préstamo debe estar entre 1 y 12");
        }
        if (tasaInteresMensualPrestamos == null || tasaInteresMensualPrestamos.signum() < 0) {
            throw new ReglaNegocioException("La tasa de interés mensual no puede ser negativa");
        }
        if (tasaEACajitasNu == null || tasaEACajitasNu.signum() < 0) {
            throw new ReglaNegocioException("La tasa E.A. de las cajitas no puede ser negativa");
        }
        if (gastosAdministrativos == null || gastosAdministrativos.esNegativo()) {
            throw new ReglaNegocioException("Los gastos administrativos no pueden ser negativos");
        }
        if (maxActividades < 0) {
            throw new ReglaNegocioException("El máximo de actividades no puede ser negativo");
        }
        if (estado == null) {
            estado = EstadoCiclo.INSCRIPCION;
        }
    }

    /** tasaMensualNu = (1 + tasaEA)^(1/12) − 1 */
    public BigDecimal tasaMensualNu() {
        double mensual = Math.pow(1 + tasaEACajitasNu.doubleValue(), 1.0 / 12) - 1;
        return new BigDecimal(mensual, MathContext.DECIMAL64);
    }

    /** fechaCierre = fechaInicio + (numeroMeses − 1) meses */
    public LocalDate fechaCierre() {
        return fechaInicio.plusMonths(numeroMeses - 1L);
    }

    public int mesDelCiclo(LocalDate fecha) {
        return CalendarioCiclo.mesDelCiclo(fechaInicio, fecha);
    }

    public int mesesTranscurridos(LocalDate hoy) {
        return CalendarioCiclo.mesesTranscurridos(fechaInicio, numeroMeses, hoy);
    }

    public boolean permiteEditarCuotaPropia() {
        return estado == EstadoCiclo.INSCRIPCION;
    }

    public boolean estaCerrado() {
        return estado == EstadoCiclo.CERRADO;
    }

    public Ciclo conEstado(EstadoCiclo nuevo) {
        return new Ciclo(id, nombre, fechaInicio, numeroMeses, tasaInteresMensualPrestamos, gastosAdministrativos,
                maxCuotasPrestamo, tasaEACajitasNu, maxActividades, permitirPrestamoDespuesDelCierre, nuevo);
    }

    public static Ciclo porDefecto() {
        return new Ciclo(null, "Natillera Familiar", LocalDate.of(2027, 1, 1), 12, new BigDecimal("0.03"),
                Dinero.CERO, 12, new BigDecimal("0.09"), 2, false, EstadoCiclo.INSCRIPCION);
    }
}

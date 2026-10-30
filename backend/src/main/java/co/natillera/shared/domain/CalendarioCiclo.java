package co.natillera.shared.domain;

import java.time.LocalDate;

/** Funciones auxiliares de fechas del ciclo (sección 3.1). */
public final class CalendarioCiclo {

    private CalendarioCiclo() {
    }

    /** mesDelCiclo(fecha) = (año(fecha) − año(inicio))·12 + mes(fecha) − mes(inicio) + 1 */
    public static int mesDelCiclo(LocalDate inicio, LocalDate fecha) {
        return (fecha.getYear() - inicio.getYear()) * 12 + fecha.getMonthValue() - inicio.getMonthValue() + 1;
    }

    /** mesesTranscurridos(hoy) = max(0, min(numeroMeses, mesDelCiclo(hoy))) */
    public static int mesesTranscurridos(LocalDate inicio, int numeroMeses, LocalDate hoy) {
        return Math.max(0, Math.min(numeroMeses, mesDelCiclo(inicio, hoy)));
    }

    /** Primer día del mes m del ciclo (EDATE(inicio, m − 1)). */
    public static LocalDate fechaDelMes(LocalDate inicio, int mes) {
        return inicio.plusMonths(mes - 1L);
    }
}

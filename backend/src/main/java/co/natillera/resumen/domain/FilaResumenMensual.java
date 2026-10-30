package co.natillera.resumen.domain;

import co.natillera.shared.domain.Dinero;

import java.time.LocalDate;

/** Una fila de la hoja "Resumen" (sección 3.8). */
public record FilaResumenMensual(
        int mes,
        LocalDate fechaMes,
        Dinero ahorroEsperado,
        Dinero ahorroReal,
        Dinero prestamosEntregados,
        Dinero capitalRecuperado,
        Dinero capitalRecuperadoReal,
        Dinero intereses,
        Dinero utilidadActividades,
        Dinero flujoNeto,
        Dinero cajaDisponibleAcumulada,
        Dinero fondoTotalAcumulado) {

    /** Alerta roja: "se está prestando más dinero del que hay disponible". */
    public boolean alertaCajaNegativa() {
        return cajaDisponibleAcumulada.esNegativo();
    }
}

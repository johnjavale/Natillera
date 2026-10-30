package co.natillera.integrantes.domain;

import co.natillera.shared.domain.Dinero;

import java.math.BigDecimal;
import java.util.List;

/** Estado de los aportes de un integrante (fila de la hoja "Ahorros Mensuales"). */
public record EstadoAportes(
        List<Dinero> aportesPorMes,
        Dinero totalAportado,
        Dinero ahorroEsperado,
        Dinero diferencia,
        BigDecimal cumplimiento,
        Dinero esperadoALaFecha,
        Dinero atraso) {

    public boolean enAtraso() {
        return atraso.esPositivo();
    }
}

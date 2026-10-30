package co.natillera.integrantes.application;

import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.domain.Dinero;

import java.math.BigDecimal;

/** Fila de la hoja "Integrantes" con sus columnas calculadas y el saldo de su cajita. */
public record IntegranteVista(
        Integrante integrante,
        Long cajitaId,
        String nombreTesorero,
        Dinero ahorroEsperado,
        BigDecimal porcentajeDelTotal,
        Dinero rendimientoProyectadoCajita,
        Dinero saldoCajita) {
}

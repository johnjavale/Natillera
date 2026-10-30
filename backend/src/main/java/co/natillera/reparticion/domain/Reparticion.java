package co.natillera.reparticion.domain;

import co.natillera.shared.domain.Dinero;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de la hoja "Repartición": conceptos proyectados/reales y una fila por integrante. */
public record Reparticion(Conceptos proyectado, Conceptos real, List<Fila> filas) {

    public record Conceptos(Dinero interesesPrestamos, Dinero gastosAdministrativos, Dinero interesesNetos,
                            Dinero utilidadActividades, Dinero utilidadActividadesPorIntegrante) {
    }

    public record Fila(
            Long integranteId,
            String nombre,
            Dinero ahorroEsperado,
            BigDecimal participacionProyectada,
            Dinero interesesProyectados,
            Dinero actividadesProyectadas,
            Dinero rendimientoNuProyectado,
            Dinero totalProyectado,
            Dinero ahorroReal,
            BigDecimal participacionReal,
            Dinero interesesReales,
            Dinero actividadesReales,
            Dinero rendimientoNuReal,
            Dinero totalReal,
            int puesto,
            int puestoReal) {
    }

    public Dinero totalProyectado() {
        return Dinero.sumarTodos(filas.stream().map(Fila::totalProyectado).toList());
    }

    public Dinero totalReal() {
        return Dinero.sumarTodos(filas.stream().map(Fila::totalReal).toList());
    }
}

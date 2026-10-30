package co.natillera.actividades.domain;

import co.natillera.shared.domain.Dinero;

import java.util.List;

/** Resultado calculado de una actividad: boletas por integrante, recaudos y utilidades. */
public record ResumenActividad(
        Actividad actividad,
        int mesDeLaActividad,
        List<BoletasIntegrante> boletas,
        Dinero recaudoEsperado,
        Dinero recaudoReal,
        Dinero costos,
        Dinero utilidadEsperada,
        Dinero utilidadReal,
        Dinero utilidadEsperadaPorIntegrante,
        Dinero utilidadRealPorIntegrante) {

    public record BoletasIntegrante(Long integranteId, String nombre, int boletas, Dinero valorAPagar,
                                    Dinero pagado, Dinero pendiente) {
    }
}

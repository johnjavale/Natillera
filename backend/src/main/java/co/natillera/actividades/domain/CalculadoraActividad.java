package co.natillera.actividades.domain;

import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Fórmulas de la hoja "Actividades" (sección 3.5). */
public final class CalculadoraActividad {

    private CalculadoraActividad() {
    }

    public record ParticipanteActividad(Long integranteId, String nombre) {
    }

    /**
     * @param boletasAsignadas boletas por integrante cuando difieren del valor por defecto de la actividad
     * @param pagado           Σ movimientos PAGO_BOLETAS_RIFA por integrante para esta actividad
     */
    public static ResumenActividad resumir(Actividad actividad, LocalDate inicioCiclo,
                                           List<ParticipanteActividad> participantes,
                                           Map<Long, Integer> boletasAsignadas, Map<Long, Dinero> pagado) {
        List<ResumenActividad.BoletasIntegrante> filas = new ArrayList<>(participantes.size());
        Dinero recaudoEsperado = Dinero.CERO;
        Dinero recaudoReal = Dinero.CERO;
        for (ParticipanteActividad p : participantes) {
            int boletas = boletasAsignadas.getOrDefault(p.integranteId(), actividad.boletasPorIntegrante());
            Dinero valorAPagar = actividad.valorBoleta().multiplicar(boletas);
            Dinero pagadoIntegrante = pagado.getOrDefault(p.integranteId(), Dinero.CERO);
            filas.add(new ResumenActividad.BoletasIntegrante(p.integranteId(), p.nombre(), boletas, valorAPagar,
                    pagadoIntegrante, valorAPagar.restar(pagadoIntegrante)));
            recaudoEsperado = recaudoEsperado.sumar(valorAPagar);
            recaudoReal = recaudoReal.sumar(pagadoIntegrante);
        }
        Dinero costos = actividad.costos();
        Dinero utilidadEsperada = recaudoEsperado.restar(costos);
        Dinero utilidadReal = recaudoReal.restar(costos);
        int numeroIntegrantes = participantes.size();
        return new ResumenActividad(actividad, CalendarioCiclo.mesDelCiclo(inicioCiclo, actividad.fechaSorteo()),
                filas, recaudoEsperado, recaudoReal, costos, utilidadEsperada, utilidadReal,
                porIntegrante(utilidadEsperada, numeroIntegrantes), porIntegrante(utilidadReal, numeroIntegrantes));
    }

    /** utilidadPorIntegrante = utilidad / numeroIntegrantes (partes iguales); 0 sin integrantes. */
    public static Dinero porIntegrante(Dinero utilidad, int numeroIntegrantes) {
        return numeroIntegrantes == 0 ? Dinero.CERO : utilidad.dividir(numeroIntegrantes);
    }

    /** Restricción: máximo N actividades por ciclo (2 por defecto). */
    public static void validarCupo(int actividadesExistentes, int maxActividades) {
        if (actividadesExistentes >= maxActividades) {
            throw new ReglaNegocioException("El ciclo admite máximo " + maxActividades + " actividades");
        }
    }
}

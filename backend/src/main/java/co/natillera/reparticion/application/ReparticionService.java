package co.natillera.reparticion.application;

import co.natillera.actividades.application.ActividadService;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.auth.application.AccessScopeService;
import co.natillera.auth.domain.Alcance;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.application.AportesService;
import co.natillera.integrantes.domain.CalculadoraAhorro;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.integrantes.application.IntegranteService;
import co.natillera.prestamos.application.PrestamoService;
import co.natillera.reparticion.domain.CalculadoraReparticion;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.LibroConsultas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Repartición proyectada y real (sección 3.7), con el alcance del rol aplicado a las filas. */
@Service
public class ReparticionService {

    /** conceptos == null cuando el usuario solo puede ver su propia fila. */
    public record VistaReparticion(Reparticion.Conceptos proyectado, Reparticion.Conceptos real,
                                   List<Reparticion.Fila> filas, Dinero totalProyectado, Dinero totalReal) {
    }

    private final IntegranteService integrantes;
    private final AportesService aportes;
    private final PrestamoService prestamos;
    private final ActividadService actividades;
    private final LibroConsultas libro;
    private final CicloService ciclos;
    private final AccessScopeService access;
    private final CalculadoraReparticion calculadora = CalculadoraReparticion.porDefecto();

    public ReparticionService(IntegranteService integrantes, AportesService aportes, PrestamoService prestamos,
                              ActividadService actividades, LibroConsultas libro, CicloService ciclos,
                              AccessScopeService access) {
        this.integrantes = integrantes;
        this.aportes = aportes;
        this.prestamos = prestamos;
        this.actividades = actividades;
        this.libro = libro;
        this.ciclos = ciclos;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public VistaReparticion ver() {
        Reparticion r = calcularSinAlcance();
        Set<Long> visibles = access.integrantesVisibles().orElse(null);
        List<Reparticion.Fila> filas = visibles == null ? r.filas()
                : r.filas().stream().filter(f -> visibles.contains(f.integranteId())).toList();
        boolean soloPropia = access.alcance() == Alcance.OWN;
        return new VistaReparticion(soloPropia ? null : r.proyectado(), soloPropia ? null : r.real(), filas,
                soloPropia ? null : r.totalProyectado(), soloPropia ? null : r.totalReal());
    }

    /** API interna sin alcance (resumen y reportes). */
    @Transactional(readOnly = true)
    public Reparticion calcularSinAlcance() {
        Ciclo ciclo = ciclos.actual();
        Map<Long, Dinero> aportado = new HashMap<>();
        aportes.calcular(null).filas().forEach(f -> aportado.put(f.integranteId(), f.estado().totalAportado()));
        Map<Long, Dinero> rendimientoReal = libro.rendimientoNuPorIntegrante(null);

        List<CalculadoraReparticion.Integrante> participantes = integrantes.activos().stream()
                .map((Integrante i) -> new CalculadoraReparticion.Integrante(i.id(), i.nombre(),
                        CalculadoraAhorro.ahorroEsperado(i.cuotaMensual(), ciclo.numeroMeses()),
                        aportado.getOrDefault(i.id(), Dinero.CERO),
                        CalculadoraAhorro.rendimientoProyectadoCajita(i.cuotaMensual(), ciclo.numeroMeses(),
                                ciclo.tasaMensualNu()),
                        rendimientoReal.getOrDefault(i.id(), Dinero.CERO)))
                .toList();

        Dinero interesesProyectados = Dinero.CERO;
        Dinero interesesRecibidos = Dinero.CERO;
        for (PrestamoService.PrestamoVista p : prestamos.todosSinAlcance()) {
            interesesProyectados = interesesProyectados.sumar(p.resumen().totalIntereses());
            interesesRecibidos = interesesRecibidos.sumar(p.resumen().interesesRecibidos());
        }
        Dinero utilidadEsperada = Dinero.CERO;
        Dinero utilidadReal = Dinero.CERO;
        for (ResumenActividad a : actividades.todasSinAlcance()) {
            utilidadEsperada = utilidadEsperada.sumar(a.utilidadEsperada());
            utilidadReal = utilidadReal.sumar(a.utilidadReal());
        }
        return calculadora.calcular(new CalculadoraReparticion.Entrada(participantes, interesesProyectados,
                interesesRecibidos, ciclo.gastosAdministrativos(), utilidadEsperada, utilidadReal));
    }
}

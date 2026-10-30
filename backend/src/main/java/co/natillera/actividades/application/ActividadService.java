package co.natillera.actividades.application;

import co.natillera.actividades.application.port.out.ActividadRepositoryPort;
import co.natillera.actividades.domain.Actividad;
import co.natillera.actividades.domain.CalculadoraActividad;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.actividades.domain.evento.PagoBoletasRegistrado;
import co.natillera.auth.application.AccessScopeService;
import co.natillera.auth.application.SesionActual;
import co.natillera.auth.domain.Alcance;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.application.IntegranteService;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.application.LibroConsultas;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import co.natillera.tesoreria.domain.Cajita;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Casos de uso de actividades (rifas), sección 3.5. */
@Service
public class ActividadService {

    public record DatosActividad(String nombre, LocalDate fechaSorteo, Dinero valorBoleta, int boletasPorIntegrante,
                                 Dinero costoPremio, Dinero otrosGastos, String ganador) {
    }

    private final ActividadRepositoryPort repository;
    private final IntegranteService integrantes;
    private final LibroConsultas libro;
    private final TesoreriaRepositoryPort tesoreria;
    private final CicloService ciclos;
    private final AccessScopeService access;
    private final SesionActual sesion;
    private final ApplicationEventPublisher eventos;
    private final AuditoriaPort auditoria;

    public ActividadService(ActividadRepositoryPort repository, IntegranteService integrantes, LibroConsultas libro,
                            TesoreriaRepositoryPort tesoreria, CicloService ciclos, AccessScopeService access,
                            SesionActual sesion, ApplicationEventPublisher eventos, AuditoriaPort auditoria) {
        this.repository = repository;
        this.integrantes = integrantes;
        this.libro = libro;
        this.tesoreria = tesoreria;
        this.ciclos = ciclos;
        this.access = access;
        this.sesion = sesion;
        this.eventos = eventos;
        this.auditoria = auditoria;
    }

    /** El integrante ve sus boletas y el resultado general; tesorero y admin ven todas las filas. */
    @Transactional(readOnly = true)
    public List<ResumenActividad> listar() {
        return todasSinAlcance().stream().map(this::filtrarPorAlcance).toList();
    }

    @Transactional(readOnly = true)
    public ResumenActividad obtener(Long id) {
        return listar().stream().filter(r -> r.actividad().id().equals(id)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad", id));
    }

    @Transactional
    public ResumenActividad crear(DatosActividad d) {
        ciclos.exigirCicloAbierto();
        CalculadoraActividad.validarCupo(repository.contar(), ciclos.actual().maxActividades());
        Actividad nueva = repository.guardar(new Actividad(null, d.nombre().trim(), d.fechaSorteo(), d.valorBoleta(),
                d.boletasPorIntegrante(), d.costoPremio(), d.otrosGastos(), d.ganador()));
        return obtener(nueva.id());
    }

    @Transactional
    public ResumenActividad actualizar(Long id, DatosActividad d) {
        ciclos.exigirCicloAbierto();
        buscar(id);
        repository.guardar(new Actividad(id, d.nombre().trim(), d.fechaSorteo(), d.valorBoleta(),
                d.boletasPorIntegrante(), d.costoPremio(), d.otrosGastos(), d.ganador()));
        return obtener(id);
    }

    @Transactional
    public ResumenActividad asignarBoletas(Long actividadId, Long integranteId, int boletas) {
        ciclos.exigirCicloAbierto();
        buscar(actividadId);
        if (boletas < 0) {
            throw new ReglaNegocioException("Las boletas no pueden ser negativas");
        }
        integrantes.buscarSinAlcance(integranteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Integrante", integranteId));
        repository.asignarBoletas(actividadId, integranteId, boletas);
        return obtener(actividadId);
    }

    /** Pago de boletas: genera un movimiento PAGO_BOLETAS_RIFA en la cajita del integrante. */
    @Transactional
    public ResumenActividad registrarPago(Long actividadId, Long integranteId, LocalDate fecha, Dinero monto) {
        Actividad actividad = buscar(actividadId);
        access.exigirGestionarIntegrante(integranteId);
        if (monto == null || !monto.esPositivo()) {
            throw new ReglaNegocioException("El valor pagado debe ser mayor que cero");
        }
        Cajita cajita = tesoreria.cajitaDeIntegrante(integranteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajita del integrante", integranteId));
        eventos.publishEvent(new PagoBoletasRegistrado(actividadId, actividad.nombre(), cajita.id(), fecha, monto));
        return obtener(actividadId);
    }

    /** API interna sin alcance (resumen, repartición, reportes). */
    @Transactional(readOnly = true)
    public List<ResumenActividad> todasSinAlcance() {
        Ciclo ciclo = ciclos.actual();
        List<CalculadoraActividad.ParticipanteActividad> participantes = integrantes.activos().stream()
                .map(i -> new CalculadoraActividad.ParticipanteActividad(i.id(), i.nombre())).toList();
        Map<Long, Map<Long, Integer>> asignaciones = repository.asignaciones();
        Map<Long, Map<Long, Dinero>> pagos = libro.pagosBoletas(null);
        return repository.listar().stream()
                .map(a -> CalculadoraActividad.resumir(a, ciclo.fechaInicio(), participantes,
                        asignaciones.getOrDefault(a.id(), Map.of()), pagos.getOrDefault(a.id(), Map.of())))
                .toList();
    }

    private ResumenActividad filtrarPorAlcance(ResumenActividad r) {
        if (access.alcance() != Alcance.OWN) {
            return r;
        }
        Long propio = sesion.usuario().integranteId();
        List<ResumenActividad.BoletasIntegrante> propias = r.boletas().stream()
                .filter(b -> b.integranteId().equals(propio)).toList();
        return new ResumenActividad(r.actividad(), r.mesDeLaActividad(), propias, r.recaudoEsperado(),
                r.recaudoReal(), r.costos(), r.utilidadEsperada(), r.utilidadReal(),
                r.utilidadEsperadaPorIntegrante(), r.utilidadRealPorIntegrante());
    }

    private Actividad buscar(Long id) {
        return repository.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Actividad", id));
    }
}

package co.natillera.prestamos.application;

import co.natillera.auth.application.SesionActual;
import co.natillera.auth.application.UsuarioActual;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.application.IntegranteService;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.prestamos.application.port.out.PrestamoRepositoryPort;
import co.natillera.prestamos.domain.CalculadoraPrestamo;
import co.natillera.prestamos.domain.CuotaPlan;
import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.prestamos.domain.ResumenPrestamo;
import co.natillera.prestamos.domain.evento.CuotaPagada;
import co.natillera.prestamos.domain.evento.PagoCuotaAnulado;
import co.natillera.prestamos.domain.evento.PrestamoCreado;
import co.natillera.prestamos.domain.regla.ReglaPrestamo;
import co.natillera.prestamos.domain.regla.ReglasPrestamo;
import co.natillera.prestamos.domain.regla.SolicitudPrestamo;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.application.LibroConsultas;
import co.natillera.tesoreria.application.TesoreriaService;
import co.natillera.tesoreria.domain.Cajita;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Casos de uso de préstamos (sección 3.4). */
@Service
public class PrestamoService {

    public record NuevoPrestamo(Long integranteId, Long cajitaOrigenId, LocalDate fechaDesembolso, Dinero monto,
                                Integer mesDelPrestamo, int numeroCuotas, BigDecimal tasaMensual) {
    }

    public record NuevoPago(LocalDate fecha, Long cajitaDestinoId, Integer numeroCuota, Dinero monto) {
    }

    /** Préstamo con su estado calculado y datos para mostrar. */
    public record PrestamoVista(ResumenPrestamo resumen, String nombreIntegrante, List<PagoCuota> pagos) {
    }

    public record ProyeccionIntereses(List<FilaProyeccion> filas, List<Dinero> totalesPorMes, Dinero total) {
    }

    public record FilaProyeccion(Long prestamoId, String nombreIntegrante, List<Dinero> interesesPorMes) {
    }

    private final PrestamoRepositoryPort repository;
    private final PrestamoAccessPolicy policy;
    private final SesionActual sesion;
    private final CicloService ciclos;
    private final IntegranteService integrantes;
    private final TesoreriaService tesoreria;
    private final LibroConsultas libro;
    private final ApplicationEventPublisher eventos;
    private final AuditoriaPort auditoria;
    private final ReglaPrestamo reglas = ReglasPrestamo.cadenaPorDefecto();

    public PrestamoService(PrestamoRepositoryPort repository, PrestamoAccessPolicy policy, SesionActual sesion,
                           CicloService ciclos, IntegranteService integrantes, TesoreriaService tesoreria,
                           LibroConsultas libro, ApplicationEventPublisher eventos, AuditoriaPort auditoria) {
        this.repository = repository;
        this.policy = policy;
        this.sesion = sesion;
        this.ciclos = ciclos;
        this.integrantes = integrantes;
        this.tesoreria = tesoreria;
        this.libro = libro;
        this.eventos = eventos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PrestamoVista> listar() {
        UsuarioActual u = sesion.usuario();
        return vistas(repository.listar().stream().filter(p -> policy.puedeVer(u, p)).toList());
    }

    @Transactional(readOnly = true)
    public PrestamoVista obtener(Long id) {
        return vistas(List.of(visible(id))).get(0);
    }

    /** Vista previa del préstamo (mismas validaciones, sin guardar). */
    @Transactional(readOnly = true)
    public ResumenPrestamo simular(NuevoPrestamo cmd) {
        Prestamo p = validar(cmd);
        return CalculadoraPrestamo.resumir(p, List.of(), ciclos.mesesTranscurridos());
    }

    @Transactional
    public PrestamoVista crear(NuevoPrestamo cmd) {
        Prestamo validado = validar(cmd);
        Prestamo guardado = repository.guardar(validado);
        String nombre = integrantes.buscarSinAlcance(guardado.integranteId()).map(Integrante::nombre).orElse("");
        eventos.publishEvent(new PrestamoCreado(guardado.id(), guardado.cajitaOrigenId(), guardado.fechaDesembolso(),
                guardado.monto(), nombre));
        return obtener(guardado.id());
    }

    @Transactional
    public PrestamoVista registrarPago(Long prestamoId, NuevoPago cmd) {
        ciclos.exigirCicloAbierto();
        Prestamo p = visible(prestamoId);
        Long cajitaDestinoId = cmd.cajitaDestinoId() != null ? cmd.cajitaDestinoId() : p.cajitaOrigenId();
        Cajita destino = tesoreria.cajitaGestionable(cajitaDestinoId);
        List<PagoCuota> pagos = repository.pagosDe(prestamoId).stream().filter(pc -> !pc.anulado()).toList();
        if (pagos.size() >= p.numeroCuotas()) {
            throw new ReglaNegocioException("El préstamo ya está pagado");
        }
        List<Integer> pagadas = pagos.stream().map(PagoCuota::numeroCuota).toList();
        List<CuotaPlan> plan = CalculadoraPrestamo.planDePagos(p);
        CuotaPlan cuota = cmd.numeroCuota() == null
                ? plan.stream().filter(c -> !pagadas.contains(c.numero())).findFirst().orElseThrow()
                : plan.stream().filter(c -> c.numero() == cmd.numeroCuota()).findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La cuota " + cmd.numeroCuota() + " no existe en el plan"));
        if (pagadas.contains(cuota.numero())) {
            throw new ReglaNegocioException("La cuota " + cuota.numero() + " ya tiene un pago registrado");
        }
        Dinero monto = cmd.monto() != null ? cmd.monto() : cuota.valor();
        if (!monto.esPositivo()) {
            throw new ReglaNegocioException("El valor del pago debe ser mayor que cero");
        }
        PagoCuota pago = repository.guardarPago(new PagoCuota(null, prestamoId, cuota.numero(), cmd.fecha(),
                monto.redondearCentavos(), destino.id(), false, null));
        eventos.publishEvent(new CuotaPagada(prestamoId, pago.id(), pago.numeroCuota(), destino.id(), pago.fecha(),
                pago.monto()));
        return obtener(prestamoId);
    }

    @Transactional
    public PrestamoVista anularPago(Long prestamoId, Long pagoId, String motivo) {
        visible(prestamoId);
        PagoCuota pago = repository.buscarPago(pagoId)
                .filter(pc -> pc.prestamoId().equals(prestamoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", pagoId));
        tesoreria.cajitaGestionable(pago.cajitaDestinoId());
        if (pago.anulado()) {
            throw new ReglaNegocioException("El pago ya fue anulado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("La anulación requiere un motivo");
        }
        repository.anularPago(pagoId, motivo);
        eventos.publishEvent(new PagoCuotaAnulado(pagoId, ciclos.hoy(), motivo));
        auditoria.registrar("pago_cuota", pagoId, "ANULAR", "Préstamo #" + prestamoId + " cuota "
                + pago.numeroCuota() + " — motivo: " + motivo);
        return obtener(prestamoId);
    }

    @Transactional(readOnly = true)
    public ProyeccionIntereses proyeccionIntereses() {
        int meses = ciclos.actual().numeroMeses();
        List<FilaProyeccion> filas = new ArrayList<>();
        List<Dinero> totales = new ArrayList<>();
        for (int m = 0; m < meses; m++) {
            totales.add(Dinero.CERO);
        }
        for (PrestamoVista v : listar()) {
            List<Dinero> fila = CalculadoraPrestamo.proyeccionIntereses(v.resumen().prestamo(), meses);
            for (int m = 0; m < meses; m++) {
                totales.set(m, totales.get(m).sumar(fila.get(m)));
            }
            filas.add(new FilaProyeccion(v.resumen().prestamo().id(), v.nombreIntegrante(), fila));
        }
        return new ProyeccionIntereses(filas, totales, Dinero.sumarTodos(totales));
    }

    /** API interna sin alcance: todos los préstamos con su estado (para resumen, repartición y reportes). */
    @Transactional(readOnly = true)
    public List<PrestamoVista> todosSinAlcance() {
        return vistas(repository.listar());
    }

    private List<PrestamoVista> vistas(List<Prestamo> prestamos) {
        int transcurridos = ciclos.mesesTranscurridos();
        Map<Long, List<PagoCuota>> pagos = repository.pagosVigentes().stream()
                .collect(Collectors.groupingBy(PagoCuota::prestamoId));
        Map<Long, String> nombres = integrantes.nombres();
        return prestamos.stream()
                .sorted(Comparator.comparing(Prestamo::id))
                .map(p -> {
                    List<PagoCuota> ps = pagos.getOrDefault(p.id(), List.of());
                    ResumenPrestamo r = CalculadoraPrestamo.resumir(p,
                            ps.stream().map(PagoCuota::numeroCuota).toList(), transcurridos);
                    return new PrestamoVista(r, nombres.getOrDefault(p.integranteId(), ""), ps);
                }).toList();
    }

    private Prestamo visible(Long id) {
        Prestamo p = repository.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Préstamo", id));
        if (!policy.puedeVer(sesion.usuario(), p)) {
            throw new RecursoNoEncontradoException("Préstamo", id);
        }
        return p;
    }

    private Prestamo validar(NuevoPrestamo cmd) {
        ciclos.exigirCicloAbierto();
        Ciclo ciclo = ciclos.actual();
        Cajita origen = tesoreria.cajitaGestionable(cmd.cajitaOrigenId());
        Integrante integrante = integrantes.buscarSinAlcance(cmd.integranteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Integrante", cmd.integranteId()));
        int mes = cmd.mesDelPrestamo() != null ? cmd.mesDelPrestamo() : ciclo.mesDelCiclo(cmd.fechaDesembolso());
        BigDecimal tasa = cmd.tasaMensual() != null ? cmd.tasaMensual() : ciclo.tasaInteresMensualPrestamos();
        if (tasa.signum() < 0) {
            throw new ReglaNegocioException("La tasa mensual no puede ser negativa");
        }
        reglas.validar(new SolicitudPrestamo(cmd.monto(), mes, cmd.numeroCuotas(), integrante.activo(),
                libro.saldoCajita(origen.id()), ciclo.maxCuotasPrestamo(), ciclo.numeroMeses(),
                ciclo.permitirPrestamoDespuesDelCierre()));
        return new Prestamo(null, integrante.id(), origen.id(), cmd.fechaDesembolso(), cmd.monto().redondearCentavos(),
                mes, cmd.numeroCuotas(), tasa);
    }
}

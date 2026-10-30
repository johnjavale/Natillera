package co.natillera.reportes.application;

import co.natillera.actividades.application.ActividadService;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.ciclo.application.CicloService;
import co.natillera.integrantes.application.AportesService;
import co.natillera.integrantes.application.IntegranteService;
import co.natillera.integrantes.application.IntegranteVista;
import co.natillera.prestamos.application.PrestamoService;
import co.natillera.prestamos.domain.EstadoPrestamo;
import co.natillera.reparticion.application.ReparticionService;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.reportes.domain.CalculadoraDeudores;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.tesoreria.application.TesoreriaService;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.domain.CalculadoraTesoreria;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.TipoMovimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reportes consolidados (sección 4). Todos respetan el alcance del rol. */
@Service
@Transactional(readOnly = true)
public class ReportesService {

    public record FilaConsolidado(Long integranteId, String nombre, Long tesoreroId, String nombreTesorero,
                                  Dinero cuotaMensual, Dinero ahorroEsperado, Dinero totalAportado,
                                  Dinero esperadoALaFecha, Dinero atraso, BigDecimal cumplimiento, Dinero saldoCajita,
                                  int prestamosVigentes, Dinero saldoPrestamos, Dinero vencidoPrestamos,
                                  int cuotasVencidas, Dinero boletasPendientes, Dinero interesesProyectados,
                                  Dinero totalProyectado, Dinero totalReal) {
    }

    public record BoletasDeActividad(Long actividadId, String actividad, int boletas, Dinero valorAPagar,
                                     Dinero pagado, Dinero pendiente) {
    }

    public record EstadoCuenta(FilaConsolidado resumen, AportesService.FilaAportes aportes, int numeroMeses,
                               List<LibroQueryPort.MovimientoVista> movimientos,
                               List<PrestamoService.PrestamoVista> prestamos, List<BoletasDeActividad> boletas,
                               Reparticion.Fila reparto) {
    }

    public record ReporteTesoreria(List<SaldoCajita> cajitas, List<CalculadoraTesoreria.SaldoTesorero> porTesorero,
                                   List<LibroQueryPort.MovimientoVista> movimientos) {
    }

    private final IntegranteService integrantes;
    private final AportesService aportes;
    private final PrestamoService prestamos;
    private final ActividadService actividades;
    private final ReparticionService reparticion;
    private final TesoreriaService tesoreria;
    private final CicloService ciclos;

    public ReportesService(IntegranteService integrantes, AportesService aportes, PrestamoService prestamos,
                           ActividadService actividades, ReparticionService reparticion, TesoreriaService tesoreria,
                           CicloService ciclos) {
        this.integrantes = integrantes;
        this.aportes = aportes;
        this.prestamos = prestamos;
        this.actividades = actividades;
        this.reparticion = reparticion;
        this.tesoreria = tesoreria;
        this.ciclos = ciclos;
    }

    /** 1. Consolidado por integrante (solo los integrantes visibles para el usuario). */
    public List<FilaConsolidado> consolidado() {
        return consolidar(integrantes.listar());
    }

    /** 2. ¿Quién debe y cuánto? */
    public List<CalculadoraDeudores.Deudor> deudores(Long tesoreroId, CalculadoraDeudores.TipoDeuda tipo,
                                                     boolean soloMora) {
        List<CalculadoraDeudores.Deudor> todos = consolidado().stream()
                .map(f -> new CalculadoraDeudores.Deudor(f.integranteId(), f.nombre(), f.tesoreroId(),
                        f.nombreTesorero(), f.atraso(), f.saldoPrestamos(), f.vencidoPrestamos(),
                        f.boletasPendientes()))
                .toList();
        return CalculadoraDeudores.filtrarYOrdenar(todos, tesoreroId, tipo, soloMora);
    }

    /** 3. Estado de cuenta individual. */
    public EstadoCuenta estadoCuenta(Long integranteId) {
        IntegranteVista vista = integrantes.obtener(integranteId);
        FilaConsolidado resumen = consolidar(List.of(vista)).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Integrante", integranteId));
        AportesService.FilaAportes filaAportes = aportes.calcular(Set.of(integranteId)).filas().stream().findFirst()
                .orElse(null);
        List<LibroQueryPort.MovimientoVista> movimientos = vista.cajitaId() == null ? List.of()
                : tesoreria.movimientos(vista.cajitaId(), null, null, null);
        List<PrestamoService.PrestamoVista> suyos = prestamos.todosSinAlcance().stream()
                .filter(p -> p.resumen().prestamo().integranteId().equals(integranteId)).toList();
        List<BoletasDeActividad> boletas = new ArrayList<>();
        for (ResumenActividad a : actividades.todasSinAlcance()) {
            a.boletas().stream().filter(b -> b.integranteId().equals(integranteId)).findFirst()
                    .ifPresent(b -> boletas.add(new BoletasDeActividad(a.actividad().id(), a.actividad().nombre(),
                            b.boletas(), b.valorAPagar(), b.pagado(), b.pendiente())));
        }
        Reparticion.Fila reparto = reparticion.calcularSinAlcance().filas().stream()
                .filter(f -> f.integranteId().equals(integranteId)).findFirst().orElse(null);
        return new EstadoCuenta(resumen, filaAportes, ciclos.actual().numeroMeses(), movimientos, suyos, boletas,
                reparto);
    }

    /** 4. Tesorería: saldos por cajita y por tesorero, conciliación y movimientos por rango y tipo. */
    public ReporteTesoreria tesoreria(LocalDate desde, LocalDate hasta, TipoMovimiento tipo) {
        return new ReporteTesoreria(tesoreria.cajitas(), tesoreria.porTesorero(),
                tesoreria.movimientos(null, tipo, desde, hasta));
    }

    private List<FilaConsolidado> consolidar(List<IntegranteVista> vistas) {
        Set<Long> ids = vistas.stream().map(v -> v.integrante().id()).collect(Collectors.toSet());
        Map<Long, AportesService.FilaAportes> filasAportes = aportes.calcular(ids).filas().stream()
                .collect(Collectors.toMap(AportesService.FilaAportes::integranteId, Function.identity()));
        Map<Long, List<PrestamoService.PrestamoVista>> prestamosPorIntegrante = prestamos.todosSinAlcance().stream()
                .collect(Collectors.groupingBy(p -> p.resumen().prestamo().integranteId()));
        Map<Long, Dinero> boletasPendientes = new HashMap<>();
        for (ResumenActividad a : actividades.todasSinAlcance()) {
            for (ResumenActividad.BoletasIntegrante b : a.boletas()) {
                boletasPendientes.merge(b.integranteId(), b.pendiente().max(Dinero.CERO), Dinero::sumar);
            }
        }
        Map<Long, Reparticion.Fila> reparto = reparticion.calcularSinAlcance().filas().stream()
                .collect(Collectors.toMap(Reparticion.Fila::integranteId, Function.identity()));

        List<FilaConsolidado> filas = new ArrayList<>();
        for (IntegranteVista v : vistas) {
            Long id = v.integrante().id();
            AportesService.FilaAportes fa = filasAportes.get(id);
            int vigentes = 0;
            int vencidas = 0;
            Dinero saldoPrestamos = Dinero.CERO;
            Dinero vencido = Dinero.CERO;
            for (PrestamoService.PrestamoVista p : prestamosPorIntegrante.getOrDefault(id, List.of())) {
                if (p.resumen().estado() != EstadoPrestamo.PAGADO) {
                    vigentes++;
                }
                vencidas += p.resumen().cuotasVencidas();
                saldoPrestamos = saldoPrestamos.sumar(p.resumen().saldoPendiente());
                vencido = vencido.sumar(p.resumen().saldoVencido());
            }
            Reparticion.Fila r = reparto.get(id);
            filas.add(new FilaConsolidado(id, v.integrante().nombre(), v.integrante().tesoreroId(), v.nombreTesorero(),
                    v.integrante().cuotaMensual(), v.ahorroEsperado(),
                    fa == null ? Dinero.CERO : fa.estado().totalAportado(),
                    fa == null ? Dinero.CERO : fa.estado().esperadoALaFecha(),
                    fa == null ? Dinero.CERO : fa.estado().atraso(),
                    fa == null ? BigDecimal.ZERO : fa.estado().cumplimiento(),
                    v.saldoCajita(), vigentes, saldoPrestamos, vencido, vencidas,
                    boletasPendientes.getOrDefault(id, Dinero.CERO),
                    r == null ? Dinero.CERO : r.interesesProyectados(),
                    r == null ? Dinero.CERO : r.totalProyectado(),
                    r == null ? Dinero.CERO : r.totalReal()));
        }
        return filas;
    }
}

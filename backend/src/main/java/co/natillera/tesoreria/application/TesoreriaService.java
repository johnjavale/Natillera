package co.natillera.tesoreria.application;

import co.natillera.auth.application.AccessScopeService;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import co.natillera.tesoreria.domain.CalculadoraTesoreria;
import co.natillera.tesoreria.domain.Cajita;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.MovimientoFactory;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.Tesorero;
import co.natillera.tesoreria.domain.TipoMovimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Casos de uso de tesorería: cajitas, movimientos, aportes en lote y conciliación con Nu. */
@Service
public class TesoreriaService {

    public record AporteLote(Long integranteId, Dinero monto) {
    }

    public record Resumen(List<LibroQueryPort.TotalTipo> porTipo, List<CalculadoraTesoreria.ResumenMes> porMes,
                          List<Tesorero> tesoreros) {
    }

    private static final int LIMITE_MOVIMIENTOS = 500;

    private final TesoreriaRepositoryPort repository;
    private final LibroQueryPort libro;
    private final AccessScopeService access;
    private final CicloService ciclos;
    private final AuditoriaPort auditoria;

    public TesoreriaService(TesoreriaRepositoryPort repository, LibroQueryPort libro, AccessScopeService access,
                            CicloService ciclos, AuditoriaPort auditoria) {
        this.repository = repository;
        this.libro = libro;
        this.access = access;
        this.ciclos = ciclos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<SaldoCajita> cajitas() {
        return libro.saldosCajitas(visibles());
    }

    @Transactional(readOnly = true)
    public List<CalculadoraTesoreria.SaldoTesorero> porTesorero() {
        return CalculadoraTesoreria.porTesorero(cajitas());
    }

    @Transactional(readOnly = true)
    public List<LibroQueryPort.MovimientoVista> movimientos(Long cajitaId, TipoMovimiento tipo, LocalDate desde,
                                                            LocalDate hasta) {
        if (cajitaId != null) {
            Cajita c = cajitaVisible(cajitaId);
            cajitaId = c.id();
        }
        return libro.movimientos(new LibroQueryPort.FiltroMovimientos(visibles(), cajitaId, tipo, desde, hasta,
                LIMITE_MOVIMIENTOS));
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        Ciclo ciclo = ciclos.actual();
        List<Tesorero> tesoreros = repository.listarTesoreros();
        List<CalculadoraTesoreria.FlujoMes> flujos = new ArrayList<>();
        for (LibroQueryPort.FlujoAnioMes f : libro.flujoPorMesYTesorero(visibles())) {
            int mes = CalendarioCiclo.mesDelCiclo(ciclo.fechaInicio(), LocalDate.of(f.anio(), f.mes(), 1));
            flujos.add(new CalculadoraTesoreria.FlujoMes(mes, f.tesoreroId(), f.entradas(), f.salidas()));
        }
        List<CalculadoraTesoreria.ResumenMes> porMes = CalculadoraTesoreria.porMes(ciclo.numeroMeses(),
                tesoreros.stream().map(Tesorero::id).toList(), flujos);
        return new Resumen(libro.totalesPorTipo(visibles()), porMes, tesoreros);
    }

    @Transactional
    public Movimiento registrarMovimiento(Long cajitaId, LocalDate fecha, TipoMovimiento tipo, String descripcion,
                                          Dinero monto) {
        Cajita cajita = cajitaGestionable(cajitaId);
        Movimiento m = MovimientoFactory.manual(cajita.id(), fecha, tipo, descripcion, monto);
        validarSaldoSuficiente(cajita, m);
        return repository.guardarMovimiento(m);
    }

    /** Registro rápido de aportes del mes para varios integrantes. Todo o nada. */
    @Transactional
    public List<Movimiento> registrarAportesLote(LocalDate fecha, String descripcion, List<AporteLote> aportes) {
        if (aportes == null || aportes.isEmpty()) {
            throw new ReglaNegocioException("Indique al menos un aporte");
        }
        String texto = descripcion == null || descripcion.isBlank() ? "Aporte" : descripcion;
        // Primero se valida todo el lote (alcance y montos) y solo después se guarda
        List<Movimiento> nuevos = new ArrayList<>();
        for (AporteLote a : aportes) {
            if (a.monto() == null || a.monto().esCero()) {
                continue;
            }
            access.exigirGestionarIntegrante(a.integranteId());
            Cajita cajita = repository.cajitaDeIntegrante(a.integranteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cajita del integrante", a.integranteId()));
            nuevos.add(MovimientoFactory.ahorro(cajita.id(), fecha, texto, a.monto()));
        }
        if (nuevos.isEmpty()) {
            throw new ReglaNegocioException("Todos los aportes están en cero");
        }
        return nuevos.stream().map(repository::guardarMovimiento).toList();
    }

    @Transactional
    public Movimiento anularMovimiento(Long movimientoId, String motivo) {
        Movimiento original = repository.buscarMovimiento(movimientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento", movimientoId));
        cajitaGestionable(original.cajitaId());
        if (original.prestamoId() != null) {
            throw new ReglaNegocioException("Los movimientos de préstamos se anulan desde el módulo de préstamos");
        }
        if (repository.estaAnulado(movimientoId)) {
            throw new ReglaNegocioException("El movimiento ya fue anulado");
        }
        Movimiento anulacion = repository.guardarMovimiento(
                MovimientoFactory.contramovimiento(original, ciclos.hoy(), motivo));
        auditoria.registrar("movimiento", movimientoId, "ANULAR",
                original.tipo() + " " + original.monto().aCentavos() + " — motivo: " + motivo);
        return anulacion;
    }

    @Transactional
    public void registrarSaldoReportado(Long cajitaId, LocalDate fecha, Dinero saldoApp) {
        Cajita cajita = cajitaGestionable(cajitaId);
        if (saldoApp == null || saldoApp.esNegativo()) {
            throw new ReglaNegocioException("El saldo reportado no puede ser negativo");
        }
        repository.guardarSaldoReportado(cajita.id(), fecha, saldoApp);
    }

    /** Movimientos generados por otros módulos (el caso de uso de origen ya validó permisos). */
    @Transactional
    public Movimiento registrarDerivado(Movimiento movimiento) {
        return repository.guardarMovimiento(movimiento);
    }

    @Transactional(readOnly = true)
    public List<Tesorero> tesoreros() {
        return repository.listarTesoreros();
    }

    /** Cajita visible para el usuario (404 si no existe o está fuera de su alcance). */
    public Cajita cajitaVisible(Long cajitaId) {
        Cajita cajita = repository.buscarCajita(cajitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajita", cajitaId));
        if (!access.puedeVerIntegrante(cajita.integranteId())) {
            throw new RecursoNoEncontradoException("Cajita", cajitaId);
        }
        return cajita;
    }

    /** Cajita que el usuario administra (404 si no la ve, 403 si la ve pero no es su tesorero). */
    public Cajita cajitaGestionable(Long cajitaId) {
        Cajita cajita = repository.buscarCajita(cajitaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cajita", cajitaId));
        access.exigirGestionarIntegrante(cajita.integranteId());
        return cajita;
    }

    private void validarSaldoSuficiente(Cajita cajita, Movimiento m) {
        if (m.salida().esPositivo() && m.salida().esMayorQue(libro.saldoCajita(cajita.id()))) {
            throw new ReglaNegocioException("La salida supera el saldo de la cajita");
        }
    }

    private Set<Long> visibles() {
        return access.integrantesVisibles().orElse(null);
    }
}

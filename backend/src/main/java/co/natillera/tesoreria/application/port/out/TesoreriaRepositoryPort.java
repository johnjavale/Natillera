package co.natillera.tesoreria.application.port.out;

import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.domain.Cajita;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.Tesorero;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de escritura/lectura puntual del módulo de tesorería. */
public interface TesoreriaRepositoryPort {

    Movimiento guardarMovimiento(Movimiento movimiento);

    Optional<Movimiento> buscarMovimiento(Long id);

    /** Movimiento original (no anulación) generado por un pago de cuota. */
    Optional<Movimiento> movimientoDePagoCuota(Long pagoCuotaId);

    boolean estaAnulado(Long movimientoId);

    Optional<Cajita> buscarCajita(Long cajitaId);

    Optional<Cajita> cajitaDeIntegrante(Long integranteId);

    Cajita crearCajita(Long integranteId, String nombre);

    void renombrarCajita(Long integranteId, String nombre);

    void guardarSaldoReportado(Long cajitaId, LocalDate fecha, Dinero saldoApp);

    List<Tesorero> listarTesoreros();

    Optional<Tesorero> buscarTesorero(Long tesoreroId);

    Tesorero asegurarTesorero(Long usuarioId, String nombre);
}

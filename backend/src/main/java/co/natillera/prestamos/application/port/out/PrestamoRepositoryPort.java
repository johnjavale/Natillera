package co.natillera.prestamos.application.port.out;

import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;

import java.util.List;
import java.util.Optional;

public interface PrestamoRepositoryPort {

    Prestamo guardar(Prestamo prestamo);

    Optional<Prestamo> buscar(Long id);

    List<Prestamo> listar();

    PagoCuota guardarPago(PagoCuota pago);

    Optional<PagoCuota> buscarPago(Long pagoId);

    /** Pagos no anulados de todos los préstamos. */
    List<PagoCuota> pagosVigentes();

    List<PagoCuota> pagosDe(Long prestamoId);

    void anularPago(Long pagoId, String motivo);
}

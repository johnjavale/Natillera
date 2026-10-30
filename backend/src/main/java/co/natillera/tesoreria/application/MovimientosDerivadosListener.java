package co.natillera.tesoreria.application;

import co.natillera.actividades.domain.evento.PagoBoletasRegistrado;
import co.natillera.prestamos.domain.evento.CuotaPagada;
import co.natillera.prestamos.domain.evento.PagoCuotaAnulado;
import co.natillera.prestamos.domain.evento.PrestamoCreado;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.MovimientoFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Domain Events (en memoria, síncronos y dentro de la misma transacción): cada evento de préstamos o rifas
 * genera su movimiento en el libro, de modo que el préstamo y su dinero se guardan juntos o no se guardan.
 */
@Component
public class MovimientosDerivadosListener {

    private final TesoreriaService tesoreria;
    private final TesoreriaRepositoryPort repository;
    private final AuditoriaPort auditoria;

    public MovimientosDerivadosListener(TesoreriaService tesoreria, TesoreriaRepositoryPort repository,
                                        AuditoriaPort auditoria) {
        this.tesoreria = tesoreria;
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @EventListener
    public void alCrearPrestamo(PrestamoCreado e) {
        tesoreria.registrarDerivado(MovimientoFactory.desembolsoPrestamo(e.cajitaOrigenId(), e.fecha(), e.prestamoId(),
                e.nombreIntegrante(), e.monto()));
    }

    @EventListener
    public void alPagarCuota(CuotaPagada e) {
        tesoreria.registrarDerivado(MovimientoFactory.pagoCuota(e.cajitaDestinoId(), e.fecha(), e.prestamoId(),
                e.pagoCuotaId(), e.numeroCuota(), e.monto()));
    }

    @EventListener
    public void alAnularPagoCuota(PagoCuotaAnulado e) {
        Movimiento original = repository.movimientoDePagoCuota(e.pagoCuotaId())
                .orElseThrow(() -> new IllegalStateException("El pago " + e.pagoCuotaId() + " no tiene movimiento"));
        tesoreria.registrarDerivado(MovimientoFactory.contramovimiento(original, e.fecha(), e.motivo()));
        auditoria.registrar("movimiento", original.id(), "ANULAR", "Anulación de pago de cuota: " + e.motivo());
    }

    @EventListener
    public void alPagarBoletas(PagoBoletasRegistrado e) {
        tesoreria.registrarDerivado(MovimientoFactory.pagoBoletas(e.cajitaId(), e.fecha(), e.actividadId(),
                e.nombreActividad(), e.monto()));
    }
}

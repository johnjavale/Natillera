package co.natillera.tesoreria.application.port.out;

import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.TipoMovimiento;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Consultas agregadas sobre el libro de movimientos (SQL con GROUP BY, sin cargar entidades en memoria).
 * Un filtro {@code integrantes == null} significa "todos" (alcance ALL).
 */
public interface LibroQueryPort {

    record AporteAnioMes(Long integranteId, int anio, int mes, Dinero monto) {
    }

    record MontoPorIntegrante(Long integranteId, Dinero monto) {
    }

    record PagoBoletas(Long actividadId, Long integranteId, Dinero monto) {
    }

    record TotalTipo(TipoMovimiento tipo, Dinero entradas, Dinero salidas) {
    }

    record FlujoAnioMes(int anio, int mes, Long tesoreroId, Dinero entradas, Dinero salidas) {
    }

    record FiltroMovimientos(Collection<Long> integrantes, Long cajitaId, TipoMovimiento tipo, LocalDate desde,
                             LocalDate hasta, int limite) {
    }

    record MovimientoVista(Long id, LocalDate fecha, Long cajitaId, String nombreCajita, Long integranteId,
                           String nombreIntegrante, Long tesoreroId, String nombreTesorero, TipoMovimiento tipo,
                           String descripcion, Dinero entrada, Dinero salida, Long prestamoId, Long actividadId,
                           Long anulaA, Long anuladoPor, String creadoPor, OffsetDateTime creadoEn) {
    }

    List<SaldoCajita> saldosCajitas(Collection<Long> integrantes);

    Dinero saldoCajita(Long cajitaId);

    List<AporteAnioMes> aportesPorIntegranteYMes(Collection<Long> integrantes);

    List<MontoPorIntegrante> rendimientoNuPorIntegrante(Collection<Long> integrantes);

    List<PagoBoletas> pagosBoletas(Collection<Long> integrantes);

    List<TotalTipo> totalesPorTipo(Collection<Long> integrantes);

    List<FlujoAnioMes> flujoPorMesYTesorero(Collection<Long> integrantes);

    List<MovimientoVista> movimientos(FiltroMovimientos filtro);
}

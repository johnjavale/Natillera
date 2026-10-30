package co.natillera.prestamos.domain;

import co.natillera.prestamos.domain.regla.ReglasPrestamo;
import co.natillera.prestamos.domain.regla.SolicitudPrestamo;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPrestamoTest {

    static Prestamo prestamo(long monto, int mes, int cuotas) {
        return new Prestamo(1L, 1L, 1L, LocalDate.of(2027, 1, 1).plusMonths(mes - 1L), Dinero.de(monto), mes, cuotas,
                new BigDecimal("0.03"));
    }

    static SolicitudPrestamo solicitud(long monto, int mes, int cuotas, long saldoCajita) {
        return new SolicitudPrestamo(Dinero.de(monto), mes, cuotas, true, Dinero.de(saldoCajita), 12, 12, false);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 7, 11})
    void planDePagosSumaExactamenteElMontoConResiduoEnLaUltimaCuota(int cuotas) {
        Prestamo p = prestamo(1_000_000, 1, cuotas);
        List<CuotaPlan> plan = CalculadoraPrestamo.planDePagos(p);
        Dinero capital = Dinero.sumarTodos(plan.stream().map(CuotaPlan::capital).toList());
        assertThat(capital.valor()).isEqualByComparingTo("1000000");
        assertThat(plan).hasSize(cuotas);
        plan.forEach(c -> assertThat(c.valor().valor().scale()).isLessThanOrEqualTo(0));
    }

    @Test
    void residuoDeRedondeoVaEnLaUltimaCuota() {
        List<CuotaPlan> plan = CalculadoraPrestamo.planDePagos(prestamo(1_000_000, 2, 6));
        assertThat(plan.get(0).capital().valor()).isEqualByComparingTo("166667");
        assertThat(plan.get(5).capital().valor()).isEqualByComparingTo("166665");
        assertThat(plan.get(0).mesVencimiento()).isEqualTo(3);
        assertThat(plan.get(5).mesVencimiento()).isEqualTo(8);
    }

    @Test
    void prestamoDeUnaCuota() {
        ResumenPrestamo r = CalculadoraPrestamo.resumir(prestamo(500_000, 4, 1), List.of(), 4);
        assertThat(r.valorCuota().valor()).isEqualByComparingTo("515000");
        assertThat(r.estado()).isEqualTo(EstadoPrestamo.VIGENTE);
    }

    @Test
    void prestamoDeDoceCuotasDesdeElMes1TerminaDespuesDelCierre() {
        assertThatThrownBy(() -> ReglasPrestamo.cadenaPorDefecto().validar(solicitud(100_000, 1, 12, 1_000_000)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El préstamo termina después del cierre de la natillera");
    }

    @Test
    void prestamoQueTerminaJustoEnElMes12EsValido() {
        ReglasPrestamo.cadenaPorDefecto().validar(solicitud(100_000, 6, 6, 1_000_000));
        ResumenPrestamo r = CalculadoraPrestamo.resumir(prestamo(100_000, 6, 6), List.of(), 12);
        assertThat(r.plan().get(5).mesVencimiento()).isEqualTo(12);
    }

    @Test
    void enMoraCuandoHayCuotasVencidasSinPago() {
        ResumenPrestamo r = CalculadoraPrestamo.resumir(prestamo(1_000_000, 2, 6), List.of(1), 5);
        assertThat(r.cuotasVencidas()).isEqualTo(2);
        assertThat(r.estado()).isEqualTo(EstadoPrestamo.EN_MORA);
        assertThat(r.saldoVencido().valor()).isEqualByComparingTo("393334");
    }

    @Test
    void pagadoCuandoTodasLasCuotasTienenPago() {
        ResumenPrestamo r = CalculadoraPrestamo.resumir(prestamo(1_000_000, 2, 6), List.of(1, 2, 3, 4, 5, 6), 12);
        assertThat(r.estado()).isEqualTo(EstadoPrestamo.PAGADO);
        assertThat(r.saldoPendiente().esCero()).isTrue();
        assertThat(r.interesesRecibidos().valor()).isEqualByComparingTo("180000");
    }

    @Test
    void masDeDoceCuotasSeRechaza() {
        assertThatThrownBy(() -> ReglasPrestamo.cadenaPorDefecto().validar(solicitud(100_000, 1, 13, 1_000_000)))
                .hasMessage("Los préstamos no pueden ser a más de 12 cuotas (un año)");
    }

    @Test
    void montoMayorAlSaldoDeLaCajitaSeRechaza() {
        assertThatThrownBy(() -> ReglasPrestamo.cadenaPorDefecto().validar(solicitud(1_000_000, 2, 6, 600_000)))
                .hasMessageStartingWith("El monto supera el saldo disponible de la cajita origen");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void montoNoPositivoSeRechaza(long monto) {
        assertThatThrownBy(() -> ReglasPrestamo.cadenaPorDefecto().validar(solicitud(monto, 2, 6, 600_000)))
                .hasMessage("El monto del préstamo debe ser mayor que cero");
    }

    @Test
    void integranteInactivoSeRechaza() {
        SolicitudPrestamo s = new SolicitudPrestamo(Dinero.de(1000), 2, 2, false, Dinero.de(10_000), 12, 12, false);
        assertThatThrownBy(() -> ReglasPrestamo.cadenaPorDefecto().validar(s)).hasMessage("El integrante no está activo");
    }

    @Test
    void flagPermiteTerminarDespuesDelCierre() {
        SolicitudPrestamo s = new SolicitudPrestamo(Dinero.de(1000), 10, 6, true, Dinero.de(10_000), 12, 12, true);
        ReglasPrestamo.cadenaPorDefecto().validar(s);
    }
}

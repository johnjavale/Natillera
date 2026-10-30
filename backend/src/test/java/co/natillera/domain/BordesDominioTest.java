package co.natillera.domain;

import co.natillera.actividades.domain.CalculadoraActividad;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.ciclo.domain.EstadoCiclo;
import co.natillera.integrantes.domain.CalculadoraAhorro;
import co.natillera.integrantes.domain.CalculadoraAportes;
import co.natillera.integrantes.domain.EstadoAportes;
import co.natillera.reparticion.domain.CalculadoraReparticion;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.domain.CalculadoraTesoreria;
import co.natillera.tesoreria.domain.Conciliacion;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.MovimientoFactory;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Fórmulas y casos borde del dominio")
class BordesDominioTest {

    static final LocalDate INICIO = LocalDate.of(2027, 1, 1);

    @Test
    void mesDelCicloYMesesTranscurridos() {
        assertThat(CalendarioCiclo.mesDelCiclo(INICIO, LocalDate.of(2027, 2, 15))).isEqualTo(2);
        assertThat(CalendarioCiclo.mesDelCiclo(INICIO, LocalDate.of(2028, 1, 1))).isEqualTo(13);
        assertThat(CalendarioCiclo.mesesTranscurridos(INICIO, 12, LocalDate.of(2026, 10, 26))).isZero();
        assertThat(CalendarioCiclo.mesesTranscurridos(INICIO, 12, LocalDate.of(2027, 3, 20))).isEqualTo(3);
        assertThat(CalendarioCiclo.mesesTranscurridos(INICIO, 12, LocalDate.of(2029, 1, 1))).isEqualTo(12);
    }

    @Test
    void fechaDeCierreYTasaMensualNu() {
        Ciclo c = Ciclo.porDefecto();
        assertThat(c.fechaCierre()).isEqualTo(LocalDate.of(2027, 12, 1));
        assertThat(c.tasaMensualNu()).isCloseTo(new BigDecimal("0.0072073233"), org.assertj.core.data.Offset.offset(new BigDecimal("0.0000000001")));
    }

    @Test
    void cicloRechazaMasDe12Meses() {
        assertThatThrownBy(() -> new Ciclo(null, "X", INICIO, 13, BigDecimal.ZERO, Dinero.CERO, 12, BigDecimal.ZERO, 2,
                false, EstadoCiclo.ACTIVO)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void tasaEACeroDaRendimientoCero() {
        Ciclo c = new Ciclo(null, "X", INICIO, 12, BigDecimal.ZERO, Dinero.CERO, 12, BigDecimal.ZERO, 2, false, null);
        assertThat(CalculadoraAhorro.rendimientoProyectadoCajita(Dinero.de(100000), 12, c.tasaMensualNu()).esCero())
                .isTrue();
    }

    @Test
    void porcentajeConTotalCeroEsCero() {
        assertThat(CalculadoraAhorro.porcentajeDelTotal(Dinero.CERO, Dinero.CERO)).isEqualByComparingTo("0");
    }

    @Test
    void cuotaCeroNoGeneraAtrasoNiDivisionPorCero() {
        EstadoAportes e = CalculadoraAportes.estado(Dinero.CERO, 12, 5, Map.of());
        assertThat(e.atraso().esCero()).isTrue();
        assertThat(e.cumplimiento()).isEqualByComparingTo("0");
    }

    @Test
    void atrasoAportes() {
        EstadoAportes e = CalculadoraAportes.estado(Dinero.de(100000), 12, 3,
                Map.of(1, Dinero.de(100000), 2, Dinero.de(50000)));
        assertThat(e.totalAportado().valor()).isEqualByComparingTo("150000");
        assertThat(e.esperadoALaFecha().valor()).isEqualByComparingTo("300000");
        assertThat(e.atraso().valor()).isEqualByComparingTo("150000");
        assertThat(e.enAtraso()).isTrue();
        assertThat(e.diferencia().valor()).isEqualByComparingTo("-1050000");
        assertThat(e.cumplimiento()).isEqualByComparingTo("0.125");
    }

    @Test
    void totalesPorMesYAcumulados() {
        EstadoAportes a = CalculadoraAportes.estado(Dinero.de(100), 3, 3, Map.of(1, Dinero.de(100), 2, Dinero.de(100)));
        EstadoAportes b = CalculadoraAportes.estado(Dinero.de(50), 3, 3, Map.of(1, Dinero.de(50)));
        List<CalculadoraAportes.TotalMes> t = CalculadoraAportes.totalesPorMes(3, Dinero.de(150), List.of(a, b));
        assertThat(t.get(0).totalReal().valor()).isEqualByComparingTo("150");
        assertThat(t.get(1).acumuladoReal().valor()).isEqualByComparingTo("250");
        assertThat(t.get(2).acumuladoEsperado().valor()).isEqualByComparingTo("450");
    }

    @Test
    void reparticionSinIntegrantes() {
        Reparticion r = CalculadoraReparticion.porDefecto().calcular(new CalculadoraReparticion.Entrada(List.of(),
                Dinero.de(1000), Dinero.CERO, Dinero.CERO, Dinero.de(500), Dinero.CERO));
        assertThat(r.filas()).isEmpty();
        assertThat(r.proyectado().utilidadActividadesPorIntegrante().esCero()).isTrue();
    }

    @Test
    void reparticionConAhorroTotalCeroNoReparteIntereses() {
        Reparticion r = CalculadoraReparticion.porDefecto().calcular(new CalculadoraReparticion.Entrada(
                List.of(new CalculadoraReparticion.Integrante(1L, "A", Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO)),
                Dinero.de(1000), Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO));
        assertThat(r.filas().get(0).interesesProyectados().esCero()).isTrue();
    }

    @Test
    void gastosMayoresQueInteresesDanInteresesNetosCero() {
        Reparticion r = CalculadoraReparticion.porDefecto().calcular(new CalculadoraReparticion.Entrada(
                List.of(new CalculadoraReparticion.Integrante(1L, "A", Dinero.de(10), Dinero.de(10), Dinero.CERO, Dinero.CERO)),
                Dinero.de(1000), Dinero.de(100), Dinero.de(5000), Dinero.CERO, Dinero.CERO));
        assertThat(r.proyectado().interesesNetos().esCero()).isTrue();
        assertThat(r.real().interesesNetos().esCero()).isTrue();
    }

    @Test
    void empatesEnElRankingCompartenPuesto() {
        List<CalculadoraReparticion.Integrante> lista = List.of(
                new CalculadoraReparticion.Integrante(1L, "A", Dinero.de(100), Dinero.CERO, Dinero.CERO, Dinero.CERO),
                new CalculadoraReparticion.Integrante(2L, "B", Dinero.de(300), Dinero.CERO, Dinero.CERO, Dinero.CERO),
                new CalculadoraReparticion.Integrante(3L, "C", Dinero.de(100), Dinero.CERO, Dinero.CERO, Dinero.CERO),
                new CalculadoraReparticion.Integrante(4L, "D", Dinero.de(50), Dinero.CERO, Dinero.CERO, Dinero.CERO));
        Reparticion r = CalculadoraReparticion.porDefecto().calcular(new CalculadoraReparticion.Entrada(lista,
                Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO));
        assertThat(r.filas()).extracting(Reparticion.Fila::puesto).containsExactly(2, 1, 2, 4);
    }

    @Test
    void movimientoConEntradaYSalidaALaVezSeRechaza() {
        assertThatThrownBy(() -> new Movimiento(null, 1L, INICIO, TipoMovimiento.AHORRO, "x", Dinero.de(1),
                Dinero.de(1), null, null, null, null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no ambas");
    }

    @Test
    void movimientoDeSalidaConTipoDeEntradaSeRechaza() {
        assertThatThrownBy(() -> new Movimiento(null, 1L, INICIO, TipoMovimiento.AHORRO, "x", Dinero.CERO,
                Dinero.de(1), null, null, null, null, null)).hasMessageContaining("es de entrada");
    }

    @Test
    void tiposDerivadosNoSeRegistranAMano() {
        assertThatThrownBy(() -> MovimientoFactory.manual(1L, INICIO, TipoMovimiento.DESEMBOLSO_PRESTAMO, "x",
                Dinero.de(10))).hasMessageContaining("automáticamente");
    }

    @Test
    void contramovimientoInvierteElLadoYExigeMotivo() {
        Movimiento original = new Movimiento(9L, 1L, INICIO, TipoMovimiento.AHORRO, "x", Dinero.de(100), Dinero.CERO,
                null, null, null, null, null);
        Movimiento anulacion = MovimientoFactory.contramovimiento(original, INICIO, "error de digitación");
        assertThat(anulacion.salida().valor()).isEqualByComparingTo("100");
        assertThat(anulacion.neto().valor()).isEqualByComparingTo("-100");
        assertThat(anulacion.anulaA()).isEqualTo(9L);
        assertThatThrownBy(() -> MovimientoFactory.contramovimiento(original, INICIO, " "))
                .hasMessage("La anulación requiere un motivo");
        assertThatThrownBy(() -> MovimientoFactory.contramovimiento(anulacion, INICIO, "otra"))
                .hasMessage("No se puede anular una anulación");
    }

    @Test
    void conciliacionSinSaldoReportadoEsCero() {
        assertThat(Conciliacion.diferencia(null, Dinero.de(500)).esCero()).isTrue();
        assertThat(Conciliacion.diferencia(Dinero.de(450), Dinero.de(500)).valor()).isEqualByComparingTo("-50");
    }

    @Test
    void saldosPorTesoreroYPorMes() {
        List<SaldoCajita> cajitas = List.of(
                new SaldoCajita(1L, "c1", 1L, "A", 10L, "T1", Dinero.de(300), Dinero.de(100), Dinero.de(200), INICIO),
                new SaldoCajita(2L, "c2", 2L, "B", 10L, "T1", Dinero.de(50), Dinero.CERO, null, null),
                new SaldoCajita(3L, "c3", 3L, "C", 20L, "T2", Dinero.de(70), Dinero.CERO, null, null));
        List<CalculadoraTesoreria.SaldoTesorero> t = CalculadoraTesoreria.porTesorero(cajitas);
        assertThat(t).hasSize(2);
        assertThat(t.get(0).numeroCajitas()).isEqualTo(2);
        assertThat(t.get(0).saldoRegistro().valor()).isEqualByComparingTo("250");
        assertThat(t.get(0).diferencia().valor()).isEqualByComparingTo("-50");
        assertThat(t.get(1).diferencia().esCero()).isTrue();

        List<CalculadoraTesoreria.ResumenMes> meses = CalculadoraTesoreria.porMes(3, List.of(10L, 20L), List.of(
                new CalculadoraTesoreria.FlujoMes(1, 10L, Dinero.de(300), Dinero.CERO),
                new CalculadoraTesoreria.FlujoMes(2, 10L, Dinero.CERO, Dinero.de(100)),
                new CalculadoraTesoreria.FlujoMes(2, 20L, Dinero.de(70), Dinero.CERO)));
        assertThat(meses.get(1).saldoAcumulado().valor()).isEqualByComparingTo("270");
        assertThat(meses.get(2).saldoAcumuladoPorTesorero().get(10L).valor()).isEqualByComparingTo("200");
    }

    @Test
    void masDeDosActividadesSeRechaza() {
        CalculadoraActividad.validarCupo(1, 2);
        assertThatThrownBy(() -> CalculadoraActividad.validarCupo(2, 2))
                .hasMessage("El ciclo admite máximo 2 actividades");
    }

    @Test
    void dineroRedondeaHalfUp() {
        assertThat(Dinero.de("0.005").redondearCentavos().valor()).isEqualByComparingTo("0.01");
        assertThat(Dinero.de("1250000.5").redondearAlPeso().valor()).isEqualByComparingTo("1250001");
        assertThat(Dinero.de(10).equals(Dinero.de("10.00"))).isTrue();
    }
}

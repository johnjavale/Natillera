package co.natillera.golden;

import co.natillera.actividades.domain.Actividad;
import co.natillera.actividades.domain.CalculadoraActividad;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.domain.CalculadoraAhorro;
import co.natillera.prestamos.domain.CalculadoraPrestamo;
import co.natillera.prestamos.domain.EstadoPrestamo;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.prestamos.domain.ResumenPrestamo;
import co.natillera.reparticion.domain.CalculadoraReparticion;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.resumen.domain.CalculadoraResumen;
import co.natillera.resumen.domain.FilaResumenMensual;
import co.natillera.shared.domain.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de referencia de la sección 3.9 del prompt: deben coincidir EXACTO con el Excel
 * (valores comparados al centavo con redondeo HALF_UP).
 */
@DisplayName("Golden tests contra el Excel (sección 3.9)")
class GoldenExcelTest {

    static final long[] CUOTAS = {100000, 50000, 200000, 100000, 150000, 50000, 300000, 100000, 80000, 120000};
    static final Ciclo CICLO = Ciclo.porDefecto();
    static final Prestamo PRESTAMO = new Prestamo(1L, 3L, 7L, LocalDate.of(2027, 2, 15), Dinero.de(1_000_000), 2, 6,
            new BigDecimal("0.03"));

    static void assertDinero(Dinero real, String esperado) {
        assertThat(real.aCentavos()).isEqualByComparingTo(new BigDecimal(esperado));
    }

    static Dinero sumaCuotas() {
        return Dinero.sumarTodos(IntStream.range(0, CUOTAS.length).mapToObj(i -> Dinero.de(CUOTAS[i])).toList());
    }

    static Actividad rifa1() {
        return new Actividad(1L, "Rifa Televisor 55\"", LocalDate.of(2027, 6, 15), Dinero.de(10000), 20,
                Dinero.de(1_800_000), Dinero.de(50_000), null);
    }

    static Actividad rifa2() {
        return new Actividad(2L, "Rifa PS5", LocalDate.of(2027, 11, 30), Dinero.de(15000), 20,
                Dinero.de(2_400_000), Dinero.de(50_000), null);
    }

    static List<CalculadoraActividad.ParticipanteActividad> participantes() {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new CalculadoraActividad.ParticipanteActividad((long) i, "Integrante " + i)).toList();
    }

    @Test
    void sumaDeCuotasYAhorroEsperadoTotal() {
        assertDinero(sumaCuotas(), "1250000");
        Dinero total = Dinero.CERO;
        for (long c : CUOTAS) {
            total = total.sumar(CalculadoraAhorro.ahorroEsperado(Dinero.de(c), 12));
        }
        assertDinero(total, "15000000");
    }

    @Nested
    @DisplayName("Préstamo 1.000.000, mes 2, 6 cuotas, 3 %")
    class PrestamoGolden {

        @Test
        void columnasCalculadas() {
            assertDinero(CalculadoraPrestamo.interesPorCuota(PRESTAMO), "30000");
            assertDinero(CalculadoraPrestamo.abonoCapitalPorCuota(PRESTAMO), "166666.67");
            assertDinero(CalculadoraPrestamo.valorCuota(PRESTAMO), "196666.67");
            assertDinero(CalculadoraPrestamo.totalIntereses(PRESTAMO), "180000");
            assertDinero(CalculadoraPrestamo.totalAPagar(PRESTAMO), "1180000");
        }

        @Test
        void conUnaCuotaPagada() {
            ResumenPrestamo r = CalculadoraPrestamo.resumir(PRESTAMO, List.of(1), 3);
            assertDinero(r.interesesRecibidos(), "30000");
            assertDinero(r.saldoPendiente(), "983333.33");
            assertThat(r.estado()).isEqualTo(EstadoPrestamo.VIGENTE);
        }

        @Test
        void interesesProyectadosEnLosMeses3a8() {
            List<Dinero> fila = CalculadoraPrestamo.proyeccionIntereses(PRESTAMO, 12);
            for (int m = 1; m <= 12; m++) {
                assertDinero(fila.get(m - 1), m >= 3 && m <= 8 ? "30000" : "0");
            }
        }
    }

    @Test
    @DisplayName("Repartición proyectada: integrante 1 → 14.400; integrante 7 → 43.200")
    void reparticionProyectada() {
        Reparticion r = reparticionGolden();
        Reparticion.Fila i1 = r.filas().get(0);
        Reparticion.Fila i7 = r.filas().get(6);
        assertDinero(i1.ahorroEsperado(), "1200000");
        assertThat(i1.participacionProyectada()).isEqualByComparingTo("0.08");
        assertDinero(i1.interesesProyectados(), "14400");
        assertDinero(i7.ahorroEsperado(), "3600000");
        assertThat(i7.participacionProyectada()).isEqualByComparingTo("0.24");
        assertDinero(i7.interesesProyectados(), "43200");
        assertDinero(r.proyectado().utilidadActividadesPorIntegrante(), "70000");
        assertThat(i7.puesto()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rendimiento proyectado de cajitas Nu: 47.568,33 y 594.604,17")
    void rendimientoCajitasNu() {
        BigDecimal tasa = CICLO.tasaMensualNu();
        assertDinero(CalculadoraAhorro.rendimientoProyectadoCajita(Dinero.de(100000), 12, tasa), "47568.33");
        Dinero total = Dinero.CERO;
        for (long c : CUOTAS) {
            total = total.sumar(CalculadoraAhorro.rendimientoProyectadoCajita(Dinero.de(c), 12, tasa));
        }
        assertDinero(total, "594604.17");
    }

    @Test
    @DisplayName("Rifas: 150.000 + 550.000 = 700.000 → 70.000 por integrante")
    void rifas() {
        ResumenActividad r1 = CalculadoraActividad.resumir(rifa1(), CICLO.fechaInicio(), participantes(), Map.of(), Map.of());
        ResumenActividad r2 = CalculadoraActividad.resumir(rifa2(), CICLO.fechaInicio(), participantes(), Map.of(), Map.of());
        assertDinero(r1.recaudoEsperado(), "2000000");
        assertDinero(r1.utilidadEsperada(), "150000");
        assertDinero(r2.recaudoEsperado(), "3000000");
        assertDinero(r2.utilidadEsperada(), "550000");
        Dinero total = r1.utilidadEsperada().sumar(r2.utilidadEsperada());
        assertDinero(total, "700000");
        assertDinero(CalculadoraActividad.porIntegrante(total, 10), "70000");
        assertThat(r1.mesDeLaActividad()).isEqualTo(6);
        assertThat(r2.mesDeLaActividad()).isEqualTo(11);
    }

    @Test
    @DisplayName("Resumen: caja acumulada mes 1 = 1.250.000; mes 2 = 1.500.000")
    void resumenCajaAcumulada() {
        List<FilaResumenMensual> filas = CalculadoraResumen.calcular(CICLO.fechaInicio(), 12, sumaCuotas(), Map.of(),
                List.of(PRESTAMO), Map.of(), List.of());
        assertDinero(filas.get(0).cajaDisponibleAcumulada(), "1250000");
        assertDinero(filas.get(1).cajaDisponibleAcumulada(), "1500000");
        assertDinero(filas.get(1).prestamosEntregados(), "1000000");
        assertDinero(filas.get(2).intereses(), "30000");
        assertDinero(filas.get(2).capitalRecuperado(), "166666.67");
        assertThat(filas).noneMatch(FilaResumenMensual::alertaCajaNegativa);
    }

    static Reparticion reparticionGolden() {
        BigDecimal tasaNu = CICLO.tasaMensualNu();
        List<CalculadoraReparticion.Integrante> integrantes = new ArrayList<>();
        for (int i = 0; i < CUOTAS.length; i++) {
            Dinero cuota = Dinero.de(CUOTAS[i]);
            integrantes.add(new CalculadoraReparticion.Integrante((long) i + 1, "Integrante " + (i + 1),
                    CalculadoraAhorro.ahorroEsperado(cuota, 12), Dinero.CERO,
                    CalculadoraAhorro.rendimientoProyectadoCajita(cuota, 12, tasaNu), Dinero.CERO));
        }
        Dinero utilidad = Dinero.de(700_000);
        return CalculadoraReparticion.porDefecto().calcular(new CalculadoraReparticion.Entrada(integrantes,
                CalculadoraPrestamo.totalIntereses(PRESTAMO), Dinero.de(30_000), Dinero.CERO, utilidad, Dinero.CERO));
    }
}

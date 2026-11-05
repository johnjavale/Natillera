package co.natillera.infraestructura;

import co.natillera.auth.application.UsuarioActual;
import co.natillera.auth.domain.Rol;
import co.natillera.prestamos.application.PrestamoAccessPolicy;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.reportes.domain.CalculadoraDeudores;
import co.natillera.reportes.domain.Reporte;
import co.natillera.reportes.infrastructure.export.ExportadorCsv;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.infrastructure.config.DatabaseUrlPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InfraestructuraTest {

    @Test
    void databaseUrlDeRailwaySeTraduceAJdbc() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "mysql://natillera:cl%40ve@mysql.railway.internal:3306/railway");
        new DatabaseUrlPostProcessor().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://mysql.railway.internal:3306/railway");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("natillera");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("cl@ve");
    }

    @Test
    void databaseUrlJdbcSeRespeta() {
        MockEnvironment env = new MockEnvironment().withProperty("DATABASE_URL", "jdbc:mysql://localhost/x");
        new DatabaseUrlPostProcessor().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    void csvFormateaDineroPorcentajeYNeutralizaFormulas() {
        Reporte r = new Reporte("T", "t", List.of(new Reporte.Tabla("Tabla", List.of(Reporte.texto("Nombre"),
                Reporte.dinero("Valor"), Reporte.porcentaje("%"), Reporte.fecha("Fecha")),
                List.of(List.of("=HYPERLINK(\"x\")", new BigDecimal("1250000.50"), new BigDecimal("0.08"),
                        LocalDate.of(2027, 1, 5)), List.of("Ana; Pérez", BigDecimal.ZERO, BigDecimal.ONE,
                        LocalDate.of(2027, 2, 1))))));
        String csv = new String(new ExportadorCsv().exportar(r), StandardCharsets.UTF_8);
        String[] lineas = csv.split("\r\n");
        assertThat(lineas[0]).isEqualTo("﻿Nombre;Valor;%;Fecha");
        assertThat(lineas[1]).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\";1250001;8,00%;2027-01-05");
        assertThat(lineas[2]).isEqualTo("\"Ana; Pérez\";0;100,00%;2027-02-01");
    }

    @Test
    void politicaDePrestamosElTesoreroVeTodos() {
        Prestamo p = new Prestamo(1L, 3L, 7L, LocalDate.of(2027, 2, 15), Dinero.de(1), 2, 6, BigDecimal.ZERO);
        PrestamoAccessPolicy policy = new PrestamoAccessPolicy();
        assertThat(policy.puedeVer(new UsuarioActual(1L, "t", "T", Set.of(Rol.TESORERO), 9L, 2L, false), p)).isTrue();
        assertThat(policy.puedeVer(new UsuarioActual(2L, "i", "I", Set.of(Rol.INTEGRANTE), 3L, null, false), p)).isTrue();
        assertThat(policy.puedeVer(new UsuarioActual(3L, "o", "O", Set.of(Rol.INTEGRANTE), 4L, null, false), p)).isFalse();
    }

    @Test
    void deudoresSeOrdenanPorTotalYFiltranPorTipoTesoreroYMora() {
        var a = new CalculadoraDeudores.Deudor(1L, "A", 1L, "T1", Dinero.de(100), Dinero.CERO, Dinero.CERO, Dinero.CERO);
        var b = new CalculadoraDeudores.Deudor(2L, "B", 2L, "T2", Dinero.CERO, Dinero.de(500), Dinero.CERO, Dinero.de(10));
        var c = new CalculadoraDeudores.Deudor(3L, "C", 2L, "T2", Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO);
        assertThat(CalculadoraDeudores.filtrarYOrdenar(List.of(a, b, c), null, null, false))
                .extracting(CalculadoraDeudores.Deudor::nombre).containsExactly("B", "A");
        assertThat(CalculadoraDeudores.filtrarYOrdenar(List.of(a, b, c), null, CalculadoraDeudores.TipoDeuda.BOLETAS, false))
                .extracting(CalculadoraDeudores.Deudor::nombre).containsExactly("B");
        assertThat(CalculadoraDeudores.filtrarYOrdenar(List.of(a, b, c), 2L, null, false)).hasSize(1);
        assertThat(CalculadoraDeudores.filtrarYOrdenar(List.of(a, b, c), null, null, true))
                .extracting(CalculadoraDeudores.Deudor::nombre).containsExactly("A");
    }
}

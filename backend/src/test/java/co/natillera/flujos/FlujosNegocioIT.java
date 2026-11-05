package co.natillera.flujos;

import co.natillera.soporte.IntegracionBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujos completos API + MySQL (seed dev, fecha simulada 2027-03-20 → mes 3). */
@DisplayName("Flujos de negocio de punta a punta")
@Transactional
class FlujosNegocioIT extends IntegracionBase {

    @Test
    @DisplayName("Préstamo del seed: valores del golden test calculados desde la base de datos")
    void prestamoSeedCoincideConElExcel() throws Exception {
        getComo(ADMIN, "/api/v1/prestamos/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interesPorCuota").value(30000.00))
                .andExpect(jsonPath("$.abonoCapitalPorCuota").value(166666.67))
                .andExpect(jsonPath("$.valorCuota").value(196666.67))
                .andExpect(jsonPath("$.totalIntereses").value(180000.00))
                .andExpect(jsonPath("$.totalAPagar").value(1180000.00))
                .andExpect(jsonPath("$.cuotasPagadas").value(1))
                .andExpect(jsonPath("$.interesesRecibidos").value(30000.00))
                .andExpect(jsonPath("$.saldoPendiente").value(983333.33))
                .andExpect(jsonPath("$.estado").value("VIGENTE"));
    }

    @Test
    @DisplayName("Crear préstamo → desembolso en el libro → pagar todas las cuotas → PAGADO")
    void cicloDeVidaDelPrestamo() throws Exception {
        BigDecimal saldoAntes = saldoCajita(3);
        JsonNode p = leer(postComo(TESORERO1, "/api/v1/prestamos", Map.of("integranteId", 4, "cajitaOrigenId", 3,
                "fechaDesembolso", "2027-03-20", "monto", 300000, "numeroCuotas", 3)).andExpect(status().isCreated()));
        long id = p.get("id").asLong();
        assertThat(p.get("mesDelPrestamo").asInt()).isEqualTo(3);
        assertThat(p.get("plan")).hasSize(3);
        assertThat(saldoCajita(3)).isEqualByComparingTo(saldoAntes.subtract(new BigDecimal("300000")));

        JsonNode movs = leer(getComo(TESORERO1, "/api/v1/tesoreria/movimientos?cajitaId=3&tipo=DESEMBOLSO_PRESTAMO"));
        assertThat(movs).anyMatch(m -> m.get("prestamoId").asLong() == id && m.get("salida").decimalValue()
                .compareTo(new BigDecimal("300000")) == 0);

        for (int i = 1; i <= 3; i++) {
            postComo(TESORERO1, "/api/v1/prestamos/" + id + "/pagos", Map.of("fecha", "2027-04-10"))
                    .andExpect(status().isCreated());
        }
        // el integrante 3 no ve el préstamo del integrante 4
        getComo(INTEGRANTE3, "/api/v1/prestamos/" + id).andExpect(status().isNotFound());
        getComo(ADMIN, "/api/v1/prestamos/" + id)
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.saldoPendiente").value(0.0))
                .andExpect(jsonPath("$.interesesRecibidos").value(27000.00));
        // la cajita recupera capital + intereses (3 × 9.000)
        assertThat(saldoCajita(3)).isEqualByComparingTo(saldoAntes.add(new BigDecimal("27000")));
        postComo(TESORERO1, "/api/v1/prestamos/" + id + "/pagos", Map.of("fecha", "2027-04-10"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El préstamo ya está pagado"));
    }

    @Test
    @DisplayName("Anular un pago de cuota genera el contramovimiento y devuelve el préstamo a VIGENTE")
    void anularPagoDeCuota() throws Exception {
        BigDecimal antes = saldoCajita(7);
        postComo(TESORERO2, "/api/v1/prestamos/1/pagos/1/anular", Map.of("motivo", "Pago duplicado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuotasPagadas").value(0))
                .andExpect(jsonPath("$.estado").value("EN_MORA"));
        assertThat(saldoCajita(7)).isEqualByComparingTo(antes.subtract(new BigDecimal("196667")));
        JsonNode auditoria = leer(getComo(ADMIN, "/api/v1/auditoria"));
        assertThat(auditoria.get("registros")).anyMatch(a -> a.get("entidad").asText().equals("pago_cuota"));
    }

    @Test
    @DisplayName("Validaciones de préstamo con 422 y mensaje en español")
    void validacionesDePrestamo() throws Exception {
        prestamo(Map.of("numeroCuotas", 13)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Los préstamos no pueden ser a más de 12 cuotas (un año)"));
        prestamo(Map.of("numeroCuotas", 10)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El préstamo termina después del cierre de la natillera"));
        prestamo(Map.of("monto", 50_000_000)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(
                        "El monto supera el saldo disponible de la cajita origen")));
        prestamo(Map.of("monto", 0)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El monto del préstamo debe ser mayor que cero"));
        prestamo(Map.of("monto", -5)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Aportes en lote: el atraso se recalcula desde el libro")
    void aportesEnLoteActualizanElAtraso() throws Exception {
        getComo(TESORERO1, "/api/v1/integrantes/2/aportes")
                .andExpect(jsonPath("$.atraso").value(100000.00))
                .andExpect(jsonPath("$.enAtraso").value(true));
        postComo(TESORERO1, "/api/v1/tesoreria/aportes-lote", Map.of("fecha", "2027-03-18", "descripcion",
                "Cuotas atrasadas", "aportes", List.of(Map.of("integranteId", 2, "monto", 100000),
                        Map.of("integranteId", 1, "monto", 0)))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
        getComo(TESORERO1, "/api/v1/integrantes/2/aportes")
                .andExpect(jsonPath("$.atraso").value(0.0))
                .andExpect(jsonPath("$.enAtraso").value(false))
                .andExpect(jsonPath("$.aportesPorMes[2]").value(100000.00));
    }

    @Test
    @DisplayName("Aporte en lote con un integrante ajeno se rechaza completo (todo o nada)")
    void aporteEnLoteConIntegranteAjenoNoGuardaNada() throws Exception {
        BigDecimal antes = saldoCajita(1);
        postComo(TESORERO1, "/api/v1/tesoreria/aportes-lote", Map.of("fecha", "2027-03-18", "aportes", List.of(
                Map.of("integranteId", 1, "monto", 1000), Map.of("integranteId", 7, "monto", 1000))))
                .andExpect(status().isNotFound());
        assertThat(saldoCajita(1)).isEqualByComparingTo(antes);
    }

    @Test
    @DisplayName("Rifas: pago de boletas → utilidad real; máximo 2 actividades")
    void rifas() throws Exception {
        getComo(ADMIN, "/api/v1/actividades/1")
                .andExpect(jsonPath("$.recaudoEsperado").value(2000000.00))
                .andExpect(jsonPath("$.utilidadEsperada").value(150000.00))
                .andExpect(jsonPath("$.recaudoReal").value(800000.00))
                .andExpect(jsonPath("$.utilidadReal").value(-1050000.00));
        postComo(TESORERO2, "/api/v1/actividades/1/pagos", Map.of("integranteId", 8, "fecha", "2027-03-19",
                "monto", 200000)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.recaudoReal").value(1000000.00));
        postComo(ADMIN, "/api/v1/actividades", Map.of("nombre", "Bingo", "fechaSorteo", "2027-09-01",
                "valorBoleta", 1000, "boletasPorIntegrante", 5, "costoPremio", 0, "otrosGastos", 0))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El ciclo admite máximo 2 actividades"));
        JsonNode propias = leer(getComo(INTEGRANTE3, "/api/v1/actividades"));
        assertThat(propias.get(0).get("boletas")).hasSize(1);
        assertThat(propias.get(0).get("recaudoReal").decimalValue()).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("Repartición proyectada (golden) y real desde la base de datos")
    void reparticion() throws Exception {
        JsonNode r = leer(getComo(ADMIN, "/api/v1/reparticion").andExpect(status().isOk()));
        assertThat(r.get("proyectado").get("interesesNetos").decimalValue()).isEqualByComparingTo("180000");
        assertThat(r.get("proyectado").get("utilidadActividadesPorIntegrante").decimalValue()).isEqualByComparingTo("70000");
        assertThat(r.get("real").get("interesesPrestamos").decimalValue()).isEqualByComparingTo("30000");
        JsonNode i1 = r.get("filas").get(0);
        assertThat(i1.get("interesesProyectados").decimalValue()).isEqualByComparingTo("14400");
        assertThat(i1.get("rendimientoNuProyectado").decimalValue()).isEqualByComparingTo("47568.33");
        assertThat(i1.get("rendimientoNuReal").decimalValue()).isEqualByComparingTo("720");
        JsonNode i7 = r.get("filas").get(6);
        assertThat(i7.get("interesesProyectados").decimalValue()).isEqualByComparingTo("43200");
        assertThat(i7.get("puesto").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Conciliación con Nu: diferencia por cajita y por tesorero")
    void conciliacion() throws Exception {
        JsonNode cajitas = leer(getComo(TESORERO1, "/api/v1/tesoreria/cajitas"));
        assertThat(cajitas.get(0).get("cuadra").asBoolean()).isTrue();
        assertThat(cajitas.get(3).get("diferencia").decimalValue()).isEqualByComparingTo("-720");
        postComo(TESORERO1, "/api/v1/tesoreria/cajitas/4/saldo-reportado", Map.of("fecha", "2027-03-20",
                "saldoApp", 300720)).andExpect(status().isNoContent());
        JsonNode despues = leer(getComo(TESORERO1, "/api/v1/tesoreria/cajitas"));
        assertThat(despues.get(3).get("cuadra").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Los movimientos no se borran: se anulan con contramovimiento y motivo")
    void anulacionDeMovimiento() throws Exception {
        JsonNode m = leer(postComo(TESORERO1, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 1,
                "fecha", "2027-03-20", "tipo", "OTRO_INGRESO", "descripcion", "Donación", "valor", 5000))
                .andExpect(status().isCreated()));
        long id = m.get("id").asLong();
        postComo(TESORERO1, "/api/v1/tesoreria/movimientos/" + id + "/anular", Map.of("motivo", "Error"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salida").value(5000.00));
        postComo(TESORERO1, "/api/v1/tesoreria/movimientos/" + id + "/anular", Map.of("motivo", "Otra vez"))
                .andExpect(status().isUnprocessableEntity());
        postComo(TESORERO1, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 1, "fecha", "2027-03-20",
                "tipo", "DESEMBOLSO_PRESTAMO", "valor", 5000)).andExpect(status().isUnprocessableEntity());
        postComo(TESORERO1, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 1, "fecha", "2027-03-20",
                "tipo", "GASTO", "valor", 99_000_000)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("La salida supera el saldo de la cajita"));
    }

    @Test
    @DisplayName("Cuota propia: editable en INSCRIPCION, bloqueada en ACTIVO; queda en auditoría")
    void cuotaPropia() throws Exception {
        putComo(INTEGRANTE3, "/api/v1/integrantes/me/cuota", Map.of("cuotaMensual", 250000))
                .andExpect(status().isUnprocessableEntity());
        putComo(ADMIN, "/api/v1/ciclo/estado", Map.of("estado", "INSCRIPCION")).andExpect(status().isOk());
        putComo(INTEGRANTE3, "/api/v1/integrantes/me/cuota", Map.of("cuotaMensual", 250000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuotaMensual").value(250000.00))
                .andExpect(jsonPath("$.ahorroEsperado").value(3000000.00));
        JsonNode auditoria = leer(getComo(ADMIN, "/api/v1/auditoria"));
        assertThat(auditoria.get("registros")).anyMatch(a -> a.get("accion").asText().equals("CAMBIAR_CUOTA"));
    }

    @Test
    @DisplayName("Resumen mensual: caja acumulada del golden test y KPIs")
    void resumen() throws Exception {
        JsonNode r = leer(getComo(ADMIN, "/api/v1/resumen").andExpect(status().isOk()));
        assertThat(r.get("mesesTranscurridos").asInt()).isEqualTo(3);
        assertThat(r.get("meses").get(0).get("cajaDisponibleAcumulada").decimalValue()).isEqualByComparingTo("1250000");
        assertThat(r.get("meses").get(1).get("cajaDisponibleAcumulada").decimalValue()).isEqualByComparingTo("1500000");
        assertThat(r.get("kpis").get("ahorroEsperadoAnual").decimalValue()).isEqualByComparingTo("15000000");
        assertThat(r.get("kpis").get("interesesProyectados").decimalValue()).isEqualByComparingTo("180000");
        assertThat(r.get("kpis").get("rendimientoProyectadoNu").decimalValue()).isEqualByComparingTo("594604.17");
        assertThat(r.get("alertaCajaNegativa").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Préstamo grande en el mes 1 dispara la alerta de caja negativa del resumen")
    void alertaDeCajaNegativa() throws Exception {
        // cajita de Haddy (7): saldo suficiente hoy, pero el préstamo queda en el mes 1 del flujo proyectado
        postComo(ADMIN, "/api/v1/tesoreria/movimientos", Map.of("cajitaId", 7, "fecha", "2027-03-20",
                "tipo", "OTRO_INGRESO", "valor", 5_000_000)).andExpect(status().isCreated());
        postComo(ADMIN, "/api/v1/prestamos", Map.of("integranteId", 5, "cajitaOrigenId", 7,
                "fechaDesembolso", "2027-01-20", "monto", 3_000_000, "numeroCuotas", 6)).andExpect(status().isCreated());
        JsonNode r = leer(getComo(ADMIN, "/api/v1/resumen"));
        assertThat(r.get("alertaCajaNegativa").asBoolean()).isTrue();
        assertThat(r.get("meses").get(0).get("alertaCajaNegativa").asBoolean()).isTrue();
    }

    private org.springframework.test.web.servlet.ResultActions prestamo(Map<String, Object> cambios) throws Exception {
        Map<String, Object> base = new java.util.HashMap<>(Map.of("integranteId", 4, "cajitaOrigenId", 3,
                "fechaDesembolso", "2027-03-20", "monto", 100000, "numeroCuotas", 3));
        base.putAll(cambios);
        return postComo(TESORERO1, "/api/v1/prestamos", base);
    }

    private BigDecimal saldoCajita(long integranteId) throws Exception {
        JsonNode cajitas = leer(getComo(ADMIN, "/api/v1/tesoreria/cajitas"));
        for (JsonNode c : cajitas) {
            if (c.get("integranteId").asLong() == integranteId) {
                return c.get("saldo").decimalValue();
            }
        }
        throw new IllegalStateException("Sin cajita " + integranteId);
    }
}

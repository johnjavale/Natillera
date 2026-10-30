package co.natillera.reportes;

import co.natillera.soporte.IntegracionBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Reportes y exportación")
class ReportesIT extends IntegracionBase {

    @Test
    void consolidadoPorIntegranteConAlcanceDelTesorero() throws Exception {
        JsonNode admin = leer(getComo(ADMIN, "/api/v1/reportes/consolidado").andExpect(status().isOk()));
        assertThat(admin).hasSize(10);
        JsonNode samay = admin.get(2);
        assertThat(samay.get("nombre").asText()).isEqualTo("Samay Moreno");
        assertThat(samay.get("prestamosVigentes").asInt()).isEqualTo(1);
        assertThat(samay.get("saldoPrestamos").decimalValue()).isEqualByComparingTo("983333.33");
        assertThat(samay.get("totalAportado").decimalValue()).isEqualByComparingTo("600000");
        assertThat(samay.get("boletasPendientes").decimalValue()).isEqualByComparingTo("300000");

        JsonNode t2 = leer(getComo(TESORERO2, "/api/v1/reportes/consolidado"));
        assertThat(t2).extracting(n -> n.get("integranteId").asLong()).containsExactly(6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void deudoresConLosTresTiposDeDeudaYFiltros() throws Exception {
        JsonNode todos = leer(getComo(ADMIN, "/api/v1/reportes/deudores"));
        List<Long> ids = new ArrayList<>();
        todos.forEach(d -> ids.add(d.get("integranteId").asLong()));
        assertThat(ids.get(0)).isEqualTo(3L); // Samay: préstamo + boletas → mayor deuda
        for (int i = 1; i < todos.size(); i++) {
            assertThat(todos.get(i - 1).get("total").decimalValue())
                    .isGreaterThanOrEqualTo(todos.get(i).get("total").decimalValue());
        }
        JsonNode aportes = leer(getComo(ADMIN, "/api/v1/reportes/deudores?tipo=APORTES"));
        assertThat(aportes).extracting(n -> n.get("integranteId").asLong()).containsExactlyInAnyOrder(2L, 9L);
        assertThat(aportes.get(0).get("atrasoAportes").decimalValue()).isPositive();

        JsonNode prestamos = leer(getComo(ADMIN, "/api/v1/reportes/deudores?tipo=PRESTAMOS"));
        assertThat(prestamos).extracting(n -> n.get("integranteId").asLong()).containsExactly(3L);

        JsonNode boletas = leer(getComo(ADMIN, "/api/v1/reportes/deudores?tipo=BOLETAS"));
        assertThat(boletas).hasSize(10); // la rifa 2 aún no tiene pagos

        JsonNode mora = leer(getComo(ADMIN, "/api/v1/reportes/deudores?soloMora=true"));
        assertThat(mora).extracting(n -> n.get("integranteId").asLong()).containsExactlyInAnyOrder(2L, 9L);

        JsonNode t1 = leer(getComo(ADMIN, "/api/v1/reportes/deudores?tesoreroId=2&tipo=APORTES"));
        assertThat(t1).extracting(n -> n.get("integranteId").asLong()).containsExactly(9L);
    }

    @Test
    void estadoDeCuentaIndividual() throws Exception {
        JsonNode e = leer(getComo(INTEGRANTE3, "/api/v1/reportes/estado-cuenta/3").andExpect(status().isOk()));
        assertThat(e.get("resumen").get("nombre").asText()).isEqualTo("Samay Moreno");
        assertThat(e.get("aportesPorMes")).hasSize(12);
        assertThat(e.get("movimientos")).hasSizeGreaterThanOrEqualTo(4);
        assertThat(e.get("prestamos")).hasSize(1);
        assertThat(e.get("prestamos").get(0).get("plan")).hasSize(6);
        assertThat(e.get("boletas")).hasSize(2);
        assertThat(e.get("reparto").get("totalProyectado").decimalValue()).isPositive();
    }

    @Test
    void exportacionExcelConUnaHojaPorTablaYFormatoCop() throws Exception {
        MvcResult r = getComo(ADMIN, "/api/v1/reportes/estado-cuenta/3?formato=xlsx")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("estado-cuenta-3.xlsx")))
                .andReturn();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))) {
            List<String> hojas = new ArrayList<>();
            libro.forEach(h -> hojas.add(h.getSheetName()));
            assertThat(hojas).contains("Resumen", "Aportes", "Movimientos", "Préstamo 1", "Boletas", "Repartición");
            Sheet resumen = libro.getSheet("Resumen");
            assertThat(resumen.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Integrante");
            assertThat(resumen.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Samay Moreno");
            assertThat(resumen.getRow(3).getCell(2).getNumericCellValue()).isEqualTo(200000.0);
            assertThat(resumen.getRow(3).getCell(2).getCellStyle().getDataFormatString()).contains("$");
        }
    }

    @Test
    void exportacionCsvSeparadaPorPuntoYComaConBom() throws Exception {
        MvcResult r = getComo(ADMIN, "/api/v1/reportes/consolidado?formato=csv")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andReturn();
        String csv = new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Integrante;Tesorero;Cuota mensual");
        String[] lineas = csv.split("\r\n");
        assertThat(lineas).hasSize(11);
        assertThat(lineas[1]).startsWith("John Jairo Valencia;Pascuala Leyes;100000;1200000;300000");
    }

    @Test
    void exportacionDelResumenYTesoreria() throws Exception {
        getComo(ADMIN, "/api/v1/reportes/resumen?formato=xlsx").andExpect(status().isOk());
        getComo(TESORERO1, "/api/v1/reportes/tesoreria?formato=csv&tipo=AHORRO").andExpect(status().isOk());
        getComo(ADMIN, "/api/v1/reportes/consolidado?formato=pdf").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reporteDeTesoreriaPorRangoDeFechasYTipo() throws Exception {
        JsonNode t = leer(getComo(TESORERO1,
                "/api/v1/reportes/tesoreria?desde=2027-02-01&hasta=2027-02-28&tipo=AHORRO"));
        assertThat(t.get("cajitas")).hasSize(5);
        assertThat(t.get("porTesorero")).hasSize(1);
        assertThat(t.get("movimientos")).hasSize(4); // febrero: integrantes 1, 3, 4, 5 (Pascuala no aportó)
        t.get("movimientos").forEach(m -> assertThat(m.get("tipo").asText()).isEqualTo("AHORRO"));
    }
}

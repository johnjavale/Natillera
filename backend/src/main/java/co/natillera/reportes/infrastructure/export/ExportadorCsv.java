package co.natillera.reportes.infrastructure.export;

import co.natillera.reportes.domain.Reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV separado por punto y coma (lo que Excel en español abre directamente), UTF-8 con BOM.
 * Dinero sin decimales; porcentajes con dos decimales.
 */
public class ExportadorCsv extends ExportadorReporte {

    private static final String SEP = ";";
    private final StringBuilder sb = new StringBuilder();
    private boolean primeraTabla = true;

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public String contentType() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    protected void iniciar(Reporte reporte) {
        sb.append('﻿');
    }

    @Override
    protected void iniciarTabla(Reporte reporte, Reporte.Tabla tabla) {
        if (!primeraTabla) {
            sb.append("\r\n");
        }
        primeraTabla = false;
        if (reporte.tablas().size() > 1) {
            sb.append(escapar(tabla.nombre())).append("\r\n");
        }
    }

    @Override
    protected void escribirEncabezados(List<Reporte.Columna> columnas) {
        sb.append(columnas.stream().map(c -> escapar(c.nombre())).collect(Collectors.joining(SEP))).append("\r\n");
    }

    @Override
    protected void escribirFila(List<Reporte.Columna> columnas, List<Object> fila) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                linea.append(SEP);
            }
            linea.append(formatear(columnas.get(i).tipo(), i < fila.size() ? fila.get(i) : null));
        }
        sb.append(linea).append("\r\n");
    }

    @Override
    protected void finalizar(ByteArrayOutputStream salida) throws IOException {
        salida.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String formatear(Reporte.TipoColumna tipo, Object valor) {
        if (valor == null) {
            return "";
        }
        return switch (tipo) {
            case DINERO -> ((BigDecimal) valor).setScale(0, RoundingMode.HALF_UP).toPlainString();
            case PORCENTAJE -> ((BigDecimal) valor).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    .toPlainString().replace('.', ',') + "%";
            case NUMERO -> valor instanceof BigDecimal b ? b.toPlainString().replace('.', ',') : valor.toString();
            case FECHA -> ((LocalDate) valor).toString();
            case TEXTO -> escapar(valor.toString());
        };
    }

    /** Escapa comillas y neutraliza fórmulas (CSV injection) en celdas de texto. */
    static String escapar(String texto) {
        String t = texto;
        if (!t.isEmpty() && "=+-@\t\r".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        if (t.contains(SEP) || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }
}

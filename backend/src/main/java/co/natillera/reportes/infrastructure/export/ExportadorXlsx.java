package co.natillera.reportes.infrastructure.export;

import co.natillera.reportes.domain.Reporte;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exportación a Excel (.xlsx) con Apache POI: una hoja por tabla, formato COP y porcentaje. */
public class ExportadorXlsx extends ExportadorReporte {

    private XSSFWorkbook libro;
    private Sheet hoja;
    private int filaActual;
    private CellStyle estiloTitulo;
    private CellStyle estiloEncabezado;
    private CellStyle estiloDinero;
    private CellStyle estiloPorcentaje;
    private CellStyle estiloFecha;
    private final Set<String> nombresHojas = new HashSet<>();

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    protected void iniciar(Reporte reporte) {
        libro = new XSSFWorkbook();
        Font negrita = libro.createFont();
        negrita.setBold(true);
        Font tituloFont = libro.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 14);

        estiloTitulo = libro.createCellStyle();
        estiloTitulo.setFont(tituloFont);
        estiloEncabezado = libro.createCellStyle();
        estiloEncabezado.setFont(negrita);
        estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloEncabezado.setWrapText(true);
        estiloDinero = libro.createCellStyle();
        estiloDinero.setDataFormat(libro.createDataFormat().getFormat("\"$\"#,##0"));
        estiloPorcentaje = libro.createCellStyle();
        estiloPorcentaje.setDataFormat(libro.createDataFormat().getFormat("0.00%"));
        estiloFecha = libro.createCellStyle();
        estiloFecha.setDataFormat(libro.createDataFormat().getFormat("yyyy-mm-dd"));
    }

    @Override
    protected void iniciarTabla(Reporte reporte, Reporte.Tabla tabla) {
        String nombre = WorkbookUtil.createSafeSheetName(tabla.nombre());
        String unico = nombre;
        for (int i = 2; nombresHojas.contains(unico); i++) {
            unico = WorkbookUtil.createSafeSheetName(nombre.substring(0, Math.min(nombre.length(), 28)) + " " + i);
        }
        nombresHojas.add(unico);
        hoja = libro.createSheet(unico);
        Cell titulo = hoja.createRow(0).createCell(0);
        titulo.setCellValue(reporte.titulo() + " — " + tabla.nombre());
        titulo.setCellStyle(estiloTitulo);
        filaActual = 2;
    }

    @Override
    protected void escribirEncabezados(List<Reporte.Columna> columnas) {
        Row fila = hoja.createRow(filaActual++);
        for (int i = 0; i < columnas.size(); i++) {
            Cell c = fila.createCell(i);
            c.setCellValue(columnas.get(i).nombre());
            c.setCellStyle(estiloEncabezado);
        }
        hoja.createFreezePane(0, filaActual);
    }

    @Override
    protected void escribirFila(List<Reporte.Columna> columnas, List<Object> valores) {
        Row fila = hoja.createRow(filaActual++);
        for (int i = 0; i < columnas.size(); i++) {
            Object v = i < valores.size() ? valores.get(i) : null;
            if (v == null) {
                continue;
            }
            Cell c = fila.createCell(i);
            switch (columnas.get(i).tipo()) {
                case DINERO -> {
                    c.setCellValue(((BigDecimal) v).doubleValue());
                    c.setCellStyle(estiloDinero);
                }
                case PORCENTAJE -> {
                    c.setCellValue(((BigDecimal) v).doubleValue());
                    c.setCellStyle(estiloPorcentaje);
                }
                case NUMERO -> c.setCellValue(v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString()));
                case FECHA -> {
                    c.setCellValue((LocalDate) v);
                    c.setCellStyle(estiloFecha);
                }
                case TEXTO -> c.setCellValue(v.toString());
            }
        }
    }

    @Override
    protected void finalizarTabla(Reporte.Tabla tabla) {
        for (int i = 0; i < tabla.columnas().size(); i++) {
            hoja.setColumnWidth(i, Math.min(60, Math.max(12, tabla.columnas().get(i).nombre().length() + 4)) * 256);
        }
    }

    @Override
    protected void finalizar(ByteArrayOutputStream salida) throws IOException {
        try (XSSFWorkbook l = libro) {
            l.write(salida);
        }
    }
}

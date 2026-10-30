package co.natillera.reportes.infrastructure.export;

import co.natillera.reportes.domain.Reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Patrón Template Method: el algoritmo de exportación es fijo (iniciar → por cada tabla: título, encabezados,
 * filas → finalizar); cada formato (Excel, CSV) implementa los pasos.
 */
public abstract class ExportadorReporte {

    public final byte[] exportar(Reporte reporte) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            iniciar(reporte);
            for (Reporte.Tabla tabla : reporte.tablas()) {
                iniciarTabla(reporte, tabla);
                escribirEncabezados(tabla.columnas());
                for (List<Object> fila : tabla.filas()) {
                    escribirFila(tabla.columnas(), fila);
                }
                finalizarTabla(tabla);
            }
            finalizar(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo", e);
        }
    }

    public abstract String extension();

    public abstract String contentType();

    protected abstract void iniciar(Reporte reporte);

    protected abstract void iniciarTabla(Reporte reporte, Reporte.Tabla tabla);

    protected abstract void escribirEncabezados(List<Reporte.Columna> columnas);

    protected abstract void escribirFila(List<Reporte.Columna> columnas, List<Object> fila);

    protected void finalizarTabla(Reporte.Tabla tabla) {
    }

    protected abstract void finalizar(ByteArrayOutputStream salida) throws IOException;
}

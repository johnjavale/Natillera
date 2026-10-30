package co.natillera.reportes.domain;

import java.util.List;

/** Reporte exportable: un título y una o varias tablas (una hoja de Excel por tabla). */
public record Reporte(String titulo, String archivo, List<Tabla> tablas) {

    public enum TipoColumna { TEXTO, DINERO, NUMERO, PORCENTAJE, FECHA }

    public record Columna(String nombre, TipoColumna tipo) {
    }

    public record Tabla(String nombre, List<Columna> columnas, List<List<Object>> filas) {
    }

    public static Columna texto(String nombre) {
        return new Columna(nombre, TipoColumna.TEXTO);
    }

    public static Columna dinero(String nombre) {
        return new Columna(nombre, TipoColumna.DINERO);
    }

    public static Columna numero(String nombre) {
        return new Columna(nombre, TipoColumna.NUMERO);
    }

    public static Columna porcentaje(String nombre) {
        return new Columna(nombre, TipoColumna.PORCENTAJE);
    }

    public static Columna fecha(String nombre) {
        return new Columna(nombre, TipoColumna.FECHA);
    }
}

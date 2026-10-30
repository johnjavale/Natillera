package co.natillera.integrantes.domain;

import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;

/** Integrante (ahorrador) de la natillera. */
public record Integrante(
        Long id,
        Long usuarioId,
        String nombre,
        String telefono,
        Dinero cuotaMensual,
        Long tesoreroId,
        String nombreCajitaNu,
        boolean activo) {

    public Integrante {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del integrante es obligatorio");
        }
        if (cuotaMensual == null || cuotaMensual.esNegativo()) {
            throw new ReglaNegocioException("La cuota mensual no puede ser negativa");
        }
        if (nombreCajitaNu == null || nombreCajitaNu.isBlank()) {
            nombreCajitaNu = nombreCajitaPorDefecto(nombre);
        }
    }

    /** Equivale a la fórmula del Excel: "Natillera - " seguido del nombre. */
    public static String nombreCajitaPorDefecto(String nombre) {
        return "Natillera - " + nombre;
    }

    public Integrante conCuota(Dinero nuevaCuota) {
        return new Integrante(id, usuarioId, nombre, telefono, nuevaCuota, tesoreroId, nombreCajitaNu, activo);
    }
}

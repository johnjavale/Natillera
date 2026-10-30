package co.natillera.tesoreria.domain;

/** Tesorero: en su cuenta Nu guarda las cajitas de los integrantes asignados. */
public record Tesorero(Long id, Long usuarioId, String nombre) {
}

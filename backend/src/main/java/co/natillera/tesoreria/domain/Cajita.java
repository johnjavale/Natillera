package co.natillera.tesoreria.domain;

/** Cajita Nu de un integrante. Su tesorero es el asignado al integrante (derivado, no se digita). */
public record Cajita(Long id, Long integranteId, String nombre, Long tesoreroId) {
}

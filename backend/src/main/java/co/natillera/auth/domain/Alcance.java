package co.natillera.auth.domain;

/** Alcance de datos de un usuario autenticado. */
public enum Alcance {
    /** Solo sus propios datos (INTEGRANTE). */
    OWN,
    /** Sus datos y los de los integrantes asignados a él (TESORERO). */
    ASSIGNED,
    /** Todo (ADMIN). */
    ALL
}

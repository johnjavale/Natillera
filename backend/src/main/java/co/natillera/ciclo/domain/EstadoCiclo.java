package co.natillera.ciclo.domain;

public enum EstadoCiclo {
    /** Los integrantes pueden proponer o editar su cuota. */
    INSCRIPCION,
    /** El ciclo está en curso: solo el ADMIN cambia cuotas. */
    ACTIVO,
    /** Ciclo cerrado: no se crean préstamos ni actividades. */
    CERRADO
}

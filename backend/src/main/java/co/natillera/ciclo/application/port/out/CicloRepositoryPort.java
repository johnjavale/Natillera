package co.natillera.ciclo.application.port.out;

import co.natillera.ciclo.domain.Ciclo;

public interface CicloRepositoryPort {

    /** El ciclo vigente (el más reciente). */
    Ciclo actual();

    Ciclo guardar(Ciclo ciclo);
}

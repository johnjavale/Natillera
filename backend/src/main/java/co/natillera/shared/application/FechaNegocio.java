package co.natillera.shared.application;

import java.time.LocalDate;

/**
 * "Hoy" para los cálculos de negocio (America/Bogota). En el perfil dev puede fijarse una fecha simulada
 * para ver el ciclo 2027 en curso; los tokens siempre usan el reloj real.
 */
public interface FechaNegocio {

    LocalDate hoy();
}

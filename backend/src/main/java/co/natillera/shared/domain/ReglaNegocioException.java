package co.natillera.shared.domain;

/** Violación de una regla de negocio. Se traduce a HTTP 422. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}

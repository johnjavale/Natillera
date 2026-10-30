package co.natillera.shared.domain;

/**
 * El recurso no existe o está fuera del alcance del usuario.
 * Se traduce a HTTP 404 para no revelar la existencia de recursos ajenos (prevención de IDOR).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " no encontrado");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

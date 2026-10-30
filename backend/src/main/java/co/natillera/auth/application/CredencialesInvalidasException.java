package co.natillera.auth.application;

/** Credenciales o refresh token inválidos. Se traduce a HTTP 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}

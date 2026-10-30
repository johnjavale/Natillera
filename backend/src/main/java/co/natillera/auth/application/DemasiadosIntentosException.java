package co.natillera.auth.application;

/** Se superó el límite de intentos de login. Se traduce a HTTP 429. */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosEspera;

    public DemasiadosIntentosException(long segundosEspera) {
        super("Demasiados intentos de ingreso. Intente de nuevo en " + Math.max(1, (segundosEspera + 59) / 60)
                + " minuto(s).");
        this.segundosEspera = segundosEspera;
    }

    public long segundosEspera() {
        return segundosEspera;
    }
}

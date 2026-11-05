package co.natillera.auth.application.port.out;

/** Envío de correos de texto plano. El envío es asíncrono: un fallo se registra en el log, no se propaga. */
public interface EnviadorCorreoPort {

    void enviar(String para, String asunto, String texto);
}

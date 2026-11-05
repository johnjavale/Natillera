package co.natillera.auth.infrastructure.correo;

import co.natillera.auth.application.port.out.EnviadorCorreoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Respaldo cuando no hay SMTP configurado (MAIL_HOST vacío). En desarrollo escribe el correo completo en el log para
 * poder probar el enlace; en producción solo advierte, porque el enlace permite cambiar la contraseña.
 */
public class CorreoEnLogAdapter implements EnviadorCorreoPort {

    private static final Logger log = LoggerFactory.getLogger(CorreoEnLogAdapter.class);

    private final boolean mostrarContenido;

    public CorreoEnLogAdapter(boolean mostrarContenido) {
        this.mostrarContenido = mostrarContenido;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        if (mostrarContenido) {
            log.info("SMTP no configurado. Correo para {}: {}\n{}", para, asunto, texto);
        } else {
            log.warn("SMTP no configurado (MAIL_HOST): no se envió el correo '{}'", asunto);
        }
    }
}

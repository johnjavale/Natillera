package co.natillera.auth.infrastructure.correo;

import co.natillera.auth.application.port.out.EnviadorCorreoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.concurrent.Executor;

/** Envía por SMTP en segundo plano: la respuesta HTTP no espera al servidor de correo ni revela si se envió. */
public class CorreoSmtpAdapter implements EnviadorCorreoPort {

    private static final Logger log = LoggerFactory.getLogger(CorreoSmtpAdapter.class);

    private final JavaMailSender mail;
    private final String remitente;
    private final Executor executor;

    public CorreoSmtpAdapter(JavaMailSender mail, String remitente, Executor executor) {
        this.mail = mail;
        this.remitente = remitente;
        this.executor = executor;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        executor.execute(() -> {
            try {
                mail.send(mensaje);
            } catch (RuntimeException e) {
                log.error("No se pudo enviar el correo '{}': {}", asunto, e.getMessage());
            }
        });
    }
}

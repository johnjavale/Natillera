package co.natillera.auth.infrastructure.correo;

import co.natillera.auth.application.port.out.EnviadorCorreoPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
import java.util.concurrent.Executor;

/** SMTP configurable por variables de entorno (MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM). */
@Configuration
public class CorreoConfig {

    @Bean
    public EnviadorCorreoPort enviadorCorreo(@Value("${app.correo.host:}") String host,
                                             @Value("${app.correo.puerto:587}") int puerto,
                                             @Value("${app.correo.usuario:}") String usuario,
                                             @Value("${app.correo.clave:}") String clave,
                                             @Value("${app.correo.remitente:}") String remitente,
                                             @Value("${app.correo.mostrar-en-log:false}") boolean mostrarEnLog,
                                             @Qualifier("applicationTaskExecutor") Executor executor) {
        if (host.isBlank()) {
            return new CorreoEnLogAdapter(mostrarEnLog);
        }
        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost(host);
        mail.setPort(puerto);
        mail.setDefaultEncoding("UTF-8");
        Properties props = mail.getJavaMailProperties();
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        if (puerto == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (!usuario.isBlank()) {
            mail.setUsername(usuario);
            mail.setPassword(clave);
            props.put("mail.smtp.auth", "true");
        }
        String de = remitente.isBlank() ? usuario : remitente;
        return new CorreoSmtpAdapter(mail, de, executor);
    }
}

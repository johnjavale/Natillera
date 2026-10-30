package co.natillera.auth.infrastructure.security;

import co.natillera.auth.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String contrasena) {
        return encoder.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return contrasena != null && hash != null && encoder.matches(contrasena, hash);
    }
}

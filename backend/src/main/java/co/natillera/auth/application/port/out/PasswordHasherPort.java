package co.natillera.auth.application.port.out;

public interface PasswordHasherPort {

    String hash(String contrasena);

    boolean coincide(String contrasena, String hash);
}

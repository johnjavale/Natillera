package co.natillera.auth.domain;

import co.natillera.shared.domain.ReglaNegocioException;

import java.util.Locale;

/** Reglas mínimas de contraseña. */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 10;

    private PoliticaContrasena() {
    }

    public static void validar(String contrasena, String login) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (contrasena.length() > 72) {
            throw new ReglaNegocioException("La contraseña no puede tener más de 72 caracteres");
        }
        if (contrasena.chars().noneMatch(Character::isLetter) || contrasena.chars().noneMatch(Character::isDigit)) {
            throw new ReglaNegocioException("La contraseña debe combinar letras y números");
        }
        if (login != null) {
            String usuario = login.split("@")[0].toLowerCase(Locale.ROOT);
            if (usuario.length() >= 3 && contrasena.toLowerCase(Locale.ROOT).contains(usuario)) {
                throw new ReglaNegocioException("La contraseña no puede contener su usuario");
            }
        }
    }
}

package co.natillera.auth.application;

import co.natillera.shared.domain.ReglaNegocioException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Acceso al usuario autenticado desde los casos de uso. */
@Component
public class SesionActual {

    public UsuarioActual usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioActual u) {
            return u;
        }
        throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
    }

    /** Id del integrante vinculado al usuario; falla si el usuario no ahorra en la natillera. */
    public Long integranteIdObligatorio() {
        Long id = usuario().integranteId();
        if (id == null) {
            throw new ReglaNegocioException("Su usuario no está vinculado a un integrante de la natillera");
        }
        return id;
    }
}

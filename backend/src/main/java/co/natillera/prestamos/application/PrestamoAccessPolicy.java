package co.natillera.prestamos.application;

import co.natillera.auth.application.UsuarioActual;
import co.natillera.prestamos.domain.Prestamo;
import org.springframework.stereotype.Component;

/**
 * Política de lectura de préstamos, aislada en un solo lugar para poder cambiarla.
 * Supuesto vigente: el TESORERO ve TODOS los préstamos (para gestionar el cobro), aunque solo vea
 * saldos y aportes de los integrantes asignados a él. El INTEGRANTE solo ve los suyos.
 */
@Component
public class PrestamoAccessPolicy {

    public boolean puedeVer(UsuarioActual usuario, Prestamo prestamo) {
        if (usuario.esAdmin() || usuario.esTesorero()) {
            return true;
        }
        return prestamo.integranteId().equals(usuario.integranteId());
    }

    /** true si el usuario ve todos los préstamos (no solo los propios). */
    public boolean veTodos(UsuarioActual usuario) {
        return usuario.esAdmin() || usuario.esTesorero();
    }
}

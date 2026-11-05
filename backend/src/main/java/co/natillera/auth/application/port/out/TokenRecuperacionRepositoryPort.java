package co.natillera.auth.application.port.out;

import co.natillera.auth.domain.TokenRecuperacion;

import java.util.Optional;

public interface TokenRecuperacionRepositoryPort {

    Optional<TokenRecuperacion> buscarPorHash(String tokenHash);

    void guardar(TokenRecuperacion token);

    /** Marca como usados todos los enlaces pendientes del usuario (al pedir uno nuevo o al usar uno). */
    void invalidarDelUsuario(Long usuarioId);
}

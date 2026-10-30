package co.natillera.auth.application.port.out;

import co.natillera.auth.application.UsuarioActual;

public interface TokenEmisorPort {

    String emitirAccessToken(UsuarioActual usuario);

    long duracionAccessTokenSegundos();
}

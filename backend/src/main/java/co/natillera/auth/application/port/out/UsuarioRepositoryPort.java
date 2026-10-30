package co.natillera.auth.application.port.out;

import co.natillera.auth.domain.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorLogin(String login);

    Optional<Usuario> buscarPorId(Long id);

    List<Usuario> listar();

    Usuario guardar(Usuario usuario);

    boolean existeLogin(String correo, String celular);
}

package co.natillera.auth.infrastructure.persistence;

import co.natillera.auth.application.port.out.UsuarioRepositoryPort;
import co.natillera.auth.domain.Rol;
import co.natillera.auth.domain.Usuario;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarios;
    private final RolJpaRepository roles;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarios, RolJpaRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        return usuarios.buscarPorLogin(login).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarios.buscarConRoles(id).map(this::aDominio);
    }

    @Override
    public List<Usuario> listar() {
        Map<Long, Long[]> vinculos = new HashMap<>();
        for (Object[] fila : usuarios.vinculos()) {
            vinculos.put(((Number) fila[0]).longValue(), new Long[]{aLong(fila[1]), aLong(fila[2])});
        }
        return usuarios.listarConRoles().stream().map(e -> {
            Long[] v = vinculos.getOrDefault(e.getId(), new Long[2]);
            return aDominio(e, v[0], v[1]);
        }).toList();
    }

    @Override
    public Usuario guardar(Usuario u) {
        UsuarioJpaEntity e = u.id() == null ? new UsuarioJpaEntity()
                : usuarios.findById(u.id()).orElseThrow();
        e.setNombre(u.nombre());
        e.setCorreo(u.correo());
        e.setCelular(u.celular());
        e.setPasswordHash(u.passwordHash());
        e.setActivo(u.activo());
        e.setDebeCambiarPassword(u.debeCambiarPassword());
        e.setRoles(new HashSet<>(roles.findByNombreIn(u.roles())));
        return aDominio(usuarios.save(e));
    }

    @Override
    public boolean existeLogin(String correo, String celular) {
        return usuarios.existeLogin(correo, celular);
    }

    private Usuario aDominio(UsuarioJpaEntity e) {
        return aDominio(e, usuarios.integranteIdDe(e.getId()).orElse(null),
                usuarios.tesoreroIdDe(e.getId()).orElse(null));
    }

    private Usuario aDominio(UsuarioJpaEntity e, Long integranteId, Long tesoreroId) {
        Set<Rol> r = EnumSet.noneOf(Rol.class);
        e.getRoles().forEach(rol -> r.add(rol.getNombre()));
        return new Usuario(e.getId(), e.getNombre(), e.getCorreo(), e.getCelular(), e.getPasswordHash(), r,
                e.isActivo(), e.isDebeCambiarPassword(), integranteId, tesoreroId);
    }

    private static Long aLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}

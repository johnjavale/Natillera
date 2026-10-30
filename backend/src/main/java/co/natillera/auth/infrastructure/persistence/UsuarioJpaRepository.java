package co.natillera.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    @EntityGraph(attributePaths = "roles")
    @Query("select u from UsuarioJpaEntity u where lower(u.correo) = lower(:login) or u.celular = :login")
    Optional<UsuarioJpaEntity> buscarPorLogin(@Param("login") String login);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from UsuarioJpaEntity u where u.id = :id")
    Optional<UsuarioJpaEntity> buscarConRoles(@Param("id") Long id);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from UsuarioJpaEntity u order by u.nombre")
    List<UsuarioJpaEntity> listarConRoles();

    @Query("select count(u) > 0 from UsuarioJpaEntity u where (:correo is not null and lower(u.correo) = :correo) "
            + "or (:celular is not null and u.celular = :celular)")
    boolean existeLogin(@Param("correo") String correo, @Param("celular") String celular);

    @Query(value = "select id from integrante where usuario_id = :usuarioId", nativeQuery = true)
    Optional<Long> integranteIdDe(@Param("usuarioId") Long usuarioId);

    @Query(value = "select id from tesorero where usuario_id = :usuarioId", nativeQuery = true)
    Optional<Long> tesoreroIdDe(@Param("usuarioId") Long usuarioId);

    /** Vínculos de todos los usuarios en una sola consulta (evita N+1 al listar). */
    @Query(value = "select u.id as usuario_id, i.id as integrante_id, t.id as tesorero_id from usuario u "
            + "left join integrante i on i.usuario_id = u.id left join tesorero t on t.usuario_id = u.id",
            nativeQuery = true)
    List<Object[]> vinculos();
}

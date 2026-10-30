package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tesorero")
public class TesoreroJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nombre;

    protected TesoreroJpaEntity() {
    }

    public TesoreroJpaEntity(Long usuarioId, String nombre) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }
}

package co.natillera.auth.infrastructure.persistence;

import co.natillera.auth.domain.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class RolJpaEntity {

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Rol nombre;

    protected RolJpaEntity() {
    }

    public Short getId() {
        return id;
    }

    public Rol getNombre() {
        return nombre;
    }
}

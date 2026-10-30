package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "cajita")
public class CajitaJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "integrante_id", nullable = false, unique = true)
    private Long integranteId;

    @Column(nullable = false)
    private String nombre;

    /** El tesorero de la cajita es el asignado a su integrante (derivado). */
    @Formula("(select i.tesorero_id from integrante i where i.id = integrante_id)")
    private Long tesoreroId;

    protected CajitaJpaEntity() {
    }

    public CajitaJpaEntity(Long integranteId, String nombre) {
        this.integranteId = integranteId;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getTesoreroId() {
        return tesoreroId;
    }
}

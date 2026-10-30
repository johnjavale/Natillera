package co.natillera.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria")
public class AuditoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false)
    private String accion;

    private String detalle;

    private String usuario;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    protected AuditoriaJpaEntity() {
    }

    public AuditoriaJpaEntity(String entidad, Long entidadId, String accion, String detalle, String usuario,
                              OffsetDateTime fecha) {
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.detalle = detalle;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}

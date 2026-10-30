package co.natillera.actividades.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "asignacion_boletas")
@IdClass(AsignacionBoletasJpaEntity.Clave.class)
public class AsignacionBoletasJpaEntity {

    public static class Clave implements Serializable {
        private Long actividadId;
        private Long integranteId;

        public Clave() {
        }

        public Clave(Long actividadId, Long integranteId) {
            this.actividadId = actividadId;
            this.integranteId = integranteId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Clave c && Objects.equals(actividadId, c.actividadId)
                    && Objects.equals(integranteId, c.integranteId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(actividadId, integranteId);
        }
    }

    @Id
    @Column(name = "actividad_id")
    private Long actividadId;

    @Id
    @Column(name = "integrante_id")
    private Long integranteId;

    @Column(nullable = false)
    private int boletas;

    protected AsignacionBoletasJpaEntity() {
    }

    public AsignacionBoletasJpaEntity(Long actividadId, Long integranteId, int boletas) {
        this.actividadId = actividadId;
        this.integranteId = integranteId;
        this.boletas = boletas;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public int getBoletas() {
        return boletas;
    }

    public void setBoletas(int boletas) {
        this.boletas = boletas;
    }
}

package co.natillera.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/** creadoPor, creadoEn, modificadoPor, modificadoEn en todas las entidades (Spring Data Auditing). */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadAuditable {

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private String creadoPor;

    @CreatedDate
    @Column(name = "creado_en", updatable = false, nullable = false)
    private OffsetDateTime creadoEn;

    @LastModifiedBy
    @Column(name = "modificado_por")
    private String modificadoPor;

    @LastModifiedDate
    @Column(name = "modificado_en")
    private OffsetDateTime modificadoEn;

    public String getCreadoPor() {
        return creadoPor;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public OffsetDateTime getModificadoEn() {
        return modificadoEn;
    }
}

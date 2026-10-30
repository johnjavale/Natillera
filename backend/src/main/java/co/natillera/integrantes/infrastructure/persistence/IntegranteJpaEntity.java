package co.natillera.integrantes.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

@Entity
@Table(name = "integrante")
public class IntegranteJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    @Column(name = "cuota_mensual", nullable = false)
    private BigDecimal cuotaMensual;

    @Column(name = "tesorero_id")
    private Long tesoreroId;

    @Column(nullable = false)
    private boolean activo = true;

    /** El nombre de la cajita vive en la tabla cajita (módulo de tesorería); aquí es de solo lectura. */
    @Formula("(select c.nombre from cajita c where c.integrante_id = id)")
    private String nombreCajitaNu;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public BigDecimal getCuotaMensual() {
        return cuotaMensual;
    }

    public void setCuotaMensual(BigDecimal cuotaMensual) {
        this.cuotaMensual = cuotaMensual;
    }

    public Long getTesoreroId() {
        return tesoreroId;
    }

    public void setTesoreroId(Long tesoreroId) {
        this.tesoreroId = tesoreroId;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCajitaNu() {
        return nombreCajitaNu;
    }
}

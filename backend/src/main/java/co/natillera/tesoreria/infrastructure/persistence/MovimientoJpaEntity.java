package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
import co.natillera.tesoreria.domain.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del libro de movimientos. Inmutable una vez creada (solo se anula con un contramovimiento). */
@Entity
@Table(name = "movimiento")
public class MovimientoJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cajita_id", nullable = false, updatable = false)
    private Long cajitaId;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimiento tipo;

    @Column(updatable = false)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private BigDecimal entrada;

    @Column(nullable = false, updatable = false)
    private BigDecimal salida;

    @Column(name = "prestamo_id", updatable = false)
    private Long prestamoId;

    @Column(name = "pago_cuota_id", updatable = false)
    private Long pagoCuotaId;

    @Column(name = "actividad_id", updatable = false)
    private Long actividadId;

    @Column(name = "anula_a", updatable = false)
    private Long anulaA;

    @Column(name = "motivo_anulacion", updatable = false)
    private String motivoAnulacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCajitaId() {
        return cajitaId;
    }

    public void setCajitaId(Long cajitaId) {
        this.cajitaId = cajitaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getEntrada() {
        return entrada;
    }

    public void setEntrada(BigDecimal entrada) {
        this.entrada = entrada;
    }

    public BigDecimal getSalida() {
        return salida;
    }

    public void setSalida(BigDecimal salida) {
        this.salida = salida;
    }

    public Long getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Long prestamoId) {
        this.prestamoId = prestamoId;
    }

    public Long getPagoCuotaId() {
        return pagoCuotaId;
    }

    public void setPagoCuotaId(Long pagoCuotaId) {
        this.pagoCuotaId = pagoCuotaId;
    }

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public Long getAnulaA() {
        return anulaA;
    }

    public void setAnulaA(Long anulaA) {
        this.anulaA = anulaA;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}

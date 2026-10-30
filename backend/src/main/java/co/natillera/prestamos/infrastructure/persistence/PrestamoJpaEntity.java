package co.natillera.prestamos.infrastructure.persistence;

import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "prestamo")
public class PrestamoJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "integrante_id", nullable = false)
    private Long integranteId;

    @Column(name = "cajita_origen_id", nullable = false)
    private Long cajitaOrigenId;

    @Column(name = "fecha_desembolso", nullable = false)
    private LocalDate fechaDesembolso;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "mes_del_prestamo", nullable = false)
    private int mesDelPrestamo;

    @Column(name = "numero_cuotas", nullable = false)
    private int numeroCuotas;

    @Column(name = "tasa_mensual", nullable = false)
    private BigDecimal tasaMensual;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public Long getCajitaOrigenId() {
        return cajitaOrigenId;
    }

    public void setCajitaOrigenId(Long cajitaOrigenId) {
        this.cajitaOrigenId = cajitaOrigenId;
    }

    public LocalDate getFechaDesembolso() {
        return fechaDesembolso;
    }

    public void setFechaDesembolso(LocalDate fechaDesembolso) {
        this.fechaDesembolso = fechaDesembolso;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public int getMesDelPrestamo() {
        return mesDelPrestamo;
    }

    public void setMesDelPrestamo(int mesDelPrestamo) {
        this.mesDelPrestamo = mesDelPrestamo;
    }

    public int getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(int numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public BigDecimal getTasaMensual() {
        return tasaMensual;
    }

    public void setTasaMensual(BigDecimal tasaMensual) {
        this.tasaMensual = tasaMensual;
    }
}

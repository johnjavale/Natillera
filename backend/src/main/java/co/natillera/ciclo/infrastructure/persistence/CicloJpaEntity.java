package co.natillera.ciclo.infrastructure.persistence;

import co.natillera.ciclo.domain.EstadoCiclo;
import co.natillera.shared.infrastructure.persistence.EntidadAuditable;
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

@Entity
@Table(name = "ciclo")
public class CicloJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "numero_meses", nullable = false)
    private int numeroMeses;

    @Column(name = "tasa_interes_mensual_prestamos", nullable = false)
    private BigDecimal tasaInteresMensualPrestamos;

    @Column(name = "gastos_administrativos", nullable = false)
    private BigDecimal gastosAdministrativos;

    @Column(name = "max_cuotas_prestamo", nullable = false)
    private int maxCuotasPrestamo;

    @Column(name = "tasa_ea_cajitas_nu", nullable = false)
    private BigDecimal tasaEACajitasNu;

    @Column(name = "max_actividades", nullable = false)
    private int maxActividades;

    @Column(name = "permitir_prestamo_despues_del_cierre", nullable = false)
    private boolean permitirPrestamoDespuesDelCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCiclo estado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public int getNumeroMeses() {
        return numeroMeses;
    }

    public void setNumeroMeses(int numeroMeses) {
        this.numeroMeses = numeroMeses;
    }

    public BigDecimal getTasaInteresMensualPrestamos() {
        return tasaInteresMensualPrestamos;
    }

    public void setTasaInteresMensualPrestamos(BigDecimal tasaInteresMensualPrestamos) {
        this.tasaInteresMensualPrestamos = tasaInteresMensualPrestamos;
    }

    public BigDecimal getGastosAdministrativos() {
        return gastosAdministrativos;
    }

    public void setGastosAdministrativos(BigDecimal gastosAdministrativos) {
        this.gastosAdministrativos = gastosAdministrativos;
    }

    public int getMaxCuotasPrestamo() {
        return maxCuotasPrestamo;
    }

    public void setMaxCuotasPrestamo(int maxCuotasPrestamo) {
        this.maxCuotasPrestamo = maxCuotasPrestamo;
    }

    public BigDecimal getTasaEACajitasNu() {
        return tasaEACajitasNu;
    }

    public void setTasaEACajitasNu(BigDecimal tasaEACajitasNu) {
        this.tasaEACajitasNu = tasaEACajitasNu;
    }

    public int getMaxActividades() {
        return maxActividades;
    }

    public void setMaxActividades(int maxActividades) {
        this.maxActividades = maxActividades;
    }

    public boolean isPermitirPrestamoDespuesDelCierre() {
        return permitirPrestamoDespuesDelCierre;
    }

    public void setPermitirPrestamoDespuesDelCierre(boolean permitirPrestamoDespuesDelCierre) {
        this.permitirPrestamoDespuesDelCierre = permitirPrestamoDespuesDelCierre;
    }

    public EstadoCiclo getEstado() {
        return estado;
    }

    public void setEstado(EstadoCiclo estado) {
        this.estado = estado;
    }
}

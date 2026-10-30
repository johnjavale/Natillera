package co.natillera.actividades.infrastructure.persistence;

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
@Table(name = "actividad")
public class ActividadJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_sorteo", nullable = false)
    private LocalDate fechaSorteo;

    @Column(name = "valor_boleta", nullable = false)
    private BigDecimal valorBoleta;

    @Column(name = "boletas_por_integrante", nullable = false)
    private int boletasPorIntegrante;

    @Column(name = "costo_premio", nullable = false)
    private BigDecimal costoPremio;

    @Column(name = "otros_gastos", nullable = false)
    private BigDecimal otrosGastos;

    private String ganador;

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

    public LocalDate getFechaSorteo() {
        return fechaSorteo;
    }

    public void setFechaSorteo(LocalDate fechaSorteo) {
        this.fechaSorteo = fechaSorteo;
    }

    public BigDecimal getValorBoleta() {
        return valorBoleta;
    }

    public void setValorBoleta(BigDecimal valorBoleta) {
        this.valorBoleta = valorBoleta;
    }

    public int getBoletasPorIntegrante() {
        return boletasPorIntegrante;
    }

    public void setBoletasPorIntegrante(int boletasPorIntegrante) {
        this.boletasPorIntegrante = boletasPorIntegrante;
    }

    public BigDecimal getCostoPremio() {
        return costoPremio;
    }

    public void setCostoPremio(BigDecimal costoPremio) {
        this.costoPremio = costoPremio;
    }

    public BigDecimal getOtrosGastos() {
        return otrosGastos;
    }

    public void setOtrosGastos(BigDecimal otrosGastos) {
        this.otrosGastos = otrosGastos;
    }

    public String getGanador() {
        return ganador;
    }

    public void setGanador(String ganador) {
        this.ganador = ganador;
    }
}

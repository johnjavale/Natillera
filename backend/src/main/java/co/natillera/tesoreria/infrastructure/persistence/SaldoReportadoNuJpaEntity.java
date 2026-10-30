package co.natillera.tesoreria.infrastructure.persistence;

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
@Table(name = "saldo_reportado_nu")
public class SaldoReportadoNuJpaEntity extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cajita_id", nullable = false)
    private Long cajitaId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "saldo_app", nullable = false)
    private BigDecimal saldoApp;

    protected SaldoReportadoNuJpaEntity() {
    }

    public SaldoReportadoNuJpaEntity(Long cajitaId, LocalDate fecha, BigDecimal saldoApp) {
        this.cajitaId = cajitaId;
        this.fecha = fecha;
        this.saldoApp = saldoApp;
    }

    public Long getId() {
        return id;
    }
}

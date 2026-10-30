package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import co.natillera.tesoreria.domain.Cajita;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.Tesorero;
import co.natillera.tesoreria.infrastructure.persistence.TesoreriaJpaRepositories.CajitaJpaRepository;
import co.natillera.tesoreria.infrastructure.persistence.TesoreriaJpaRepositories.MovimientoJpaRepository;
import co.natillera.tesoreria.infrastructure.persistence.TesoreriaJpaRepositories.SaldoReportadoNuJpaRepository;
import co.natillera.tesoreria.infrastructure.persistence.TesoreriaJpaRepositories.TesoreroJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TesoreriaPersistenceAdapter implements TesoreriaRepositoryPort {

    private final MovimientoJpaRepository movimientos;
    private final CajitaJpaRepository cajitas;
    private final TesoreroJpaRepository tesoreros;
    private final SaldoReportadoNuJpaRepository saldos;
    private final MovimientoMapper mapper;
    private final EntityManager em;

    public TesoreriaPersistenceAdapter(MovimientoJpaRepository movimientos, CajitaJpaRepository cajitas,
                                       TesoreroJpaRepository tesoreros, SaldoReportadoNuJpaRepository saldos,
                                       MovimientoMapper mapper, EntityManager em) {
        this.movimientos = movimientos;
        this.cajitas = cajitas;
        this.tesoreros = tesoreros;
        this.saldos = saldos;
        this.mapper = mapper;
        this.em = em;
    }

    @Override
    public Movimiento guardarMovimiento(Movimiento movimiento) {
        return mapper.aDominio(movimientos.save(mapper.aEntidad(movimiento)));
    }

    @Override
    public Optional<Movimiento> buscarMovimiento(Long id) {
        return movimientos.findById(id).map(mapper::aDominio);
    }

    @Override
    public Optional<Movimiento> movimientoDePagoCuota(Long pagoCuotaId) {
        return movimientos.findFirstByPagoCuotaIdAndAnulaAIsNull(pagoCuotaId).map(mapper::aDominio);
    }

    @Override
    public boolean estaAnulado(Long movimientoId) {
        return movimientos.existsByAnulaA(movimientoId);
    }

    @Override
    public Optional<Cajita> buscarCajita(Long cajitaId) {
        return cajitas.findById(cajitaId).map(TesoreriaPersistenceAdapter::cajita);
    }

    @Override
    public Optional<Cajita> cajitaDeIntegrante(Long integranteId) {
        return cajitas.findByIntegranteId(integranteId).map(TesoreriaPersistenceAdapter::cajita);
    }

    @Override
    public Cajita crearCajita(Long integranteId, String nombre) {
        Cajita creada = cajita(cajitas.saveAndFlush(new CajitaJpaEntity(integranteId, nombre)));
        // Las entidades ya cargadas calculan con @Formula el nombre/tesorero de la cajita: se recargan
        em.clear();
        return creada;
    }

    @Override
    public void renombrarCajita(Long integranteId, String nombre) {
        cajitas.renombrar(integranteId, nombre);
    }

    @Override
    public void guardarSaldoReportado(Long cajitaId, LocalDate fecha, Dinero saldoApp) {
        saldos.save(new SaldoReportadoNuJpaEntity(cajitaId, fecha, saldoApp.aCentavos()));
    }

    @Override
    public List<Tesorero> listarTesoreros() {
        return tesoreros.findAllByOrderByIdAsc().stream().map(TesoreriaPersistenceAdapter::tesorero).toList();
    }

    @Override
    public Optional<Tesorero> buscarTesorero(Long tesoreroId) {
        return tesoreros.findById(tesoreroId).map(TesoreriaPersistenceAdapter::tesorero);
    }

    @Override
    public Tesorero asegurarTesorero(Long usuarioId, String nombre) {
        return tesorero(tesoreros.findByUsuarioId(usuarioId)
                .orElseGet(() -> tesoreros.save(new TesoreroJpaEntity(usuarioId, nombre))));
    }

    private static Cajita cajita(CajitaJpaEntity e) {
        return new Cajita(e.getId(), e.getIntegranteId(), e.getNombre(), e.getTesoreroId());
    }

    private static Tesorero tesorero(TesoreroJpaEntity e) {
        return new Tesorero(e.getId(), e.getUsuarioId(), e.getNombre());
    }
}

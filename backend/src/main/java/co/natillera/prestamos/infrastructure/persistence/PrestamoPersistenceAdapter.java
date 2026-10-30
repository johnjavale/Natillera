package co.natillera.prestamos.infrastructure.persistence;

import co.natillera.prestamos.application.port.out.PrestamoRepositoryPort;
import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.prestamos.infrastructure.persistence.PrestamoJpaRepositories.PagoCuotaJpaRepository;
import co.natillera.prestamos.infrastructure.persistence.PrestamoJpaRepositories.PrestamoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PrestamoPersistenceAdapter implements PrestamoRepositoryPort {

    private final PrestamoJpaRepository prestamos;
    private final PagoCuotaJpaRepository pagos;
    private final PrestamoMapper mapper;

    public PrestamoPersistenceAdapter(PrestamoJpaRepository prestamos, PagoCuotaJpaRepository pagos,
                                      PrestamoMapper mapper) {
        this.prestamos = prestamos;
        this.pagos = pagos;
        this.mapper = mapper;
    }

    @Override
    public Prestamo guardar(Prestamo prestamo) {
        return mapper.aDominio(prestamos.save(mapper.aEntidad(prestamo)));
    }

    @Override
    public Optional<Prestamo> buscar(Long id) {
        return prestamos.findById(id).map(mapper::aDominio);
    }

    @Override
    public List<Prestamo> listar() {
        return prestamos.findAllByOrderByIdAsc().stream().map(mapper::aDominio).toList();
    }

    @Override
    public PagoCuota guardarPago(PagoCuota pago) {
        return mapper.aDominio(pagos.saveAndFlush(mapper.aEntidad(pago)));
    }

    @Override
    public Optional<PagoCuota> buscarPago(Long pagoId) {
        return pagos.findById(pagoId).map(mapper::aDominio);
    }

    @Override
    public List<PagoCuota> pagosVigentes() {
        return pagos.findByAnuladoFalse().stream().map(mapper::aDominio).toList();
    }

    @Override
    public List<PagoCuota> pagosDe(Long prestamoId) {
        return pagos.findByPrestamoIdOrderByNumeroCuotaAsc(prestamoId).stream().map(mapper::aDominio).toList();
    }

    @Override
    public void anularPago(Long pagoId, String motivo) {
        PagoCuotaJpaEntity e = pagos.findById(pagoId).orElseThrow();
        e.setAnulado(true);
        e.setMotivoAnulacion(motivo);
        pagos.saveAndFlush(e);
    }
}

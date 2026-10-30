package co.natillera.ciclo.application;

import co.natillera.ciclo.application.port.out.CicloRepositoryPort;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.ciclo.domain.EstadoCiclo;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.application.FechaNegocio;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Configuración del ciclo y su ciclo de vida (INSCRIPCION → ACTIVO → CERRADO). */
@Service
public class CicloService {

    public record ActualizarCiclo(String nombre, LocalDate fechaInicio, int numeroMeses,
                                  BigDecimal tasaInteresMensualPrestamos, BigDecimal gastosAdministrativos,
                                  int maxCuotasPrestamo, BigDecimal tasaEACajitasNu, int maxActividades,
                                  boolean permitirPrestamoDespuesDelCierre) {
    }

    private final CicloRepositoryPort repository;
    private final AuditoriaPort auditoria;
    private final FechaNegocio fecha;
    /** El ciclo se consulta en casi todos los casos de uso y cambia muy poco: se guarda en memoria. */
    private final AtomicReference<Ciclo> cache = new AtomicReference<>();

    public CicloService(CicloRepositoryPort repository, AuditoriaPort auditoria, FechaNegocio fecha) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.fecha = fecha;
    }

    @Transactional(readOnly = true)
    public Ciclo actual() {
        Ciclo c = cache.get();
        if (c == null) {
            c = repository.actual();
            cache.set(c);
        }
        return c;
    }

    public LocalDate hoy() {
        return fecha.hoy();
    }

    public int mesesTranscurridos() {
        return actual().mesesTranscurridos(fecha.hoy());
    }

    @Transactional
    public Ciclo actualizar(ActualizarCiclo d) {
        Ciclo actual = repository.actual();
        if (actual.estaCerrado()) {
            throw new ReglaNegocioException("El ciclo está cerrado y no se puede modificar");
        }
        Ciclo nuevo = new Ciclo(actual.id(), d.nombre(), d.fechaInicio(), d.numeroMeses(),
                d.tasaInteresMensualPrestamos(), Dinero.de(d.gastosAdministrativos()), d.maxCuotasPrestamo(),
                d.tasaEACajitasNu(), d.maxActividades(), d.permitirPrestamoDespuesDelCierre(), actual.estado());
        Ciclo guardado = repository.guardar(nuevo);
        invalidarCache();
        String cambios = describirCambios(actual, guardado);
        if (!cambios.isEmpty()) {
            auditoria.registrar("ciclo", guardado.id(), "ACTUALIZAR_CONFIGURACION", cambios);
        }
        return guardado;
    }

    @Transactional
    public Ciclo cambiarEstado(EstadoCiclo nuevoEstado) {
        Ciclo actual = repository.actual();
        if (actual.estaCerrado()) {
            throw new ReglaNegocioException("El ciclo ya está cerrado");
        }
        if (nuevoEstado == actual.estado()) {
            return actual;
        }
        Ciclo guardado = repository.guardar(actual.conEstado(nuevoEstado));
        invalidarCache();
        auditoria.registrar("ciclo", guardado.id(), nuevoEstado == EstadoCiclo.CERRADO ? "CERRAR_CICLO" : "CAMBIAR_ESTADO",
                actual.estado() + " -> " + nuevoEstado);
        return guardado;
    }

    /** Se invalida ya y otra vez al terminar la transacción (por si se revierte). */
    private void invalidarCache() {
        cache.set(null);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    cache.set(null);
                }
            });
        }
    }

    /** Los préstamos y actividades no se crean ni modifican con el ciclo cerrado. */
    public void exigirCicloAbierto() {
        if (actual().estaCerrado()) {
            throw new ReglaNegocioException("El ciclo está cerrado");
        }
    }

    private static String describirCambios(Ciclo a, Ciclo b) {
        List<String> cambios = new ArrayList<>();
        comparar(cambios, "nombre", a.nombre(), b.nombre());
        comparar(cambios, "fechaInicio", a.fechaInicio(), b.fechaInicio());
        comparar(cambios, "numeroMeses", a.numeroMeses(), b.numeroMeses());
        compararNumero(cambios, "tasaInteresMensualPrestamos", a.tasaInteresMensualPrestamos(), b.tasaInteresMensualPrestamos());
        compararNumero(cambios, "gastosAdministrativos", a.gastosAdministrativos().valor(), b.gastosAdministrativos().valor());
        comparar(cambios, "maxCuotasPrestamo", a.maxCuotasPrestamo(), b.maxCuotasPrestamo());
        compararNumero(cambios, "tasaEACajitasNu", a.tasaEACajitasNu(), b.tasaEACajitasNu());
        comparar(cambios, "maxActividades", a.maxActividades(), b.maxActividades());
        comparar(cambios, "permitirPrestamoDespuesDelCierre", a.permitirPrestamoDespuesDelCierre(),
                b.permitirPrestamoDespuesDelCierre());
        return String.join("; ", cambios);
    }

    private static void comparar(List<String> cambios, String campo, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            cambios.add(campo + ": " + antes + " -> " + despues);
        }
    }

    private static void compararNumero(List<String> cambios, String campo, BigDecimal antes, BigDecimal despues) {
        if (antes.compareTo(despues) != 0) {
            cambios.add(campo + ": " + antes.toPlainString() + " -> " + despues.toPlainString());
        }
    }
}

package co.natillera.integrantes.application;

import co.natillera.auth.application.AccessScopeService;
import co.natillera.auth.application.SesionActual;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.application.port.out.IntegranteRepositoryPort;
import co.natillera.integrantes.domain.CalculadoraAhorro;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.application.AuditoriaPort;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.application.LibroConsultas;
import co.natillera.tesoreria.application.port.out.TesoreriaRepositoryPort;
import co.natillera.tesoreria.domain.SaldoCajita;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Casos de uso de integrantes (sección 3.2). */
@Service
public class IntegranteService {

    public record DatosIntegrante(String nombre, String telefono, Dinero cuotaMensual, Long tesoreroId,
                                  String nombreCajitaNu, boolean activo) {
    }

    private final IntegranteRepositoryPort repository;
    private final TesoreriaRepositoryPort tesoreria;
    private final LibroConsultas libro;
    private final CicloService ciclos;
    private final AccessScopeService access;
    private final SesionActual sesion;
    private final AuditoriaPort auditoria;

    public IntegranteService(IntegranteRepositoryPort repository, TesoreriaRepositoryPort tesoreria,
                             LibroConsultas libro, CicloService ciclos, AccessScopeService access,
                             SesionActual sesion, AuditoriaPort auditoria) {
        this.repository = repository;
        this.tesoreria = tesoreria;
        this.libro = libro;
        this.ciclos = ciclos;
        this.access = access;
        this.sesion = sesion;
        this.auditoria = auditoria;
    }

    /** Integrantes visibles para el usuario, con columnas calculadas. */
    @Transactional(readOnly = true)
    public List<IntegranteVista> listar() {
        Optional<Set<Long>> visibles = access.integrantesVisibles();
        return vistas(repository.listar(), visibles.orElse(null));
    }

    @Transactional(readOnly = true)
    public IntegranteVista obtener(Long id) {
        access.exigirVerIntegrante(id);
        return vistas(repository.listar(), Set.of(id)).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Integrante", id));
    }

    /** API interna sin alcance: integrantes activos (para repartición, resumen y reportes). */
    @Transactional(readOnly = true)
    public List<Integrante> activos() {
        return repository.listar().stream().filter(Integrante::activo).toList();
    }

    /** API interna sin alcance: nombre de cada integrante (activos e inactivos). */
    @Transactional(readOnly = true)
    public Map<Long, String> nombres() {
        return repository.listar().stream().collect(Collectors.toMap(Integrante::id, Integrante::nombre));
    }

    /** API interna sin alcance. */
    @Transactional(readOnly = true)
    public Optional<Integrante> buscarSinAlcance(Long id) {
        return repository.buscar(id);
    }

    @Transactional(readOnly = true)
    public int contarActivos() {
        return repository.contarActivos();
    }

    @Transactional
    public IntegranteVista crear(DatosIntegrante d) {
        validarTesorero(d.tesoreroId());
        Integrante nuevo = repository.guardar(new Integrante(null, null, d.nombre().trim(), d.telefono(),
                d.cuotaMensual(), d.tesoreroId(), d.nombreCajitaNu(), true));
        tesoreria.crearCajita(nuevo.id(), nuevo.nombreCajitaNu());
        auditoria.registrar("integrante", nuevo.id(), "CREAR", "cuota=" + d.cuotaMensual().aCentavos());
        return obtener(nuevo.id());
    }

    @Transactional
    public IntegranteVista actualizar(Long id, DatosIntegrante d) {
        Integrante actual = buscar(id);
        validarTesorero(d.tesoreroId());
        String nombreCajita = d.nombreCajitaNu() == null || d.nombreCajitaNu().isBlank()
                ? actual.nombreCajitaNu() : d.nombreCajitaNu().trim();
        Integrante nuevo = new Integrante(id, actual.usuarioId(), d.nombre().trim(), d.telefono(), d.cuotaMensual(),
                d.tesoreroId(), nombreCajita, d.activo());
        repository.guardar(nuevo);
        if (!Objects.equals(actual.nombreCajitaNu(), nuevo.nombreCajitaNu())) {
            tesoreria.renombrarCajita(id, nuevo.nombreCajitaNu());
        }
        if (!actual.cuotaMensual().equals(nuevo.cuotaMensual())) {
            auditoria.registrar("integrante", id, "CAMBIAR_CUOTA",
                    actual.cuotaMensual().aCentavos() + " -> " + nuevo.cuotaMensual().aCentavos() + " (admin)");
        }
        if (!Objects.equals(actual.tesoreroId(), nuevo.tesoreroId())) {
            auditoria.registrar("integrante", id, "ASIGNAR_TESORERO", actual.tesoreroId() + " -> " + nuevo.tesoreroId());
        }
        if (actual.activo() != nuevo.activo()) {
            auditoria.registrar("integrante", id, nuevo.activo() ? "ACTIVAR" : "DESACTIVAR", null);
        }
        return obtener(id);
    }

    /** El integrante propone o edita su propia cuota mientras el ciclo está en INSCRIPCION. */
    @Transactional
    public IntegranteVista proponerCuotaPropia(Dinero cuota) {
        Long id = sesion.integranteIdObligatorio();
        Ciclo ciclo = ciclos.actual();
        if (!ciclo.permiteEditarCuotaPropia()) {
            throw new ReglaNegocioException(
                    "La cuota solo se puede editar durante la inscripción. Después, solo el administrador la cambia.");
        }
        if (cuota == null || cuota.esNegativo()) {
            throw new ReglaNegocioException("La cuota mensual no puede ser negativa");
        }
        Integrante actual = buscar(id);
        repository.guardar(actual.conCuota(cuota.redondearAlPeso()));
        auditoria.registrar("integrante", id, "CAMBIAR_CUOTA",
                actual.cuotaMensual().aCentavos() + " -> " + cuota.redondearAlPeso().aCentavos() + " (propia)");
        return obtener(id);
    }

    private List<IntegranteVista> vistas(List<Integrante> todos, Set<Long> filtro) {
        Ciclo ciclo = ciclos.actual();
        int meses = ciclo.numeroMeses();
        Dinero totalAhorro = Dinero.sumarTodos(todos.stream().filter(Integrante::activo)
                .map(i -> CalculadoraAhorro.ahorroEsperado(i.cuotaMensual(), meses)).toList());
        Map<Long, SaldoCajita> cajitas = libro.saldosCajitas(filtro).stream()
                .collect(Collectors.toMap(SaldoCajita::integranteId, Function.identity()));
        return todos.stream()
                .filter(i -> filtro == null || filtro.contains(i.id()))
                .map(i -> {
                    Dinero esperado = CalculadoraAhorro.ahorroEsperado(i.cuotaMensual(), meses);
                    SaldoCajita c = cajitas.get(i.id());
                    return new IntegranteVista(i, c == null ? null : c.cajitaId(),
                            c == null ? null : c.nombreTesorero(), esperado,
                            i.activo() ? CalculadoraAhorro.porcentajeDelTotal(esperado, totalAhorro) : BigDecimal.ZERO,
                            CalculadoraAhorro.rendimientoProyectadoCajita(i.cuotaMensual(), meses, ciclo.tasaMensualNu()),
                            c == null ? Dinero.CERO : c.saldo());
                }).toList();
    }

    private Integrante buscar(Long id) {
        return repository.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Integrante", id));
    }

    private void validarTesorero(Long tesoreroId) {
        if (tesoreroId != null && tesoreria.buscarTesorero(tesoreroId).isEmpty()) {
            throw new ReglaNegocioException("El tesorero seleccionado no existe");
        }
    }
}

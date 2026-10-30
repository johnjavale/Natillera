package co.natillera.integrantes.application;

import co.natillera.auth.application.AccessScopeService;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.domain.CalculadoraAportes;
import co.natillera.integrantes.domain.EstadoAportes;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.LibroConsultas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Matriz integrantes × meses de la hoja "Ahorros Mensuales", derivada del libro (movimientos AHORRO). */
@Service
public class AportesService {

    public record FilaAportes(Long integranteId, String nombre, Dinero cuotaMensual, EstadoAportes estado) {
    }

    public record MatrizAportes(int numeroMeses, int mesesTranscurridos, List<FilaAportes> filas,
                                List<CalculadoraAportes.TotalMes> totales) {
    }

    private final IntegranteService integrantes;
    private final LibroConsultas libro;
    private final CicloService ciclos;
    private final AccessScopeService access;

    public AportesService(IntegranteService integrantes, LibroConsultas libro, CicloService ciclos,
                          AccessScopeService access) {
        this.integrantes = integrantes;
        this.libro = libro;
        this.ciclos = ciclos;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public MatrizAportes matriz() {
        Set<Long> visibles = access.integrantesVisibles().orElse(null);
        return calcular(visibles);
    }

    @Transactional(readOnly = true)
    public FilaAportes deIntegrante(Long integranteId) {
        access.exigirVerIntegrante(integranteId);
        return calcular(Set.of(integranteId)).filas().stream().findFirst().orElseThrow();
    }

    /** API interna sin alcance (para reportes y repartición). */
    @Transactional(readOnly = true)
    public MatrizAportes calcular(Collection<Long> filtro) {
        Ciclo ciclo = ciclos.actual();
        int transcurridos = ciclos.mesesTranscurridos();
        Map<Long, Map<Integer, Dinero>> aportes = libro.aportesPorIntegranteYMes(ciclo, filtro);
        List<FilaAportes> filas = new ArrayList<>();
        Dinero sumaCuotas = Dinero.CERO;
        for (Integrante i : integrantes.activos()) {
            if (filtro != null && !filtro.contains(i.id())) {
                continue;
            }
            EstadoAportes estado = CalculadoraAportes.estado(i.cuotaMensual(), ciclo.numeroMeses(), transcurridos,
                    aportes.getOrDefault(i.id(), Map.of()));
            filas.add(new FilaAportes(i.id(), i.nombre(), i.cuotaMensual(), estado));
            sumaCuotas = sumaCuotas.sumar(i.cuotaMensual());
        }
        List<CalculadoraAportes.TotalMes> totales = CalculadoraAportes.totalesPorMes(ciclo.numeroMeses(), sumaCuotas,
                filas.stream().map(FilaAportes::estado).toList());
        return new MatrizAportes(ciclo.numeroMeses(), transcurridos, filas, totales);
    }
}

package co.natillera.tesoreria.application;

import co.natillera.ciclo.domain.Ciclo;
import co.natillera.shared.domain.CalendarioCiclo;
import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.domain.SaldoCajita;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API de lectura del libro para los demás módulos (aportes, rendimientos, pagos de boletas, saldos),
 * ya traducida a meses del ciclo. No aplica alcance: lo hace el caso de uso que la invoca.
 */
@Service
@Transactional(readOnly = true)
public class LibroConsultas {

    private final LibroQueryPort libro;

    public LibroConsultas(LibroQueryPort libro) {
        this.libro = libro;
    }

    /** aporteReal(integrante, mes): integranteId → (mes del ciclo → Σ AHORRO). */
    public Map<Long, Map<Integer, Dinero>> aportesPorIntegranteYMes(Ciclo ciclo, Collection<Long> integrantes) {
        Map<Long, Map<Integer, Dinero>> resultado = new HashMap<>();
        for (LibroQueryPort.AporteAnioMes a : libro.aportesPorIntegranteYMes(integrantes)) {
            int mes = CalendarioCiclo.mesDelCiclo(ciclo.fechaInicio(), LocalDate.of(a.anio(), a.mes(), 1));
            resultado.computeIfAbsent(a.integranteId(), k -> new HashMap<>()).merge(mes, a.monto(), Dinero::sumar);
        }
        return resultado;
    }

    public Map<Long, Dinero> rendimientoNuPorIntegrante(Collection<Long> integrantes) {
        Map<Long, Dinero> r = new HashMap<>();
        libro.rendimientoNuPorIntegrante(integrantes).forEach(m -> r.put(m.integranteId(), m.monto()));
        return r;
    }

    /** actividadId → (integranteId → Σ PAGO_BOLETAS_RIFA). */
    public Map<Long, Map<Long, Dinero>> pagosBoletas(Collection<Long> integrantes) {
        Map<Long, Map<Long, Dinero>> r = new HashMap<>();
        for (LibroQueryPort.PagoBoletas p : libro.pagosBoletas(integrantes)) {
            r.computeIfAbsent(p.actividadId(), k -> new HashMap<>()).put(p.integranteId(), p.monto());
        }
        return r;
    }

    public List<SaldoCajita> saldosCajitas(Collection<Long> integrantes) {
        return libro.saldosCajitas(integrantes);
    }

    public Dinero saldoCajita(Long cajitaId) {
        return libro.saldoCajita(cajitaId);
    }
}

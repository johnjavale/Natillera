package co.natillera.resumen.application;

import co.natillera.actividades.application.ActividadService;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.integrantes.application.AportesService;
import co.natillera.integrantes.domain.CalculadoraAportes;
import co.natillera.prestamos.application.PrestamoService;
import co.natillera.prestamos.domain.CuotaPlan;
import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.reparticion.application.ReparticionService;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.resumen.domain.CalculadoraResumen;
import co.natillera.resumen.domain.FilaResumenMensual;
import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.LibroConsultas;
import co.natillera.tesoreria.domain.SaldoCajita;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dashboard: resumen mensual y KPIs anuales del fondo completo (sección 3.8). Solo ADMIN. */
@Service
public class ResumenService {

    public record Kpis(Dinero ahorroEsperadoAnual, Dinero ahorroRealALaFecha, Dinero totalPrestado,
                       Dinero interesesProyectados, Dinero interesesRecibidos, Dinero saldoPorCobrar,
                       Dinero utilidadEsperadaActividades, Dinero rendimientoProyectadoNu,
                       Dinero totalARepartirProyectado, BigDecimal rentabilidadProyectada, Dinero saldoTotalCajitas,
                       Dinero diferenciaNu) {
    }

    public record DistribucionReparto(Dinero ahorro, Dinero intereses, Dinero actividades, Dinero rendimientoNu) {
    }

    public record Resumen(int mesesTranscurridos, List<FilaResumenMensual> meses, Kpis kpis,
                          DistribucionReparto distribucion) {
    }

    private final CicloService ciclos;
    private final AportesService aportes;
    private final PrestamoService prestamos;
    private final ActividadService actividades;
    private final ReparticionService reparticion;
    private final LibroConsultas libro;

    public ResumenService(CicloService ciclos, AportesService aportes, PrestamoService prestamos,
                          ActividadService actividades, ReparticionService reparticion, LibroConsultas libro) {
        this.ciclos = ciclos;
        this.aportes = aportes;
        this.prestamos = prestamos;
        this.actividades = actividades;
        this.reparticion = reparticion;
        this.libro = libro;
    }

    @Transactional(readOnly = true)
    public Resumen calcular() {
        Ciclo ciclo = ciclos.actual();
        AportesService.MatrizAportes matriz = aportes.calcular(null);
        Map<Integer, Dinero> ahorroRealPorMes = new HashMap<>();
        for (CalculadoraAportes.TotalMes t : matriz.totales()) {
            ahorroRealPorMes.put(t.mes(), t.totalReal());
        }
        Dinero sumaCuotas = matriz.totales().isEmpty() ? Dinero.CERO : matriz.totales().get(0).totalEsperado();

        List<PrestamoService.PrestamoVista> vistas = prestamos.todosSinAlcance();
        List<Prestamo> lista = new ArrayList<>();
        Map<Integer, Dinero> capitalReal = new HashMap<>();
        Dinero totalPrestado = Dinero.CERO;
        Dinero interesesProyectados = Dinero.CERO;
        Dinero interesesRecibidos = Dinero.CERO;
        Dinero saldoPorCobrar = Dinero.CERO;
        for (PrestamoService.PrestamoVista v : vistas) {
            Prestamo p = v.resumen().prestamo();
            lista.add(p);
            totalPrestado = totalPrestado.sumar(p.monto());
            interesesProyectados = interesesProyectados.sumar(v.resumen().totalIntereses());
            interesesRecibidos = interesesRecibidos.sumar(v.resumen().interesesRecibidos());
            saldoPorCobrar = saldoPorCobrar.sumar(v.resumen().saldoPendiente());
            for (PagoCuota pago : v.pagos()) {
                CuotaPlan cuota = v.resumen().plan().get(pago.numeroCuota() - 1);
                capitalReal.merge(ciclo.mesDelCiclo(pago.fecha()), cuota.capital(), Dinero::sumar);
            }
        }

        List<CalculadoraResumen.ActividadMes> actividadesMes = new ArrayList<>();
        Dinero utilidadEsperada = Dinero.CERO;
        for (ResumenActividad a : actividades.todasSinAlcance()) {
            actividadesMes.add(new CalculadoraResumen.ActividadMes(a.mesDeLaActividad(), a.utilidadEsperada()));
            utilidadEsperada = utilidadEsperada.sumar(a.utilidadEsperada());
        }

        List<FilaResumenMensual> meses = CalculadoraResumen.calcular(ciclo.fechaInicio(), ciclo.numeroMeses(),
                sumaCuotas, ahorroRealPorMes, lista, capitalReal, actividadesMes);

        Reparticion r = reparticion.calcularSinAlcance();
        Dinero ahorroEsperado = Dinero.sumarTodos(r.filas().stream().map(Reparticion.Fila::ahorroEsperado).toList());
        Dinero ahorroReal = Dinero.sumarTodos(r.filas().stream().map(Reparticion.Fila::ahorroReal).toList());
        Dinero rendimientoNu = Dinero.sumarTodos(r.filas().stream().map(Reparticion.Fila::rendimientoNuProyectado).toList());
        Dinero interesesReparto = Dinero.sumarTodos(r.filas().stream().map(Reparticion.Fila::interesesProyectados).toList());
        Dinero actividadesReparto = Dinero.sumarTodos(r.filas().stream().map(Reparticion.Fila::actividadesProyectadas).toList());

        Dinero ganancias = r.proyectado().interesesNetos().sumar(utilidadEsperada).sumar(rendimientoNu);
        List<SaldoCajita> cajitas = libro.saldosCajitas(null);
        Dinero saldoCajitas = Dinero.sumarTodos(cajitas.stream().map(SaldoCajita::saldo).toList());
        Dinero diferenciaNu = Dinero.sumarTodos(cajitas.stream().map(SaldoCajita::diferencia).toList());

        Kpis kpis = new Kpis(ahorroEsperado, ahorroReal, totalPrestado, interesesProyectados, interesesRecibidos,
                saldoPorCobrar, utilidadEsperada, rendimientoNu, r.totalProyectado(),
                ganancias.proporcionDe(ahorroEsperado), saldoCajitas, diferenciaNu);
        return new Resumen(ciclos.mesesTranscurridos(), meses, kpis,
                new DistribucionReparto(ahorroEsperado, interesesReparto, actividadesReparto, rendimientoNu));
    }
}

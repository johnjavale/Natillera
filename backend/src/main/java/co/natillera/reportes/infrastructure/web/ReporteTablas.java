package co.natillera.reportes.infrastructure.web;

import co.natillera.prestamos.application.PrestamoService;
import co.natillera.prestamos.domain.CuotaPlan;
import co.natillera.reportes.application.ReportesService;
import co.natillera.reportes.domain.CalculadoraDeudores;
import co.natillera.reportes.domain.Reporte;
import co.natillera.resumen.application.ResumenService;
import co.natillera.resumen.domain.FilaResumenMensual;
import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.domain.CalculadoraTesoreria;
import co.natillera.tesoreria.domain.SaldoCajita;

import java.util.ArrayList;
import java.util.List;

import static co.natillera.reportes.domain.Reporte.dinero;
import static co.natillera.reportes.domain.Reporte.fecha;
import static co.natillera.reportes.domain.Reporte.numero;
import static co.natillera.reportes.domain.Reporte.porcentaje;
import static co.natillera.reportes.domain.Reporte.texto;

/** Convierte los resultados de los reportes en tablas exportables (Excel / CSV). */
final class ReporteTablas {

    private ReporteTablas() {
    }

    static Reporte consolidado(List<ReportesService.FilaConsolidado> filas) {
        return new Reporte("Consolidado por integrante", "consolidado", List.of(tablaConsolidado("Consolidado", filas)));
    }

    static Reporte deudores(List<CalculadoraDeudores.Deudor> deudores) {
        List<List<Object>> filas = new ArrayList<>();
        for (CalculadoraDeudores.Deudor d : deudores) {
            filas.add(List.of(d.nombre(), nz(d.nombreTesorero()), c(d.atrasoAportes()), c(d.saldoPrestamos()),
                    c(d.vencidoPrestamos()), c(d.boletasPendientes()), c(d.total()), d.enMora() ? "Sí" : "No"));
        }
        return new Reporte("¿Quién debe y cuánto?", "deudores", List.of(new Reporte.Tabla("Deudores", List.of(
                texto("Integrante"), texto("Tesorero"), dinero("Atraso de aportes"), dinero("Saldo de préstamos"),
                dinero("Préstamos vencido"), dinero("Boletas sin pagar"), dinero("Total"), texto("En mora")), filas)));
    }

    static Reporte estadoCuenta(ReportesService.EstadoCuenta e) {
        List<Reporte.Tabla> tablas = new ArrayList<>();
        tablas.add(tablaConsolidado("Resumen", List.of(e.resumen())));
        if (e.aportes() != null) {
            List<List<Object>> filas = new ArrayList<>();
            for (int m = 0; m < e.numeroMeses(); m++) {
                filas.add(List.of(m + 1, c(e.aportes().cuotaMensual()), c(e.aportes().estado().aportesPorMes().get(m))));
            }
            tablas.add(new Reporte.Tabla("Aportes", List.of(numero("Mes"), dinero("Cuota esperada"),
                    dinero("Aporte real")), filas));
        }
        tablas.add(tablaMovimientos("Movimientos", e.movimientos()));
        for (PrestamoService.PrestamoVista p : e.prestamos()) {
            List<List<Object>> filas = new ArrayList<>();
            for (CuotaPlan cuota : p.resumen().plan()) {
                filas.add(List.of(cuota.numero(), cuota.mesVencimiento(), c(cuota.capital()), c(cuota.interes()),
                        c(cuota.valor()), p.resumen().numerosPagados().contains(cuota.numero()) ? "Pagada"
                                : p.resumen().numerosVencidos().contains(cuota.numero()) ? "Vencida" : "Pendiente"));
            }
            tablas.add(new Reporte.Tabla("Préstamo " + p.resumen().prestamo().id(), List.of(numero("Cuota"),
                    numero("Mes de vencimiento"), dinero("Capital"), dinero("Interés"), dinero("Valor"),
                    texto("Estado")), filas));
        }
        List<List<Object>> boletas = new ArrayList<>();
        for (ReportesService.BoletasDeActividad b : e.boletas()) {
            boletas.add(List.of(b.actividad(), b.boletas(), c(b.valorAPagar()), c(b.pagado()), c(b.pendiente())));
        }
        tablas.add(new Reporte.Tabla("Boletas", List.of(texto("Actividad"), numero("Boletas"), dinero("Valor a pagar"),
                dinero("Pagado"), dinero("Pendiente")), boletas));
        if (e.reparto() != null) {
            var r = e.reparto();
            tablas.add(new Reporte.Tabla("Repartición", List.of(texto("Concepto"), dinero("Proyectado"), dinero("Real")),
                    List.of(List.of("Ahorro", c(r.ahorroEsperado()), c(r.ahorroReal())),
                            List.of("Intereses de préstamos", c(r.interesesProyectados()), c(r.interesesReales())),
                            List.of("Actividades", c(r.actividadesProyectadas()), c(r.actividadesReales())),
                            List.of("Rendimiento cajita Nu", c(r.rendimientoNuProyectado()), c(r.rendimientoNuReal())),
                            List.of("TOTAL A RECIBIR", c(r.totalProyectado()), c(r.totalReal())))));
        }
        return new Reporte("Estado de cuenta — " + e.resumen().nombre(), "estado-cuenta-" + e.resumen().integranteId(),
                tablas);
    }

    static Reporte tesoreria(ReportesService.ReporteTesoreria t) {
        List<List<Object>> cajitas = new ArrayList<>();
        for (SaldoCajita s : t.cajitas()) {
            cajitas.add(lista(s.nombreIntegrante(), s.nombreCajita(), nz(s.nombreTesorero()), c(s.entradas()),
                    c(s.salidas()), c(s.saldo()), s.saldoApp() == null ? null : c(s.saldoApp()), c(s.diferencia())));
        }
        List<List<Object>> tesoreros = new ArrayList<>();
        for (CalculadoraTesoreria.SaldoTesorero s : t.porTesorero()) {
            tesoreros.add(List.of(s.nombreTesorero(), s.numeroCajitas(), c(s.saldoRegistro()), c(s.saldoApp()),
                    c(s.diferencia())));
        }
        return new Reporte("Tesorería", "tesoreria", List.of(
                new Reporte.Tabla("Cajitas", List.of(texto("Integrante"), texto("Cajita"), texto("Tesorero"),
                        dinero("Entradas"), dinero("Salidas"), dinero("Saldo según registro"), dinero("Saldo app Nu"),
                        dinero("Diferencia")), cajitas),
                new Reporte.Tabla("Por tesorero", List.of(texto("Tesorero"), numero("Nº cajitas"),
                        dinero("Saldo según registro"), dinero("Saldo app Nu"), dinero("Diferencia")), tesoreros),
                tablaMovimientos("Movimientos", t.movimientos())));
    }

    static Reporte resumen(ResumenService.Resumen r) {
        List<List<Object>> meses = new ArrayList<>();
        for (FilaResumenMensual f : r.meses()) {
            meses.add(List.of(f.mes(), f.fechaMes(), c(f.ahorroEsperado()), c(f.ahorroReal()),
                    c(f.prestamosEntregados()), c(f.capitalRecuperado()), c(f.intereses()), c(f.utilidadActividades()),
                    c(f.flujoNeto()), c(f.cajaDisponibleAcumulada()), c(f.fondoTotalAcumulado())));
        }
        ResumenService.Kpis k = r.kpis();
        List<List<Object>> kpis = List.of(
                List.of("Ahorro esperado del año", c(k.ahorroEsperadoAnual())),
                List.of("Ahorro real a la fecha", c(k.ahorroRealALaFecha())),
                List.of("Total prestado", c(k.totalPrestado())),
                List.of("Intereses proyectados", c(k.interesesProyectados())),
                List.of("Intereses recibidos", c(k.interesesRecibidos())),
                List.of("Saldo por cobrar de préstamos", c(k.saldoPorCobrar())),
                List.of("Utilidad esperada de actividades", c(k.utilidadEsperadaActividades())),
                List.of("Rendimiento proyectado cajitas Nu", c(k.rendimientoProyectadoNu())),
                List.of("Total a repartir al cierre (proyectado)", c(k.totalARepartirProyectado())),
                List.of("Saldo total en cajitas (registro)", c(k.saldoTotalCajitas())),
                List.of("Diferencia contra la app Nu", c(k.diferenciaNu())));
        return new Reporte("Resumen mensual y anual", "resumen", List.of(
                new Reporte.Tabla("Resumen mensual", List.of(numero("Mes #"), fecha("Mes"), dinero("Ahorro esperado"),
                        dinero("Ahorro real"), dinero("Préstamos entregados"), dinero("Capital recuperado"),
                        dinero("Intereses"), dinero("Utilidad actividades"), dinero("Flujo neto del mes"),
                        dinero("Caja disponible acumulada"), dinero("Fondo total acumulado")), meses),
                new Reporte.Tabla("KPIs anuales", List.of(texto("Indicador"), dinero("Valor")), kpis),
                new Reporte.Tabla("Rentabilidad", List.of(texto("Indicador"), porcentaje("Valor")),
                        List.of(List.of("Rentabilidad proyectada", k.rentabilidadProyectada())))));
    }

    private static Reporte.Tabla tablaConsolidado(String nombre, List<ReportesService.FilaConsolidado> filas) {
        List<List<Object>> datos = new ArrayList<>();
        for (ReportesService.FilaConsolidado f : filas) {
            datos.add(List.of(f.nombre(), nz(f.nombreTesorero()), c(f.cuotaMensual()), c(f.ahorroEsperado()),
                    c(f.totalAportado()), c(f.esperadoALaFecha()), c(f.atraso()), f.cumplimiento(), c(f.saldoCajita()),
                    f.prestamosVigentes(), c(f.saldoPrestamos()), f.cuotasVencidas(), c(f.boletasPendientes()),
                    c(f.interesesProyectados()), c(f.totalProyectado()), c(f.totalReal())));
        }
        return new Reporte.Tabla(nombre, List.of(texto("Integrante"), texto("Tesorero"), dinero("Cuota mensual"),
                dinero("Ahorro esperado"), dinero("Total aportado"), dinero("Esperado a la fecha"), dinero("Atraso"),
                porcentaje("% cumplimiento"), dinero("Saldo cajita"), numero("Préstamos vigentes"),
                dinero("Saldo deuda préstamos"), numero("Cuotas vencidas"), dinero("Boletas pendientes"),
                dinero("Intereses proyectados a recibir"), dinero("Total a recibir (proy.)"),
                dinero("Total a recibir (real)")), datos);
    }

    private static Reporte.Tabla tablaMovimientos(String nombre, List<LibroQueryPort.MovimientoVista> movimientos) {
        List<List<Object>> filas = new ArrayList<>();
        for (LibroQueryPort.MovimientoVista m : movimientos) {
            filas.add(lista(m.fecha(), m.nombreIntegrante(), nz(m.nombreTesorero()), m.tipo().etiqueta(),
                    nz(m.descripcion()), c(m.entrada()), c(m.salida())));
        }
        return new Reporte.Tabla(nombre, List.of(fecha("Fecha"), texto("Cajita de"), texto("Tesorero"), texto("Tipo"),
                texto("Descripción"), dinero("Entrada (+)"), dinero("Salida (−)")), filas);
    }

    private static java.math.BigDecimal c(Dinero d) {
        return d.aCentavos();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Como List.of pero admite valores nulos (celdas vacías). */
    private static List<Object> lista(Object... valores) {
        return java.util.Arrays.asList(valores);
    }
}

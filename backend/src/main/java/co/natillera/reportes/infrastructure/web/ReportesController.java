package co.natillera.reportes.infrastructure.web;

import co.natillera.prestamos.infrastructure.web.PrestamoController;
import co.natillera.reportes.application.ReportesService;
import co.natillera.reportes.domain.CalculadoraDeudores;
import co.natillera.reportes.domain.Reporte;
import co.natillera.reportes.infrastructure.export.ExportadorCsv;
import co.natillera.reportes.infrastructure.export.ExportadorReporte;
import co.natillera.reportes.infrastructure.export.ExportadorXlsx;
import co.natillera.resumen.application.ResumenService;
import co.natillera.resumen.infrastructure.web.ResumenController;
import co.natillera.shared.domain.Dinero;
import co.natillera.shared.domain.ReglaNegocioException;
import co.natillera.tesoreria.domain.TipoMovimiento;
import co.natillera.tesoreria.infrastructure.web.TesoreriaController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reportes bajo /api/v1/reportes. Cada uno responde JSON o, con ?formato=xlsx|csv, el archivo para descargar.
 */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReportesController {

    public record FilaConsolidadoDto(Long integranteId, String nombre, Long tesoreroId, String nombreTesorero,
                                     BigDecimal cuotaMensual, BigDecimal ahorroEsperado, BigDecimal totalAportado,
                                     BigDecimal esperadoALaFecha, BigDecimal atraso, BigDecimal cumplimiento,
                                     BigDecimal saldoCajita, int prestamosVigentes, BigDecimal saldoPrestamos,
                                     BigDecimal vencidoPrestamos, int cuotasVencidas, BigDecimal boletasPendientes,
                                     BigDecimal interesesProyectados, BigDecimal totalProyectado,
                                     BigDecimal totalReal) {
        static FilaConsolidadoDto de(ReportesService.FilaConsolidado f) {
            return new FilaConsolidadoDto(f.integranteId(), f.nombre(), f.tesoreroId(), f.nombreTesorero(),
                    c(f.cuotaMensual()), c(f.ahorroEsperado()), c(f.totalAportado()), c(f.esperadoALaFecha()),
                    c(f.atraso()), f.cumplimiento(), c(f.saldoCajita()), f.prestamosVigentes(), c(f.saldoPrestamos()),
                    c(f.vencidoPrestamos()), f.cuotasVencidas(), c(f.boletasPendientes()),
                    c(f.interesesProyectados()), c(f.totalProyectado()), c(f.totalReal()));
        }
    }

    public record DeudorDto(Long integranteId, String nombre, Long tesoreroId, String nombreTesorero,
                            BigDecimal atrasoAportes, BigDecimal saldoPrestamos, BigDecimal vencidoPrestamos,
                            BigDecimal boletasPendientes, BigDecimal total, boolean enMora) {
        static DeudorDto de(CalculadoraDeudores.Deudor d) {
            return new DeudorDto(d.integranteId(), d.nombre(), d.tesoreroId(), d.nombreTesorero(),
                    c(d.atrasoAportes()), c(d.saldoPrestamos()), c(d.vencidoPrestamos()), c(d.boletasPendientes()),
                    c(d.total()), d.enMora());
        }
    }

    public record BoletasDto(Long actividadId, String actividad, int boletas, BigDecimal valorAPagar,
                             BigDecimal pagado, BigDecimal pendiente) {
    }

    public record RepartoDto(BigDecimal ahorroEsperado, BigDecimal interesesProyectados,
                             BigDecimal actividadesProyectadas, BigDecimal rendimientoNuProyectado,
                             BigDecimal totalProyectado, BigDecimal ahorroReal, BigDecimal interesesReales,
                             BigDecimal actividadesReales, BigDecimal rendimientoNuReal, BigDecimal totalReal,
                             BigDecimal participacionProyectada, int puesto) {
    }

    public record EstadoCuentaDto(FilaConsolidadoDto resumen, List<BigDecimal> aportesPorMes,
                                  List<TesoreriaController.MovimientoDto> movimientos,
                                  List<PrestamoController.PrestamoDto> prestamos, List<BoletasDto> boletas,
                                  RepartoDto reparto) {
    }

    public record ReporteTesoreriaDto(List<TesoreriaController.CajitaDto> cajitas,
                                      List<TesoreriaController.TesoreroSaldoDto> porTesorero,
                                      List<TesoreriaController.MovimientoDto> movimientos) {
    }

    private final ReportesService reportes;
    private final ResumenService resumen;
    private final ResumenController resumenController;

    public ReportesController(ReportesService reportes, ResumenService resumen, ResumenController resumenController) {
        this.reportes = reportes;
        this.resumen = resumen;
        this.resumenController = resumenController;
    }

    @GetMapping("/consolidado")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public ResponseEntity<?> consolidado(@RequestParam(defaultValue = "json") String formato) {
        List<ReportesService.FilaConsolidado> filas = reportes.consolidado();
        return responder(formato, () -> filas.stream().map(FilaConsolidadoDto::de).toList(),
                () -> ReporteTablas.consolidado(filas));
    }

    @GetMapping("/deudores")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public ResponseEntity<?> deudores(@RequestParam(required = false) Long tesoreroId,
                                      @RequestParam(required = false) CalculadoraDeudores.TipoDeuda tipo,
                                      @RequestParam(defaultValue = "false") boolean soloMora,
                                      @RequestParam(defaultValue = "json") String formato) {
        List<CalculadoraDeudores.Deudor> deudores = reportes.deudores(tesoreroId, tipo, soloMora);
        return responder(formato, () -> deudores.stream().map(DeudorDto::de).toList(),
                () -> ReporteTablas.deudores(deudores));
    }

    @GetMapping("/estado-cuenta/{integranteId}")
    public ResponseEntity<?> estadoCuenta(@PathVariable Long integranteId,
                                          @RequestParam(defaultValue = "json") String formato) {
        ReportesService.EstadoCuenta e = reportes.estadoCuenta(integranteId);
        return responder(formato, () -> estadoCuentaDto(e), () -> ReporteTablas.estadoCuenta(e));
    }

    @GetMapping("/tesoreria")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public ResponseEntity<?> tesoreria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(defaultValue = "json") String formato) {
        ReportesService.ReporteTesoreria t = reportes.tesoreria(desde, hasta, tipo);
        return responder(formato, () -> new ReporteTesoreriaDto(
                t.cajitas().stream().map(TesoreriaController.CajitaDto::de).toList(),
                t.porTesorero().stream().map(TesoreriaController.TesoreroSaldoDto::de).toList(),
                t.movimientos().stream().map(TesoreriaController.MovimientoDto::de).toList()),
                () -> ReporteTablas.tesoreria(t));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resumen(@RequestParam(defaultValue = "json") String formato) {
        if ("json".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok(resumenController.resumen());
        }
        ResumenService.Resumen r = resumen.calcular();
        return responder(formato, () -> null, () -> ReporteTablas.resumen(r));
    }

    private ResponseEntity<?> responder(String formato, Supplier<Object> json, Supplier<Reporte> reporte) {
        ExportadorReporte exportador = switch (formato.toLowerCase()) {
            case "json" -> null;
            case "xlsx" -> new ExportadorXlsx();
            case "csv" -> new ExportadorCsv();
            default -> throw new ReglaNegocioException("Formato no soportado: use json, xlsx o csv");
        };
        if (exportador == null) {
            return ResponseEntity.ok(json.get());
        }
        Reporte r = reporte.get();
        byte[] contenido = exportador.exportar(r);
        String nombre = r.archivo() + "." + exportador.extension();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombre, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(exportador.contentType()))
                .body(contenido);
    }

    private static EstadoCuentaDto estadoCuentaDto(ReportesService.EstadoCuenta e) {
        var r = e.reparto();
        return new EstadoCuentaDto(FilaConsolidadoDto.de(e.resumen()),
                e.aportes() == null ? List.of()
                        : e.aportes().estado().aportesPorMes().stream().map(Dinero::aCentavos).toList(),
                e.movimientos().stream().map(TesoreriaController.MovimientoDto::de).toList(),
                e.prestamos().stream().map(PrestamoController::dto).toList(),
                e.boletas().stream().map(b -> new BoletasDto(b.actividadId(), b.actividad(), b.boletas(),
                        c(b.valorAPagar()), c(b.pagado()), c(b.pendiente()))).toList(),
                r == null ? null : new RepartoDto(c(r.ahorroEsperado()), c(r.interesesProyectados()),
                        c(r.actividadesProyectadas()), c(r.rendimientoNuProyectado()), c(r.totalProyectado()),
                        c(r.ahorroReal()), c(r.interesesReales()), c(r.actividadesReales()), c(r.rendimientoNuReal()),
                        c(r.totalReal()), r.participacionProyectada(), r.puesto()));
    }

    private static BigDecimal c(Dinero d) {
        return d.aCentavos();
    }
}

package co.natillera.tesoreria.infrastructure.web;

import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.TesoreriaService;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.domain.CalculadoraTesoreria;
import co.natillera.tesoreria.domain.Movimiento;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.Tesorero;
import co.natillera.tesoreria.domain.TipoMovimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tesoreria")
public class TesoreriaController {

    public record CajitaDto(Long cajitaId, String nombreCajita, Long integranteId, String nombreIntegrante,
                            Long tesoreroId, String nombreTesorero, BigDecimal entradas, BigDecimal salidas,
                            BigDecimal saldo, BigDecimal saldoApp, LocalDate fechaSaldoApp, BigDecimal diferencia,
                            boolean cuadra) {
        public static CajitaDto de(SaldoCajita s) {
            return new CajitaDto(s.cajitaId(), s.nombreCajita(), s.integranteId(), s.nombreIntegrante(),
                    s.tesoreroId(), s.nombreTesorero(), s.entradas().aCentavos(), s.salidas().aCentavos(),
                    s.saldo().aCentavos(), s.saldoApp() == null ? null : s.saldoApp().aCentavos(), s.fechaSaldoApp(),
                    s.diferencia().aCentavos(), s.diferencia().esCero());
        }
    }

    public record TesoreroSaldoDto(Long tesoreroId, String nombre, int numeroCajitas, BigDecimal saldoRegistro,
                                   BigDecimal saldoApp, BigDecimal diferencia) {
        public static TesoreroSaldoDto de(CalculadoraTesoreria.SaldoTesorero t) {
            return new TesoreroSaldoDto(t.tesoreroId(), t.nombreTesorero(), t.numeroCajitas(),
                    t.saldoRegistro().aCentavos(), t.saldoApp().aCentavos(), t.diferencia().aCentavos());
        }
    }

    public record MovimientoDto(Long id, LocalDate fecha, Long cajitaId, String nombreCajita, Long integranteId,
                                String nombreIntegrante, Long tesoreroId, String nombreTesorero, TipoMovimiento tipo,
                                String tipoEtiqueta, String descripcion, BigDecimal entrada, BigDecimal salida,
                                Long prestamoId, Long actividadId, Long anulaA, Long anuladoPor, String creadoPor,
                                OffsetDateTime creadoEn) {
        public static MovimientoDto de(LibroQueryPort.MovimientoVista m) {
            return new MovimientoDto(m.id(), m.fecha(), m.cajitaId(), m.nombreCajita(), m.integranteId(),
                    m.nombreIntegrante(), m.tesoreroId(), m.nombreTesorero(), m.tipo(), m.tipo().etiqueta(),
                    m.descripcion(), m.entrada().aCentavos(), m.salida().aCentavos(), m.prestamoId(),
                    m.actividadId(), m.anulaA(), m.anuladoPor(), m.creadoPor(), m.creadoEn());
        }
    }

    public record MovimientoCreadoDto(Long id, Long cajitaId, LocalDate fecha, TipoMovimiento tipo,
                                      BigDecimal entrada, BigDecimal salida) {
        static MovimientoCreadoDto de(Movimiento m) {
            return new MovimientoCreadoDto(m.id(), m.cajitaId(), m.fecha(), m.tipo(), m.entrada().aCentavos(),
                    m.salida().aCentavos());
        }
    }

    public record TotalTipoDto(TipoMovimiento tipo, String etiqueta, BigDecimal entradas, BigDecimal salidas) {
    }

    public record ResumenMesDto(int mes, BigDecimal entradas, BigDecimal salidas, BigDecimal saldoAcumulado,
                                Map<Long, BigDecimal> saldoAcumuladoPorTesorero) {
    }

    public record ResumenDto(List<TotalTipoDto> porTipo, List<ResumenMesDto> porMes,
                             List<TesoreroDto> tesoreros, List<TesoreroSaldoDto> porTesorero) {
    }

    public record TesoreroDto(Long id, String nombre) {
        static TesoreroDto de(Tesorero t) {
            return new TesoreroDto(t.id(), t.nombre());
        }
    }

    public record RegistrarMovimientoRequest(
            @NotNull(message = "Seleccione la cajita") Long cajitaId,
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            @NotNull(message = "Seleccione el tipo") TipoMovimiento tipo,
            @Size(max = 300) String descripcion,
            @NotNull @Positive(message = "El valor debe ser mayor que cero") BigDecimal valor) {
    }

    public record AporteLoteItem(@NotNull Long integranteId, @NotNull @PositiveOrZero BigDecimal monto) {
    }

    public record AportesLoteRequest(@NotNull(message = "La fecha es obligatoria") LocalDate fecha,
                                     @Size(max = 300) String descripcion,
                                     @NotEmpty(message = "Indique al menos un aporte") List<@Valid AporteLoteItem> aportes) {
    }

    public record AnularRequest(@NotBlank(message = "Indique el motivo") @Size(max = 300) String motivo) {
    }

    public record SaldoReportadoRequest(@NotNull LocalDate fecha, @NotNull @PositiveOrZero BigDecimal saldoApp) {
    }

    private final TesoreriaService service;

    public TesoreriaController(TesoreriaService service) {
        this.service = service;
    }

    @GetMapping("/cajitas")
    public List<CajitaDto> cajitas() {
        return service.cajitas().stream().map(CajitaDto::de).toList();
    }

    @GetMapping("/tesoreros")
    public List<TesoreroDto> tesoreros() {
        return service.tesoreros().stream().map(TesoreroDto::de).toList();
    }

    @GetMapping("/movimientos")
    public List<MovimientoDto> movimientos(@RequestParam(required = false) Long cajitaId,
                                           @RequestParam(required = false) TipoMovimiento tipo,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.movimientos(cajitaId, tipo, desde, hasta).stream().map(MovimientoDto::de).toList();
    }

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        TesoreriaService.Resumen r = service.resumen();
        return new ResumenDto(
                r.porTipo().stream().map(t -> new TotalTipoDto(t.tipo(), t.tipo().etiqueta(),
                        t.entradas().aCentavos(), t.salidas().aCentavos())).toList(),
                r.porMes().stream().map(m -> new ResumenMesDto(m.mes(), m.entradas().aCentavos(),
                        m.salidas().aCentavos(), m.saldoAcumulado().aCentavos(),
                        m.saldoAcumuladoPorTesorero().entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey, e -> e.getValue().aCentavos())))).toList(),
                r.tesoreros().stream().map(TesoreroDto::de).toList(),
                service.porTesorero().stream().map(TesoreroSaldoDto::de).toList());
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public MovimientoCreadoDto registrar(@Valid @RequestBody RegistrarMovimientoRequest r) {
        return MovimientoCreadoDto.de(service.registrarMovimiento(r.cajitaId(), r.fecha(), r.tipo(), r.descripcion(),
                Dinero.de(r.valor())));
    }

    @PostMapping("/aportes-lote")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public List<MovimientoCreadoDto> aportesLote(@Valid @RequestBody AportesLoteRequest r) {
        return service.registrarAportesLote(r.fecha(), r.descripcion(), r.aportes().stream()
                        .map(a -> new TesoreriaService.AporteLote(a.integranteId(), Dinero.de(a.monto()))).toList())
                .stream().map(MovimientoCreadoDto::de).toList();
    }

    @PostMapping("/movimientos/{id}/anular")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public MovimientoCreadoDto anular(@PathVariable Long id, @Valid @RequestBody AnularRequest r) {
        return MovimientoCreadoDto.de(service.anularMovimiento(id, r.motivo()));
    }

    @PostMapping("/cajitas/{id}/saldo-reportado")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public ResponseEntity<Void> saldoReportado(@PathVariable Long id, @Valid @RequestBody SaldoReportadoRequest r) {
        service.registrarSaldoReportado(id, r.fecha(), Dinero.de(r.saldoApp()));
        return ResponseEntity.noContent().build();
    }
}

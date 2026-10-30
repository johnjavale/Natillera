package co.natillera.prestamos.infrastructure.web;

import co.natillera.prestamos.application.PrestamoService;
import co.natillera.prestamos.domain.CuotaPlan;
import co.natillera.prestamos.domain.EstadoPrestamo;
import co.natillera.prestamos.domain.PagoCuota;
import co.natillera.prestamos.domain.Prestamo;
import co.natillera.prestamos.domain.ResumenPrestamo;
import co.natillera.shared.domain.Dinero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
public class PrestamoController {

    public record CuotaDto(int numero, int mesVencimiento, BigDecimal capital, BigDecimal interes, BigDecimal valor,
                           boolean pagada, boolean vencida) {
    }

    public record PagoDto(Long id, int numeroCuota, LocalDate fecha, BigDecimal monto, Long cajitaDestinoId,
                          boolean anulado, String motivoAnulacion) {
        static PagoDto de(PagoCuota p) {
            return new PagoDto(p.id(), p.numeroCuota(), p.fecha(), p.monto().aCentavos(), p.cajitaDestinoId(),
                    p.anulado(), p.motivoAnulacion());
        }
    }

    public record PrestamoDto(Long id, Long integranteId, String nombreIntegrante, Long cajitaOrigenId,
                              LocalDate fechaDesembolso, BigDecimal monto, int mesDelPrestamo, int numeroCuotas,
                              BigDecimal tasaMensual, BigDecimal interesPorCuota, BigDecimal abonoCapitalPorCuota,
                              BigDecimal valorCuota, BigDecimal totalIntereses, BigDecimal totalAPagar,
                              int cuotasPagadas, BigDecimal interesesRecibidos, BigDecimal saldoPendiente,
                              int cuotasVencidas, BigDecimal saldoVencido, EstadoPrestamo estado,
                              List<CuotaDto> plan, List<PagoDto> pagos) {
    }

    public record PrestamoRequest(
            @NotNull(message = "Seleccione el integrante") Long integranteId,
            @NotNull(message = "Seleccione la cajita de origen") Long cajitaOrigenId,
            @NotNull(message = "La fecha de desembolso es obligatoria") LocalDate fechaDesembolso,
            @NotNull(message = "El monto es obligatorio") BigDecimal monto,
            @Min(1) @Max(12) Integer mesDelPrestamo,
            @Min(value = 1, message = "Mínimo 1 cuota") int numeroCuotas,
            @DecimalMin("0") @DecimalMax("1") BigDecimal tasaMensual) {
    }

    public record PagoRequest(@NotNull(message = "La fecha es obligatoria") LocalDate fecha, Long cajitaDestinoId,
                              @Min(1) @Max(12) Integer numeroCuota, @Positive BigDecimal monto) {
    }

    public record AnularPagoRequest(@NotBlank(message = "Indique el motivo") @Size(max = 300) String motivo) {
    }

    public record FilaProyeccionDto(Long prestamoId, String nombreIntegrante, List<BigDecimal> interesesPorMes) {
    }

    public record ProyeccionDto(List<FilaProyeccionDto> filas, List<BigDecimal> totalesPorMes, BigDecimal total) {
    }

    private final PrestamoService service;

    public PrestamoController(PrestamoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrestamoDto> listar() {
        return service.listar().stream().map(PrestamoController::dto).toList();
    }

    @GetMapping("/{id}")
    public PrestamoDto obtener(@PathVariable Long id) {
        return dto(service.obtener(id));
    }

    @GetMapping("/proyeccion-intereses")
    public ProyeccionDto proyeccion() {
        PrestamoService.ProyeccionIntereses p = service.proyeccionIntereses();
        return new ProyeccionDto(
                p.filas().stream().map(f -> new FilaProyeccionDto(f.prestamoId(), f.nombreIntegrante(),
                        f.interesesPorMes().stream().map(Dinero::aCentavos).toList())).toList(),
                p.totalesPorMes().stream().map(Dinero::aCentavos).toList(), p.total().aCentavos());
    }

    @PostMapping("/simular")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public PrestamoDto simular(@Valid @RequestBody PrestamoRequest r) {
        return dto(new PrestamoService.PrestamoVista(service.simular(nuevo(r)), null, List.of()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public PrestamoDto crear(@Valid @RequestBody PrestamoRequest r) {
        return dto(service.crear(nuevo(r)));
    }

    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public PrestamoDto pagar(@PathVariable Long id, @Valid @RequestBody PagoRequest r) {
        return dto(service.registrarPago(id, new PrestamoService.NuevoPago(r.fecha(), r.cajitaDestinoId(),
                r.numeroCuota(), r.monto() == null ? null : Dinero.de(r.monto()))));
    }

    @PostMapping("/{id}/pagos/{pagoId}/anular")
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public PrestamoDto anularPago(@PathVariable Long id, @PathVariable Long pagoId,
                                  @Valid @RequestBody AnularPagoRequest r) {
        return dto(service.anularPago(id, pagoId, r.motivo()));
    }

    private static PrestamoService.NuevoPrestamo nuevo(PrestamoRequest r) {
        return new PrestamoService.NuevoPrestamo(r.integranteId(), r.cajitaOrigenId(), r.fechaDesembolso(),
                Dinero.de(r.monto()), r.mesDelPrestamo(), r.numeroCuotas(), r.tasaMensual());
    }

    public static PrestamoDto dto(PrestamoService.PrestamoVista v) {
        ResumenPrestamo r = v.resumen();
        Prestamo p = r.prestamo();
        List<CuotaDto> plan = r.plan().stream().map((CuotaPlan c) -> new CuotaDto(c.numero(), c.mesVencimiento(),
                c.capital().aCentavos(), c.interes().aCentavos(), c.valor().aCentavos(),
                r.numerosPagados().contains(c.numero()), r.numerosVencidos().contains(c.numero()))).toList();
        return new PrestamoDto(p.id(), p.integranteId(), v.nombreIntegrante(), p.cajitaOrigenId(), p.fechaDesembolso(),
                p.monto().aCentavos(), p.mesDelPrestamo(), p.numeroCuotas(), p.tasaMensual(),
                r.interesPorCuota().aCentavos(), r.abonoCapitalPorCuota().aCentavos(), r.valorCuota().aCentavos(),
                r.totalIntereses().aCentavos(), r.totalAPagar().aCentavos(), r.cuotasPagadas(),
                r.interesesRecibidos().aCentavos(), r.saldoPendiente().aCentavos(), r.cuotasVencidas(),
                r.saldoVencido().aCentavos(), r.estado(), plan, v.pagos().stream().map(PagoDto::de).toList());
    }
}

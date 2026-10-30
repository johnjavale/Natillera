package co.natillera.actividades.infrastructure.web;

import co.natillera.actividades.application.ActividadService;
import co.natillera.actividades.domain.Actividad;
import co.natillera.actividades.domain.ResumenActividad;
import co.natillera.shared.domain.Dinero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/actividades")
public class ActividadController {

    public record BoletasDto(Long integranteId, String nombre, int boletas, BigDecimal valorAPagar, BigDecimal pagado,
                             BigDecimal pendiente) {
    }

    public record ActividadDto(Long id, String nombre, LocalDate fechaSorteo, BigDecimal valorBoleta,
                               int boletasPorIntegrante, BigDecimal costoPremio, BigDecimal otrosGastos, String ganador,
                               int mesDeLaActividad, BigDecimal recaudoEsperado, BigDecimal recaudoReal,
                               BigDecimal costos, BigDecimal utilidadEsperada, BigDecimal utilidadReal,
                               BigDecimal utilidadEsperadaPorIntegrante, BigDecimal utilidadRealPorIntegrante,
                               List<BoletasDto> boletas) {
        static ActividadDto de(ResumenActividad r) {
            Actividad a = r.actividad();
            return new ActividadDto(a.id(), a.nombre(), a.fechaSorteo(), a.valorBoleta().aCentavos(),
                    a.boletasPorIntegrante(), a.costoPremio().aCentavos(), a.otrosGastos().aCentavos(), a.ganador(),
                    r.mesDeLaActividad(), r.recaudoEsperado().aCentavos(), r.recaudoReal().aCentavos(),
                    r.costos().aCentavos(), r.utilidadEsperada().aCentavos(), r.utilidadReal().aCentavos(),
                    r.utilidadEsperadaPorIntegrante().aCentavos(), r.utilidadRealPorIntegrante().aCentavos(),
                    r.boletas().stream().map(b -> new BoletasDto(b.integranteId(), b.nombre(), b.boletas(),
                            b.valorAPagar().aCentavos(), b.pagado().aCentavos(), b.pendiente().aCentavos())).toList());
        }
    }

    public record ActividadRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @NotNull(message = "La fecha del sorteo es obligatoria") LocalDate fechaSorteo,
            @NotNull @PositiveOrZero BigDecimal valorBoleta,
            @Min(0) @Max(1000) int boletasPorIntegrante,
            @NotNull @PositiveOrZero BigDecimal costoPremio,
            @NotNull @PositiveOrZero BigDecimal otrosGastos,
            @Size(max = 160) String ganador) {
    }

    public record BoletasRequest(@Min(0) @Max(1000) int boletas) {
    }

    public record PagoBoletasRequest(@NotNull(message = "Seleccione el integrante") Long integranteId,
                                     @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
                                     @NotNull @Positive(message = "El valor debe ser mayor que cero") BigDecimal monto) {
    }

    private final ActividadService service;

    public ActividadController(ActividadService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActividadDto> listar() {
        return service.listar().stream().map(ActividadDto::de).toList();
    }

    @GetMapping("/{id}")
    public ActividadDto obtener(@PathVariable Long id) {
        return ActividadDto.de(service.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ActividadDto crear(@Valid @RequestBody ActividadRequest r) {
        return ActividadDto.de(service.crear(datos(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ActividadDto actualizar(@PathVariable Long id, @Valid @RequestBody ActividadRequest r) {
        return ActividadDto.de(service.actualizar(id, datos(r)));
    }

    @PutMapping("/{id}/boletas/{integranteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ActividadDto boletas(@PathVariable Long id, @PathVariable Long integranteId,
                                @Valid @RequestBody BoletasRequest r) {
        return ActividadDto.de(service.asignarBoletas(id, integranteId, r.boletas()));
    }

    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TESORERO','ADMIN')")
    public ActividadDto pagar(@PathVariable Long id, @Valid @RequestBody PagoBoletasRequest r) {
        return ActividadDto.de(service.registrarPago(id, r.integranteId(), r.fecha(), Dinero.de(r.monto())));
    }

    private static ActividadService.DatosActividad datos(ActividadRequest r) {
        return new ActividadService.DatosActividad(r.nombre(), r.fechaSorteo(), Dinero.de(r.valorBoleta()),
                r.boletasPorIntegrante(), Dinero.de(r.costoPremio()), Dinero.de(r.otrosGastos()), r.ganador());
    }
}

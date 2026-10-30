package co.natillera.ciclo.infrastructure.web;

import co.natillera.ciclo.application.CicloService;
import co.natillera.ciclo.domain.Ciclo;
import co.natillera.ciclo.domain.EstadoCiclo;
import co.natillera.integrantes.application.IntegranteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ciclo")
public class CicloController {

    public record CicloDto(Long id, String nombre, LocalDate fechaInicio, int numeroMeses,
                           BigDecimal tasaInteresMensualPrestamos, BigDecimal gastosAdministrativos,
                           int maxCuotasPrestamo, BigDecimal tasaEACajitasNu, BigDecimal tasaMensualNu,
                           int maxActividades, boolean permitirPrestamoDespuesDelCierre, EstadoCiclo estado,
                           LocalDate fechaCierre, int numeroIntegrantes, int mesesTranscurridos, LocalDate hoy) {
    }

    public record ActualizarCicloRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
            @Min(1) @Max(12) int numeroMeses,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal tasaInteresMensualPrestamos,
            @NotNull @PositiveOrZero BigDecimal gastosAdministrativos,
            @Min(1) @Max(12) int maxCuotasPrestamo,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal tasaEACajitasNu,
            @Min(0) @Max(12) int maxActividades,
            boolean permitirPrestamoDespuesDelCierre) {
    }

    public record EstadoRequest(@NotNull EstadoCiclo estado) {
    }

    private final CicloService ciclos;
    private final IntegranteService integrantes;

    public CicloController(CicloService ciclos, IntegranteService integrantes) {
        this.ciclos = ciclos;
        this.integrantes = integrantes;
    }

    @GetMapping
    public CicloDto obtener() {
        return dto(ciclos.actual());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CicloDto actualizar(@Valid @RequestBody ActualizarCicloRequest r) {
        return dto(ciclos.actualizar(new CicloService.ActualizarCiclo(r.nombre(), r.fechaInicio(), r.numeroMeses(),
                r.tasaInteresMensualPrestamos(), r.gastosAdministrativos(), r.maxCuotasPrestamo(), r.tasaEACajitasNu(),
                r.maxActividades(), r.permitirPrestamoDespuesDelCierre())));
    }

    @PutMapping("/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public CicloDto estado(@Valid @RequestBody EstadoRequest r) {
        return dto(ciclos.cambiarEstado(r.estado()));
    }

    private CicloDto dto(Ciclo c) {
        LocalDate hoy = ciclos.hoy();
        return new CicloDto(c.id(), c.nombre(), c.fechaInicio(), c.numeroMeses(), c.tasaInteresMensualPrestamos(),
                c.gastosAdministrativos().aCentavos(), c.maxCuotasPrestamo(), c.tasaEACajitasNu(), c.tasaMensualNu(),
                c.maxActividades(), c.permitirPrestamoDespuesDelCierre(), c.estado(), c.fechaCierre(),
                integrantes.contarActivos(), c.mesesTranscurridos(hoy), hoy);
    }
}

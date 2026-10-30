package co.natillera.integrantes.infrastructure.web;

import co.natillera.integrantes.application.AportesService;
import co.natillera.integrantes.application.IntegranteService;
import co.natillera.integrantes.application.IntegranteVista;
import co.natillera.integrantes.domain.CalculadoraAportes;
import co.natillera.integrantes.domain.EstadoAportes;
import co.natillera.integrantes.domain.Integrante;
import co.natillera.shared.domain.Dinero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/integrantes")
public class IntegranteController {

    public record IntegranteDto(Long id, Long usuarioId, String nombre, String telefono, BigDecimal cuotaMensual,
                                Long tesoreroId, String nombreTesorero, Long cajitaId, String nombreCajitaNu,
                                boolean activo, BigDecimal ahorroEsperado, BigDecimal porcentajeDelTotal,
                                BigDecimal rendimientoProyectadoCajita, BigDecimal saldoCajita) {
        static IntegranteDto de(IntegranteVista v) {
            Integrante i = v.integrante();
            return new IntegranteDto(i.id(), i.usuarioId(), i.nombre(), i.telefono(), i.cuotaMensual().aCentavos(),
                    i.tesoreroId(), v.nombreTesorero(), v.cajitaId(), i.nombreCajitaNu(), i.activo(),
                    v.ahorroEsperado().aCentavos(), v.porcentajeDelTotal(), v.rendimientoProyectadoCajita().aCentavos(),
                    v.saldoCajita().aCentavos());
        }
    }

    public record EstadoAportesDto(Long integranteId, String nombre, BigDecimal cuotaMensual,
                                   List<BigDecimal> aportesPorMes, BigDecimal totalAportado, BigDecimal ahorroEsperado,
                                   BigDecimal diferencia, BigDecimal cumplimiento, BigDecimal esperadoALaFecha,
                                   BigDecimal atraso, boolean enAtraso) {
        static EstadoAportesDto de(AportesService.FilaAportes f) {
            EstadoAportes e = f.estado();
            return new EstadoAportesDto(f.integranteId(), f.nombre(), f.cuotaMensual().aCentavos(),
                    e.aportesPorMes().stream().map(Dinero::aCentavos).toList(), e.totalAportado().aCentavos(),
                    e.ahorroEsperado().aCentavos(), e.diferencia().aCentavos(), e.cumplimiento(),
                    e.esperadoALaFecha().aCentavos(), e.atraso().aCentavos(), e.enAtraso());
        }
    }

    public record TotalMesDto(int mes, BigDecimal totalReal, BigDecimal totalEsperado, BigDecimal acumuladoReal,
                              BigDecimal acumuladoEsperado) {
        static TotalMesDto de(CalculadoraAportes.TotalMes t) {
            return new TotalMesDto(t.mes(), t.totalReal().aCentavos(), t.totalEsperado().aCentavos(),
                    t.acumuladoReal().aCentavos(), t.acumuladoEsperado().aCentavos());
        }
    }

    public record MatrizAportesDto(int numeroMeses, int mesesTranscurridos, List<EstadoAportesDto> filas,
                                   List<TotalMesDto> totales) {
    }

    public record IntegranteRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @Pattern(regexp = "^$|^[0-9+ ]{7,20}$", message = "Teléfono inválido") String telefono,
            @NotNull @PositiveOrZero(message = "La cuota no puede ser negativa") BigDecimal cuotaMensual,
            Long tesoreroId,
            @Size(max = 160) String nombreCajitaNu,
            Boolean activo) {
    }

    public record CuotaRequest(@NotNull @PositiveOrZero(message = "La cuota no puede ser negativa") BigDecimal cuotaMensual) {
    }

    private final IntegranteService integrantes;
    private final AportesService aportes;

    public IntegranteController(IntegranteService integrantes, AportesService aportes) {
        this.integrantes = integrantes;
        this.aportes = aportes;
    }

    @GetMapping
    public List<IntegranteDto> listar() {
        return integrantes.listar().stream().map(IntegranteDto::de).toList();
    }

    @GetMapping("/{id}")
    public IntegranteDto obtener(@PathVariable Long id) {
        return IntegranteDto.de(integrantes.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public IntegranteDto crear(@Valid @RequestBody IntegranteRequest r) {
        return IntegranteDto.de(integrantes.crear(datos(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public IntegranteDto actualizar(@PathVariable Long id, @Valid @RequestBody IntegranteRequest r) {
        return IntegranteDto.de(integrantes.actualizar(id, datos(r)));
    }

    @PutMapping("/me/cuota")
    public IntegranteDto proponerCuota(@Valid @RequestBody CuotaRequest r) {
        return IntegranteDto.de(integrantes.proponerCuotaPropia(Dinero.de(r.cuotaMensual())));
    }

    @GetMapping("/aportes")
    public MatrizAportesDto matriz() {
        AportesService.MatrizAportes m = aportes.matriz();
        return new MatrizAportesDto(m.numeroMeses(), m.mesesTranscurridos(),
                m.filas().stream().map(EstadoAportesDto::de).toList(),
                m.totales().stream().map(TotalMesDto::de).toList());
    }

    @GetMapping("/{id}/aportes")
    public EstadoAportesDto aportesDe(@PathVariable Long id) {
        return EstadoAportesDto.de(aportes.deIntegrante(id));
    }

    private static IntegranteService.DatosIntegrante datos(IntegranteRequest r) {
        return new IntegranteService.DatosIntegrante(r.nombre(), r.telefono(), Dinero.de(r.cuotaMensual()),
                r.tesoreroId(), r.nombreCajitaNu(), r.activo() == null || r.activo());
    }
}

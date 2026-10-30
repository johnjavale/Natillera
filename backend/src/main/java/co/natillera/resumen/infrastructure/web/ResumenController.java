package co.natillera.resumen.infrastructure.web;

import co.natillera.resumen.application.ResumenService;
import co.natillera.resumen.domain.FilaResumenMensual;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resumen")
@PreAuthorize("hasRole('ADMIN')")
public class ResumenController {

    public record MesDto(int mes, LocalDate fechaMes, BigDecimal ahorroEsperado, BigDecimal ahorroReal,
                         BigDecimal prestamosEntregados, BigDecimal capitalRecuperado,
                         BigDecimal capitalRecuperadoReal, BigDecimal intereses, BigDecimal utilidadActividades,
                         BigDecimal flujoNeto, BigDecimal cajaDisponibleAcumulada, BigDecimal fondoTotalAcumulado,
                         boolean alertaCajaNegativa) {
        static MesDto de(FilaResumenMensual f) {
            return new MesDto(f.mes(), f.fechaMes(), f.ahorroEsperado().aCentavos(), f.ahorroReal().aCentavos(),
                    f.prestamosEntregados().aCentavos(), f.capitalRecuperado().aCentavos(),
                    f.capitalRecuperadoReal().aCentavos(), f.intereses().aCentavos(),
                    f.utilidadActividades().aCentavos(), f.flujoNeto().aCentavos(),
                    f.cajaDisponibleAcumulada().aCentavos(), f.fondoTotalAcumulado().aCentavos(),
                    f.alertaCajaNegativa());
        }
    }

    public record KpisDto(BigDecimal ahorroEsperadoAnual, BigDecimal ahorroRealALaFecha, BigDecimal totalPrestado,
                          BigDecimal interesesProyectados, BigDecimal interesesRecibidos, BigDecimal saldoPorCobrar,
                          BigDecimal utilidadEsperadaActividades, BigDecimal rendimientoProyectadoNu,
                          BigDecimal totalARepartirProyectado, BigDecimal rentabilidadProyectada,
                          BigDecimal saldoTotalCajitas, BigDecimal diferenciaNu) {
    }

    public record DistribucionDto(BigDecimal ahorro, BigDecimal intereses, BigDecimal actividades,
                                  BigDecimal rendimientoNu) {
    }

    public record ResumenDto(int mesesTranscurridos, List<MesDto> meses, KpisDto kpis, DistribucionDto distribucion,
                             boolean alertaCajaNegativa) {
    }

    private final ResumenService service;

    public ResumenController(ResumenService service) {
        this.service = service;
    }

    @GetMapping
    public ResumenDto resumen() {
        ResumenService.Resumen r = service.calcular();
        ResumenService.Kpis k = r.kpis();
        ResumenService.DistribucionReparto d = r.distribucion();
        return new ResumenDto(r.mesesTranscurridos(), r.meses().stream().map(MesDto::de).toList(),
                new KpisDto(k.ahorroEsperadoAnual().aCentavos(), k.ahorroRealALaFecha().aCentavos(),
                        k.totalPrestado().aCentavos(), k.interesesProyectados().aCentavos(),
                        k.interesesRecibidos().aCentavos(), k.saldoPorCobrar().aCentavos(),
                        k.utilidadEsperadaActividades().aCentavos(), k.rendimientoProyectadoNu().aCentavos(),
                        k.totalARepartirProyectado().aCentavos(), k.rentabilidadProyectada(),
                        k.saldoTotalCajitas().aCentavos(), k.diferenciaNu().aCentavos()),
                new DistribucionDto(d.ahorro().aCentavos(), d.intereses().aCentavos(), d.actividades().aCentavos(),
                        d.rendimientoNu().aCentavos()),
                r.meses().stream().anyMatch(FilaResumenMensual::alertaCajaNegativa));
    }
}

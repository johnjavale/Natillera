package co.natillera.reparticion.infrastructure.web;

import co.natillera.reparticion.application.ReparticionService;
import co.natillera.reparticion.domain.Reparticion;
import co.natillera.shared.domain.Dinero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reparticion")
public class ReparticionController {

    public record ConceptosDto(BigDecimal interesesPrestamos, BigDecimal gastosAdministrativos,
                               BigDecimal interesesNetos, BigDecimal utilidadActividades,
                               BigDecimal utilidadActividadesPorIntegrante) {
        static ConceptosDto de(Reparticion.Conceptos c) {
            return c == null ? null : new ConceptosDto(c.interesesPrestamos().aCentavos(),
                    c.gastosAdministrativos().aCentavos(), c.interesesNetos().aCentavos(),
                    c.utilidadActividades().aCentavos(), c.utilidadActividadesPorIntegrante().aCentavos());
        }
    }

    public record FilaDto(Long integranteId, String nombre, BigDecimal ahorroEsperado,
                          BigDecimal participacionProyectada, BigDecimal interesesProyectados,
                          BigDecimal actividadesProyectadas, BigDecimal rendimientoNuProyectado,
                          BigDecimal totalProyectado, BigDecimal ahorroReal, BigDecimal participacionReal,
                          BigDecimal interesesReales, BigDecimal actividadesReales, BigDecimal rendimientoNuReal,
                          BigDecimal totalReal, int puesto, int puestoReal) {
        static FilaDto de(Reparticion.Fila f) {
            return new FilaDto(f.integranteId(), f.nombre(), f.ahorroEsperado().aCentavos(),
                    f.participacionProyectada(), f.interesesProyectados().aCentavos(),
                    f.actividadesProyectadas().aCentavos(), f.rendimientoNuProyectado().aCentavos(),
                    f.totalProyectado().aCentavos(), f.ahorroReal().aCentavos(), f.participacionReal(),
                    f.interesesReales().aCentavos(), f.actividadesReales().aCentavos(),
                    f.rendimientoNuReal().aCentavos(), f.totalReal().aCentavos(), f.puesto(), f.puestoReal());
        }
    }

    public record ReparticionDto(ConceptosDto proyectado, ConceptosDto real, List<FilaDto> filas,
                                 BigDecimal totalProyectado, BigDecimal totalReal, String regla) {
    }

    private final ReparticionService service;

    public ReparticionController(ReparticionService service) {
        this.service = service;
    }

    @GetMapping
    public ReparticionDto ver() {
        ReparticionService.VistaReparticion v = service.ver();
        return new ReparticionDto(ConceptosDto.de(v.proyectado()), ConceptosDto.de(v.real()),
                v.filas().stream().map(FilaDto::de).toList(), centavos(v.totalProyectado()), centavos(v.totalReal()),
                "Intereses: proporcionales a lo ahorrado (ej.: quien ahorra el 20 % del total recibe el 20 % de los "
                        + "intereses). Actividades: partes iguales. Rendimiento Nu: lo que generó su propia cajita.");
    }

    private static BigDecimal centavos(Dinero d) {
        return d == null ? null : d.aCentavos();
    }
}

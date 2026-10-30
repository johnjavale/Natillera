package co.natillera.shared.infrastructure.web;

import co.natillera.shared.infrastructure.persistence.AuditoriaAdapter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    public record RegistroAuditoriaDto(Long id, String entidad, Long entidadId, String accion, String detalle,
                                       String usuario, OffsetDateTime fecha) {
    }

    public record PaginaAuditoriaDto(List<RegistroAuditoriaDto> registros, int pagina, int totalPaginas,
                                     long totalRegistros) {
    }

    private final AuditoriaAdapter auditoria;

    public AuditoriaController(AuditoriaAdapter auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public PaginaAuditoriaDto listar(@RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "50") int tamano) {
        var page = auditoria.listar(Math.max(0, pagina), Math.min(200, Math.max(1, tamano)));
        return new PaginaAuditoriaDto(page.map(a -> new RegistroAuditoriaDto(a.getId(), a.getEntidad(),
                a.getEntidadId(), a.getAccion(), a.getDetalle(), a.getUsuario(), a.getFecha())).getContent(),
                page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }
}

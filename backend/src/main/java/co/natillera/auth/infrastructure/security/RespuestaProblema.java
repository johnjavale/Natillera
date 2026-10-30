package co.natillera.auth.infrastructure.security;

import co.natillera.shared.infrastructure.web.ManejadorErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Escribe ProblemDetail desde filtros de seguridad (fuera del alcance de @RestControllerAdvice). */
@Component
public class RespuestaProblema {

    private final ObjectMapper mapper;

    public RespuestaProblema(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escribir(HttpServletResponse response, HttpStatus status, String titulo, String detalle, String tipo)
            throws IOException {
        ProblemDetail pd = ManejadorErrores.problema(status, titulo, detalle, tipo);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), pd);
    }
}

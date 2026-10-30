package co.natillera.shared.infrastructure.web;

import co.natillera.auth.application.DemasiadosIntentosException;
import co.natillera.auth.application.CredencialesInvalidasException;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import co.natillera.shared.domain.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Manejo centralizado de errores: respuestas RFC 7807 (ProblemDetail) en español. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public static ProblemDetail problema(HttpStatus status, String titulo, String detalle, String tipo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        pd.setType(URI.create("https://natillera.app/problemas/" + tipo));
        return pd;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", ex.getMessage(), "regla-negocio");
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, NoResourceFoundException.class})
    public ProblemDetail noEncontrado(Exception ex) {
        String detalle = ex instanceof RecursoNoEncontradoException ? ex.getMessage() : "Recurso no encontrado";
        return problema(HttpStatus.NOT_FOUND, "No encontrado", detalle, "no-encontrado");
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credenciales(CredencialesInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage(), "no-autenticado");
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException ex) {
        ProblemDetail pd = problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", ex.getMessage(),
                "demasiados-intentos");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.segundosEspera()))
                .body(pd);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ProblemDetail accesoDenegado(Exception ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permiso para realizar esta acción",
                "acceso-denegado");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "Revise los campos marcados", "validacion");
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail violacion(ConstraintViolationException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", ex.getMessage(), "validacion");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ProblemDetail solicitudInvalida(Exception ex) {
        return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "La solicitud tiene un formato inválido o le faltan datos", "solicitud-invalida");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return problema(HttpStatus.CONFLICT, "Conflicto",
                "La operación entra en conflicto con datos existentes (registro duplicado o referenciado)",
                "conflicto");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intente de nuevo más tarde.", "error-interno");
    }
}

package co.natillera.auth.infrastructure.security;

import co.natillera.auth.application.UsuarioActual;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Valida el Bearer token. Un token expirado responde 401 con tipo "token-expirado" (el BFF refresca y reintenta).
 * Si el usuario debe cambiar la contraseña, solo puede usar los endpoints de sesión.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDOS_CON_CAMBIO_PENDIENTE = Set.of(
            "/api/v1/auth/cambiar-password", "/api/v1/auth/me", "/api/v1/auth/logout");

    private final JwtTokenProvider tokens;
    private final RespuestaProblema problema;

    public JwtAuthenticationFilter(JwtTokenProvider tokens, RespuestaProblema problema) {
        this.tokens = tokens;
        this.problema = problema;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        UsuarioActual usuario;
        try {
            usuario = tokens.validar(header.substring(7));
        } catch (ExpiredJwtException e) {
            problema.escribir(response, HttpStatus.UNAUTHORIZED, "Sesión expirada", "El token de acceso expiró",
                    "token-expirado");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            problema.escribir(response, HttpStatus.UNAUTHORIZED, "No autenticado", "Token de acceso inválido",
                    "token-invalido");
            return;
        }
        if (usuario.debeCambiarPassword() && !PERMITIDOS_CON_CAMBIO_PENDIENTE.contains(request.getRequestURI())) {
            problema.escribir(response, HttpStatus.FORBIDDEN, "Cambio de contraseña obligatorio",
                    "Debe cambiar su contraseña antes de continuar", "cambio-password-obligatorio");
            return;
        }
        var authorities = usuario.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

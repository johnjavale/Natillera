package co.natillera.auth.infrastructure.security;

import co.natillera.auth.application.UsuarioActual;
import co.natillera.auth.application.port.out.TokenEmisorPort;
import co.natillera.auth.domain.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Emite y valida access tokens JWT HS256 (15 min). Claims: sub, roles, integranteId, tesoreroId. */
@Component
public class JwtTokenProvider implements TokenEmisorPort {

    private static final String ISSUER = "natillera-api";

    private final SecretKey clave;
    private final Duration duracion;
    private final Clock clock;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secreto,
                            @Value("${app.jwt.access-minutos:15}") int minutos, Clock clock) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes)");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    @Override
    public String emitirAccessToken(UsuarioActual u) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(u.usuarioId()))
                .claim("login", u.login())
                .claim("nombre", u.nombre())
                .claim("roles", u.roles().stream().map(Enum::name).sorted().toList())
                .claim("integranteId", u.integranteId())
                .claim("tesoreroId", u.tesoreroId())
                .claim("pwdChange", u.debeCambiarPassword())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public long duracionAccessTokenSegundos() {
        return duracion.toSeconds();
    }

    /** @throws io.jsonwebtoken.ExpiredJwtException si expiró; otra JwtException si es inválido */
    public UsuarioActual validar(String token) {
        Claims c = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
        List<?> lista = c.get("roles", List.class);
        if (lista != null) {
            lista.forEach(r -> roles.add(Rol.valueOf(r.toString())));
        }
        return new UsuarioActual(Long.valueOf(c.getSubject()), c.get("login", String.class),
                c.get("nombre", String.class), roles, aLong(c.get("integranteId")), aLong(c.get("tesoreroId")),
                Boolean.TRUE.equals(c.get("pwdChange", Boolean.class)));
    }

    private static Long aLong(Object valor) {
        return valor instanceof Number n ? n.longValue() : null;
    }
}

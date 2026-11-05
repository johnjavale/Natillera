package co.natillera.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected TokenRecuperacionJpaEntity() {
    }

    public TokenRecuperacionJpaEntity(Long usuarioId, String tokenHash, Instant expiraEn, Instant creadoEn) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.creadoEn = creadoEn;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public boolean isUsado() {
        return usado;
    }
}

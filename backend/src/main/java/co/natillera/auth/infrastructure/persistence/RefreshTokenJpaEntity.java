package co.natillera.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familia;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(Long usuarioId, String tokenHash, UUID familia, Instant expiraEn, Instant creadoEn) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.familia = familia;
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

    public UUID getFamilia() {
        return familia;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public boolean isUsado() {
        return usado;
    }

    public boolean isRevocado() {
        return revocado;
    }
}

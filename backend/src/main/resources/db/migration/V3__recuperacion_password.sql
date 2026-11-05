-- Enlaces de "¿Olvidó su contraseña?": se guarda solo el hash SHA-256 del token, de un solo uso y con expiración.
CREATE TABLE token_recuperacion (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_en  TIMESTAMPTZ NOT NULL,
    usado      BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion (usuario_id) WHERE NOT usado;

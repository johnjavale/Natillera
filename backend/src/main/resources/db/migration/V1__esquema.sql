-- Natillera Familiar — esquema inicial
-- Dinero: NUMERIC(14,2). Fechas de negocio: DATE. Marcas de tiempo: TIMESTAMPTZ.

CREATE TABLE usuario (
    id                    BIGSERIAL PRIMARY KEY,
    nombre                VARCHAR(120) NOT NULL,
    correo                VARCHAR(160) UNIQUE,
    celular               VARCHAR(20) UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_por            VARCHAR(160),
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modificado_por        VARCHAR(160),
    modificado_en         TIMESTAMPTZ,
    CONSTRAINT usuario_login_chk CHECK (correo IS NOT NULL OR celular IS NOT NULL)
);

CREATE TABLE rol (
    id     SMALLINT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO rol (id, nombre) VALUES (1, 'ADMIN'), (2, 'TESORERO'), (3, 'INTEGRANTE');

CREATE TABLE usuario_rol (
    usuario_id BIGINT   NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_id     SMALLINT NOT NULL REFERENCES rol (id),
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    familia    UUID        NOT NULL,
    expira_en  TIMESTAMPTZ NOT NULL,
    usado      BOOLEAN     NOT NULL DEFAULT FALSE,
    revocado   BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_familia ON refresh_token (familia);
CREATE INDEX idx_refresh_token_usuario ON refresh_token (usuario_id);

CREATE TABLE ciclo (
    id                                   BIGSERIAL PRIMARY KEY,
    nombre                               VARCHAR(120)  NOT NULL,
    fecha_inicio                         DATE          NOT NULL,
    numero_meses                         INTEGER       NOT NULL CHECK (numero_meses BETWEEN 1 AND 12),
    tasa_interes_mensual_prestamos       NUMERIC(8, 6) NOT NULL CHECK (tasa_interes_mensual_prestamos >= 0),
    gastos_administrativos               NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (gastos_administrativos >= 0),
    max_cuotas_prestamo                  INTEGER       NOT NULL CHECK (max_cuotas_prestamo BETWEEN 1 AND 12),
    tasa_ea_cajitas_nu                   NUMERIC(8, 6) NOT NULL CHECK (tasa_ea_cajitas_nu >= 0),
    max_actividades                      INTEGER       NOT NULL DEFAULT 2,
    permitir_prestamo_despues_del_cierre BOOLEAN       NOT NULL DEFAULT FALSE,
    estado                               VARCHAR(20)   NOT NULL DEFAULT 'INSCRIPCION',
    creado_por                           VARCHAR(160),
    creado_en                            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modificado_por                       VARCHAR(160),
    modificado_en                        TIMESTAMPTZ
);

INSERT INTO ciclo (nombre, fecha_inicio, numero_meses, tasa_interes_mensual_prestamos, gastos_administrativos,
                   max_cuotas_prestamo, tasa_ea_cajitas_nu, max_actividades, estado)
VALUES ('Natillera Familiar', DATE '2027-01-01', 12, 0.03, 0, 12, 0.09, 2, 'INSCRIPCION');

CREATE TABLE tesorero (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT UNIQUE REFERENCES usuario (id),
    nombre         VARCHAR(120) NOT NULL,
    creado_por     VARCHAR(160),
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modificado_por VARCHAR(160),
    modificado_en  TIMESTAMPTZ
);

CREATE TABLE integrante (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT UNIQUE REFERENCES usuario (id),
    nombre         VARCHAR(120)   NOT NULL,
    telefono       VARCHAR(20),
    cuota_mensual  NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (cuota_mensual >= 0),
    tesorero_id    BIGINT REFERENCES tesorero (id),
    activo         BOOLEAN        NOT NULL DEFAULT TRUE,
    creado_por     VARCHAR(160),
    creado_en      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por VARCHAR(160),
    modificado_en  TIMESTAMPTZ
);
CREATE INDEX idx_integrante_tesorero ON integrante (tesorero_id);

-- Una cajita Nu por integrante; pertenece a la cuenta del tesorero asignado al integrante.
CREATE TABLE cajita (
    id             BIGSERIAL PRIMARY KEY,
    integrante_id  BIGINT       NOT NULL UNIQUE REFERENCES integrante (id),
    nombre         VARCHAR(160) NOT NULL,
    creado_por     VARCHAR(160),
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modificado_por VARCHAR(160),
    modificado_en  TIMESTAMPTZ
);

CREATE TABLE prestamo (
    id                BIGSERIAL PRIMARY KEY,
    integrante_id     BIGINT         NOT NULL REFERENCES integrante (id),
    cajita_origen_id  BIGINT         NOT NULL REFERENCES cajita (id),
    fecha_desembolso  DATE           NOT NULL,
    monto             NUMERIC(14, 2) NOT NULL CHECK (monto > 0),
    mes_del_prestamo  INTEGER        NOT NULL CHECK (mes_del_prestamo BETWEEN 1 AND 12),
    numero_cuotas     INTEGER        NOT NULL CHECK (numero_cuotas BETWEEN 1 AND 12),
    tasa_mensual      NUMERIC(8, 6)  NOT NULL CHECK (tasa_mensual >= 0),
    creado_por        VARCHAR(160),
    creado_en         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por    VARCHAR(160),
    modificado_en     TIMESTAMPTZ
);
CREATE INDEX idx_prestamo_integrante ON prestamo (integrante_id);

CREATE TABLE pago_cuota (
    id                BIGSERIAL PRIMARY KEY,
    prestamo_id       BIGINT         NOT NULL REFERENCES prestamo (id),
    numero_cuota      INTEGER        NOT NULL CHECK (numero_cuota >= 1),
    fecha             DATE           NOT NULL,
    monto             NUMERIC(14, 2) NOT NULL CHECK (monto > 0),
    cajita_destino_id BIGINT         NOT NULL REFERENCES cajita (id),
    anulado           BOOLEAN        NOT NULL DEFAULT FALSE,
    motivo_anulacion  VARCHAR(300),
    creado_por        VARCHAR(160),
    creado_en         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por    VARCHAR(160),
    modificado_en     TIMESTAMPTZ
);
CREATE INDEX idx_pago_cuota_prestamo ON pago_cuota (prestamo_id);
CREATE UNIQUE INDEX uq_pago_cuota_vigente ON pago_cuota (prestamo_id, numero_cuota) WHERE NOT anulado;

CREATE TABLE actividad (
    id                     BIGSERIAL PRIMARY KEY,
    nombre                 VARCHAR(120)   NOT NULL,
    fecha_sorteo           DATE           NOT NULL,
    valor_boleta           NUMERIC(14, 2) NOT NULL CHECK (valor_boleta >= 0),
    boletas_por_integrante INTEGER        NOT NULL CHECK (boletas_por_integrante >= 0),
    costo_premio           NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (costo_premio >= 0),
    otros_gastos           NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (otros_gastos >= 0),
    ganador                VARCHAR(160),
    creado_por             VARCHAR(160),
    creado_en              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por         VARCHAR(160),
    modificado_en          TIMESTAMPTZ
);

-- Boletas asignadas cuando difieren del valor por defecto de la actividad.
CREATE TABLE asignacion_boletas (
    actividad_id  BIGINT   NOT NULL REFERENCES actividad (id) ON DELETE CASCADE,
    integrante_id BIGINT   NOT NULL REFERENCES integrante (id),
    boletas       INTEGER  NOT NULL CHECK (boletas >= 0),
    PRIMARY KEY (actividad_id, integrante_id)
);

-- Libro de movimientos: fuente única de verdad del dinero. Nunca se borra; se anula con contramovimientos.
CREATE TABLE movimiento (
    id               BIGSERIAL PRIMARY KEY,
    cajita_id        BIGINT         NOT NULL REFERENCES cajita (id),
    fecha            DATE           NOT NULL,
    tipo             VARCHAR(30)    NOT NULL,
    descripcion      VARCHAR(300),
    entrada          NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (entrada >= 0),
    salida           NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (salida >= 0),
    prestamo_id      BIGINT REFERENCES prestamo (id),
    pago_cuota_id    BIGINT REFERENCES pago_cuota (id),
    actividad_id     BIGINT REFERENCES actividad (id),
    anula_a          BIGINT UNIQUE REFERENCES movimiento (id),
    motivo_anulacion VARCHAR(300),
    creado_por       VARCHAR(160),
    creado_en        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por   VARCHAR(160),
    modificado_en    TIMESTAMPTZ,
    CONSTRAINT movimiento_un_lado_chk CHECK ((entrada > 0 AND salida = 0) OR (salida > 0 AND entrada = 0))
);
CREATE INDEX idx_movimiento_cajita_fecha ON movimiento (cajita_id, fecha);
CREATE INDEX idx_movimiento_tipo ON movimiento (tipo);
CREATE INDEX idx_movimiento_actividad ON movimiento (actividad_id) WHERE actividad_id IS NOT NULL;
CREATE INDEX idx_movimiento_pago_cuota ON movimiento (pago_cuota_id) WHERE pago_cuota_id IS NOT NULL;

CREATE TABLE saldo_reportado_nu (
    id             BIGSERIAL PRIMARY KEY,
    cajita_id      BIGINT         NOT NULL REFERENCES cajita (id),
    fecha          DATE           NOT NULL,
    saldo_app      NUMERIC(14, 2) NOT NULL CHECK (saldo_app >= 0),
    creado_por     VARCHAR(160),
    creado_en      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    modificado_por VARCHAR(160),
    modificado_en  TIMESTAMPTZ
);
CREATE INDEX idx_saldo_reportado_cajita_fecha ON saldo_reportado_nu (cajita_id, fecha DESC, id DESC);

-- Solo cambios sensibles (cuotas, tasas, anulaciones, roles, cierre).
CREATE TABLE auditoria (
    id         BIGSERIAL PRIMARY KEY,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id BIGINT,
    accion     VARCHAR(40)  NOT NULL,
    detalle    VARCHAR(1000),
    usuario    VARCHAR(160),
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha DESC);

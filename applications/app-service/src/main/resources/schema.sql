-- CrediYa: base de datos relacional, según el diagrama "BD relacional" de la guía.
-- Autenticación: rol, usuario. Solicitudes: estados, tipo_prestamo, solicitud.
-- Spring la ejecuta al arrancar (spring.sql.init); todo es idempotente.

-- ===================== Autenticación =====================

CREATE TABLE IF NOT EXISTS rol (
    id_rol      SMALLINT     PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario          BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(100)  NOT NULL,
    apellido            VARCHAR(100)  NOT NULL,
    email               VARCHAR(150)  NOT NULL UNIQUE,
    documento_identidad VARCHAR(20)   NOT NULL UNIQUE,
    telefono            VARCHAR(20),
    fecha_nacimiento    DATE,                                -- HU1
    direccion           VARCHAR(200),                        -- HU1
    salario_base        NUMERIC(12, 2) NOT NULL,
    id_rol              SMALLINT      NOT NULL REFERENCES rol (id_rol),
    contrasena          VARCHAR(100)  NOT NULL,              -- HU3: hash BCrypt, nunca la clave en texto plano
    CONSTRAINT ck_usuario_salario_base CHECK (salario_base BETWEEN 0 AND 15000000)
);

-- ===================== Solicitudes =====================

CREATE TABLE IF NOT EXISTS estados (
    id_estado   SMALLINT     PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS tipo_prestamo (
    id_tipo_prestamo      BIGSERIAL      PRIMARY KEY,
    nombre                VARCHAR(100)   NOT NULL UNIQUE,
    monto_minimo          NUMERIC(15, 2) NOT NULL,
    monto_maximo          NUMERIC(15, 2) NOT NULL,
    tasa_interes          NUMERIC(5, 2)  NOT NULL,           -- porcentaje mensual, p. ej. 1.80 = 1,8 %
    validacion_automatica BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_tipo_prestamo_montos CHECK (monto_minimo > 0 AND monto_minimo <= monto_maximo),
    CONSTRAINT ck_tipo_prestamo_tasa CHECK (tasa_interes >= 0)
);

-- El email une la solicitud con el usuario. No es llave foránea porque en el diagrama
-- usuario y solicitud pertenecen a microservicios distintos.
CREATE TABLE IF NOT EXISTS solicitud (
    id_solicitud     BIGSERIAL      PRIMARY KEY,
    monto            NUMERIC(15, 2) NOT NULL,
    plazo            INTEGER        NOT NULL,                -- en meses
    email            VARCHAR(150)   NOT NULL,
    id_estado        SMALLINT       NOT NULL REFERENCES estados (id_estado),
    id_tipo_prestamo BIGINT         NOT NULL REFERENCES tipo_prestamo (id_tipo_prestamo),
    CONSTRAINT ck_solicitud_monto CHECK (monto > 0),
    CONSTRAINT ck_solicitud_plazo CHECK (plazo > 0)
);

CREATE INDEX IF NOT EXISTS ix_solicitud_email ON solicitud (email);
CREATE INDEX IF NOT EXISTS ix_solicitud_estado ON solicitud (id_estado);

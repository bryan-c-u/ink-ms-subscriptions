-- Script de referencia para la base de datos del microservicio de Suscripciones.
-- NO se ejecuta automaticamente (no hay Flyway/Liquibase en el proyecto, igual que en
-- ink-ms-auth e ink-ms-users): el esquema real lo crea Hibernate via
-- spring.jpa.hibernate.ddl-auto=update. Este archivo documenta el esquema esperado.
--
-- Diferencia frente al documento de diseno original: organizador_id, usuario_id y
-- evento_id se definieron aqui como BIGINT, pero ink-ms-users usa ids UUID (CHAR/VARCHAR),
-- no numericos, y el JWT solo expone el email del usuario autenticado (no un id numerico).
-- Por eso organizador_id/usuario_id se guardan como VARCHAR(100) con el EMAIL del usuario
-- (el unico identificador estable disponible sin llamar a otro microservicio), y evento_id
-- como VARCHAR(36) para el futuro id UUID de ink-ms-eventos. El resto del script se dejo
-- identico al documento de diseno entregado.

CREATE DATABASE IF NOT EXISTS inklusport_subscriptions;
USE inklusport_subscriptions;

CREATE TABLE plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL,
    limite_eventos_mes INT NOT NULL,
    porcentaje_comision DECIMAL(5,2) NOT NULL,
    duracion_dias INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE beneficio_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    beneficio VARCHAR(255) NOT NULL,

    CONSTRAINT fk_beneficio_plan
    FOREIGN KEY (plan_id)
    REFERENCES plan(id)
);

CREATE TABLE suscripcion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    organizador_id VARCHAR(100) NOT NULL, -- email del organizador (ver nota arriba)

    plan_id BIGINT NOT NULL,

    fecha_inicio DATE NOT NULL,

    fecha_fin DATE NOT NULL,

    estado ENUM(
        'ACTIVA',
        'VENCIDA',
        'CANCELADA',
        'SUSPENDIDA'
    ) NOT NULL,

    eventos_creados_mes INT DEFAULT 0,

    renovacion_automatica BOOLEAN DEFAULT FALSE,

    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_suscripcion_plan
    FOREIGN KEY (plan_id)
    REFERENCES plan(id)
);

CREATE TABLE historial_suscripcion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    suscripcion_id BIGINT NOT NULL,

    tipo_movimiento ENUM(
        'CREACION',
        'RENOVACION',
        'CAMBIO_PLAN',
        'CANCELACION'
    ) NOT NULL,

    plan_anterior_id BIGINT NULL,

    plan_nuevo_id BIGINT NULL,

    fecha_movimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_suscripcion
    FOREIGN KEY (suscripcion_id)
    REFERENCES suscripcion(id)
);

CREATE TABLE pago_suscripcion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    suscripcion_id BIGINT NOT NULL,

    monto DECIMAL(10,2) NOT NULL,

    metodo_pago VARCHAR(50),

    referencia_transaccion VARCHAR(150),

    estado ENUM(
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO'
    ) NOT NULL,

    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pago_suscripcion
    FOREIGN KEY (suscripcion_id)
    REFERENCES suscripcion(id)
);

CREATE TABLE configuracion_evento_pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    evento_id VARCHAR(36) NOT NULL, -- id UUID del futuro ink-ms-eventos (ver nota arriba)

    organizador_id VARCHAR(100) NOT NULL, -- email del organizador (ver nota arriba)

    es_pago BOOLEAN DEFAULT FALSE,

    valor_inscripcion DECIMAL(10,2),

    porcentaje_comision DECIMAL(5,2),

    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pago_evento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    usuario_id VARCHAR(100) NOT NULL, -- email del usuario (ver nota arriba)

    evento_id VARCHAR(36) NOT NULL, -- id UUID del futuro ink-ms-eventos (ver nota arriba)

    monto DECIMAL(10,2) NOT NULL,

    metodo_pago VARCHAR(50),

    referencia_transaccion VARCHAR(150),

    estado ENUM(
        'PENDIENTE',
        'APROBADO',
        'RECHAZADO'
    ) NOT NULL,

    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comprobante_pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    pago_evento_id BIGINT NULL,

    pago_suscripcion_id BIGINT NULL,

    numero_comprobante VARCHAR(100) UNIQUE NOT NULL,

    url_pdf VARCHAR(500),

    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comprobante_evento
    FOREIGN KEY (pago_evento_id)
    REFERENCES pago_evento(id),

    CONSTRAINT fk_comprobante_suscripcion
    FOREIGN KEY (pago_suscripcion_id)
    REFERENCES pago_suscripcion(id)
);

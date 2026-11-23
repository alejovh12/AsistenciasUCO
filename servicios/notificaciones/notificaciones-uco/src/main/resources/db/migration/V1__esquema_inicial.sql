-- Esquema inicial de notificacionesdb: catalogo de tipos y marcadores, plantillas versionadas y cola de envio.
-- Componente autonomo: no comparte tablas ni procedimientos con la BD de Asistencias.

CREATE TABLE dbo.EstadoNotificacion (
    codigo     VARCHAR(30)  NOT NULL,
    nombre     NVARCHAR(60) NOT NULL,
    esTerminal BIT          NOT NULL,
    CONSTRAINT PK_EstadoNotificacion PRIMARY KEY CLUSTERED (codigo)
);

INSERT INTO dbo.EstadoNotificacion (codigo, nombre, esTerminal) VALUES
    ('PENDIENTE', N'Pendiente', 0),
    ('RESERVADA', N'Reservada', 0),
    ('FALLIDA_REINTENTABLE', N'Fallida reintentable', 0),
    ('ENTREGADA', N'Entregada', 1),
    ('FALLIDA_DEFINITIVA', N'Fallida definitiva', 1);

-- Tipos que el servicio sabe entregar. interno = 1: tipos que solo emite el propio servicio (correo de prueba generico).
CREATE TABLE dbo.TipoNotificacion (
    tipo        VARCHAR(60)   NOT NULL,
    descripcion NVARCHAR(300) NOT NULL,
    interno     BIT           NOT NULL CONSTRAINT DF_TipoNotificacion_interno DEFAULT 0,
    CONSTRAINT PK_TipoNotificacion PRIMARY KEY CLUSTERED (tipo),
    CONSTRAINT CK_TipoNotificacion_tipo CHECK (LEN(tipo) BETWEEN 3 AND 60 AND tipo NOT LIKE '%[^A-Z0-9_]%')
);

-- Marcadores {nombre} permitidos por tipo, con su descripcion y un ejemplo para vistas previas y correos de prueba.
CREATE TABLE dbo.TipoNotificacionMarcador (
    id          UNIQUEIDENTIFIER NOT NULL,
    tipo        VARCHAR(60)      NOT NULL,
    marcador    VARCHAR(40)      NOT NULL,
    descripcion NVARCHAR(200)    NOT NULL,
    ejemplo     NVARCHAR(300)    NOT NULL,
    obligatorio BIT              NOT NULL,
    CONSTRAINT PK_TipoNotificacionMarcador PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_TipoNotificacionMarcador_tipo FOREIGN KEY (tipo) REFERENCES dbo.TipoNotificacion (tipo),
    CONSTRAINT UQ_TipoNotificacionMarcador UNIQUE (tipo, marcador),
    CONSTRAINT CK_TipoNotificacionMarcador_marcador CHECK (marcador LIKE '[a-zA-Z]%' AND marcador NOT LIKE '%[^a-zA-Z0-9]%')
);

-- Versiones de plantilla por tipo e idioma. La version 1 es la de fabrica (respaldo del renderizado); solo una activa.
CREATE TABLE dbo.PlantillaNotificacion (
    id        UNIQUEIDENTIFIER  NOT NULL,
    tipo      VARCHAR(60)       NOT NULL,
    idioma    CHAR(2)           NOT NULL CONSTRAINT DF_PlantillaNotificacion_idioma DEFAULT 'es',
    version   INT               NOT NULL,
    asunto    NVARCHAR(200)     NOT NULL,
    cuerpo    NVARCHAR(4000)    NOT NULL,
    activa    BIT               NOT NULL,
    creadoPor NVARCHAR(100)     NULL,
    creadoEn  DATETIMEOFFSET(7) NOT NULL,
    CONSTRAINT PK_PlantillaNotificacion PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_PlantillaNotificacion_tipo FOREIGN KEY (tipo) REFERENCES dbo.TipoNotificacion (tipo),
    CONSTRAINT UQ_PlantillaNotificacion_version UNIQUE (tipo, idioma, version),
    CONSTRAINT CK_PlantillaNotificacion_version CHECK (version >= 1)
);

CREATE UNIQUE NONCLUSTERED INDEX UX_PlantillaNotificacion_activa
    ON dbo.PlantillaNotificacion (tipo, idioma) WHERE activa = 1;

-- Cola de envio. (aplicacion, idempotencyKey) es unica: un reintento del cliente nunca duplica el envio.
CREATE TABLE dbo.Notificacion (
    id               UNIQUEIDENTIFIER  NOT NULL,
    aplicacion       VARCHAR(60)       NOT NULL,
    idempotencyKey   VARCHAR(120)      NOT NULL,
    tipo             VARCHAR(60)       NOT NULL,
    idioma           CHAR(2)           NOT NULL,
    asunto           NVARCHAR(200)     NULL,
    mensaje          NVARCHAR(4000)    NULL,
    destinatarios    NVARCHAR(MAX)     NOT NULL,
    datos            NVARCHAR(MAX)     NOT NULL CONSTRAINT DF_Notificacion_datos DEFAULT '{}',
    estado           VARCHAR(30)       NOT NULL,
    intentos         INT               NOT NULL CONSTRAINT DF_Notificacion_intentos DEFAULT 0,
    creadoEn         DATETIMEOFFSET(7) NOT NULL,
    proximoIntentoEn DATETIMEOFFSET(7) NULL,
    reservadoHasta   DATETIMEOFFSET(7) NULL,
    reservadoPor     NVARCHAR(100)     NULL,
    entregadoEn      DATETIMEOFFSET(7) NULL,
    falloEn          DATETIMEOFFSET(7) NULL,
    codigoError      VARCHAR(40)       NULL,
    detalleError     NVARCHAR(300)     NULL,
    idCorrelacion    VARCHAR(64)       NULL,
    traceparent      VARCHAR(80)       NULL,
    CONSTRAINT PK_Notificacion PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_Notificacion_tipo FOREIGN KEY (tipo) REFERENCES dbo.TipoNotificacion (tipo),
    CONSTRAINT FK_Notificacion_estado FOREIGN KEY (estado) REFERENCES dbo.EstadoNotificacion (codigo),
    CONSTRAINT UQ_Notificacion_idempotencia UNIQUE (aplicacion, idempotencyKey),
    CONSTRAINT CK_Notificacion_destinatarios_json CHECK (ISJSON(destinatarios) = 1),
    CONSTRAINT CK_Notificacion_datos_json CHECK (ISJSON(datos) = 1)
);

CREATE NONCLUSTERED INDEX IX_Notificacion_despacho ON dbo.Notificacion (estado, proximoIntentoEn, creadoEn) INCLUDE (reservadoHasta);
CREATE NONCLUSTERED INDEX IX_Notificacion_retencion ON dbo.Notificacion (estado, creadoEn);
CREATE NONCLUSTERED INDEX IX_Notificacion_consulta ON dbo.Notificacion (creadoEn DESC, tipo, aplicacion);

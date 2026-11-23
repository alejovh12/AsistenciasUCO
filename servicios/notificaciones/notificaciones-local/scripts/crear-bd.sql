-- Crea (idempotente) la base notificacionesdb y su login. La clave llega por la variable de entorno NOTIFICACIONES_DB_PASSWORD
-- (sqlcmd la expande como $(NOTIFICACIONES_DB_PASSWORD)); nunca se escribe en este archivo.
-- Flyway (dentro del servicio) crea las tablas y siembra las plantillas al primer arranque; el login es db_owner solo de esta base.
IF DB_ID(N'notificacionesdb') IS NULL
    CREATE DATABASE [notificacionesdb];
GO

IF SUSER_ID(N'notificacionesdb') IS NULL
    CREATE LOGIN [notificacionesdb] WITH PASSWORD = N'$(NOTIFICACIONES_DB_PASSWORD)', DEFAULT_DATABASE = [notificacionesdb];
ELSE
    ALTER LOGIN [notificacionesdb] WITH PASSWORD = N'$(NOTIFICACIONES_DB_PASSWORD)';
GO

USE [notificacionesdb];
GO

IF USER_ID(N'notificacionesdb') IS NULL
    CREATE USER [notificacionesdb] FOR LOGIN [notificacionesdb];
GO

ALTER ROLE db_owner ADD MEMBER [notificacionesdb];
GO

SELECT DB_NAME() AS baseDeDatos, SUSER_NAME() AS ejecutor, (SELECT COUNT(*) FROM sys.database_principals WHERE name = N'notificacionesdb') AS usuarios;
GO

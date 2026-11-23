-- Elimina la base notificacionesdb y su login (SOLO laboratorio). Se pierden plantillas editadas y la cola de envio.
IF DB_ID(N'notificacionesdb') IS NOT NULL
BEGIN
    ALTER DATABASE [notificacionesdb] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [notificacionesdb];
END
GO

IF SUSER_ID(N'notificacionesdb') IS NOT NULL
    DROP LOGIN [notificacionesdb];
GO

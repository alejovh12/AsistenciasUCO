USE [gestionasistenciadb];
GO

-- 42-46. SolicitudIngresoGrupo (HU053/HU054/HU055): solo FKs DESDE la tabla nueva; ninguna tabla existente cambia.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_SolicitudIngresoGrupo_Estudiante')
BEGIN
    ALTER TABLE [dbo].[SolicitudIngresoGrupo] ADD CONSTRAINT [FK_SolicitudIngresoGrupo_Estudiante] FOREIGN KEY ([estudiante]) REFERENCES [dbo].[Estudiante] ([id]);
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_SolicitudIngresoGrupo_Grupo')
BEGIN
    ALTER TABLE [dbo].[SolicitudIngresoGrupo] ADD CONSTRAINT [FK_SolicitudIngresoGrupo_Grupo] FOREIGN KEY ([grupo]) REFERENCES [dbo].[Grupo] ([id]);
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_SolicitudIngresoGrupo_Estado')
BEGIN
    ALTER TABLE [dbo].[SolicitudIngresoGrupo] ADD CONSTRAINT [FK_SolicitudIngresoGrupo_Estado] FOREIGN KEY ([estado]) REFERENCES [dbo].[Estado] ([id]);
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_SolicitudIngresoGrupo_Docente')
BEGIN
    ALTER TABLE [dbo].[SolicitudIngresoGrupo] ADD CONSTRAINT [FK_SolicitudIngresoGrupo_Docente] FOREIGN KEY ([docenteResolutor]) REFERENCES [dbo].[Docente] ([id]);
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_SolicitudIngresoGrupo_EstudianteGrupo')
BEGIN
    ALTER TABLE [dbo].[SolicitudIngresoGrupo] ADD CONSTRAINT [FK_SolicitudIngresoGrupo_EstudianteGrupo] FOREIGN KEY ([estudianteGrupo]) REFERENCES [dbo].[EstudianteGrupo] ([id]);
END
GO

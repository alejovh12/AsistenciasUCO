-- Corrige la ortografia (tildes y enie) de las descripciones de los tipos sembrados en V2. V1/V2 ya estan aplicadas: no se editan.
-- Solo cambia dbo.TipoNotificacion.descripcion; marcadores, asuntos y cuerpos de V2 ya tenian la ortografia correcta.

UPDATE dbo.TipoNotificacion SET descripcion = N'Activación de cuenta: aviso a una persona registrada por otra (decano, coordinador, docente o estudiante) para definir su contraseña con un enlace de un solo uso.' WHERE tipo = 'ACTIVACION_CUENTA';
UPDATE dbo.TipoNotificacion SET descripcion = N'Alerta de pérdida de asignatura: el estudiante superó el límite institucional de inasistencia.' WHERE tipo = 'ALERTA_PERDIDA_INASISTENCIA';
UPDATE dbo.TipoNotificacion SET descripcion = N'Alerta de riesgo: el estudiante se acerca al límite institucional de inasistencia.' WHERE tipo = 'ALERTA_RIESGO_INASISTENCIA';
UPDATE dbo.TipoNotificacion SET descripcion = N'Cancelación por inasistencia aprobada: aviso al estudiante y al docente titular.' WHERE tipo = 'CANCELACION_APROBADA';
UPDATE dbo.TipoNotificacion SET descripcion = N'Propuesta de cancelación rechazada: aviso al docente titular; el estudiante sigue activo.' WHERE tipo = 'CANCELACION_RECHAZADA';
UPDATE dbo.TipoNotificacion SET descripcion = N'Cierre masivo del período académico ejecutado por el administrador institucional.' WHERE tipo = 'CIERRE_MASIVO_PERIODO_EJECUTADO';
UPDATE dbo.TipoNotificacion SET descripcion = N'Aviso al estudiante de las tomas en falta de una sesión terminada, con el plazo para reclamar.' WHERE tipo = 'FALTA_ASISTENCIA_SESION_ESTUDIANTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Propuesta de cancelación por inasistencia: aviso al coordinador (o al decano por escalamiento).' WHERE tipo = 'PROPUESTA_CANCELACION';
UPDATE dbo.TipoNotificacion SET descripcion = N'Receso académico: resumen al docente titular de sus clases canceladas ese día.' WHERE tipo = 'RECESO_CLASES_CANCELADAS_DOCENTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Receso académico: resumen al estudiante de las clases canceladas ese día.' WHERE tipo = 'RECESO_CLASES_CANCELADAS_ESTUDIANTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Solicitud de revisión de asistencia radicada por el estudiante: aviso al docente titular.' WHERE tipo = 'RECLAMO_ASISTENCIA_RADICADO';
UPDATE dbo.TipoNotificacion SET descripcion = N'Solicitud de revisión de asistencia resuelta por el docente: aviso al estudiante.' WHERE tipo = 'RECLAMO_ASISTENCIA_RESUELTO';
UPDATE dbo.TipoNotificacion SET descripcion = N'Revisión de cancelación por inasistencia pendiente: aviso al docente titular.' WHERE tipo = 'REVISION_CANCELACION_PENDIENTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Sesión creada manualmente por el docente: aviso al coordinador del programa.' WHERE tipo = 'SESION_MANUAL_CREADA_COORDINADOR';
UPDATE dbo.TipoNotificacion SET descripcion = N'Sesión creada manualmente por el docente: aviso a cada estudiante activo del grupo.' WHERE tipo = 'SESION_MANUAL_CREADA_ESTUDIANTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Sesión terminada sin toma de asistencia: aviso informativo al coordinador.' WHERE tipo = 'SESION_SIN_TOMA_COORDINADOR';
UPDATE dbo.TipoNotificacion SET descripcion = N'Solicitud de sesión extraordinaria aprobada por el coordinador: aviso al docente titular.' WHERE tipo = 'SESION_SOLICITUD_APROBADA_DOCENTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Solicitud de sesión extraordinaria del docente: aviso a los coordinadores del programa.' WHERE tipo = 'SESION_SOLICITUD_CREADA_COORDINADOR';
UPDATE dbo.TipoNotificacion SET descripcion = N'Solicitud de sesión extraordinaria rechazada: aviso al docente titular con motivo y observación.' WHERE tipo = 'SESION_SOLICITUD_RECHAZADA_DOCENTE';
UPDATE dbo.TipoNotificacion SET descripcion = N'Verificación de correo: aviso al estudiante registrado por el docente, con un enlace de un solo uso.' WHERE tipo = 'VERIFICACION_CORREO';
UPDATE dbo.TipoNotificacion SET descripcion = N'Correo de prueba genérico del canal (sin plantilla; lo emite el propio servicio).' WHERE tipo = 'PRUEBA_CANAL_NOTIFICACION';

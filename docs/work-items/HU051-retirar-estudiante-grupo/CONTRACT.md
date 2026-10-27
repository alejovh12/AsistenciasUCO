---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT — HU051: retiro de un estudiante del grupo

## 1. HTTP

`POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro`

- **Rol:** `DOCENTE` (`SecurityConfig` `/api/v1/docente/**`; catálogo de acceso del baseline: fila nueva con la misma regla de titularidad que el resto de rutas docente).
- **Path:** `grupoId`, `estudianteId` (UUID; inválido ⇒ 400 `INVALID_REQUEST`).
- **Body:** `{ "motivo": "texto" }` (obligatorio, 1..500 caracteres tras quitar espacios extremos).
- **200:** `ApiDataResponse<Void>` `{ "exitoso": true, "datos": null }`.

| Situación | HTTP | Origen |
|---|---|---|
| Usuario sin docente institucional | 403 | Application (`ForbiddenException`) |
| Motivo nulo, vacío, solo espacios o > 500 | 400 | Application (`ValidationException`); la DB lo revalida con `RET_004` |
| Grupo inexistente o de otro docente | 404 | DB `RET_001` (indistinguibles) |
| Estudiante sin matrícula activa en el grupo | 404 | DB `RET_002` |
| Período del grupo terminado | 409 | DB `RET_003` |

## 2. Base de datos

### `dbo.usp_retirar_estudiante_grupo` (comando, resultset canónico de 4 columnas)

Parámetros: `@idGrupo`, `@idEstudiante`, `@motivo NVARCHAR(MAX)`, `@idCorrelacion`, `@idUsuarioEjecutor`.

Orden de validación: correlación → ejecutor (`GEN_002`) → motivo (`RET_004`) → en transacción con `UPDLOCK, HOLDLOCK`: titularidad (`RET_001`) → período (`RET_003`) → matrícula activa (`RET_002`).

Efecto en éxito (`RET_005`): `EstudianteGrupo.estado` A → I; `INSERT dbo.AuditoriaEvento` (`action = 'RETIRO_ESTUDIANTE_GRUPO'`, `resourceType = 'EstudianteGrupo'`, `resourceId` = id de la matrícula, `actorId` = ejecutor, `correlationId`, `metadata` JSON `{idGrupo, idEstudiante, motivo}`). Asistencias intactas; el cupo se libera solo.

### `dbo.usp_consultar_destinatarios_retiro_estudiante_grupo` (READ_QUERY_RESULTSET)

Parámetros: `@idGrupo`, `@idEstudiante`, `@idCorrelacion`, `@idUsuarioEjecutor` (nulo ⇒ `THROW` `VAL_001`).

| Columna | Tipo |
|---|---|
| `rolDestinatario` | VARCHAR(20): `ESTUDIANTE`, `DOCENTE`, `COORDINADOR` (en ese orden) |
| `idUsuario` | UNIQUEIDENTIFIER |
| `nombreCompleto` | NVARCHAR(400) |
| `correo` | NVARCHAR(100) (uso interno del `NotificacionPort`) |
| `idGrupo`, `codigoGrupo`, `nombreGrupo`, `nombreAsignatura` | contexto del aviso |

Solo el docente titular obtiene filas; no exige matrícula activa (se consulta después del retiro).

## 3. Puertos de Application

- `RetiroEstudianteGrupoPort.retirarEstudiante(idGrupo, idEstudiante, motivo, idUsuarioEjecutor)`; `consultarDestinatarios(idGrupo, idEstudiante, idUsuarioEjecutor)`.
- `NotificacionPort.notificar(Notificacion)`: `Notificacion(tipo, asunto, mensaje, destinatarios[rol, idUsuario, nombre, correo], datos)`; tipo `RETIRO_ESTUDIANTE_GRUPO`. Mejor esfuerzo: el caso de uso registra y descarta un fallo del aviso.

## 4. Compatibilidad

Aditivo: no cambia ningún contrato existente. `DELETE /api/v1/coordinador/grupos/{grupoId}/estudiantes/{estudianteId}` sigue 501.

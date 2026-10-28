---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## 1. HTTP

| Método y ruta | Rol | Cuerpo / parámetros | Respuesta |
|---|---|---|---|
| `POST /api/v1/estudiante/solicitudes-matricula` | ESTUDIANTE | `{ "grupoId": UUID }` | `ApiDataResponse<{ id }>` (solicitud PENDIENTE) |
| `GET /api/v1/estudiante/solicitudes-matricula` | ESTUDIANTE | `estado`, `pagina`, `tamano` | `ApiListResponse<SolicitudIngresoEstudianteResponse>` |
| `POST /api/v1/estudiante/solicitudes-matricula/{id}/retiro` | ESTUDIANTE | — | `ApiDataResponse<Void>` |
| `GET /api/v1/docente/grupos/{grupoId}/solicitudes-ingreso` | DOCENTE | `estado`, `pagina`, `tamano` | `ApiListResponse<SolicitudIngresoDocenteResponse>` |
| `PATCH /api/v1/docente/solicitudes-ingreso/{id}` | DOCENTE | `{ "accion": "ACEPTAR" \| "RECHAZAR", "motivo"? }` | `ApiDataResponse<{ id, estadoResultante }>` con `ACEPTADA`, `EN_ESPERA` o `RECHAZADA` |

- `estado` acepta código (`P`, `A`, `R`, `E`, `X`) o nombre (`PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `EN_ESPERA`, `RETIRADA`); desconocido ⇒ 400. Paginación acotada: página ≥ 1, tamaño 1..100.
- Fechas: instantes UTC ISO-8601 con `Z` (`fechaSolicitudUtc`, `fechaResolucionUtc`).
- `SolicitudIngresoEstudianteResponse`: `id, grupoId, grupoCodigo, grupoNombre, asignaturaId, asignaturaNombre, docenteNombre, estado, fechaSolicitudUtc, fechaResolucionUtc, motivoRechazo`.
- `SolicitudIngresoDocenteResponse`: `id, estudianteId, estudianteNombre, numeroIdentificacion, estado, fechaSolicitudUtc, fechaResolucionUtc, motivoRechazo, posicionEspera, capacidadGrupo, cuposDisponibles`.

| Situación | HTTP | Código DB |
|---|---|---|
| Usuario sin el perfil institucional (estudiante o docente) | 403 | Application / `SIG_001` |
| Acción inválida, motivo de rechazo vacío o > 500, estado desconocido, ids nulos | 400 | Application / `SIG_009`, `SIG_010` |
| Grupo inexistente | 404 | `SIG_002` |
| Solicitud inexistente o ajena (otro docente / otro estudiante) | 404 | `SIG_007` |
| Período terminado, ya matriculado, solicitud abierta duplicada, ya resuelta, sigue sin cupo | 409 | `SIG_003`, `SIG_004`, `SIG_005`, `SIG_008`, `SIG_013` |

## 2. Base de datos

- `usp_registrar_solicitud_ingreso_grupo(@idSolicitud, @idGrupo, @idCorrelacion, @idUsuarioEjecutor)` — canónico; `SIG_006`.
- `usp_resolver_solicitud_ingreso_grupo(@idSolicitud, @accion, @motivo, @idCorrelacion, @idUsuarioEjecutor)` — canónico + `codigoEstadoResultante CHAR(1)`; con `UPDLOCK, HOLDLOCK` del grupo; matricula con `usp_registrar_estudiante_en_grupo_interno`; `SIG_011` (A), `SIG_012` (E), `SIG_014` (R).
- `usp_retirar_solicitud_ingreso_grupo(@idSolicitud, @idCorrelacion, @idUsuarioEjecutor)` — canónico; `SIG_015`.
- `usp_consultar_solicitudes_ingreso_grupo_docente` / `usp_consultar_solicitudes_ingreso_estudiante` — READ_QUERY_RESULTSET + `totalRegistros`; `THROW` `VAL_001` con ejecutor nulo.
- `usp_consultar_destinatarios_solicitud_ingreso_grupo` — ESTUDIANTE y DOCENTE (con correo, uso interno del aviso).

## 3. Puertos de Application

`SolicitudIngresoGrupoPort`: `registrarSolicitud`, `resolverSolicitud` (devuelve el código resultante), `retirarSolicitud`, `consultarSolicitudesGrupo`, `consultarSolicitudesEstudiante`, `consultarDestinatarios`. Avisos por `NotificacionPort` (tipos `SOLICITUD_INGRESO_REGISTRADA`, `SOLICITUD_INGRESO_RESUELTA`, `SOLICITUD_INGRESO_RETIRADA`), de mejor esfuerzo.

## 4. Compatibilidad

`POST /api/v1/estudiante/solicitudes-matricula` deja de responder 501 (mismo método y ruta, ahora real). Las rutas del coordinador siguen 501. El resto es aditivo.

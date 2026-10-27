---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# CONTRACT — HU010/HU011 (consulta del estudiante)

Principio: AS-IS → diff → TARGET. Todo es aditivo; ningún contrato de LB-004D cambia.

## 1. Matriz AS-IS / TARGET

| Elemento | AS-IS | TARGET | Tipo |
|---|---|---|---|
| `usp_consultar_solicitudes_revision_estudiante` | No existe | SP de lectura nuevo, paginado, propiedad en el SP | Aditivo |
| `GET /api/v1/estudiante/reclamos` | 501 | `ApiListResponse<ReclamoEstudianteResponse>` | Aditivo (sale de 501) |
| `DELETE /api/v1/estudiante/reclamos/{id}` | 501 | **Sin cambios** (TD-059) | Fuera de alcance |
| `POST /api/v1/asistencias/revisiones`, `/api/v1/archivos/**`, `/api/v1/docente/reclamos/**` | Implementados | **Sin cambios** | Congelado (LB-004D) |

## 2. Base de datos — `dbo.usp_consultar_solicitudes_revision_estudiante` (NUEVO)

Clasificación: `NON_GOLDEN_PATH_EXTENSION`, `READ_QUERY_RESULTSET` (parámetros inválidos ⇒ `THROW 51000`, como R2).

| Parámetro | Tipo | Requerido | Regla |
|---|---|---|---|
| `@idUsuarioEjecutor` | UNIQUEIDENTIFIER | SÍ | NULL o GUID vacío ⇒ `VAL_001` (`UsuarioEjecutor`) |
| `@idCorrelacion` | UNIQUEIDENTIFIER | SÍ | `usp_validar_id_correlacion_esta_presente_interno` |
| `@codigoEstado` | NVARCHAR(5) | NO | Filtro por `Estado.codigo` |
| `@numeroPagina` | INT | NO (1) | < 1 o NULL ⇒ 1 (acotado, igual que R2) |
| `@tamanoPagina` | INT | NO (10) | < 1 o NULL ⇒ 10; > 100 ⇒ 100 (acotado, igual que R2) |

Reglas: propiedad dentro del SP (`SolicitudRevisionAsistencia → Asistencia → EstudianteGrupo → Estudiante.usuario = @idUsuarioEjecutor`); otro usuario ⇒ 0 filas. Soporte adjunto con `ArchivoSoporte.estado = ufn_obtener_parametro('ARCHIVO','ESTADO_ADJUNTO')`. Orden: `fechaSolicitud DESC, nombreSolicitud, id`. El resultset tiene **las mismas columnas y tipos que R2** (`usp_consultar_solicitudes_revision_docente`); `idEstudiante`/`nombreEstudiante` son los del propio ejecutor.

Resultset:

| Columna | Tipo | Nulo |
|---|---|:---:|
| `idSolicitudRevisionAsistencia` | UNIQUEIDENTIFIER | NO |
| `nombreSolicitud` | NVARCHAR(50) | NO |
| `fechaSolicitud` | DATE | NO |
| `codigoEstado` / `nombreEstado` | NVARCHAR | NO |
| `justificacionSolicitud` / `justificacionRespuesta` | NVARCHAR(MAX) | NO |
| `idSesion`, `numeroSesion`, `fechaHoraInicioSesion`, `procedenciaTemporalSesion` | UNIQUEIDENTIFIER, INT, DATETIME2(7), NVARCHAR | procedencia SÍ |
| `idGrupo`, `codigoGrupo`, `nombreGrupo` | UNIQUEIDENTIFIER, INT, NVARCHAR | NO |
| `idAsignatura`, `codigoAsignatura`, `nombreAsignatura` | UNIQUEIDENTIFIER, NVARCHAR, NVARCHAR | NO |
| `idArchivoSoporte`, `nombreSoporte`, `tipoContenidoSoporte`, `tamanioSoporteBytes`, `fechaRegistroSoporteUtc` | metadatos del soporte | SÍ |
| `totalRegistros` | INT | NO |

## 3. HTTP — `GET /api/v1/estudiante/reclamos`

- **Rol:** ESTUDIANTE (regla existente `/api/v1/estudiante/**`).
- **Query:** `estado` (opcional: `P`/`A`/`R` o `PENDIENTE`/`APROBADA`/`RECHAZADA`), `pagina` (por defecto 1), `tamano` (por defecto 10, máximo 100). Igual que `GET /api/v1/docente/reclamos`: valores fuera de rango se acotan.
- **200:** `ApiListResponse<ReclamoEstudianteResponse>` `{ exitoso, datos[], total }`. Mismos nombres de campo que `ReclamoDocenteResponse` sin los datos del estudiante: `id, nombre, fechaSolicitud (yyyy-MM-dd), estadoSolicitud (PENDIENTE/APROBADA/RECHAZADA), justificacionSolicitud, justificacionRespuesta, sesionId, sesionNumero, fechaSesionUtc` (cadena UTC con `Z`; `null` si la procedencia no es verificable) `, procedenciaTemporalSesion, grupoId, grupoCodigo, grupoNombre, materiaId, materiaCodigo, materiaNombre, soporteAdjunto{ fileId, nombre, tipo, tamanioBytes, fechaSubidaUtc, url } | null`.
- **Errores:** sin estudiante institucional ⇒ 403; estado desconocido ⇒ 400; fallo técnico ⇒ error de BD estándar.
- El soporte se descarga con `GET /api/v1/archivos/{fileId}` (contrato LB-004D, sin cambios).

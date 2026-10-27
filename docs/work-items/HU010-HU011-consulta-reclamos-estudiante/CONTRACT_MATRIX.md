---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# CONTRACT MATRIX — HU010-HU011-consulta-reclamos-estudiante

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db `feature/HU010-HU011-consulta-reclamos-estudiante` | `usp_consultar_solicitudes_revision_estudiante` | rama `feature/HU010-HU011-consulta-reclamos-estudiante` (PR a `develop`) | OWNER |
| Backend | AsistenciasUCO `feature/HU010-HU011-consulta-reclamos-estudiante` | `SolicitudRevisionEstudianteJpaRepository`, `EstudiantePortalController#consultarReclamos` | rama `feature/HU010-HU011-consulta-reclamos-estudiante` (PR a `develop`) | CONSUMER / PROVIDER HTTP |
| Frontend | AsistenciasUCO-Frontend `feature/HU010-HU011-consulta-reclamos-estudiante` | `AttendanceClaimService#getReclamosEstudiante`, `StudentClaimsComponent` | rama `feature/HU010-HU011-consulta-reclamos-estudiante` (PR a `develop`) | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-010-DB | Solicitudes propias | READ_QUERY_RESULTSET + `totalRegistros`; `@idUsuarioEjecutor`, `@idCorrelacion`, `@codigoEstado`, `@numeroPagina`, `@tamanoPagina` (acotados) | Repositorio: binding nombrado, UTC solo con procedencia | MATCH | E-01..E-08, RP-01..RP-04 | — | — |
| C-010-HTTP | `GET /api/v1/estudiante/reclamos` | `ApiListResponse<ReclamoEstudianteResponse>`; 403 sin estudiante; 400 estado desconocido | Front: `datos` + `total`, params `estado/pagina/tamano` | MATCH | CT-01..03, SC-01..SC-04 | — | — |
| C-010-SOP | Descarga del soporte | `soporteAdjunto.url = /api/v1/archivos/{fileId}` (regla PROPIETARIO de `usp_consultar_archivo_soporte_autorizado`) | Front descarga blob por `GET /archivos/{fileId}` | MATCH | UC-06, SC-05..SC-07 | — | — |
| C-010-R2 | Compatibilidad con la bandeja docente | Mismas columnas y tipos que `usp_consultar_solicitudes_revision_docente` | Entidad hereda `SpSolicitudRevisionDocenteBaseEntity` | MATCH | E-08a | — | — |

## Tipos y campos

| Campo | DB type/nullability | Java type | HTTP | Status |
|---|---|---|---|---|
| `fechaSolicitud` | DATE | LocalDate | `yyyy-MM-dd` | MATCH |
| `fechaHoraInicioSesion` + `procedenciaTemporalSesion` | DATETIME2(7) + NVARCHAR NULL | Instant (null sin procedencia) | `fechaSesionUtc` ISO `Z` o null | MATCH |
| `fechaRegistroSoporteUtc` | DATETIME2(7) NULL | Instant | `soporteAdjunto.fechaSubidaUtc` ISO `Z` | MATCH |
| `tamanioSoporteBytes` | BIGINT NULL | Long → long | `tamanioBytes` | MATCH |
| `codigoEstado` | NVARCHAR | String P/A/R | `estadoSolicitud` PENDIENTE/APROBADA/RECHAZADA | MATCH |
| `idEstudiante`/`nombreEstudiante` | presentes (propios) | leídos por la entidad base | NO expuestos | MATCH (intencional) |

## Códigos/enums

`VAL_001` (ejecutor nulo, DB), `FORBIDDEN` (403), `INVALID_REQUEST`/validación (400).

---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT MATRIX — HU051-retirar-estudiante-grupo

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db `feature/HU051-retirar-estudiante-grupo` | `usp_retirar_estudiante_grupo`, `usp_consultar_destinatarios_retiro_estudiante_grupo` | commit local (sin push) | OWNER |
| Backend | AsistenciasUCO `feature/HU051-retirar-estudiante-grupo` | `RetiroEstudianteGrupoJpaRepository`, `DocenteGrupoEstudianteController`, `access-catalog.json` | commit local (sin push) | CONSUMER / PROVIDER HTTP |
| Frontend | AsistenciasUCO-Frontend `feature/HU051-retirar-estudiante-grupo` | `GroupService.retirarEstudiante`, `TeacherRetiroEstudianteModalComponent` | commit local (sin push) | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-051-CMD | Retiro | Resultset canónico; `RET_001..RET_005`, `GEN_002` | Repositorio vía `JpaProcedureExecutor`; `DbFailureClassifier` 404/404/409/400 | MATCH | R-01..R-07, RP-01/02, CL-01..03 | — | — |
| C-051-DEST | Destinatarios | READ_QUERY_RESULTSET (8 columnas), solo titular | Proyección `DestinatarioRetiroProjection` | MATCH | R-08/R-09, RP-03..05 | — | — |
| C-051-HTTP | `POST .../retiro` | `ApiDataResponse<Void>`; 400/403/404/409 | Front: POST `{motivo}` sin toast global; muestra el mensaje del backend | MATCH | CT-01..04, RT-04/05 | — | — |
| C-051-CAT | Autorización | Fila nueva en `access-catalog.json` (DOCENTE, titularidad) | PEP del baseline | MATCH en código / PENDIENTE en el PDP | `AccessCatalogConsistencyTest` | Ruta denegada en modo baseline hasta reaprovisionar | Alejo: `provision_asistencias.py` |
| C-051-NOT | Aviso | `NotificacionPort` (tipo, destinatarios, datos) | Provider de log (temporal) | MATCH | UC-02, NT-01..03 | Sin entrega real | TD-066 |

## Códigos/enums

`RET_001` 404, `RET_002` 404, `RET_003` 409, `RET_004` 400, `RET_005` éxito, `GEN_002`, `VAL_001`.

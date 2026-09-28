---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-24
---

# Guía HTTP Command / Query

La norma única es [API_DESIGN_RULES](../governance/API_DESIGN_RULES.md). LB-001C.1 resolvió
CF-001: la API usa `PRAGMATIC_RESOURCE_PLUS_BUSINESS_COMMAND`; no aplica una regla simplista
`COMMAND→POST` a toda mutación ni normaliza endpoints existentes automáticamente.

AsistenciasUCO modela las operaciones HTTP segun su intencion de negocio:

- QUERY: usa `GET`, no modifica estado y recibe datos por `PathVariable` o `RequestParam`.
- BUSINESS COMMAND: usa `POST` y un verbo de dominio cuando representa una acción/transición.
- PARTIAL UPDATE: usa `PATCH` cuando modifica parcialmente una representación.
- PUT/DELETE nuevos: restringidos por defecto y sujetos a `METHOD_EXCEPTION`.

PUT, PATCH y DELETE no están obsoletos. Los PUT/DELETE existentes se inventarían y clasifican;
una migración exige análisis de compatibilidad. El PUT de sesión del Golden Path es
`MIGRATION_CANDIDATE` y permanece AS-IS en LB-001C.1.

Rutas canonicas de consulta:

- `GET /api/v1/docentes/{docenteId}`
- `GET /api/v1/docentes/{docenteId}/asignaciones`
- `GET /api/v1/sesiones/{sesionId}`
- `GET /api/v1/grupos/{grupoId}/asistencias`

Los POST de consulta anteriores se conservan temporalmente por compatibilidad. Se eliminaran cuando el frontend consuma las rutas GET canonicas.

CORS permite GET, POST, PUT, PATCH, DELETE y OPTIONS; evidencia: `SecurityConfig.corsConfigurationSource`. La API vigente incluye esos métodos.

Los parametros booleanos HTTP utilizan representacion estricta exacta `true`/`false`.

---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# CLOSURE — HU010/HU011 (consulta): Mis Reclamos del estudiante

## Resultado

DONE (gates locales verdes y revalidados tras integrar `develop`, ver [VALIDATION.md](VALIDATION.md)). Ramas subidas; pendiente: merge de los PR (DB primero), CI remoto y MV-008.

## Alcance entregado y decisiones

- DB: `usp_consultar_solicitudes_revision_estudiante` (ownership por `Estudiante.usuario`, mismo resultset que R2, soporte por `ARCHIVO.ESTADO_ADJUNTO`) + permiso aditivo + prueba E-01..E-08.
- Backend: feature `estudiante/consultarreclamos` (independiente del feature docente), puerto `SolicitudRevisionEstudianteQueryPort`, repositorio JPA nativo, `GET /api/v1/estudiante/reclamos` real.
- Frontend: página "Mis Reclamos" (`/app/estudiante/reclamos`, menú ESTUDIANTE) con filtro por estado, paginación del servidor y descarga autenticada del soporte; mock alineado al contrato.
- Decisiones D1–D3 en [PLAN.md](PLAN.md).

## Deuda

- TD-059 (DELETE falla con Msg 8624): `DELETE /estudiante/reclamos/{id}` sigue 501.
- TD-060 (categoría no persistida): HU014 sigue bloqueada.
- Sin deuda nueva.

## Validación manual

MV-008 (portal del estudiante) en espera del componente de seguridad (Security Baseline 0.1.7).

## ADR relacionados

ADR-LB004D-001 (soportes y bandeja docente): no modificado; este trabajo lo consume.

## Elementos pendientes y bloqueos

PR de los 3 repos hacia `develop` (orden: DB → backend → frontend). Infra no se tocó.

## Cambios fuera de alcance

Ninguno. No se modificaron tablas, objetos congelados, `SecurityConfig` ni contratos LB-004D.

## Condición de parada

No iniciar automáticamente DELETE (TD-059) ni HU014 (TD-060).

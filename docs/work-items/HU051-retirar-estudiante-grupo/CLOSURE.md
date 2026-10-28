---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU051: retiro de un estudiante del grupo

## Resultado

DONE (gates locales verdes, ver [VALIDATION.md](VALIDATION.md)). Rama subida; pendiente: merge de los PR (DB → backend → frontend), CI remoto, reaprovisionar el PDP con la ruta nueva y MV-008.

## Alcance entregado y decisiones

- DB: `usp_retirar_estudiante_grupo` (matrícula A → I, auditoría con motivo, bloqueo por período terminado, solo docente titular), `usp_consultar_destinatarios_retiro_estudiante_grupo`, mensajes `RET_001..RET_005`, permisos, prueba R-01..R-09.
- Backend: feature `docente/retirarestudiantegrupo`, `POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro`, `NotificacionPort` reutilizable con provider temporal de log, mapeo de errores, fila en el catálogo de acceso.
- Frontend: botón "Retirar" en el padrón (solo matrículas activas) y modal con motivo obligatorio; el padrón se recarga tras el retiro.
- Decisiones D1-D5 en [PLAN.md](PLAN.md).

## Deuda

- [TD-066](../../baseline/TECHNICAL_DEBT.md#td-066): `NotificacionPort` sin canal real (provider de log) hasta integrar el componente institucional de notificaciones.

## Validación manual

MV-008 (incluye HU051) en espera del componente de seguridad.

## ADR relacionados

Ninguno nuevo.

## Elementos pendientes y bloqueos

- Reaprovisionar el PDP del Security Baseline con la fila nueva del catálogo (Alejo).
- PR de DB, backend y frontend hacia `develop` (orden: DB → backend → frontend).

## Cambios fuera de alcance

Ninguno: sin tablas nuevas ni modificadas, sin objetos congelados, sin `SecurityConfig`, sin Infra.

## Condición de parada

No iniciar HU053-055 en esta rama.

## Cambio posterior: INT-HU-01 cerrado (2026-10-28)

Hallazgo `INT-HU-01` de [INTEGRACION-DEVELOP-20261027](../INTEGRACION-DEVELOP-20261027/README.md): tras el retiro (HU051, matrícula `I`) el estudiante podía volver a solicitar (HU053) pero `ACEPTAR` (HU055) fallaba con **409 `ERR_MATRICULA_DUPLICADA`** y la solicitud quedaba pendiente. Decisión de John (opción a): `usp_resolver_solicitud_ingreso_grupo` **reactiva** la matrícula retirada (`I` → `A`) y responde `SIG_011`/`ACEPTADA`. Cambio solo en DB (rama `fix/INT-HU-01-reactivar-matricula-retirada` del repo de DB, pruebas RM-01..RM-03); el backend no cambia (mismo código y mismo mapeo HTTP).

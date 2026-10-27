---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## Resultado

DONE localmente (gates locales verdes, ver [VALIDATION.md](VALIDATION.md)). Pendiente: integrar `origin/develop`, push/PR (con confirmación de John), CI remoto, reaprovisionar el PDP y MV-008.

## Alcance entregado y decisiones

- DB: tabla `SolicitudIngresoGrupo` (aprobada), estados E/X, mensajes `SIG_*`, 6 SP con ámbito en tablas base y bloqueo de cupo, permisos y prueba S-01..S-12.
- Backend: `SolicitudIngresoGrupoPort` y su repositorio; 5 features; 2 controladores; 4 rutas nuevas en el catálogo de acceso; `POST /estudiante/solicitudes-matricula` deja de responder 501; avisos por `NotificacionPort`.
- Frontend: página "Solicitudes de Ingreso" del estudiante, pestaña del docente en el hub del grupo, enlace de auto-matrícula a la página nueva, mock.
- Decisiones D1, D2, D4 (D3/D5 descartados) en [PLAN.md](PLAN.md).

## Deuda

- [TD-066](../../baseline/TECHNICAL_DEBT.md#td-066): avisos sin canal real (también cubre HU053-HU055).

## Validación manual

MV-008 (incluye HU053-HU055 y la prueba de concurrencia del último cupo).

## Elementos pendientes y bloqueos

- Reaprovisionar el PDP con `GET /docente/grupos/{grupoId}/solicitudes-ingreso`, `PATCH /docente/solicitudes-ingreso/{id}`, `GET /estudiante/solicitudes-matricula` y `POST /estudiante/solicitudes-matricula/{id}/retiro` (Alejo).
- PR de DB, backend y frontend hacia `develop` (orden: DB → backend → frontend).

## Cambios fuera de alcance

Ninguno: sin `SecurityConfig`, sin objetos congelados, sin Infra; las rutas del coordinador siguen 501.

## Condición de parada

No iniciar HU014/HU038 en esta rama.

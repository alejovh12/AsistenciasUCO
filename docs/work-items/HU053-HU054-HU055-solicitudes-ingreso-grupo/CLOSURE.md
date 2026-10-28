---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## Resultado

DONE localmente (gates locales verdes, ver [VALIDATION.md](VALIDATION.md)). Rama subida (con `origin/develop` integrado); pendiente: merge de los PR (DB → backend → frontend), CI remoto, reaprovisionar el PDP y MV-008.

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

## Cambio posterior por HU008 (2026-10-27)

Regla de John: el estudiante solo queda inscrito en un programa (`EstudiantePrograma`) cuando ingresa a un grupo de asignatura **específica (ESP)**; al aceptar una solicitud en una asignatura transversal (**BAS** o **INS**) se matricula en el grupo pero **no** se inscribe en el programa. Cambio de 1 línea en `usp_registrar_estudiante_en_grupo_interno` (y `_autonomo`), con regresión RI-01..RI-03 en `test/test_hu008_regla_inscripcion_programa.sql`; S-01..S-12 siguen en verde. Detalle e impacto: work item `HU008-oferta-grupos-estudiante`.

## Cambio posterior: INT-HU-01 cerrado (2026-10-28)

Hallazgo `INT-HU-01` de [INTEGRACION-DEVELOP-20261027](../INTEGRACION-DEVELOP-20261027/README.md): tras el retiro (HU051, matrícula `I`) el estudiante podía volver a solicitar (HU053) pero `ACEPTAR` (HU055) fallaba con **409 `ERR_MATRICULA_DUPLICADA`** y la solicitud quedaba pendiente. Decisión de John (opción a): `usp_resolver_solicitud_ingreso_grupo` **reactiva** la matrícula retirada (`I` → `A`) y responde `SIG_011`/`ACEPTADA`. Cambio solo en DB (rama `fix/INT-HU-01-reactivar-matricula-retirada` del repo de DB, pruebas RM-01..RM-03); el backend no cambia (mismo código y mismo mapeo HTTP).

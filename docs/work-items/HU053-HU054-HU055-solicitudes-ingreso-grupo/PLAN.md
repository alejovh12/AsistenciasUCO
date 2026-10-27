---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# PLAN — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## 1. Identidad y objetivo

- **Historias:** HU053 (el docente acepta el ingreso de un estudiante al grupo), HU054 (el docente rechaza con motivo), HU055 (control de cupo con lista de espera).
- **Work Item ID:** `HU053-HU054-HU055-solicitudes-ingreso-grupo`
- **Fecha:** 2026-10-27
- **Base Git:** rama apilada sobre `feature/HU051-retirar-estudiante-grupo` (reutiliza `NotificacionPort` y `usp_retirar_estudiante_grupo`): DB `a01a3de`, backend `06aa560`, frontend `df925f2`. Se integra **después** de HU051.
- **Clase de cambio:** funcional con **tabla nueva** (aprobada por John, skill `consistencia-tablas`).

### Decisiones de John (2026-10-27)

| # | Decisión |
|---|---|
| D1 | Se aprueba la tabla nueva `SolicitudIngresoGrupo` y los estados nuevos de `dbo.Estado`. |
| D2 | El docente puede aceptar a **cualquiera** de la lista de espera; el orden de llegada solo se muestra. |
| D3 / D5 | **Descartados:** no hay cierre de matrícula ni vencimiento; una solicitud abierta queda hasta que el docente la resuelva o el estudiante la retire. |
| D4 | Se mantiene la matrícula directa por el docente (sin cambios). |
| — | Estados: PENDIENTE (P), ACEPTADA (A), RECHAZADA (R), EN_ESPERA (E), RETIRADA (X). |
| — | Avisos por `NotificacionPort`: al docente cuando llega o se retira una solicitud; al estudiante cuando se resuelve. |

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| No existía tabla de solicitudes; `usp_resolver_solicitud_matricula` solo devuelve `ERR_SOLICITUD_MATRICULA_NO_IMPLEMENTADA` | SP existente (sin cambios) |
| `POST /api/v1/estudiante/solicitudes-matricula` y las rutas de coordinador respondían 501 | `EstudiantePortalController`, `CoordinadorPortalController` |
| El cupo es `Grupo.cantidadEstudiantes` (capacidad) − matrículas activas | `uv_grupo.cuposDisponibles` |

### Criterios de aceptación

1. Un estudiante solicita el ingreso a un grupo vigente; no puede duplicar una solicitud abierta ni solicitar un grupo en el que ya está matriculado.
2. El docente titular ve las solicitudes de su grupo en orden de llegada, con cupo disponible y posición en la lista de espera.
3. Aceptar con cupo matricula; sin cupo deja la solicitud EN_ESPERA; una solicitud en espera puede aceptarse cuando se libere cupo (p. ej. con HU051), sin orden estricto. Dos aceptaciones simultáneas del último cupo no producen sobrecupo (bloqueo de la fila del grupo).
4. Rechazar exige motivo (1..500) y el estudiante lo ve.
5. El estudiante retira su solicitud abierta y puede volver a solicitar.
6. Otro docente, otro estudiante o un usuario sin el perfil no acceden (403/404).

## 2. Alcance

| Capa | Cambio |
|---|---|
| DB | Tabla `SolicitudIngresoGrupo` + 5 FKs + índice único filtrado de solicitud abierta + índice de bandeja; estados `E`/`X`; mensajes `SIG_001..SIG_015`; SP `usp_registrar_/usp_resolver_/usp_retirar_solicitud_ingreso_grupo`, `usp_consultar_solicitudes_ingreso_grupo_docente`, `usp_consultar_solicitudes_ingreso_estudiante`, `usp_consultar_destinatarios_solicitud_ingreso_grupo`; permisos; prueba S-01..S-12 |
| Backend | `SolicitudIngresoGrupoPort` + `SolicitudIngresoGrupoJpaRepository`; features `estudiante/solicitaringresogrupo`, `estudiante/consultarsolicitudesingreso`, `estudiante/retirarsolicitudingreso`, `docente/consultarsolicitudesingreso`, `docente/resolversolicitudingreso`; `EstudianteSolicitudIngresoController`, `DocenteSolicitudIngresoController`; se retira el stub 501 de `POST /estudiante/solicitudes-matricula`; mapeo `SIG_*`; 4 filas nuevas en `access-catalog.json`; conteo de repositorios 46 → 47 |
| Frontend | `SolicitudIngresoService`; página "Solicitudes de Ingreso" del estudiante (`/app/estudiante/solicitudes`, acepta `?grupo=`); pestaña "Solicitudes de Ingreso" en el hub del grupo del docente; el enlace de auto-matrícula del docente apunta a la página nueva; mock |

**No alcance:** matrícula directa (sin cambios), rutas `/coordinador/solicitudes-matricula` (siguen 501: aprueba el docente), vencimiento, `SecurityConfig` (las reglas `/api/v1/estudiante/**` y `/api/v1/docente/**` ya cubren las rutas), objetos congelados, Infra.

## 3. Análisis de impacto de la tabla nueva (consistencia-tablas, aprobado por John)

Ver el diseño aprobado `DISENO_HU053_HU054_HU055_SolicitudIngresoGrupo.md` (carpeta del proyecto) y el PLAN del work item homónimo de la DB. Resumen: ninguna tabla existente cambia (solo FKs **desde** la tabla nueva), ninguna vista `uv_*` cambia, se reutiliza `usp_registrar_estudiante_en_grupo_interno` sin modificarlo, `freeze-manifest` en 0.

## 4. Riesgos, rollback y parada

- **`dbo.Estado` compartido** con los reclamos: los reclamos siguen usando P/A/R (cubierto por la suite DB completa).
- **Concurrencia de cupo:** `UPDLOCK, HOLDLOCK` sobre la fila del grupo en la aceptación; no hay prueba automática de dos sesiones simultáneas (limitación documentada; se valida en MV).
- **PDP del baseline:** las 4 rutas nuevas del catálogo deben reaprovisionarse en el PDP (Alejo).
- **Rollback:** `DROP` de los 6 SP y de la tabla (con sus FKs), borrar los estados E/X y los mensajes `SIG_*`; revertir commits backend/frontend.
- **Parada:** no iniciar HU014/HU038 en esta rama.

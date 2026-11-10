---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-10
---

# PLAN — Correcciones de la certificación de CULM-001 (2026-11-10)

## Origen

Recorrido en pantalla de las 17 HU de CULM-001 que estaban "implementadas, a certificar" (2026-11-09, lab Security Baseline con la BD local). Resultado: 11 PASA, 4 FALLA (HU007, HU026C, HU156 y HU171) y 2 BLOQUEADAS (HU014 y HU015, porque ClamAV está apagado), más un hallazgo alto fuera de las 17: con varias tomas de asistencia, el estudiante veía el estado de la última toma. El reporte completo está fuera de los repos, en `informes/CERTIFICACION_FASE3_CULM001_2026-11-10/REPORTE.md`.

John pidió el 2026-11-10 pasar las 11 a CERTIFICADA y corregir las fallas. El código de CULM-001 lo integró el equipo de Sergio; las correcciones se avisan en el mensaje consolidado de seguimiento.

## Clase de cambio y alcance

- **Clase:** corrección de defectos. El único cambio de contrato es aditivo: `estadoAsistencia` puede valer `PARCIAL`.
- **Rutas permitidas:** `CancelacionRowSupport`, `AlertaInasistenciaJpaRepository`, `RevisionCancelacionJpaRepository`, `EjecutarCargaUsuariosUseCaseImpl`, `ConsultarSesionesMateriaEstudianteUseCaseImpl`, `EstadoAsistenciaSesion`, la documentación de `SesionMateriaEstudianteDTO`, `MessageCatalogJpaRepository` (solo el formato de los marcadores) y sus pruebas.
- **Rutas prohibidas:** SQL y migraciones (la BD no cambia), `SecurityConfig`, `access-catalog.json` y el PDP.
- **Variable principal:** que cada HU que falló cumpla su criterio de aceptación en pantalla.
- **No alcance:** HU014 y HU015 (las bloquea el entorno, no el código) y HU042 (falta C.2, la campana de notificaciones, que es de Sergio).
- **DoR:** READY. La causa de cada falla está reproducida y el criterio de aceptación es el de la HU y el de CULM-001.

## Cambios

| HU | Causa | Corrección |
|---|---|---|
| HU026C (motor de alertas de inasistencia) | `CancelacionRowSupport.exec` arma el `EXEC` solo con los parámetros no nulos, pero `JpaProcedureExecutor.executeWithExtras` enlaza todo el mapa que recibe. En la evaluación del grupo entero, `idEstudianteGrupo` es nulo y no está en el `EXEC`, así que Hibernate lanzaba `IllegalArgumentException`. El job `evaluarAlertasInasistenciaGrupo` fallaba cada minuto con "SQL operation failed" y nunca evaluaba. El SP sí funcionaba a mano | `CancelacionRowSupport.enlazables(parametros)` devuelve solo los parámetros del `EXEC`, en el mismo orden. Se usa en los cuatro llamados al ejecutor que arman el `EXEC` con `exec`: evaluar alertas, actualizar resolutores, decidir y resolver la revisión de cancelación. En las dos últimas, un `motivo` nulo tenía el mismo defecto latente |
| HU171 (carga masiva de docentes) | `EjecutarCargaUsuariosUseCaseImpl` pasaba `UUID.randomUUID()` como `idDocente` y como `idUsuario`. `VincularDocenteProgramaUseCaseImpl` lo tomaba como un docente existente y el SP respondía `DOC_005`, así que ninguna fila creaba al docente | Pasa `null` en ambos, que es el modo "docente nuevo" del caso de uso de vinculación |
| Hallazgo alto (varias tomas) | `resolverEstado` tomaba solo el detalle de mayor código, es decir, la última toma escrita. Una sesión con una toma `SJC` y otra `AN` se veía PRESENTE y no se podía reclamar, mientras la planilla del docente mostraba `SJC` | Estado consolidado según CULM-001 B.2 [SUPUESTO-B-15]: si alguna toma es `SJC`, la sesión es AUSENTE, o PARCIAL si otra toma es `AN`; si no, JUSTIFICADA si alguna es `EX`; si no, PRESENTE. PARCIAL es reclamable si no hay solicitud previa |

### Hallazgos de la recertificación (2026-11-10, 01:33–02:04)

La re-prueba en pantalla confirmó las correcciones y encontró defectos que antes no se veían, porque los flujos fallaban antes de llegar a ellos.

| Hallazgo | Causa | Corrección |
|---|---|---|
| N1 (HU026C): las alertas del estudiante decían "Llevas {}% de inasistencia en {}" | El catálogo de la BD usa marcadores secuenciales `{}`. `MessageCatalogJpaRepository.formatMessage` solo aplicaba `MessageFormat`, que entiende `{0}` y, ante `{}`, devolvía la plantilla sin sustituir. Los adaptadores de Azure y de MessageUcoLab sí aceptan los dos formatos | El adaptador SQL Server sustituye `{}` en orden, con el argumento insertado de forma literal, y conserva `{0}` |
| N2 (HU026C): "Ver detalle de la materia" llevaba a un 404 | Frontend | Ver el work item homónimo del frontend |
| N4 (HU156): la clave inicial no se pedía sola | Frontend | Ver el work item homónimo del frontend |
| N3 (HU156): el estudiante admitido no aparece en Personas → Estudiantes | `uv_estudiante` tiene una fila por (estudiante, grupo, programa): un estudiante con programa y sin grupo no tiene filas | **Sin cambio: decide John.** Es la consecuencia de admitir sin grupo, que John aceptó "por ahora" y que su directriz del 2026-11-07 no permite. Hacerlo visible cambia `usp_buscar_estudiantes_institucion_paginado` |
| N5 (HU026C, riesgo): el diálogo del coordinador abre con "Aprobar cancelación" marcado y envía sin confirmar | Frontend de C.1 | **Sin cambio:** se avisa a Sergio en el mensaje de seguimiento |

HU007 (filtro de "Mis materias") y HU156 (admisión del estudiante) se corrigen solo en el frontend. El work item homónimo del frontend también agrega el botón "¿Olvidaste tu contraseña?" de HU177, que sigue PENDIENTE de implementación (decisión de John del 2026-11-09).

## Contrato

- `GET /api/v1/estudiante/materias/{materiaId}/sesiones`: `estadoAsistencia` puede valer `PARCIAL`. Es un valor nuevo y aditivo; los demás no cambian. Con `PARCIAL` y sin solicitud previa, `reclamable` es `true`. La radicación sigue validándose en la BD al crear la solicitud.
- Los demás endpoints no cambian. `access-catalog` y `SecurityConfig` tampoco, así que el PDP no necesita reaprovisionarse.

## Pruebas (RED → GREEN)

- `AlertaInasistenciaJpaRepositoryTest`: la evaluación del grupo entero y la actualización de resolutores no enlazan `idEstudianteGrupo` ni `tamanoLote` cuando son nulos. El stub lanza `IllegalArgumentException` si se enlazan, como Hibernate.
- `RevisionCancelacionJpaRepositoryTest`: decidir sin motivo no enlaza `motivo`.
- `EjecutarCargaUsuariosUseCaseImplTest`: la fila DOCENTE llega a la vinculación con `idDocente` e `idUsuario` nulos.
- `ConsultarSesionesMateriaEstudianteUseCaseImplTest`: `SJC` + `AN` da PARCIAL y reclamable; `SJC` prevalece sobre `EX` y `EX` sobre `AN`. Reemplaza la prueba anterior que fijaba "la toma de mayor código".
- `MessageCatalogJpaMarcadoresSecuencialesTest` (nueva): sustituye `{}` en orden; un argumento con signos de expresión regular se inserta literal; con menos argumentos deja los marcadores sobrantes; `{0}` sigue funcionando.

## Riesgos y rollback

- El job de alertas empieza a evaluar de verdad cada minuto: puede crear alertas para estudiantes que ya superaban el umbral. Es el comportamiento esperado de HU026C.
- Rollback: revertir el commit. No hay cambios de BD que deshacer.

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-11
---

# PLAN — PC: tomas de asistencia como puntos de control (backend, paquetes BE-1 y BE-2)

## Origen

Diseño `informes/DISENO_TOMAS_PUNTOS_CONTROL_2026-11-11.md` (secciones A y C) y contrato real de la base de datos en `Repositorio_GestionAsistenciadb/docs/work-items/PC-puntos-control-tomas/` (`PLAN.md` y `CONTRACT.md`), que manda sobre el diseño cuando difieren. La BD ya calcula las horas por tramos entre puntos de control (cada toma es un punto; un tramo es presente solo si el estudiante marcó sus dos tomas). Este work item adapta el backend a ese contrato. Rama local `feature/PC-tomas-puntos-control`, sin subir.

## Clase de cambio y alcance

Cambio funcional y aditivo de contrato HTTP. **Java no recalcula nada:** minutos, estado de la sesión, reclamabilidad y editabilidad vienen de la base de datos.

- **BE-1 (tomas):** campos nuevos en la toma, ruta nueva de resumen por estudiante y el error `TOM_012`.
- **BE-2 (estudiante y reclamos):** estado y minutos de la sesión desde la BD, lista de tomas por sesión, reclamo por toma (v4), resolución con tres salidas y datos de toma en las consultas de reclamos.

No alcance: frontend (FE-1, FE-2), objetos de BD, `SecurityConfig` más allá de la ruta nueva, notificaciones, QR y sesiones (trabajan otros miembros del equipo).

## Contrato HTTP (todo aditivo)

### Rutas

| Método y ruta | Rol | Cambio |
|---|---|---|
| `GET /api/v1/sesiones/{sesionId}/tomas/resumen` | DOCENTE titular | NUEVA. `data` es un arreglo con una fila por matrícula activa: `estudianteId`, `nombreCompleto`, `minutosSesion`, `minutosAsistidos`, `minutosFalta`, `minutosExcusa`, `minutosSinRegistro`, `estado` (`PRESENTE`, `AUSENTE`, `JUSTIFICADA`, `PARCIAL`, `NO_DISPONIBLE`), `tomasAusentes` |
| `GET /api/v1/sesiones/{sesionId}/tomas` | DOCENTE titular | Cada toma agrega `fechaPuntoControlUtc`, `ordenPuntoControl`, `minutosTramoAnterior`, `minutosTramoSiguiente`, `esEditable`, `editableHastaUtc` |
| `POST /api/v1/sesiones/{sesionId}/tomas`, `POST /api/v1/tomas/{tomaId}/cierre` | DOCENTE | La toma de la respuesta trae los mismos campos nuevos (ver desviación D-1) |
| `POST /api/v1/tomas/{tomaId}/asistencias` | DOCENTE | Puede responder 409 `TOM_012` (toma cerrada por la siguiente) |
| `GET /api/v1/estudiante/materias/{materiaId}/sesiones` | ESTUDIANTE | Cada sesión agrega `minutosSesion`, `minutosAsistidos`, `minutosFalta`, `minutosExcusa`, `minutosSinRegistro` (nulos sin cabecera de asistencia) y `tomas[]`; `estadoAsistencia` sale de la BD |
| `POST /api/v1/asistencias/revisiones` | ESTUDIANTE | Cuerpo con `tomaId` opcional. Con `tomaId`, radica el reclamo de esa toma (`usp_radicar_solicitud_revision_asistencia_v4`); sin él, el reclamo de la sesión (v3, compatibilidad) |
| `PATCH /api/v1/docente/reclamos/{id}` | DOCENTE | Cuerpo con `resultado` opcional (`PRESENTE` o `JUSTIFICADA`), solo con `accion = APROBADA`; por defecto `JUSTIFICADA` |
| `GET /api/v1/estudiante/reclamos` | ESTUDIANTE | Cada reclamo agrega `idToma`, `numeroToma`, `fechaPuntoControlUtc`, `resultadoAprobacion` |
| `GET /api/v1/docente/reclamos/{id}` | DOCENTE | Agrega esos cuatro más `estadoToma`, `minutosSesion`, `minutosAsistidos` |

Ninguna de estas rutas está en `openapi-golden-path.yaml` (verificado), así que no hay cambio de OpenAPI. La ruta nueva se agregó a `infra/security-baseline/access-catalog.json` (DOCENTE) y a `SecurityConfig` (`hasRole(DOCENTE)`); la prueba `AccessCatalogConsistencyTest` exige ambas.

### Valores

- `tomas[].estado` (sesiones del estudiante): `PRESENTE`, `AUSENTE`, `JUSTIFICADA`, `SIN_REGISTRO`.
- `tomas[]` agrega `idToma`, `numero`, `fechaPuntoControlUtc`, `reclamable`, `idSolicitudRevision`, `estadoSolicitudRevision` (`PENDIENTE`, `APROBADA`, `RECHAZADA` o nulo) y `resultadoAprobacion` (`PRESENTE`, `JUSTIFICADA` o nulo). La BD entrega los códigos `P/A/R` y `AN/EX`; el backend los traduce.
- `reclamable` de la sesión = alguna toma reclamable (si la sesión no tiene filas de toma, se conserva la regla anterior).
- `estadoToma` del detalle del docente: marca actual del estudiante en la toma reclamada (`PRESENTE`, `AUSENTE`, `JUSTIFICADA`, `SIN_REGISTRO`); nulo en un reclamo de la sesión.
- Errores nuevos: `TOM_012` (409), `ERR_TOMA_NO_RECLAMABLE` (REV_026, 409), `ERR_RECLAMO_TOMA_DUPLICADO` (REV_027, 409), `ERR_RESULTADO_APROBACION_INVALIDO` (REV_028, 400) y `ERR_APROBACION_PRESENTE_SIN_TOMA` (REV_029, 409). `TOM_012` se expone con su código del catálogo y el mensaje de usuario que arma la BD.

## Piezas tocadas

- **Tomas:** `TomaAsistenciaResumen` (6 campos), `ResumenTomasSesionFila` (nuevo), `TomaAsistenciaRepositoryPort` y `TomaAsistenciaJpaRepository` (`consultarResumenTomasSesion`, mapeo por nombre de las columnas nuevas), `UvTomaAsistenciaEntity`, feature nueva `consultarresumensesion` (puerto, caso de uso con titularidad, wiring), `SesionTomaController`, `TomaAsistenciaHttpResponses`, `TomaAsistenciaErrorCode.TOM_012`.
- **Estudiante:** `AsistenciaSesionEstudianteQueryPort` y su adaptador (HQL sobre las vistas nuevas `uv_asistencia_toma_estudiante` y `uv_asistencia_sesion_horas`, mismo aislamiento por estudiante y asignatura y mismo troceo del `IN`), entidades `UvAsistenciaTomaEstudianteEntity` y `UvAsistenciaSesionHorasEntity`, proyecciones nuevas, `ConsultarSesionesMateriaEstudianteUseCaseImpl` (`resolverEstado` por marcas queda solo como respaldo si no hay fila de la BD), DTO, dominio, mappers y respuesta.
- **Reclamos:** `tomaId` por request, DTO, dominio, mapper y `AsistenciaJpaRepository` (v4); `resultado` por request, DTO, dominio (`ResultadoAprobacionReclamo`, valida y traduce a `AN/EX`) y `@codigoResultadoAprobacion` en el `EXEC` del resolver; consultas de reclamos del estudiante y del docente (entidades `Sp*`, proyecciones, dominios, DTO, respuestas); códigos nuevos en `AsistenciaErrorCode`, `DbFailureClassifier` y `DbExceptionTranslator`.
- Se mantienen constructores de compatibilidad en los records y DTO ampliados, para no romper consumidores ni pruebas.

## Desviaciones del diseño

- **D-1.** `esEditable` y `editableHastaUtc` solo son exactos en el listado por sesión (`usp_consultar_tomas_sesion`). Las respuestas de abrir y cerrar releen la toma de la vista `uv_toma_asistencia`, que no trae la ventana: allí `esEditable` vale "abierta, o sin toma posterior" y `editableHastaUtc` es la apertura de la toma siguiente (nulo si no hay). El frontend debe usar el listado para decidir la edición.
- **D-2.** `resultado` con `accion = RECHAZADA` se ignora (el diseño decía "solo con APROBADA"); un valor distinto de `PRESENTE`/`JUSTIFICADA` con APROBADA es 400.
- **D-3.** `estadoToma` del detalle del docente se calcula en la capa de aplicación a partir de `codigoRazonCausaToma` (el SP no entrega el estado ya traducido).
- **D-4.** `estadoSolicitudRevision` de la toma usa nombres en mayúscula (`PENDIENTE`, `APROBADA`, `RECHAZADA`), traducidos desde los códigos de `dbo.Estado`; el de la sesión conserva el nombre de la BD como hasta ahora.

## Riesgos y rollback

- La ruta nueva requiere aprovisionar el PDP (`provision_asistencias.py`, sin `--prune`); no se ejecutó.
- Las pruebas de integración contra SQL Server (`-Pintegration`) requieren la BD con la migración 37; no se corrieron aquí.
- Rollback: revertir los cambios del backend; la BD no depende de ellos y los campos nuevos de respuesta son aditivos.

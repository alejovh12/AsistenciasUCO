# MEJORAS-SERGIO-2026-11-18 — Backend

Orden de John (2026-11-18): ejecutar las 13 mejoras reportadas por Sergio. El plan común (13 ítems y decisiones) está en el work
item homónimo del frontend; la migración 50 y el detalle de BD en el work item homónimo de la base de datos.

- **Clase de cambio:** retiro de un concepto de negocio (tipo de vinculación del docente) en las tres capas + campo aditivo.
- **Contrato:** cambia de forma coordinada con la BD de la misma rama (no hay consumidores externos además del frontend).

## Ítems con cambio en el backend

| Ítem | Cambio |
|---|---|
| 1 y 12 | Se retira el tipo de vinculación: feature `tipovinculacion` completa (`GET /api/v1/tipos-vinculacion`, caso de uso, puerto, proyección, entidad `UvTipoVinculacionEntity`, repositorio JPA y wiring), el campo `idTipoVinculacion` de `VincularDocenteProgramaRequest`/`ActualizarDocenteProgramaRequest` y sus validadores, el filtro `idTipoVinculacion` de `GET /coordinador/docentes`, las columnas `idTipoVinculacion/codigoTipoVinculacion/nombreTipoVinculacion` del directorio (DTO, dominio, proyección, entidad), el desglose `porTipoVinculacion` de `GET /coordinador/docentes/resumen` (ahora `totalDocentes`, `totalActivos`, `totalInactivos` desde una sola fila del SP) y el código `ERR_TIPO_VINCULACION_INVALIDO` (enum, traductor `DOC_004`, alias `ERR109` de MessageUcoLab y fixture del catálogo). Entrada del catálogo de accesos retirada. |
| 5 | `GET /decano/estudiantes` agrega `codigoTipoIdentificacion` (última columna de `usp_consultar_estudiantes_facultad_paginado`). Aditivo. |
| 2 | Sin cambio: `AprobarSolicitudSesionUseCaseImpl` ya encola `SESION_MANUAL_CREADA_ESTUDIANTE` a los estudiantes activos (outbox del `NotificacionPort`), avisa al docente y publica el evento SSE. Cubierto por `AprobarSolicitudSesionUseCaseImplTest`. |
| 10 | Sin cambio: `CancelarSesionUseCaseImpl` (HU047, `PATCH /docente/sesiones/{id}/cancelar`) ya encola `SESION_CANCELADA` tras el commit. El frontend expone el mismo flujo desde `/app/asistencia`. |
| 8 | Sin cambio: no hay estado financiero ni promedio ponderado en el backend (búsqueda en `src/`: 0 coincidencias). |

## Verificación de los ítems 2 y 10 (contrato con el frontend)

- **2:** `POST /coordinador/solicitudes-sesion/{id}/aprobar` → `AprobarSolicitudSesionUseCaseImpl`: tras el SP consulta
  `usp_consultar_destinatarios_solicitud_sesion`, encola UNA notificación `SESION_MANUAL_CREADA_ESTUDIANTE` (plantilla por
  defecto en `PlantillaPorDefectoCatalogo`), otra `SESION_SOLICITUD_APROBADA_DOCENTE` y publica `SESION_MANUAL_CREADA` por SSE.
- **10:** `PATCH /docente/sesiones/{sesionId}/cancelar` con cuerpo `{ "motivo": "..." }` (`@NotBlank`, máx. 500; la BD exige
  10..500 con `SES_013`) responde `SesionCanceladaResponse { idSesion, idGrupo, estado, cancelacion, estudiantesNotificados }`.
  El frontend (`SessionService.cancelarSesion`, modelo `SesionCancelada`) envía y lee exactamente esos campos.

## Búsqueda de remanentes

`git grep` de `tipoVinculacion`, `TipoVinculacion`, `tipo_vinculacion`, `tipos-vinculacion`, `porTipoVinculacion`,
`estado financiero`, `promedio ponderado` y `promedioAcumulado` en `src/` y `docs/contracts/`: solo quedan comentarios que
documentan el retiro y aserciones de pruebas que verifican la ausencia (`doesNotExist()`). Se corrigieron 5 comentarios que aún
describían el tipo como vigente. Quedan, a propósito:

- Work items históricos (HU062, HU091-HU095, HU006, etc.): registran lo que se hizo en su momento; no se reescriben.
- `scratch/test_hu091.ps1`, `test_hu092.ps1`, `test_hu093.ps1`: E2E manuales históricos que ya estaban desactualizados (envían
  `password`, retirado por IDENT-002); no forman parte de ninguna compuerta.

`GET /decano/estudiantes` (ítem 5) no tiene fila propia en `HTTP_AS_IS_MATRIX.md` (brecha previa de la matriz); el cambio es
aditivo y lo cubre `DecanoFacultadConsultaControllerTest`.

## Validación

Ver `VALIDATION.md` (ejecución dirigida en el equipo local; el `verify` completo queda para el CI por el 401 de GitHub Packages).

## Rollback

Revertir los commits de la rama junto con la BD (migración 50 down + `deploy_schema.ps1` del commit anterior).

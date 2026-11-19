# PLAN — HU074: Activar o inactivar asignaturas (backend)

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-11-19 / implementador (agente) por orden de John / `develop` en `9adf99fb`, rama
  `feat/HU074-HU178-asignatura-estado-ambito` (sin commit).
- Objetivo: que el backend transporte las reglas de HU074 que la migración 57 de la BD pone en los SP:
  1. `PATCH /api/v1/coordinador/asignaturas/{id}/estado` rechaza inactivar con **409 `ASI_009`** si la asignatura tiene grupos en
     el período vigente o en períodos futuros, y sigue rechazando con **409 `ASI_006`** si la comparten programas que el
     coordinador no coordina.
  2. Una asignatura inactiva no se usa: `usp_crear_grupo` y `usp_asociar_asignatura_plan` la rechazan con **409 `ASI_010`**.
  3. El éxito del PATCH lleva el mensaje del SP (`ASI_011` quedó activa / `ASI_012` quedó inactiva) y el estado nuevo.
  4. (Ajuste de la revisión independiente, 2026-11-19) El PATCH acepta un cuerpo opcional `{"estaActiva": true|false}` que
     viaja como `@estaActivaDeseada` (parámetro aditivo de la BD, `NULL` = alternar), y el estado de la respuesta sale del
     código de éxito del SP, no de la lectura previa.
- Criterios de aceptación: los tres rechazos llegan como 409 con su propio código y el texto del catálogo de mensajes (no el
  genérico de `CONFLICT`) en todos los endpoints que llaman esos SP; el 200 del PATCH trae `mensajeUsuario` y
  `datos.estaActivaAsignatura`; `mvnw verify` verde.
- Skills y fuentes: `uco-contratos`, `uco-catalogos`, `uco-testing`; migración 57 y `docs/catalogos/catalogo-mensajes.md` del
  repositorio de BD (misma rama).

## AS-IS y evidencia

| Hecho | Archivo + símbolo | Evidencia y límites |
|---|---|---|
| El PATCH respondía `200 {exitoso:true, datos:null}` sin mensaje | `CoordinadorPortalController.toggleEstadoAsignatura` | Lectura de código en `9adf99fb` |
| `ASI_006` se publicaba como `CONFLICT` con el texto genérico "La operacion genera un conflicto con el estado actual." | `DbFailureClassifier.classifyDbCode` → `CommonErrorCode.CONFLICT`; `ApiErrorCatalog.fromDefinition` busca el texto por `messageCode()` = `CONFLICT` | El texto institucional del catálogo no llegaba al usuario |
| El éxito del SP se descartaba | `AsignaturaJpaRepository.toggleEstadoAsignatura` (`void`) | `ProcedureResult.getMensajeUsuarioResultado()` disponible |
| Único llamador de `usp_crear_grupo`: `POST /api/v1/grupos` (solo COORDINADOR, DEC-065 (4)) | `GrupoJpaRepository.crearGrupo`, `SecurityConfig` | El DOCENTE recibe 403 antes de llegar al SP |
| Único llamador de `usp_asociar_asignatura_plan`: `POST /api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas` | `AsignaturaPlanEstudioJpaRepository` (`OP_ASOCIAR`) | |
| `usp_consultar_oferta_creacion_grupo_docente` no lo llama el backend | `git grep` en `src/` sin coincidencias | Sin cambio en el backend |
| Toda falla de SP pasa por `DbExceptionTranslator.throwIfFailed` / `translateDbCode` → `DbFailureClassifier` | `ProcedureResultValidator` | Clasificar el código una vez cubre todos los endpoints |

## TARGET

- `CatalogoErrorCode.ASI_006`, `ASI_009`, `ASI_010` (`ErrorKind.CONFLICT`, código público = código del catálogo, texto de
  respaldo igual al del catálogo) en el clasificador y en `CONFLICT_CODES` del traductor.
- `AsignaturaCommandPort.toggleEstadoAsignatura(idAsignatura, estaActivaDeseada, usuarioEjecutor)` envía
  `@estaActivaDeseada` y devuelve `ResultadoEstadoAsignaturaProjection(mensajeUsuario, estaActivaResultante)`; el adaptador lee
  el código del canal técnico `DBCODE=<codigo>|…` con `DbExceptionTranslator.dbCodeOf` (mismo parser que los rechazos) y lo
  traduce (`ASI_011` → `true`, `ASI_012` → `false`, otro o ausente → `null`). El protocolo del SP queda en Infrastructure
  (`UseCasesShouldNotKnowSqlResultProtocolTest` prohíbe que un `UseCaseImpl` lo conozca).
- El caso de uso devuelve `EstadoAsignaturaDomain(idAsignatura, estaActivaAsignatura, mensajeUsuario)`; el estado es el que
  confirmó el SP (`estaActivaResultante`). Sin código (SP anterior): el estado
  pedido y, si se pidió alternar, el contrario al leído en la validación de ámbito (misma fila de
  `consultarAsignaturasPorPrograma`, sin consulta extra).
- Controlador: `@Valid @RequestBody(required = false) CambiarEstadoAsignaturaRequest` (patrón de los PATCH con cuerpo opcional
  del repo); el único campo es un `Boolean` opcional, así que no hay validador propio: un valor no booleano es 400 de Jackson.
- Mensaje vacío del SP: `MessageCatalogPort.findUserMessage(ASI_011|ASI_012)` y, sin catálogo, el respaldo de
  `MensajeEstadoAsignatura`.
- Respuesta del PATCH: `ApiDataMensajeResponse<EstadoAsignaturaResponse>` (ver `CONTRACT.md`).

## Clase de cambio y alcance de rutas

- Change class: BEHAVIOR_CHANGE (con cambio de contrato HTTP aditivo).
- PRIMARY_VARIABLE: traducción de los códigos `ASI_006/009/010` y respuesta del PATCH de estado de la asignatura.
- ALLOWED_PATHS: `application/features/coordinador/gestionarasignatura/**`, `application/features/admin/exception/CatalogoErrorCode.java`,
  `application/secondaryports/academic/AsignaturaCommandPort.java`, `infrastructure/.../support/error/**`,
  `infrastructure/.../jpa/repository/AsignaturaJpaRepository.java`, `infrastructure/.../controller/coordinador/**`,
  `infrastructure/config/wiring/CoordinadorWiringConfiguration.java`, sus pruebas, el fixture
  `src/test/resources/messageucolab/catalogo-mensajes-usuario.psv` y la documentación de este work item y de contratos/pruebas.
- FORBIDDEN_PATHS: SQL y esquema (son del repositorio de BD), `SecurityConfig`, OpenAPI golden path.
- CONTRACTS: HTTP del PATCH (aditivo: respuesta y cuerpo opcional) y cuerpo de error 409 de los tres códigos. DB: resultsets sin
  cambio; `usp_toggle_estado_asignatura` suma `@estaActivaDeseada BIT = NULL` al final (aditivo, owner de BD, misma rama);
  `SqlStoredProcedureContractIT` fija la firma nueva.
- PROVIDERS: SQL Server (SP de la migración 57), catálogo de mensajes (MessageUcoLab / DB).
- SECURITY_IMPACT: ninguno (RBAC y ámbito sin cambio).
- OBSERVABILITY_IMPACT: ninguno (el log de rechazo ya registra `dbCode`).
- TEST_LEVEL_REQUIRED: unitarias del clasificador/traductor, caso de uso y controladores (MockMvc standalone).
- ROLLBACK: revertir los commits de la rama; la BD debe revertir la migración 57 en el mismo despliegue.
- CONSUMERS: frontend (SPA) de la misma rama.
- STOP_CONDITIONS: códigos o textos de la migración 57 distintos de los acordados → usar los de la BD y reportar.

## Alcance

Clasificación 409 de `ASI_006/009/010`; mensaje y estado nuevo en el 200 del PATCH; filas `ASI_009..ASI_012` en el fixture del
catálogo; pruebas y documentación.

## No alcance

- `ASI_002`, `ASI_003` y `ASI_005` siguen publicándose como `CONFLICT` con el texto genérico (deuda registrada en `CONTRACT.md`).
- Oferta de creación de grupo del docente (`usp_consultar_oferta_creacion_grupo_docente`): no la consume el backend.
- Malla (`GET /coordinador/planes-estudio/{id}/asignaturas`): ya devuelve `estaActivaAsignatura`; sin cambio.

## Archivos afectados

EXISTENTES: `CatalogoErrorCode`, `DbFailureClassifier`, `DbExceptionTranslator` (+ `dbCodeOf`), `AsignaturaCommandPort`, `AsignaturaJpaRepository`,
`GestionarAsignaturaUseCase(Impl)`, `GestionarAsignaturaInputPort`, `GestionarAsignaturaInteractor`, `GestionarAsignaturaMapper`,
`CoordinadorWiringConfiguration`, `CoordinadorPortalController`, fixture `catalogo-mensajes-usuario.psv`.
NUEVOS: `EstadoAsignaturaDomain`, `MensajeEstadoAsignatura`, `EstadoAsignaturaDTO`, `EstadoAsignaturaResponse`,
`ResultadoEstadoAsignaturaProjection`, `CambiarEstadoAsignaturaRequest`, `DbExceptionTranslatorAsignaturaEstadoTest`.
PRUEBAS DE INTEGRACIÓN (solo compilan en `verify` sin `-Pintegration`): `SqlStoredProcedureContractIT` (firma),
`AcademicUserCommandsSpParityIT` y `AcademicUserCommandsSuccessParityIT` (llamada con `estaActivaDeseada = null`).

## Contratos y consumidores afectados

HTTP: ver `CONTRACT.md`. El consumidor es la SPA de la misma rama; el 200 conserva `exitoso` y solo llena `datos` y agrega
`mensajeUsuario` (aditivo). `ASI_006` cambia su `code` público de `CONFLICT` a `ASI_006` (mismo 409) y su `message` pasa del
genérico al institucional.

## Riesgos y dependencias

- Depende de la migración 57 de la BD (misma rama) **con** `@estaActivaDeseada`: el backend siempre envía el parámetro (con
  `NULL` si no hay cuerpo), así que una BD sin él rechaza la llamada ("no es un parámetro del procedimiento"). Despliegue
  conjunto BD + backend. Con la migración 57 sin el código de éxito, el backend usa el estado pedido o el contrario al leído.
- Las filas `ASI_009..ASI_012` deben registrarse en MessageUcoLab; mientras tanto, el 409 usa el texto de respaldo del enum y el
  200 usa el mensaje que entregó la BD.

## Test plan

Ver `TEST_PLAN.md`.

## Rollback y stop conditions

Sin conflictos abiertos. Ver `VALIDATION.md` para la confirmación de los códigos contra la migración 57.

## Deuda conocida y validación manual

- Registrar `ASI_009`, `ASI_010`, `ASI_011` y `ASI_012` en MessageUcoLab y regenerar el fixture con `build-message-catalog.py`
  cuando la semilla de la BD esté publicada.
- Validación manual E2E en pantalla: HECHA el 2026-11-19 con `uco-coordinador` (ver `VALIDATION.md`), antes del cuerpo
  opcional. El cuerpo `estaActiva` queda cubierto por pruebas unitarias y MockMvc; su E2E queda para cuando el frontend lo envíe.

## Definition of Ready

READY: orden de John (2026-11-19), reglas de la migración 57 descritas por el owner de BD, sin cambios de firma de SP.

# TEST_PLAN — HU074: Activar o inactivar asignaturas

## Fuentes

`PLAN.md`, `CONTRACT.md`, migración 57 de la BD (misma rama).

## Behavioral Matrix

| ID | Requirement | Scenario | Action | Expected | Wrong implementation caught | Level / prueba |
|---|---|---|---|---|---|---|
| T01 | ASI_006/009/010 son 409 con su código | DBCODE del SP | `DbFailureClassifier.classify` | `CatalogoErrorCode.ASI_xxx`, `ErrorKind.CONFLICT`, `messageCode` = código | Mapear a `CONFLICT` genérico o a 500 | unitaria `DbExceptionTranslatorAsignaturaEstadoTest` |
| T02 | Rechazo del SP (comando) | `throwIfFailed` con DBCODE en toggle, crear grupo y asociar | traducir | `ConflictException` con `code` ASI_xxx y texto de respaldo | Perder el código o el tipo | unitaria `DbExceptionTranslatorAsignaturaEstadoTest` |
| T03 | Rechazo de un SP de consulta | `translateDbCode` | traducir | `ConflictException` ASI_xxx | Quedar como fallo técnico | unitaria `DbExceptionTranslatorAsignaturaEstadoTest` |
| T04 | ASI_002/003/005/MQR_008 no cambian | DBCODE | clasificar | `CommonErrorCode.CONFLICT` | Regresión | unitaria `DbFailureClassifierAsignaturaPlanTest` |
| T05 | 200 del PATCH | caso de uso devuelve estado y mensaje | `PATCH .../estado` | 200, `datos.idAsignatura`, `datos.estaActivaAsignatura`, `mensajeUsuario` | Contrato `datos: null` sin mensaje | web `CoordinadorPortalControllerTest` |
| T06 | 409 ASI_009 y ASI_006 del PATCH | el puerto lanza `ConflictException` | `PATCH .../estado` | 409, `code` ASI_xxx, `message` del catálogo | Texto genérico de CONFLICT | web `CoordinadorPortalControllerTest` |
| T07 | Estado nuevo = el del código de éxito | asignatura activa, SP `ASI_012` | `toggleEstado` | `estaActivaAsignatura=false`, mensaje del SP recortado, sin consultar el catálogo | Estado invertido | unitaria `GestionarAsignaturaUseCaseImplTest` |
| T08 | Mensaje vacío → catálogo | SP con mensaje en blanco, asignatura inactiva | `toggleEstado` | texto de `ASI_011` del catálogo | Mensaje vacío al usuario | unitaria `GestionarAsignaturaUseCaseImplTest` |
| T09 | Sin mensaje ni catálogo → respaldo | SP `null`, catálogo vacío | `toggleEstado` | respaldo de `ASI_012` | Código crudo o nulo | unitaria `GestionarAsignaturaUseCaseImplTest` |
| T10 | Rechazo se propaga | el comando lanza ASI_009 | `toggleEstado` | `ConflictException` ASI_009 | Tragarse el error | unitaria `GestionarAsignaturaUseCaseImplTest` |
| T11 | El comando devuelve el mensaje del SP | resultset canónico | `toggleEstadoAsignatura` | `mensajeUsuarioResultado` | Descartar el mensaje | unitaria `AsignaturaJpaCommandPatternTest` (ASG_PAT_003) |
| T12 | Crear grupo con asignatura inactiva | el puerto lanza ASI_010 | `POST /api/v1/grupos` | 409 `ASI_010` con texto de respaldo | 500 / CONFLICT genérico | web `GrupoControllerWriteEndpointsTest` |
| T13 | Asociar asignatura inactiva | el puerto lanza ASI_010 | `POST .../asignaturas-asociadas` | 409 `ASI_010` con texto del catálogo | 500 / CONFLICT genérico | web `AsignaturaPlanControllerComandosTest` |
| T15 | Cuerpo opcional | `{"estaActiva": true}`, `false`, `null`, `{}` y sin cuerpo | `PATCH .../estado` | el puerto recibe `true` / `false` / `null` / `null` / `null`; 200 | Ignorar el cuerpo o exigirlo | web `CoordinadorPortalControllerTest` (`hu074_toggle_estado_pasa_el_estado_pedido_en_el_cuerpo`, `toggleEstado` sin cuerpo) |
| T16 | Cuerpo inválido | `{"estaActiva": "quizas"}` | `PATCH .../estado` | 400, sin llamar al puerto | Alternar con un cuerpo ilegible | web `CoordinadorPortalControllerTest` |
| T17 | Estado del código del SP aunque la lectura previa diga lo contrario | lectura `true`/`false` × deseado `null`/`true`/`false` × `ASI_011`/`ASI_012` | `toggleEstado` | `ASI_011` → `true`, `ASI_012` → `false` (incluye leída `true` + `ASI_011`, leída `false` + `ASI_012`) | Derivar el estado de la lectura previa | unitaria `GestionarAsignaturaUseCaseImplTest` (6 casos) |
| T18 | Sin código de éxito (SP anterior) | código `null` o ajeno (`GEN_004`) | `toggleEstado` | estado pedido; si se pidió alternar, el contrario al leído | Estado nulo o invertido | unitaria `GestionarAsignaturaUseCaseImplTest` (5 casos) |
| T19 | Adaptador: parámetro y código | `estaActivaDeseada` `null`/`true`/`false`; técnico `DBCODE=ASI_011\|…` | `toggleEstadoAsignatura` | SQL con `@estaActivaDeseada = :estaActivaDeseada`, binding del valor, `estaActivaResultante` del código de éxito (`ASI_011` true, `ASI_012` false, otro o sin canal `null`) | Perder el parámetro o el código | unitaria `AsignaturaJpaCommandPatternTest` (ASG_PAT_003, ASG_PAT_006) |
| T20 | Parser del canal técnico | `DBCODE=ASI_011\|…`, `DBCODE=ASI_012\|`, texto libre, `null` | `DbExceptionTranslator.dbCodeOf` | código o vacío | Leer códigos de texto libre | unitaria `DbExceptionTranslatorAsignaturaEstadoTest` |
| T21 | Firma del SP | BD migrada | `SqlStoredProcedureContractIT` | `@estaActivaDeseada bit` como último parámetro de entrada | Desfase de firma backend/BD | integración (`-Pintegration`, CI) |
| T14 | Catálogo | filas ASI_009..ASI_012 en el fixture | `MessageUcoLabCatalogoTest`, `MensajeUsuarioErrorCodeCatalogoTest` | 492 filas; todo `ErrorDefinition` de Application con fila | Código sin catálogo | unitaria |

Seguridad: sin cambio (el cuerpo no cambia el RBAC ni el ámbito) (403 de ámbito ya cubierto por `toggleEstado_rechaza_asignatura_fuera_del_programa_del_actor`).

## Integration Requirement

- REAL_PROVIDER_REQUIRED: NO para las unitarias; la firma nueva de `usp_toggle_estado_asignatura` (`@estaActivaDeseada`) la fija
  `SqlStoredProcedureContractIT` (T21), que corre con `-Pintegration` en el CI contra la BD de la misma rama.
- ENVIRONMENT: unitarias y MockMvc standalone.
- MOCK_SUFFICIENT: YES para la traducción y el contrato HTTP; la regla de negocio (grupos vigentes o futuros, inactiva) es de la
  BD y la prueba su repositorio.
- WHY: el backend solo transporta códigos y mensajes.

## Negative / Boundary Coverage

T04, T06, T09, T10, T12, T13, T16, T17, T18.

## Side Effects / Rollback

Sin efectos nuevos en el backend.

## Pruebas RED requeridas / RED_SNAPSHOT

NO APLICA como RED formal: las pruebas se escribieron junto con la implementación en la misma sesión (sin tester independiente);
se registra en `VALIDATION.md`.

---
status: draft
type: work-item
scope: backend-db-frontend
owner: backend-team
last-reviewed: 2026-11-19
---

# CONTRACT PROPOSAL — SES-GEN-01: calendario de sesiones del semestre al definir el grupo

**Estado: DRAFT, sin implementar.** Change class `CONTRACT_ANALYSIS`: este documento **no crea** ningún endpoint, clase, SP ni columna; propone el contrato TARGET a partir del AS-IS de la [CONTRACT_MATRIX](CONTRACT_MATRIX.md) y de las decisiones 1-7 del [PLAN](PLAN.md). Todo lo que aquí figure como «existe» está respaldado en la matriz; todo lo demás es **propuesta** sujeta a aprobación (decisiones `DR-xx` en §9, bloqueos `BM-xx` en [CONTRACT_CONFLICTS](CONTRACT_CONFLICTS.md)). Reglas aplicadas: [API_DESIGN_RULES](../../governance/API_DESIGN_RULES.md), [OPENAPI_STANDARD](../../contracts/OPENAPI_STANDARD.md), [REALTIME_EVENT_STANDARD](../../contracts/REALTIME_EVENT_STANDARD.md).

## 1. Resumen

| # | Contrato | AS-IS (comprobado) | TARGET propuesto | Tipo de cambio |
|---|---|---|---|---|
| 1 | Previsualización del horario | No existe | `POST /api/v1/coordinador/grupos/previsualizacion-horario`, solo lectura, 200 con conflictos como datos | Ruta nueva, aditiva (PDP +1) |
| 2 | `franjas` al crear grupo | Opcional | Obligatoria (≥1) en `POST /api/v1/grupos`; en `PUT` ausente = «no cambia» (igual) | Comportamiento (rompe a quien no las envíe) |
| 3 | Respuesta de crear/editar grupo | `{id, mensajeUsuario}` | Sin cambio (opción recomendada); resumen aditivo solo si se aprueba (DR-08) | Ninguno / aditivo |
| 4 | `horasSemanales` de la asignatura | No existe | Campo `number \| null` en lectura y escritura de asignaturas del coordinador | Aditivo, con secuencia de despliegue por conteo estricto de columnas |
| 5 | Período académico | Solo `inicio < fin` | Duración fija de 112 días, `fechaFin` calculada | Comportamiento (rechaza duraciones distintas) |
| 6 | Códigos y mensajes | HOR_001..005, PER_001..009, … | Códigos nuevos provisionales (§6) + mapeo de `SES_005` | Catálogo (DB) y clasificador (backend) |
| 7 | `POST /api/v1/sesiones/grupo/{grupoId}/generacion` | Solo DOCENTE, legado, fuera del OpenAPI | Sin cambio; interacción documentada | Ninguno |

## 2. Previsualización del horario propuesto

### 2.1 Clasificación (API_DESIGN_RULES)

| Criterio | Propuesta | Justificación |
|---|---|---|
| Estilo | `PRAGMATIC_RESOURCE_PLUS_BUSINESS_COMMAND` | Vigente |
| RESOURCE vs BUSINESS_COMMAND | **BUSINESS_COMMAND de solo lectura** («previsualizar» un horario propuesto) | El resultado no es un recurso guardado ni tiene identidad: se calcula a partir de un cuerpo estructurado (franjas + ids). No encaja en «¿lectura de un recurso? → GET» porque el insumo es una lista de franjas, no un identificador |
| Método | **POST** | Alternativa **GET** con query (`franjas=LU@08:00-10:00,…`): descartada por longitud, escape y por codificar una lista de objetos en la URL; el GET queda reservado a lecturas de recursos. La regla exige documentar alternativas cuando una operación encaja en más de una rama |
| URL | `/api/v1/coordinador/grupos/previsualizacion-horario` | Verbo de dominio tras el recurso (sustantivo compuesto en español, kebab-case). Va bajo `/api/v1/coordinador/**`, que ya limita a `COORDINADOR` (`SecurityConfig` L151) y excluye a `ADMINISTRADOR` (DEC-022 §2). Alternativa `/api/v1/grupos/previsualizacion-horario`: exigiría un matcher explícito previo a `POST /api/v1/grupos/**` (que admite `ADMINISTRADOR`) |
| Una sola ruta para crear y editar | Sí: `idGrupo` opcional en el cuerpo | Cumple la decisión 5 (**+1 ruta**). Dos rutas serían +2 en el PDP |
| `METHOD_EXCEPTION` | **No aplica** (ni PUT ni DELETE) | — |
| Idempotencia | `IDEMPOTENT: YES` (no escribe estado de negocio) | Sujeto a DR-07 (carga de festivos, efecto lateral de catálogo) |
| Idempotency-Key / ETag | `NOT_DEFINED` (no se inventan) | La previsualización no reserva nada; la confirmación puede fallar por cambios intermedios |
| Golden Path | **No entra**. `NON_GOLDEN_PATH_EXTENSION` (como `/generacion`) | `openapi-golden-path.yaml` cubre el flujo de sesiones/asistencia; las rutas de portales se contratan en el work item |
| Gate Contract First | El contrato se congela en un `CONTRACT.md` de este work item (+ fila en `HTTP_AS_IS_MATRIX` + entrada en `access-catalog.json`) **antes** del TEST_PLAN; el OpenAPI canónico no cambia salvo que DR-12 decida lo contrario | Es la práctica vigente (HU096, CIERRE-W2A). `mvn verify` solo valida el OpenAPI del Golden Path |
| Versionado | `/api/v1`, aditiva; sin v2 | — |

### 2.2 Petición

```
POST /api/v1/coordinador/grupos/previsualizacion-horario
Authorization: Bearer <JWT>          # rol COORDINADOR
Content-Type: application/json

{
  "idGrupo": null,                     // uuid; null/ausente = grupo nuevo
  "idAsignatura": "<uuid>",            // obligatorio si no hay idGrupo; con idGrupo se toma del grupo
  "idPeriodoAcademico": "<uuid>",      // obligatorio si no hay idGrupo; con idGrupo se toma del grupo
  "idDocente": "<uuid>",               // obligatorio si no hay idGrupo; con idGrupo ausente = titular actual
  "franjas": [ { "dia": "LU", "horaInicio": "08:00", "horaFin": "10:00" } ]   // ≥ 1
}
```

- Nombres canónicos únicos: no se replican los alias `asignaturaId`, `periodoAcademicoId`, `docenteId` ni el formato legado `dias + horaInicio/horaFin` (ruta nueva, sin clientes anteriores).
- `franjas[]` reutiliza la forma de `FranjaHorarioRequest`: `dia` (código `LU…DO` o nombre) y horas `HH:mm` locales. Tope máximo de franjas: `NOT_DEFINED` (no hay evidencia; a decidir en el TEST_PLAN).
- Con `idGrupo`, si `idAsignatura`/`idPeriodoAcademico` vienen y difieren del grupo ⇒ 400 `VALIDATION_ERROR`.

### 2.3 Respuesta `200` (`ApiDataResponse<PrevisualizacionHorarioGrupoDTO>`, nombre ilustrativo)

Las decisiones de negocio salen **de la DB** (una sola definición de ranuras compartida con la precarga, ver H-10); el backend solo traduce.

| Campo | Tipo | Nulo | Semántica |
|---|---|---|---|
| `idGrupo` | uuid | sí | Eco; `null` al crear |
| `asignatura` | `{id, codigo, nombre, horasSemanales}` | no | `horasSemanales`: `number`, **`null` = sin horas cargadas** (decisión 1c) |
| `periodo` | `{id, nombre, fechaInicio, fechaFin, diasPeriodo, semanasPeriodo, estado}` | no | Fechas `yyyy-MM-dd` (calendario institucional, sin hora); `semanasPeriodo = diasPeriodo / 7` (16 en períodos nuevos); `estado` = PLANEACION/ACTIVO/CERRADO como `uv_periodo_academico` |
| `zonaHoraria` | string | no | Zona institucional usada (`TIEMPO/ZONA_HORARIA_IANA`) |
| `franjas[]` | `{dia, nombreDia, horaInicio, horaFin, minutos}` | no | **Horas por franja**: `minutos` (entero) = fin − inicio. Orden lunes→domingo y por hora |
| `resumenSemanal` | `{franjasPorSemana, minutosSemanales, horasSemanales, horasSemanalesMateria, diferenciaMinutos, coincideConMateria}` | no | `horasSemanales` = `minutosSemanales / 60` (la unidad real de «hora» está en BM-02). `horasSemanalesMateria`, `diferenciaMinutos` y `coincideConMateria` son `null` si la materia no tiene horas (B5 no aplica) |
| `resumenPeriodo` | ver abajo | no | Totales del período |
| `festivos[]` | `{fecha, nombre, sesionesAfectadas}` | no | Festivos activos que caen en una ranura de la propuesta |
| `sesiones[]` | `{numero, numeroSemana, fecha, horaInicio, horaFin, fechaHoraInicioUtc, fechaHoraFinUtc, minutos, estado, nombreFestivo}` | no | Todas las ranuras del horario propuesto en el período. `numeroSemana` ∈ 1..16 en períodos de 112 días (requiere B2b). `estado`: `PROGRAMADA`, `CANCELADA_POR_FESTIVO`, `NO_PROGRAMABLE_POR_FECHA` (ya inició: el pasado no se genera, decisión 6). Fechas UTC con `Z` |
| `cambioSobreGrupoExistente` | `{sesionesFuturasQueSeCrean, sesionesFuturasQueSeRetiran, sesionesConservadas, horasYaDictadas}` | sí | Solo con `idGrupo`: respeta pasado, manuales y sesiones con asistencia/toma/clasificación (B10) |
| `conflictos[]` | `{codigo, severidad, mensaje, franja}` | no | `codigo` = código de catálogo; `mensaje` = texto del catálogo (no hay texto en el código); `severidad` ∈ `BLOQUEANTE` (la confirmación lo rechazaría), `ADVERTENCIA`, `INFORMATIVO`; `franja` opcional |
| `puedeConfirmar` | boolean | no | `true` si no hay conflictos `BLOQUEANTE`. Informativo: la confirmación **revalida todo** (decisión 5) |

`resumenPeriodo`:

| Campo | Semántica (cada ranura cae en **una** categoría) |
|---|---|
| `sesionesCalendario` | Ranuras del horario propuesto en todo el período (según calendario real, sin escribir «16» en el código) |
| `sesionesProgramadas` | Futuras y no festivas (nacerán activas) |
| `sesionesEnFestivo` | Futuras en día festivo (nacen canceladas; no se pierden del registro) |
| `sesionesNoProgramables` | Ya iniciaron (grupo creado tarde, decisión 6) |
| `horasCalendario`, `horasProgramadas`, `horasPerdidasPorFestivo`, `horasNoProgramables` | Mismo reparto en horas; `horasCalendario = horasProgramadas + horasPerdidasPorFestivo + horasNoProgramables`. Al editar, `horasProgramadas` suma además las sesiones conservadas |
| `horasEsperadasMateria` | `horasSemanales × semanasPeriodo`; `null` si la materia no tiene horas (decisión 2b) |
| `diferenciaHoras`, `cubreTotalEsperado` | `horasProgramadas − horasEsperadasMateria`; `null` si no hay horas de la materia. **Informa, no bloquea** (decisiones 2c y 3) |

Ejemplo **ilustrativo** (no es dato real; festivo de ejemplo): materia de 4 h semanales, 2 franjas de 2 h (LU y MI 08:00-10:00), período `2026-02-02 … 2026-05-24` (112 días):

```json
{ "exitoso": true, "datos": {
  "idGrupo": null,
  "asignatura": { "id": "…", "codigo": "…", "nombre": "…", "horasSemanales": 4 },
  "periodo": { "id": "…", "nombre": "…", "fechaInicio": "2026-02-02", "fechaFin": "2026-05-24", "diasPeriodo": 112, "semanasPeriodo": 16, "estado": "PLANEACION" },
  "zonaHoraria": "America/Bogota",
  "franjas": [ { "dia": "LU", "nombreDia": "Lunes", "horaInicio": "08:00", "horaFin": "10:00", "minutos": 120 },
               { "dia": "MI", "nombreDia": "Miércoles", "horaInicio": "08:00", "horaFin": "10:00", "minutos": 120 } ],
  "resumenSemanal": { "franjasPorSemana": 2, "minutosSemanales": 240, "horasSemanales": 4, "horasSemanalesMateria": 4, "diferenciaMinutos": 0, "coincideConMateria": true },
  "resumenPeriodo": { "sesionesCalendario": 32, "sesionesProgramadas": 30, "sesionesEnFestivo": 2, "sesionesNoProgramables": 0,
                      "horasCalendario": 64, "horasProgramadas": 60, "horasPerdidasPorFestivo": 4, "horasNoProgramables": 0,
                      "horasEsperadasMateria": 64, "diferenciaHoras": -4, "cubreTotalEsperado": false },
  "festivos": [ { "fecha": "2026-03-23", "nombre": "Festivo de ejemplo", "sesionesAfectadas": 1 } ],
  "sesiones": [ { "numero": 1, "numeroSemana": 1, "fecha": "2026-02-02", "horaInicio": "08:00", "horaFin": "10:00",
                  "fechaHoraInicioUtc": "2026-02-02T13:00:00Z", "fechaHoraFinUtc": "2026-02-02T15:00:00Z", "minutos": 120, "estado": "PROGRAMADA", "nombreFestivo": null } ],
  "cambioSobreGrupoExistente": null,
  "conflictos": [ { "codigo": "HOR_010", "severidad": "INFORMATIVO", "mensaje": "<texto del catálogo>", "franja": null } ],
  "puedeConfirmar": true } }
```

(`sesiones` se abrevia a un elemento; el ejemplo respeta la regla de que cualquier tramo de 112 días contiene 16 veces cada día de la semana: 16 + 16 = 32.)

### 2.4 Estados y errores

| Caso | Status | `code` | Nota |
|---|---|---|---|
| Éxito, con o sin conflictos | **200** | — | No es 201: no se crea nada. Los conflictos de negocio van como datos (criterio de aceptación 2: verlos **antes** de confirmar) |
| Cuerpo mal formado, `franjas` vacío, hora ilegible, día inexistente | 400 | `VALIDATION_ERROR` (HOR_004 / `VAL_APP_*`) | No se puede calcular |
| Asignatura, período, grupo o docente inexistente | 400 | `VALIDATION_ERROR` (VAL_002, como hoy) | Clasificación AS-IS de `VAL_002` |
| Sin Bearer / token inválido | 401 | del filtro de seguridad | — |
| Rol distinto de COORDINADOR, o ámbito ajeno (SEC_001/SEC_002) | 403 | `FORBIDDEN` | El PEP/PDP y la DB distinguen 401/403 |
| Cruce con el docente (HOR_002), franjas solapadas (HOR_003), B5/B7/B9/B1/B2 | **200**, conflicto `BLOQUEANTE` | el mismo `codigo` que daría la confirmación | Un solo catálogo para preview y confirmación |
| Falla técnica / `SES_005` sin mapear | 500 `ERR_DB_UNCLASSIFIED` | — | Ver §6 para `SES_005` |

No se añaden 404, 409 ni 422 «por convención»: ninguna condición real de esta ruta los produce.

### 2.5 Tiempo, seguridad, observabilidad

- Tiempo: franjas locales `HH:mm`; instantes nuevos con `Z`; fechas del período `yyyy-MM-dd`. La zona sale del parámetro `TIEMPO/*` (si falta, la DB aborta como hoy con `GEN_001`).
- Seguridad: rol `COORDINADOR`; el ámbito (programas de la asignatura/grupo) lo fuerza la DB con la misma regla que las escrituras. La previsualización **no debe** revelar más que lo que ya revela el error `HOR_002` (nombre y franja del otro grupo del docente). Regla de ámbito para asignaturas compartidas por varios programas: DR-14. Sin rate limit nuevo (el AS-IS solo limita subida de archivos y flujos QR); evaluar si el costo del SP lo exige.
- Observabilidad: `X-Correlation-Id` como en el resto; sin `@AuditableOperation` (no hay escritura); no registrar el cuerpo ni datos personales.

## 3. `franjas` obligatorias al crear (decisión 4)

- **Consumidor**: el único evidenciado es el formulario del coordinador (`course.service.ts crearGrupo`); además, pruebas del backend que crean grupos sin `franjas` (`GrupoControllerWriteEndpointsTest`) que habrá que reescribir en RED. No se evidencian clientes externos (BM-04). El flujo docente F3 ya no existe (CC-01), por lo que no hay otro consumidor.
- **Regla TARGET**: `POST /api/v1/grupos` exige ≥ 1 franja tras normalizar (`franjas` o el formato legado `dias + horaInicio/horaFin`). Ausente o `[]` ⇒ **400** con código `VAL_APP_076` (provisional; backend, Application). `PUT /api/v1/grupos/{id}`: ausente = «el horario no cambia» (sin cambio); `[]` = 400 (HOR_004, como hoy).

| Opción | Compatibilidad | Veredicto |
|---|---|---|
| O1. Endurecer en `/api/v1` (guarda en Application; el SP ya rechaza `[]`) | Rompe a todo cliente que no envíe `franjas`; el FE ya las envía (fila inicial por defecto), salvo editor vaciado | **Recomendada**, con aprobación explícita del cambio de comportamiento (DR-02) |
| O2. Ruta/versión nueva obligatoria y la actual opcional | Convivencia indefinida, +1 ruta en el PDP, contradice «sin v2» | Descartada |
| O3. Interruptor por parámetro | El contrato dependería de configuración | Descartada |
| O4. Mantener opcional y solo avisar | No cumple la decisión 4 | Descartada |

Secuencia de despliegue de O1: (1) Frontend: exigir ≥1 franja y no enviar `[]` (compatible con el backend actual); (2) Backend: guarda en Application + pruebas; (3) retirar el supuesto «sin horario» de la documentación. `generarSesionesAutomaticas` sigue respondiendo 501 (sin cambio). Sin `Idempotency-Key`.

Garantía en la DB: `usp_crear_grupo` no conoce las franjas; un llamador directo del SP puede crear un grupo sin horario. Si se quiere garantía en la base, hace falta un SP orquestador o un control al confirmar: decisión del dueño DB (DR-10).

## 4. Asignaturas: `horasSemanales`, y respuesta de crear/editar grupo

### 4.1 `horasSemanales` (lectura y escritura)

- **Nombre público**: `horasSemanales` (el Frontend ya lo espera con ese nombre). Tipo en la red: `number | null` (`null` = sin horas cargadas). Tipo y escala en DB: propuesta `DECIMAL(5,2)` nullable con `> 0` y tope por parámetro; unidad y decimales: **BM-02**.
- **Lectura** (aditivo, mismo `ApiPageResponse<AsignaturaPlanDTO>`): `GET /api/v1/coordinador/asignaturas` y `GET /api/v1/{coordinador,decano}/planes-estudio/{planId}/asignaturas` agregan `horasSemanales`. No se toca `credito`.
- **Escritura** (aditivo): `POST/PUT /api/v1/coordinador/planes-estudio/{id}/asignaturas…` aceptan `horasSemanales` opcional en `GuardarAsignaturaRequest`. En `POST`, ausente = `NULL`. En `PUT`, ausente = «no cambia» (propuesta; a confirmar en DR). Error nuevo propuesto: `ASI_009` (400).
- **Secuencia obligatoria por el conteo estricto de columnas** (`COLUMNAS_ASIGNATURA_PLAN = 21`): (1) Backend con lectura tolerante (acepta 21 o 22 columnas, sin cambio funcional); (2) DB agrega la columna a las vistas y a los dos SP de lectura, y el parámetro opcional a los SP de escritura; (3) Backend expone y envía `horasSemanales`; (4) Frontend lee `number | null` (hoy `?? 0` convierte `NULL` en `0`) y envía el valor que ya captura; (5) opcional: el backend vuelve al conteo estricto de 22. Cambiar la tupla de un SP sin esta secuencia produce 500.
- Mientras la materia no tenga horas (`NULL`): B5 no bloquea y la previsualización avisa (`HOR_013`, decisión 1c y 3).

### 4.2 Respuesta de crear/editar grupo

- **P1 (recomendada)**: sin cambio (`{id, mensajeUsuario}`); el cliente confirma el resultado con la previsualización previa y, si lo necesita, con `GET /api/v1/coordinador/grupos/{grupoId}/sesiones` (existente).
- **P2 (aditiva, opcional)**: agregar `resumenHorario {sesionesCreadas, sesionesRetiradas, sesionesCanceladasPorFestivo, horasProgramadas, horasEsperadas}`. Hoy `usp_precargar_sesiones_grupo` solo escribe esos conteos en el mensaje técnico y el `ProcedureResultMapper` exige 4 columnas exactas: requiere un SP o columnas nuevas, con la misma secuencia expandir/contraer. DR-08.

## 5. Período académico (decisión 2)

Rutas existentes (administrador): `POST /api/v1/admin/periodos-academicos` y `PUT /api/v1/admin/periodos-academicos/{id}`. La lectura `GET /api/v1/coordinador/periodos-academicos` no cambia.

| Caso | Resultado propuesto |
|---|---|
| `fechaFin` ausente | La DB calcula `fechaFin = fechaInicio + 7 × semanas − 1` (112 días − 1 = +111) y la persiste |
| `fechaFin` presente e igual a la calculada | Éxito (compatible con el FE actual si manda el valor correcto) |
| `fechaFin` presente y distinta | 400 con código nuevo `PER_011` |
| `fechaInicio >= fechaFin` | Sin cambio: `PER_002` |
| Respuesta del `POST` | 201 con la `fechaFin` **persistida** (no el eco): el SP hoy no la devuelve (ver H-09) |
| Respuesta del `PUT` | Sin cambio (200 `datos:null`) |

- **Fuente de la regla**: la DB (fuente única). Application sigue validando solo forma (`fechaInicio` obligatoria; `inicio < fin` solo si se envía `fechaFin`). La duración como parámetro `PERIODO/SEMANAS_DURACION` (16) o constante: DR-09.
- **Períodos existentes** (decisión 2d): se conservan. Propuesta: validar la duración al crear y cuando cambien `fechaInicio` o `fechaFin` respecto de lo guardado; si solo cambia el nombre, no se valida. Texto de 2d ambiguo: DR-03.
- **Editar con grupos/sesiones ya generados**: hoy sin guarda (`PER_004` solo bloquea eliminar). Mover `fechaInicio` desalinea el calendario sin reprecargar: DR-03.
- **Compatibilidad**: el modal del administrador permite fechas libres; tras el cambio solo puede ofrecer `fechaInicio` y mostrar `fechaFin` calculada de solo lectura. Cliente: único el FE (`academic-period-form-modal`, `coordinator-management.service`).
- Fechas `yyyy-MM-dd`, sin zona. El fin es inclusivo (confirmado en `uv_periodo_academico` y en la precarga).

## 6. Códigos de error y mensajes de catálogo propuestos

Provisionales: la numeración final la asigna el dueño DB al sembrar `CatalogoMensajeUsuario` (`MERGE` por código, con migración up/down). **Ningún texto en el código**: el backend conserva solo el código y el respaldo genérico existente; el texto y los marcadores viven en el catálogo. Estado `PROVISIONAL`.

| Código DB | Código público propuesto | HTTP | Regla | Dónde se emite | Parámetros del mensaje |
|---|---|---|---|---|---|
| `HOR_006` | `ERR_HORAS_SEMANALES_NO_COINCIDEN` | 409 (DR-11) | B5: horas de las franjas ≠ `horasSemanales` de la materia | Confirmar (bloquea) y previsualización (`BLOQUEANTE`) | horas de las franjas, horas de la materia |
| `HOR_007` | `ERR_DURACION_FRANJA_FUERA_DE_RANGO` | 400 | B7: duración mínima/máxima | ídem | franja, mínimo, máximo (**valores: BM-01**) |
| `HOR_008` | `ERR_FRANJA_FUERA_DE_VENTANA_HORARIA` | 400 | B7: ventana institucional | ídem | franja, ventana (**valores: BM-01**) |
| `HOR_009` | `ERR_CRUCE_HORARIO_ESTUDIANTES_INSCRITOS` | 409 | B9: el cambio cruza a estudiantes ya inscritos | ídem | cantidad de estudiantes, franja |
| `HOR_010` | — (solo previsualización) | 200 `INFORMATIVO` | B8: total programado < esperado | previsualización | horas programadas, esperadas |
| `HOR_011` | — | 200 `INFORMATIVO` | Horas perdidas por festivo | previsualización | horas, cantidad de sesiones |
| `HOR_012` | — | 200 `INFORMATIVO` | B11: horas no programables por inicio tardío | previsualización | horas, fecha desde la que se programa |
| `HOR_013` | — | 200 `ADVERTENCIA` | La materia no tiene horas cargadas: B5/B8 no se evalúan | previsualización | — |
| `HOR_014` | — | 200 `INFORMATIVO` | B10: sesiones conservadas por estar dictadas o tener asistencia | previsualización (edición) | cantidad, horas |
| `ASI_009` | `ERR_HORAS_SEMANALES_INVALIDAS` | 400 | `horasSemanales` ≤ 0 o sobre el tope | `usp_crear/actualizar_asignatura` | tope |
| `ASI_010` | `ERR_ASIGNATURA_NO_DISPONIBLE_PARA_GRUPO` | 409 | B1: asignatura inactiva o sin vínculo activo con plan activo del programa del coordinador | `usp_crear_grupo`, previsualización | — |
| `PER_010` | `ERR_PERIODO_NO_VIGENTE_PARA_GRUPO` | 409 | B2: período terminado, cerrado o de otra institución | ídem | nombre del período |
| `PER_011` | `ERR_PERIODO_DURACION_INVALIDA` | 400 | 112 días / fin calculado | `usp_crear/actualizar_periodo_academico` | semanas, fecha fin calculada |
| — (backend) `VAL_APP_076` | `ERR_CAMPO_OBLIGATORIO` (como `VAL_APP_067/068`) | 400 | `franjas` obligatorias al crear | Application | campo |
| `SES_005` (ya existe en la DB) | `SES_005` en el clasificador (como `SES_011…`) | 409 | Hora local inexistente por DST | precarga | zona, fecha-hora |

- **Clasificación** en el backend (fase de implementación): casos nuevos en `DbFailureClassifier.classifyDbCode`; códigos públicos en `GrupoErrorCode`/`CatalogoErrorCode`. HOR_006 es 409 porque depende del estado de otra entidad (la materia), igual que `ERR_CUPO_INFERIOR_OCUPACION`; HOR_007/008 son 400 porque la franja es inválida por sí sola. DR-11 pide confirmar.
- **`SES_005`**: hoy cae en 500 técnico (MISSING_IN_CONSUMER). El mapeo es independiente del resto y de bajo riesgo.
- **Sin canal público para `DBCODE`**: los códigos de la tabla son internos DB→backend; el cliente decide por `code`.
- B6 (franjas por semana coherentes) **no tiene código**: no hay dato que la defina (BM-03).

## 7. PDP/access-catalog, eventos y observabilidad

- **Catálogo de accesos (+1 entrada, propuesta, no aplicada)**: `{"method":"POST","path":"/api/v1/coordinador/grupos/previsualizacion-horario","roles":["COORDINADOR"],"access":"ROLES","ownership":"Ámbito del coordinador sobre los programas de la asignatura/grupo (lo fuerza la DB); sin escritura"}`. `SecurityConfig` no necesita matcher nuevo con esta URL (`/api/v1/coordinador/**` ⇒ `COORDINADOR`; los matchers previos son GET); `AccessCatalogConsistencyTest` exige que ambos coincidan. Si DR-01 elige `/api/v1/grupos/…`, hay que añadir un matcher explícito antes de `POST /api/v1/grupos/**`.
- **PDP**: un recurso nuevo en la aplicación `asistencias-uco` con concesión a `COORDINADOR`. Lo aprovisiona el responsable con `infra/security-baseline/scripts/provision_asistencias.py` (verificable con `verify` y `verify-decisions`); sin él, el PEP responde 403. Sin otros cambios de PDP: períodos y asignaturas usan rutas existentes.
- **Eventos/SSE**: no aplica. La previsualización no escribe; `crear/editar grupo` y la precarga **no** publican eventos hoy (ningún caso de uso de `grupo`/`sesion` usa `RealtimePublisherPort` para esto) y las ranuras en festivo nacen canceladas en la DB sin evento. No se propone evento nuevo; si más adelante se quisiera refrescar calendarios abiertos, sería un contrato aparte con su audiencia en `REALTIME_EVENT_STANDARD`.
- **Observabilidad**: ver §2.5. Sin datos personales nuevos; `X-Correlation-Id` sin cambio.

## 8. Handoff para el dueño DB (John)

**El backend no administra el esquema.** Todo lo de esta sección lo implementa, versiona (migración up/down, semilla, pruebas) y despliega el dueño del repo DB; el backend solo consume tras recibir el objeto liberado. Nada de esto está implementado ni autorizado todavía. Restricción de lectura del backend: consulta SP **por posición, con conteo estricto de columnas**, y una consulta nativa devuelve **un solo** conjunto de resultados.

| # | Objeto | Cambio solicitado | Regla del PLAN | Compatibilidad |
|---|---|---|---|---|
| H-01 | Tabla `Asignatura` | Columna nueva `horasSemanales` (propuesta `DECIMAL(5,2)` NULL, `CHECK > 0`); filas existentes `NULL` (decisión 1c); decidir `Asignatura` vs `AsignaturaPlanEstudio` (1a: propuesta `Asignatura`, `codigo` es único global) | Decisión 1 | Aditivo |
| H-02 | Vistas `uv_asignatura`, `uv_asignatura_plan_estudio` | Exponer `horasSemanales` (la segunda la hereda de la primera) | Decisión 1d | Aditivo: las entidades JPA mapean columnas por nombre (`ddl-auto: none`) |
| H-03 | `usp_consultar_asignaturas_coordinador_paginado`, `usp_consultar_asignaturas_plan_paginado` | Agregar `horasSemanales` antes de `totalRegistros` | Decisión 1d | **Rompe al backend actual** (21 columnas estrictas): usar la secuencia de §4.1 |
| H-04 | `usp_crear_asignatura`, `usp_actualizar_asignatura` | Parámetro opcional `@horasSemanales … = NULL`; validar `> 0` y tope por parámetro (`ASI_009`) | Decisión 1c | Aditivo (el backend actual no lo envía) |
| H-05 | `usp_crear_grupo` | B1: asignatura activa + vínculo activo con plan activo + programa del coordinador (`ASI_010`); B2: período existente, de la institución del coordinador, no terminado ni cerrado (`PER_010`), y **retirar la sustitución silenciosa por el período más reciente** (DR-06); B3 y B4 ya existen. Opcional: índice único `UX_Grupo_asignatura_periodo_codigo` tras depurar duplicados (hoy B4 es solo un `EXISTS`, sin protección de concurrencia) | B1, B2, B3, B4 | Endurecimiento: puede rechazar llamadas que hoy pasan |
| H-06 | `usp_actualizar_grupo` | Ámbito del coordinador sobre el grupo (cierra `SECURITY_FINDING-GRP-001`); decidir el rol `ADMINISTRADOR` (DR-05) | B1, CC-02 | Endurecimiento |
| H-07 | `usp_guardar_horarios_grupo(_interno)` | B5 (`HOR_006`; `NULL` ⇒ no valida), B7 (`HOR_007/008`, con parámetros), B9 (`HOR_009`: validar cruce de cada estudiante inscrito contra sus otros grupos del período con `usp_validar_cruce_horario_estudiante_interno`), reporte de B10; decidir la rama `HOR_005` (docente, sin camino HTTP desde DEC-065 (4)) | B5, B6\*, B7, B9, B10 | Endurecimiento. \*B6 sin definición (BM-03) |
| H-08 | `usp_precargar_sesiones_grupo_interno` (y SP congelado) | **B2b**: reemplazar `DATEDIFF(WEEK, inicio, fecha) + 1` por `DATEDIFF(DAY, inicio, fecha) / 7 + 1` (1..16 en 112 días) y **probarlo en BD desechable** (la hipótesis no se ejecutó). `usp_generar_sesiones_grupo` (congelado) tiene la misma expresión: decidir si se descongela o se acepta (CC-04); decidir el saneamiento de filas con `numeroSemana = 17` ya existentes | B2b | Cambia datos nuevos; no la firma |
| H-09 | `usp_crear_periodo_academico`, `usp_actualizar_periodo_academico` | Regla de 112 días (`PER_011`); `@fechaFin` opcional que se calcula; devolver la `fechaFin` persistida o que el backend la lea de `uv_periodo_academico` | B2, decisión 2 | `crear` ya declara una columna extra (`idPeriodo`): una más rompe al backend; preferir la lectura posterior |
| H-10 | SP **nuevo** de solo lectura (nombre provisional `usp_previsualizar_horario_grupo`) | Parámetros: `@idGrupo` NULL, `@idAsignatura`, `@idPeriodoAcademico`, `@idDocente`, `@horariosJSON`, `@idCorrelacion`, `@idUsuarioEjecutor`. Un solo conjunto de resultados con columnas fijas y orden estable (p. ej. filas discriminadas por `tipoFila`: resumen, franja, sesión, festivo, conflicto). Sin escrituras ni transacciones. **Misma definición de ranuras que la precarga** (función o núcleo compartido) para que previsualización = confirmación. Mismo ámbito que las escrituras. Mensajes de conflicto resueltos desde el catálogo. EXECUTE para el rol runtime | Decisión 5; B1-B11 | Nuevo, aditivo |
| H-11 | `CatalogoParametro` | `PERIODO/SEMANAS_DURACION` = 16 (DR-09); tope de `horasSemanales`; parámetros de B7 (duración mínima/máxima, ventana; **valores: BM-01**); si la hora no es de 60 min, parámetro de minutos por hora (BM-02). Decidir si son editables | B2, B5, B7 | Aditivo |
| H-12 | `CatalogoMensajeUsuario` | Filas para todos los códigos de §6 (incluido `VAL_APP_076`) con sus marcadores; migración up/down | — | Aditivo |
| H-13 | Pruebas del repo DB | Fixtures y casos: 16 y 32 sesiones empezando en lunes y a mitad de semana; `numeroSemana` ≤ 16; período con fin ≠ inicio+111 rechazado; fin calculado; período histórico conservado; B5/B7/B9; edición con sesiones dictadas | TEST_PLAN | — |

Lo que **no** se pide: cambiar el modelo de aula, cohortes (B12, sin dato) ni reabrir el endpoint docente de generación.

Seguimiento fuera del repo DB (informativo; no autoriza nada): Backend (DTO/mapper/caso de uso/controlador de la previsualización, clasificador de errores, `AsignaturaPlanDTO` y `GuardarAsignaturaRequest`, guarda de `franjas`, `GuardarPeriodoAcademicoRequest`, lectura tolerante de columnas, `access-catalog.json`, documentación) y Frontend (panel de previsualización en el formulario de grupo, ≥1 franja, `fechaFin` solo lectura, `horasSemanales` enviada y leída como `null`, manejo de los códigos nuevos, comentarios que aún citan F3).

## 9. Decisiones requeridas

| ID | Decisión | Aprobador | Recomendación |
|---|---|---|---|
| DR-01 | Método y URL de la previsualización (POST bajo `/coordinador`) y su clasificación `BUSINESS_COMMAND` de solo lectura | Responsable / aprobador del contrato | §2.1 |
| DR-02 | Compatibilidad de `franjas` obligatorias (O1, cambio de comportamiento) | Responsable | O1 con secuencia FE→BE |
| DR-03 | Períodos: regla en edición (solo si cambian fechas), edición con grupos/sesiones ya generados | Responsable + John | Validar solo al crear o al cambiar fechas; decidir guarda con grupos |
| DR-04 | `fechaFin` opcional (calculada) frente a obligatoria y validada | Responsable + John | Opcional y calculada |
| DR-05 | `ADMINISTRADOR` en `PUT /api/v1/grupos/{id}` (hoy pasa el PEP y la DB lo rechaza) | Responsable | Retirarlo del PEP/catálogo, salvo que se quiera habilitar en la DB |
| DR-06 | Retirar la sustitución silenciosa del período al crear grupo | John + responsable | Retirarla y exigir `idPeriodoAcademico` (el FE ya lo exige) |
| DR-07 | ¿La previsualización carga festivos faltantes (efecto lateral de catálogo) o solo avisa? | Responsable | Mantener paridad con la confirmación (cargar, mejor esfuerzo) y documentarlo; si no, avisar con `festivosCargados:false` |
| DR-08 | Respuesta de crear/editar: P1 sin cambio, o P2 con `resumenHorario` | Responsable + John | P1 |
| DR-09 | Duración de 16 semanas como parámetro o constante | John | Parámetro |
| DR-10 | Garantía de horario en la DB y destino de la rama `HOR_005` | John | Documentar el orquestador del backend; decidir `HOR_005` |
| DR-11 | Status de HOR_006 (409 propuesto) | Aprobador del contrato | 409 |
| DR-12 | Dónde se congela el contrato de rutas fuera del Golden Path (`CONTRACT.md` del work item frente al OpenAPI canónico) | Aprobador del contrato | `CONTRACT.md` + matriz (práctica vigente) |
| DR-13 | Unidad de «hora» de la materia (60 min de reloj u hora cátedra) y decimales | Responsable + John | Ver BM-02; sin ella B5 no es contratable con exactitud |
| DR-14 | Ámbito del coordinador para asignaturas compartidas: ¿alguno o todos los programas? (la edición exige **todos**, la creación hoy no valida) | John + responsable | Igual que el AS-IS de edición (todos), por mínimo privilegio |

Bloqueos de evidencia (BM-01..BM-07) en [CONTRACT_CONFLICTS](CONTRACT_CONFLICTS.md).

## 10. Estado frente al TEST_PLAN

**El contrato queda en DRAFT y NO está listo para el TEST_PLAN completo.** Falta: aprobación de DR-01/02/03/04/07/09/11/12/14; valores de B7 y unidad de la hora (BM-01, BM-02); definición de B6 (BM-03); la columna `horasSemanales` y los objetos de §8 del dueño DB. Pueden avanzar de forma independiente, una vez aprobadas sus decisiones: (1) B2b `numeroSemana` (DB, con prueba en BD desechable); (2) mapeo de `SES_005` (backend); (3) regla de 112 días y `fechaFin` calculada (DR-03, DR-04, DR-09); (4) `franjas` obligatorias (DR-02, con ajuste previo del FE).

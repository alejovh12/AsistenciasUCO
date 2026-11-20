---
status: draft
type: work-item
scope: db
owner: backend-team
last-reviewed: 2026-11-19
---

# HANDOFF DB — SES-GEN-01: calendario de sesiones del semestre al definir el grupo

**Para:** John (dueño del repo DB). **De:** backend-team. **Estado: DRAFT.** Este documento es una **solicitud**, no una orden ni una autorización: nada de lo que se pide está implementado, y el backend no administra el esquema. Cada objeto lo versiona (migración up/down, semilla, pruebas) y lo despliega el dueño DB. El backend consume solo cuando el objeto esté liberado.

Documentos de origen (en esta carpeta): [PLAN](PLAN.md) · [CONTRACT_PROPOSAL §8](CONTRACT_PROPOSAL.md) (tabla H-01..H-13, de la que este documento es la versión operativa) · [CONTRACT_MATRIX](CONTRACT_MATRIX.md) · [CONTRACT_CONFLICTS](CONTRACT_CONFLICTS.md).

**Fuentes leídas:** Backend `2fdc7a0f`; repo DB `develop @ dadf8dc` (instantánea del repositorio). **No se verificó el SQL Server vivo**: si lo desplegado difiere del repo, manda lo desplegado y hay que avisarnos (BM-07).

## 1. Qué se quiere lograr (en tres líneas)

1. Al definir un grupo, el coordinador indica los días y horas; el sistema calcula **todas** las sesiones del semestre (1 día/semana ⇒ 16 sesiones; 2 días/semana ⇒ 32) y las horas totales, y muestra los conflictos **antes** de confirmar.
2. Para saber si el horario cubre la materia, cada **materia debe tener `horasSemanales`** (decisión del responsable: columna nueva en la DB). La hora es de **60 minutos de reloj** (DR-13).
3. El semestre dura siempre **16 semanas = 112 días corridos**; la fecha de fin se calcula desde la de inicio y la DB rechaza períodos nuevos o editados que no midan 112 días.

Todo el cálculo y la validación deben vivir **en la DB**; el backend solo traduce y el Frontend solo muestra. La previsualización y la confirmación deben dar **el mismo resultado** porque comparten la misma definición de ranuras.

## 2. Decisiones del responsable que ya puedes dar por tomadas

| Tema | Decisión | Efecto para la DB |
|---|---|---|
| Fuente de las horas | Columna nueva en la DB (no se deriva de `credito`) | H-01 |
| Hora | 60 min de reloj (DR-13) | B5 compara minutos de franjas con `horasSemanales × 60`. No hay parámetro de hora cátedra |
| Semestre | 16 semanas fijas, impuestas **en el período** (no en el grupo) | H-09, H-11 |
| Períodos antiguos | Se conservan tal cual, sin migración destructiva | Validar solo al crear o al cambiar fechas (DR-03 por confirmar) |
| Festivos | No bloquean: la sesión nace cancelada y se informa | Ya existe (CULM-001 B.1); solo se reporta en la previsualización |
| Rigor de horas | B5 **bloquea exacto** (sin tolerancia); con `horasSemanales` NULL solo avisa | H-07 (`HOR_006`) |
| Total del semestre | **Informa, no bloquea** (B8/B11) | Solo previsualización |
| Franjas al crear | Obligatorias, con guarda en el backend (DR-02: Frontend primero) | Garantía en DB: DR-10 |
| Dos franjas el mismo día | Permitido si no se traslapan (contiguas incluidas, como en `HOR_003`) | B5 suma duraciones; no cambiar `HOR_003` |
| Grupo creado tarde | Se genera desde hoy; el pasado no se toca | Sin cambio; solo reportar horas no programables |
| `ADMINISTRADOR` en `PUT /grupos/{id}` | Se retira del permiso en el backend (DR-05) | Tu SP ya exige perfil `COORDINADOR`: **no hay que habilitar al administrador**, solo no revertirlo |
| Asignaturas compartidas | El coordinador debe coordinar **todos** los programas del grupo (DR-14) | Misma regla en crear, editar y previsualizar |
| Aula/salón, cohortes (B12) | Fuera de alcance | No tocar `Grupo` para aula (DB-GP-001) |

## 3. Orden de entrega propuesto

El backend lee los SP de asignaturas **por posición y con conteo estricto de columnas** (`COLUMNAS_ASIGNATURA_PLAN = 21`), y la precarga exige 4 columnas exactas. Si cambia la tupla de un SP antes de que el backend la tolere, el endpoint responde 500. Por eso:

| Fase | Quién | Qué | Depende de |
|---|---|---|---|
| **0** | DB (BD desechable) | Prueba de **B2b** (`numeroSemana` = 17), solo lectura/diagnóstico | Nada. Puede empezar ya |
| **1 · expandir** | DB | H-01, H-02, H-04, H-11, H-12 (aditivos, sin romper al backend actual) | Nada |
| **1b** | Backend | Lectura tolerante (acepta 21 o 22 columnas) | Nada; sin cambio funcional |
| **2** | DB | H-03 (agregar `horasSemanales` a los SP de lectura), H-09 (períodos), H-08 (corrección de `numeroSemana`) | **Aviso de que el backend 1b está desplegado** (H-03); resultado de la Fase 0 (H-08) |
| **3 · endurecer** | DB | H-05, H-06, H-07 (validaciones B1, B2, B5, B7, B9) | Fase 1; DR-06, BM-01 |
| **4** | DB | H-10 (SP de previsualización) | H-08 y H-07: comparte el núcleo de ranuras y las validaciones |
| **5** | DB | H-13 (pruebas del repo DB) | Se escriben junto a cada fase, no al final |

Cuando liberes cada fase, avísanos con el commit/versión y la lista de objetos para actualizar el backend y el Frontend.

## 4. Solicitudes por objeto

### H-01 — Columna `horasSemanales` en `Asignatura`

- Columna nueva `horasSemanales` `DECIMAL(5,2) NULL`, `CHECK (horasSemanales > 0)`. Propuesta; **decimales (¿se admiten 4.5?) por definir contigo**.
- Filas existentes quedan en `NULL` (el coordinador las completa; sin carga inicial). Con `NULL`, B5 y B8 **no se evalúan** y la previsualización avisa.
- Tabla: propuesta `Asignatura` (la materia tiene `codigo` único global, DEC-019). Alternativa `AsignaturaPlanEstudio` solo si una misma materia pudiera tener horas distintas por plan. **Confirmar.**
- Migración up/down.

### H-02 — Vistas

Exponer `horasSemanales` en `uv_asignatura` y `uv_asignatura_plan_estudio`. Aditivo: las entidades JPA del backend mapean por nombre (`ddl-auto: none`).

### H-03 — SP de lectura de asignaturas (rompe al backend actual)

`usp_consultar_asignaturas_coordinador_paginado` y `usp_consultar_asignaturas_plan_paginado`: agregar `horasSemanales` **antes de `totalRegistros`**. **No liberar hasta que el backend 1b esté desplegado.** Hoy el backend espera exactamente 21 columnas.

### H-04 — SP de escritura de asignaturas

`usp_crear_asignatura` y `usp_actualizar_asignatura`: parámetro opcional `@horasSemanales DECIMAL(5,2) = NULL`.
- Validar `> 0` y tope (parámetro, ver H-11); error `ASI_009`.
- En crear, ausente = NULL. En actualizar, **ausente = no cambia** (propuesta; confirmar si quieres poder volver a NULL).
- Aditivo: el backend actual no lo envía.

### H-05 — `usp_crear_grupo`

- **B1:** asignatura activa y con vínculo activo a un plan activo del programa del coordinador, con la regla de DR-14 (todos los programas). Código propuesto `ASI_010`. Hoy solo se comprueba que exista en `uv_asignatura`.
- **B2:** período existente, de la institución del coordinador, no terminado ni cerrado. Código propuesto `PER_010`. **Retirar la sustitución silenciosa por el período más reciente** (hoy sin filtro de institución) y exigir `idPeriodoAcademico` (DR-06; el Frontend ya lo exige).
- B3 (docente existente, vínculo activo) y B4 (código único por asignatura + período) **ya existen**; no debilitar.
- Opcional: índice único `UX_Grupo_asignatura_periodo_codigo` tras depurar duplicados. Hoy B4 es solo un `EXISTS`, sin protección ante concurrencia (dos coordinadores a la vez).
- Es un endurecimiento: puede rechazar llamadas que hoy pasan.

### H-06 — `usp_actualizar_grupo` (cierra `SECURITY_FINDING-GRP-001`)

Hoy un usuario con perfil `COORDINADOR` pasa sin que se compruebe que el grupo pertenezca a un programa que coordina; con `franjas` el hueco se cierra por `SEC_002` en `usp_guardar_horarios_grupo`, **sin franjas queda abierto** (nombre, código, titular, cupo de grupos ajenos).
- Pedido: aplicar el mismo ámbito (todos los programas del grupo) antes de cualquier cambio. Idéntico control en `usp_crear_grupo` (ver B1).
- `ADMINISTRADOR`: el backend lo retira del permiso (DR-05). Mantén el SP exigiendo perfil `COORDINADOR`.
- Severidad propuesta: media. **Esto no depende del resto del work item** y puede salir antes si lo prefieres.

### H-07 — `usp_guardar_horarios_grupo` y `_interno`

| Regla | Qué | Código |
|---|---|---|
| **B5** | La suma de minutos de las franjas = `horasSemanales × 60` de la materia, **exacto**. Con `horasSemanales` NULL no valida | `HOR_006` |
| **B7** | Duración mínima/máxima de franja y ventana horaria institucional, leídas de parámetros | `HOR_007`, `HOR_008` |
| **B9** | Al cambiar franjas con estudiantes inscritos, validar el cruce de **cada** inscrito (matrícula INSCRITA, mismo período) contra sus otros grupos con `usp_validar_cruce_horario_estudiante_interno` antes de aplicar. Hoy es un hueco confirmado | `HOR_009` |
| **B10** | Reportar sesiones conservadas por estar dictadas o tener asistencia/toma/clasificación (la precarga ya las respeta; falta contarlas) | informativo |
| **B6** | Franjas por semana coherentes con la materia: **sin definición** (no hay dato de «veces por semana»). Se reduce a B5 + B7 + ≥ 1 franja. No se pide nada (BM-03) | — |

- `HOR_005` (docente titular define el horario inicial): hoy no tiene camino HTTP desde DEC-065 (4). **Decide su destino** (mantener o retirar); no lo tocamos desde el backend (DR-10).
- Los valores de B7 **no existen** hoy como parámetro; faltan los valores (BM-01, los fija el responsable).

### H-08 — `numeroSemana` (B2b): probar primero

`usp_precargar_sesiones_grupo_interno` (~L174) calcula `numeroSemana = DATEDIFF(WEEK, inicio, fecha) + 1`. `DATEDIFF(WEEK, …)` cuenta cruces de semana de calendario (depende de `DATEFIRST`), por lo que en un período de 112 días que **no empieza en el primer día de la semana** podría dar **17**. **Es una hipótesis por lectura del texto; no se ejecutó.**
1. **Fase 0:** reproducirlo en BD desechable (guardas `e2e_safety`, nunca la viva): período de 112 días empezando en lunes y a mitad de semana, 1 y 2 franjas; consultar el máximo `numeroSemana`.
2. Si se confirma: reemplazar por `DATEDIFF(DAY, inicio, fecha) / 7 + 1` (1..16 en 112 días).
3. `usp_generar_sesiones_grupo` (congelado) tiene la misma expresión (~L305). **Decide**: descongelarlo para corregir o aceptar que `/generacion` (solo DOCENTE, legado) pueda dar 17 (CC-04).
4. **Decide** si se sanean las filas existentes con `numeroSemana = 17` (propuesta: contarlas primero y no tocar datos sin tu aval).
5. Cambia datos nuevos; no cambia la firma.

### H-09 — Períodos (112 días, fin calculado)

`usp_crear_periodo_academico`, `usp_actualizar_periodo_academico` (hoy solo exigen `fechaInicio < fechaFin`, `PER_002`):
- `@fechaFin` opcional: si falta, `fechaFin = fechaInicio + 111` y se persiste. Si viene y es distinta ⇒ error nuevo `PER_011`. Si viene y es igual ⇒ éxito (compatible con el Frontend actual).
- Duración como parámetro `PERIODO/SEMANAS_DURACION = 16` (DR-09, recomendado) en lugar de constante.
- Períodos existentes: se conservan. Propuesta: validar al crear y cuando cambie `fechaInicio` o `fechaFin`; si solo cambia el nombre, no validar (DR-03).
- **Editar con grupos/sesiones ya generados:** hoy sin guarda (`PER_004` solo bloquea eliminar). Mover `fechaInicio` desalinea el calendario sin reprecargar. **Decide la guarda** (DR-03).
- Devolver la `fechaFin` persistida: `usp_crear_periodo_academico` ya declara una columna extra (`idPeriodo`) y una más rompe al backend. **Preferimos que no agregues columna**: el backend la leerá de `uv_periodo_academico` después de crear.

### H-10 — SP nuevo de previsualización (solo lectura)

Nombre provisional `usp_previsualizar_horario_grupo`. Sin escrituras ni transacciones; `EXECUTE` para el rol runtime.
- Parámetros: `@idGrupo` NULL (grupo nuevo), `@idAsignatura`, `@idPeriodoAcademico`, `@idDocente`, `@horariosJSON`, `@idCorrelacion`, `@idUsuarioEjecutor`.
- **Un solo conjunto de resultados** (limitación del backend), con columnas fijas y orden estable; sugerencia: filas discriminadas por `tipoFila` (resumen, franja, sesión, festivo, conflicto).
- **Misma definición de ranuras que la precarga** (función o núcleo compartido) para que previsualización = confirmación.
- Mismo ámbito y mismas validaciones que las escrituras; los conflictos se devuelven **como datos** con `codigo`, `severidad` (`BLOQUEANTE`/`ADVERTENCIA`/`INFORMATIVO`) y el mensaje resuelto desde el catálogo.
- Debe entregar (ver [CONTRACT_PROPOSAL §2.3](CONTRACT_PROPOSAL.md)): horas por franja, horas semanales, número de sesiones del período, sesiones y horas perdidas por festivo, horas no programables por inicio tardío (B11), total esperado frente al programado, y el efecto sobre un grupo existente (B10).
- No debe revelar más que lo que ya revela `HOR_002` (nombre y franja del otro grupo del docente).
- **Decide (DR-07):** ¿puede la previsualización completar festivos faltantes? Hoy ese efecto lo hace el backend antes de la confirmación (`AsegurarFestivosPeriodoInputPort`); propuesta: el backend sigue haciéndolo y el SP solo lee.

### H-11 — `CatalogoParametro`

- `PERIODO/SEMANAS_DURACION = 16`.
- Tope de `horasSemanales` (por ejemplo, análogo a `ASIGNATURA/CREDITOS_MAXIMO`; valor por acordar).
- Parámetros de B7: duración mínima/máxima de franja y ventana horaria. **Valores por definir por el responsable (BM-01); no los inventes.**
- No hace falta parámetro de minutos por hora (DR-13: 60).
- Decide si son editables desde la tabla de parámetros editables.

### H-12 — `CatalogoMensajeUsuario`

Filas para los códigos nuevos, con sus marcadores, vía `MERGE` por código y migración up/down. Numeración final: la asignas tú.

| Código | Cuándo | Parámetros |
|---|---|---|
| `HOR_006` | B5 no coincide | horas de las franjas, horas de la materia |
| `HOR_007` / `HOR_008` | B7 duración / ventana | franja, mínimo/máximo o ventana |
| `HOR_009` | B9 cruza estudiantes inscritos | cantidad, franja |
| `HOR_010`–`HOR_014` | Previsualización (informativos/advertencia): total menor al esperado, horas perdidas por festivo, horas no programables, materia sin horas, sesiones conservadas | horas, cantidades, fecha |
| `ASI_009` | `horasSemanales` inválida | tope |
| `ASI_010` | B1 | — |
| `PER_010` | B2 | nombre del período |
| `PER_011` | Duración de período inválida | semanas, fecha de fin calculada |
| `VAL_APP_076` | `franjas` obligatorias (lo emite el backend) | campo |

Ningún texto vive en el backend. Aparte, **`SES_005`** ya existe en la DB; el backend le añadirá su clasificación (no requiere cambio tuyo). Aviso (CC-05): en la semilla, `VAL_002` tiene el texto «El número de identificación debe tener entre {} y {} dígitos» pero los SP de grupo/período/ámbito lo usan con otros argumentos; decide si se le da código propio o se corrige el catálogo (basado en la semilla estática, no se comprobó el catálogo vivo).

### H-13 — Pruebas del repo DB

Casos mínimos (en BD desechable):
- Período de 112 días empezando en lunes y a mitad de semana: 1 franja ⇒ 16 sesiones, 2 franjas ⇒ 32, `numeroSemana` ≤ 16.
- Período con fin ≠ inicio + 111 rechazado (`PER_011`); fin calculado cuando falta; período histórico conservado.
- B5 (coincide, no coincide, `horasSemanales` NULL), B7, B9, B1, B2 (sin sustitución silenciosa).
- Edición con sesiones ya dictadas o con asistencia: se conservan y se reportan.
- Festivo: la sesión nace cancelada y se refleja en la previsualización.
- Previsualización = resultado de la confirmación (mismas ranuras).
- Grupo creado a mitad de período: nada se genera en el pasado.

## 5. Lo que necesitamos que decidas (o aportes)

| # | Decisión | Propuesta | Bloquea |
|---|---|---|---|
| 1 | Tabla de `horasSemanales` (`Asignatura` o `AsignaturaPlanEstudio`) | `Asignatura` | H-01 |
| 2 | Decimales en `horasSemanales` | `DECIMAL(5,2)`; confirmar si se permiten fracciones | H-01, H-04 |
| 3 | Valor de «ausente» en actualizar asignatura | No cambia | H-04 |
| 4 | DR-03: validar período solo si cambian fechas; guarda con grupos/sesiones generados | Validar al crear o al cambiar fechas; guarda a definir | H-09 |
| 5 | DR-04: `fechaFin` opcional y calculada | Opcional | H-09 |
| 6 | DR-06: retirar la sustitución silenciosa de período | Retirarla | H-05 |
| 7 | DR-08: respuesta de crear/editar con resumen de horario | Sin cambio (`{id, mensajeUsuario}`) | — |
| 8 | DR-09: duración como parámetro | Parámetro | H-09, H-11 |
| 9 | DR-10: garantía de horario en la DB y destino de `HOR_005` | El backend lo exige; tú decides `HOR_005` | H-07 |
| 10 | CC-04: descongelar `usp_generar_sesiones_grupo` | A decidir contigo | H-08 |
| 11 | Saneamiento de filas con `numeroSemana = 17` ya existentes | Contar primero; no tocar sin tu aval | H-08 |
| 12 | Valores de B7 y tope de horas | Los fija el responsable (BM-01) | H-07, H-11 |
| 13 | Orden de entrega de la sección 3 | Fases 0 a 5 | Todo |

## 6. Lo que NO se pide

- No se pide cambiar el modelo de aula ni cohortes (B12/B13).
- No se pide reabrir el endpoint docente de generación ni habilitar al administrador para editar grupos.
- No se pide migrar ni corregir períodos existentes que no midan 112 días.
- No se pide compensación de horas perdidas por festivo.

## 7. Qué hace cada capa al recibir tu entrega (informativo)

- **Backend:** lectura tolerante de columnas (1b, antes de H-03), exponer y enviar `horasSemanales`, endpoint de previsualización que invoca el SP nuevo, guarda de `franjas` obligatorias, clasificación de los códigos nuevos y de `SES_005`, `access-catalog.json` (+1 ruta; el PDP lo aprovisiona el responsable) y retiro de `ADMINISTRADOR` en `PUT /grupos/{id}`.
- **Frontend:** panel de previsualización en el formulario de grupo, ≥ 1 franja y no enviar `[]`, `fechaFin` de solo lectura, enviar y leer `horasSemanales` (hoy `?? 0` convierte NULL en 0), manejo de los códigos nuevos.
- Ninguna capa avanza a implementar sin DoR READY, contrato aprobado y TEST_PLAN con RED (estado actual: implementación **NOT_READY**).

## 8. Cómo devolvernos el resultado

Por cada fase: commit o versión del repo DB, lista de objetos creados/modificados, migración up/down, evidencia de las pruebas de H-13 en BD desechable y, si algo se separa de esta propuesta, el motivo. Sin secretos ni logs crudos.

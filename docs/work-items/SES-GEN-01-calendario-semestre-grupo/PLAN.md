---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-19
---

# PLAN — SES-GEN-01: calendario de sesiones del semestre al definir el grupo

## Identidad y objetivo

- Fecha / rol / base Git: 2026-11-19, planificador. Backend `feature/sec-baseline-020-eventos-rabbitmq` @ `2fdc7a0f`; DB `develop` @ `dadf8dc`; Frontend `fix/mejoras-integrales-frontend` @ `0328526`.
- Objetivo (pedido del responsable): que el coordinador, al configurar materia + grupo + horario, obtenga **todas las sesiones del semestre** calculadas según cuántas veces por semana se ve la materia (1, 2…) y cuántas horas semanales tiene, con **todas las validaciones** de cruce y de coherencia, de modo que se cumpla el **total de horas esperado** de la materia. Ejemplo del responsable: 4 h semanales vistas 2 veces por semana ⇒ 16 semanas × 2 = **32 sesiones**; el coordinador elige qué día(s) se ve.
- Criterios de aceptación (borrador, a cerrar en la fase de contrato):
  1. Con horas semanales y días elegidos coherentes, el grupo queda con todas las sesiones del período y el total de horas programadas = total esperado de la materia.
  2. Si algo no cuadra (horas, cruces, período, festivos que quitan horas), el coordinador lo ve **antes** de confirmar y nada queda a medias.
  3. Ninguna validación de cruce existente se debilita.
- Skills y fuentes: `uco-baseline`, `uco-contratos`, `uco-persistencia`, `uco-seguridad`, `uco-testing`; [API_DESIGN_RULES](../../governance/API_DESIGN_RULES.md); [HTTP_AS_IS_MATRIX](../../contracts/HTTP_AS_IS_MATRIX.md); [F2-precarga-sesiones](../F2-precarga-sesiones/PLAN.md); repo DB (rutas oficiales en [infra/README.md](../../../infra/README.md)).

## Corrección del diagnóstico previo

El diagnóstico inicial de esta conversación afirmaba que crear un grupo no dispara ninguna generación y que nadie la conecta. **Eso era incompleto.** Lo comprobado en código:

- `CrearGrupoUseCaseImpl` + `GrupoJpaRepository.guardarHorariosSiCorresponde` ejecutan `usp_guardar_horarios_grupo` y luego `usp_precargar_sesiones_grupo` **en la misma transacción** cuando el comando trae `franjas` (F2, 2026-11-02). Es decir, **sí hay generación al crear el grupo, pero solo si el coordinador envía horario**.
- El gap real es otro: la precarga no conoce las horas de la materia, no calcula ni compara totales, no avisa antes de confirmar y deja pasar un grupo sin franjas (sin sesiones).

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato | Evidencia y límites |
|---|---|---|
| Crear/editar grupo con `franjas` guarda horario y precarga sesiones, misma transacción; sin `franjas` no toca horario ni sesiones | `GrupoJpaRepository` (`guardarHorariosSiCorresponde`, `precargarSesiones`, `SQL_PRECARGAR_SESIONES`) | Leído. Atomicidad y orden verificados por lectura; E2E real de la precarga **no ejecutado** en esta sesión |
| `generarSesionesAutomaticas=true` responde 501 | `CrearGrupoUseCaseImpl:28` (`DIS_APP_001`) | Leído |
| La precarga crea una sesión por franja y por día coincidente, **desde max(hoy, inicio del período) hasta fin del período**; no duplica por (grupo, inicio, fin); borra futuras `UTC_GENERADOR` que ya no encajan y no tienen asistencia/toma/clasificación; nunca toca pasado ni sesiones manuales; luego aplica festivos (nacen canceladas) | `usp_precargar_sesiones_grupo_interno.sql` | Leído. **El conteo sale del calendario real, no de "16 semanas"**; no hay tope ni objetivo de horas |
| Ejecutan la precarga el COORDINADOR del programa y el DOCENTE titular; otro ejecutor `SEC_002` | `usp_precargar_sesiones_grupo.sql`, `usp_validar_gestion_grupo_coordinador_o_titular_interno` | Leído |
| Horario: formato/`horaFin>horaInicio` (HOR_004), franjas del grupo sin traslape entre sí (HOR_003, contiguas permitidas), docente sin otro grupo a esa hora en el período (HOR_002), docente titular solo define el horario inicial (HOR_005) | `usp_guardar_horarios_grupo.sql`, `usp_validar_cruce_horario_docente_interno.sql` | Leído por cabecera y grep; las reglas se aplican en una transacción |
| Cruce de horario del **estudiante** solo se valida **al matricular**, contra sus otros grupos con matrícula INSCRITA del mismo período (HOR_001) | `usp_validar_cruce_horario_estudiante_interno.sql` | Leído. **`usp_guardar_horarios_grupo` no valida estudiantes ya inscritos al cambiar franjas** (solo docente) |
| `Asignatura` solo tiene `credito`; **no existe horas semanales ni total de horas** (ni en `Asignatura` ni en `AsignaturaPlanEstudio`) | `schema/tables/Asignatura.sql`, `AsignaturaPlanEstudio.sql` | Leído. Es la **carencia central** para "total de horas esperado" |
| `Grupo` no tiene aula; el esquema rechaza columnas fantasma (`aula`) | `schema/tables/Grupo.sql` (`SCHEMA_CONTRACT_CONFLICT`, DB-GP-001) | Leído. Sin aula no hay validación de aula |
| Período: `fechaInicio`, `fechaFin`, `fechaCierreUtc` (cerrado ⇒ PER_008/009) | `PeriodoAcademico.sql` | Leído |
| Endpoint de generación masiva `POST /api/v1/sesiones/grupo/{grupoId}/generacion` es solo DOCENTE, llama `usp_generar_sesiones_grupo_v2` (envuelve al SP congelado `usp_generar_sesiones_grupo`, desde el inicio del período) | `HTTP_AS_IS_MATRIX.md:106`, `GenerarSesionesGrupoUseCaseImpl` | Está en la matriz AS-IS, **no en el OpenAPI canónico**. No es el camino del coordinador |
| `franjas` no aparece en `openapi-golden-path.yaml` | grep sin coincidencias | El contrato HTTP de crear/editar grupo del coordinador debe localizarse y compararse (pendiente, ver bloqueos) |
| Festivos: sesión en festivo nace cancelada (no desaparece); hora local inexistente por DST aborta (SES_005) | `usp_precargar_sesiones_grupo_interno.sql`, `AsegurarFestivosPeriodoUseCaseImpl` | Leído |
| Frontend usa `franjas` en `course.service.ts`, `grupo-api-dto.model.ts` | repo Frontend | Solo localizado; no auditado |

## Erratas de este PLAN halladas por el análisis contractual (2026-11-19)

Detalle y evidencia en [CONTRACT_CONFLICTS.md](CONTRACT_CONFLICTS.md) §5; contrato en borrador en [CONTRACT_MATRIX.md](CONTRACT_MATRIX.md) y [CONTRACT_PROPOSAL.md](CONTRACT_PROPOSAL.md). Estas correcciones **prevalecen** sobre el texto de abajo cuando se contradicen:

- **F3 está retirado** (DEC-065 (4), en las tres capas): hoy solo el COORDINADOR crea grupos por HTTP. Lo dicho sobre el docente titular (AS-IS de la precarga, HOR_005, decisión 4 «alinear F3») no aplica a ningún camino HTTP; la decisión 4 rige solo para el coordinador.
- **La escritura de asignaturas sí existe** (`POST/PUT /coordinador/planes-estudio/{id}/asignaturas`); el Frontend ya tiene el campo «Horas Semanales Presenciales» pero no lo envía. Corrige la decisión 1c («no hay escritura»).
- **Solo el ADMINISTRADOR** crea/edita períodos (`/api/v1/admin/periodos-academicos`), no el coordinador (decisión 2a).
- **El contrato HTTP de crear/editar grupo está localizado**: `POST /api/v1/grupos` y `PUT /api/v1/grupos/{id}`, fuera del OpenAPI canónico.
- **B3, B4 y B7 ya verificados:** B3 sí lo cubren crear y editar; B4 existe para el *código* (409) pero sin índice único; B7 no existe parámetro de ventana horaria. **B1 y B2 no están cubiertas hoy.**
- **B2b también está en el SP congelado** `usp_generar_sesiones_grupo` (el endpoint docente `/generacion` seguiría pudiendo dar semana 17). Sigue siendo deducción analítica, sin probar.
- **Hallazgo de seguridad `SECURITY_FINDING-GRP-001`:** sin `franjas`, un coordinador puede crear/editar grupos de otro programa (el ámbito solo lo valida el SP de horarios). Las franjas obligatorias cubren la creación; la edición sin franjas sigue abierta.

## Decisiones del responsable sobre el contrato (2026-11-19, en chat)

Numeración de [CONTRACT_PROPOSAL §9](CONTRACT_PROPOSAL.md). Aún no están transcritas en ese documento (lo hará el aprobador del contrato al congelarlo).

- **DR-13 — unidad de hora: 60 minutos de reloj.** 4 horas semanales = 240 min de franjas; no hay parámetro de hora cátedra. Cierra BM-02 (los decimales siguen por definir con John).
- **DR-02 — franjas obligatorias: Frontend primero.** Se asegura que el Frontend siempre envíe `franjas` (incluido el editor vaciado, que hoy puede mandar `[]`) y después el backend lo exige (opción O1).
- **DR-05 — administrador en `PUT /api/v1/grupos/{id}`: se retira del permiso** (PEP y `access-catalog.json`); solo el coordinador edita grupos, como ya exige la DB.
- **DR-14 — ámbito con asignaturas compartidas: debe coordinar todos los programas del grupo** (igual que la edición actual; mínimo privilegio).

Siguen sin decidir: DR-01, 03, 04, 06–12 (propuestas por defecto en el documento, varias para John) y los conflictos CC-01..CC-05 / BM-01, 03–07.

## TARGET (propuesta, no decidida)

Flujo del coordinador: elige asignatura + período + docente → indica **cuántas veces por semana** y **qué días/horas** → el sistema **calcula y muestra** (sin escribir): horas por sesión, horas semanales, nº de sesiones del período, sesiones canceladas por festivo, total de horas programadas frente al esperado de la materia, y los conflictos → el coordinador **confirma** y todo se crea en una sola transacción.

Ejemplo de cálculo (sin inventar reglas): 4 h semanales en 2 días ⇒ 2 franjas de 2 h; si el período cubre 16 semanas completas, 32 sesiones y 64 h. Si el período real tiene otra duración o hay festivos, el resultado **cambia** y debe mostrarse, no forzarse a 32.

### Catálogo de validaciones

**A. Ya existen en la DB (reutilizar sin debilitar)**

| ID | Regla | Código |
|---|---|---|
| A1 | Correlación obligatoria; ámbito del ejecutor (coordinador del programa o docente titular) | SEC_002 |
| A2 | Franja bien formada, día existente, fin > inicio | HOR_004 |
| A3 | Franjas del grupo sin traslape entre sí | HOR_003 |
| A4 | Docente sin otro grupo a la misma hora en el período | HOR_002 |
| A5 | Docente titular solo define el horario inicial | HOR_005 |
| A6 | Fechas del período coherentes; zona horaria institucional configurada | usp_validar_fechas_periodo_academico_interno, GEN_001 |
| A7 | Hora local inexistente (DST) aborta antes de escribir | SES_005 |
| A8 | Idempotencia y protección de sesiones con asistencia, toma o clasificación | precarga interna |
| A9 | Festivos: sesión creada en festivo nace cancelada | CULM-001 B.1 |
| A10 | Cruce de horario del estudiante al matricular (solo matrículas INSCRITA, mismo período) | HOR_001 |

**B. Nuevas (propuestas; cada una exige contrato y dueño DB/Backend)**

| ID | Regla | Dueño probable |
|---|---|---|
| B1 | Asignatura activa y vinculada a un plan activo del programa del coordinador | DB |
| B2 | Período de la institución del coordinador, no cerrado ni terminado; **duración exacta de 16 semanas (112 días) al crear/editar períodos**, con fin calculado desde el inicio (ver decisión 2a) | DB + Backend + Frontend |
| B2b | `numeroSemana` de la sesión debe quedar en 1..16: hoy se calcula con `DATEDIFF(WEEK, inicio, fecha) + 1`, que cuenta cruces de semana de calendario y puede dar **17** si el período no empieza en el primer día de la semana (depende de `DATEFIRST`). **No probado**; verificar con una prueba y, si se confirma, calcular `DATEDIFF(DAY, inicio, fecha) / 7 + 1` | DB |
| B3 | Docente existe y tiene vínculo activo con el programa (ya lo hace `usp_crear_grupo`; verificar que cubre edición) | DB |
| B4 | Código/nombre de grupo único por asignatura + período (**no verificado** que hoy exista) | DB |
| B5 | **Bloquea:** horas semanales de las franjas = `horasSemanales` de la materia, exacto (decisión 3); con `NULL` solo avisa | DB + Backend |
| B6 | Cantidad de franjas por semana coherente con la materia (1 o 2 en el ejemplo); se permiten dos franjas el mismo día (decisión 7) | DB + Backend |
| B7 | Duración mínima/máxima por franja y ventana horaria institucional (parámetro; **no verificado** que exista) | DB |
| B8 | Total de horas programadas (excluyendo canceladas por festivo, según decisión) ≥ total esperado; si no, **informar sin bloquear** (decisiones 2c y 3) | DB |
| B9 | **Cambiar franjas con estudiantes ya inscritos**: validar cruce contra los otros grupos de cada inscrito antes de aplicar (hoy hueco) | DB |
| B10 | Sesión ya dictada o con asistencia: el nuevo horario no la altera y se descuenta del total (hoy se respeta pero no se reconcilia horas) | DB |
| B11 | Grupo creado a mitad de período: la precarga arranca hoy; mostrar horas que ya no se pueden programar | DB + Backend |
| B12 | Dos asignaturas del mismo semestre/plan que se crucen: **advertencia**, no bloqueo (no hay entidad de cohorte hoy) | decisión |
| B13 | Aula/salón: **fuera de alcance** (no hay dato; la columna está prohibida por DB-GP-001) | — |

### Decisiones abiertas (bloquean READY de implementación)

1. **Fuente de las horas de la materia — DECIDIDA (responsable, 2026-11-19, en chat): columna nueva en la DB.** No se deriva de `credito` ni se captura por grupo. Quedan por cerrar con el dueño DB (John), sin bloquear el resto del análisis:
   - 1a. Tabla: propuesta `Asignatura` (propiedad de la materia; `codigo` es único global, DEC-019). Alternativa `AsignaturaPlanEstudio` solo si la misma materia pudiera tener horas distintas por plan.
   - 1b. Qué se guarda: propuesta `horasSemanales` (decimal, > 0), porque es el dato que el responsable da por sentado (p. ej. 4). El total del semestre se calcula con la decisión 2, no se almacena.
   - 1c. Filas existentes: la columna nace sin valor; hay que decidir si es `NULL` hasta que el coordinador/administrador la complete (B5/B8 no se aplican con `NULL`, y se avisa) o si hay carga inicial. Quién la edita y por qué ruta (hoy no hay escritura de asignaturas en este alcance) queda por definir.
   - 1d. Consumidores a revisar: `uv_asignatura`, `uv_asignatura_plan_estudio`, catálogos de asignaturas del coordinador y la carga de planes.
2. **Semántica del total — DECIDIDA (responsable, 2026-11-19; diseño del planificador; 2a–2d confirmados):** el semestre dura siempre **16 semanas**, y se impone **en el período**, no en el grupo.
   - 2a. **Período fijo de 16 semanas = 112 días corridos** (`fechaFin = fechaInicio + 111 días`). El coordinador/administrador elige solo la fecha de inicio (puede ser cualquier día) y la de fin se calcula sola y es de solo lectura; la DB rechaza cualquier período nuevo o editado que no mida 112 días. Hoy `usp_crear_periodo_academico` y `usp_actualizar_periodo_academico` solo exigen `fechaInicio < fechaFin` (PER_002), así que **la regla de 16 semanas no se aplica hoy**.
   - 2b. **El conteo de sesiones sigue saliendo del calendario real, sin "16" escrito en el código del grupo.** Cualquier tramo de 112 días seguidos contiene **exactamente 16 veces cada día de la semana**; por eso, con período de 16 semanas, 1 día/semana ⇒ 16 sesiones y 2 días/semana ⇒ 32, empiece el semestre el día que empiece. El total esperado se calcula como `horasSemanales × semanas del período` (semanas = días del período / 7), así también funciona con períodos históricos que no midan 16.
   - 2c. **Festivos (CONFIRMADO por el responsable, 2026-11-19):** no bloquean; la sesión queda cancelada y el cálculo previo muestra "horas que se pierden por festivo" como informativo. La compensación de esas horas queda fuera de este work item.
   - 2d. **Períodos existentes con otra duración (CONFIRMADO por el responsable, 2026-11-19):** se respetan (sin migración destructiva); la regla de 16 semanas rige solo para períodos creados o editados desde ahora. Para esos períodos, B8 usa su duración real.
3. **Rigor — DECIDIDA (responsable, 2026-11-19): bloquear exacto.** Si las horas semanales de las franjas (suma de duraciones) no son iguales a `horasSemanales` de la materia, el grupo no se confirma (B5). Sin tolerancia. Si la materia aún no tiene horas cargadas (`NULL`, decisión 1c), no se puede validar y solo se avisa. Precisión del planificador: el bloqueo es sobre las **horas semanales** (B5); el **total del semestre** (B8/B11) **informa y no bloquea**, porque un grupo creado con el semestre empezado (decisión 6) nunca alcanzaría el total y quedaría imposible de crear.
4. **Franjas — DECIDIDA (responsable, 2026-11-19): obligatorias al crear un grupo.** Todo grupo nace con horario y con sus sesiones. Es un **cambio de comportamiento** (hoy `franjas` es opcional): el contrato de creación pasa a exigirlo, lo que rompe a clientes que no lo envían; exige decisión de compatibilidad en `02-contratos` (versión o convivencia) y ajuste del Frontend. Al **editar**, `franjas` ausente sigue significando "el horario no cambia". El flujo F3 (el docente titular crea su grupo y define el horario inicial, HOR_005) debe quedar alineado.
5. **Previsualización — DECIDIDA (responsable, 2026-11-19): operación aparte de solo lectura.** Calcula sin escribir: horas por franja, horas semanales, nº de sesiones del período, sesiones que caen en festivo y horas que se pierden, total frente al esperado, y todos los conflictos de A y B. Agrega una ruta (+1 al PDP). Clasificación `RESOURCE` vs `BUSINESS_COMMAND` y método HTTP (los datos son una propuesta de horario, no un recurso guardado) los resuelve `02-contratos` con API_DESIGN_RULES. La confirmación final **vuelve a validar todo** en la DB: la previsualización no sustituye a las reglas de escritura.
6. **Grupo creado tarde — DECIDIDA (responsable, 2026-11-19): generar desde hoy y avisar.** Se conserva la regla vigente (el pasado no se genera ni se toca); la previsualización muestra las horas que ya no se pueden programar. Informa, no bloquea (ver 3).
7. **Dos franjas el mismo día — DECIDIDA (responsable, 2026-11-19): permitido** mientras no se traslapen (contiguas incluidas, como hoy en HOR_003). Cada franja es una sesión. Consecuencia: B6 cuenta **franjas por semana**, no días distintos, y B5 suma sus duraciones.
8. **Cambios de SP**: los hace el dueño del repo DB (John); el backend no administra el esquema.

## Clase de cambio y alcance de rutas

- Change class: **CONTRACT_ANALYSIS** ahora; luego, por separado y con su DoR, `CONTRACT_CHANGE` / `BEHAVIOR_CHANGE` (y cambio DB del dueño).
- PRIMARY_VARIABLE: regla de cálculo y validación del total de horas de un grupo al definir su horario.
- ALLOWED_PATHS: `docs/work-items/SES-GEN-01-calendario-semestre-grupo/**`; índice `docs/work-items/README.md`.
- FORBIDDEN_PATHS: `src/**`, `docs/contracts/openapi/**`, todo el repo DB y el Frontend hasta tener DoR de implementación.
- CONTRACTS: HTTP (crear/editar grupo, posible previsualización), PERSISTENCE (SPs de grupo/horario/precarga, dato de horas), SECURITY (PDP si hay ruta nueva), mensajes (códigos nuevos).
- PROVIDERS: SQL Server (SP existentes), catálogo de mensajes/parámetros.
- EXTERNAL_ENVIRONMENT: DB desechable E2E (guardas `e2e_safety`, ver memoria local) para las reglas B; nunca la DB viva.
- SECURITY_IMPACT: ámbito del coordinador por programa; ruta nueva exige aprovisionar el PDP.
- OBSERVABILITY_IMPACT: correlación y auditoría de la creación/edición del horario; sin datos personales nuevos.
- TEST_LEVEL_REQUIRED: unitario + contrato HTTP + IT contra SQL Server real para reglas B (un mock no certifica el SP).
- ROLLBACK: este PLAN es documental (revertir el archivo). Fases posteriores: aditivas, versionadas, con migración inversa del dueño DB.
- CONSUMERS: Frontend (formulario de grupo del coordinador, calendario), consultas de horas (`horasProgramadas`, HU019), reportes de asistencia por horas, alertas de inasistencia, tomas de asistencia.
- STOP_CONDITIONS: `BLOCKED_BY_MISSING_EVIDENCE` (dato de horas), `CONTRACT_CONFLICT` si el OpenAPI y la matriz difieren, ninguna implementación sin DoR.

## Alcance

Analizar y decidir el contrato del cálculo/validación; ordenar el trabajo en DB → contrato → RED → Backend → Frontend → E2E.

## No alcance

Escribir código, SP, OpenAPI o frontend; aula/salón; cohortes; cambiar el SP congelado `usp_generar_sesiones_grupo`; reabrir el endpoint docente de generación.

## Archivos afectados

- EXISTENTES (analizados, no modificados): `GrupoJpaRepository`, `CrearGrupoUseCaseImpl`, `ActualizarGrupoUseCaseImpl`, `CrearGrupoRequest`, `usp_precargar_sesiones_grupo(_interno)`, `usp_guardar_horarios_grupo(_interno)`, `usp_validar_cruce_horario_*`.
- NUEVOS (propuesta, fases futuras): `TEST_PLAN.md`, `CONTRACT_MATRIX.md` del work item; DB: dato de horas de la materia y reglas B; Backend: DTO/UC de previsualización o respuesta ampliada; Frontend: formulario con cálculo en vivo.
- RETIRAR: ninguno.

## Contratos y consumidores afectados

| Contrato | Consumidor | Compatibilidad |
|---|---|---|
| HTTP crear/editar grupo (`franjas`, ¿respuesta con totales?) | Frontend coordinador | Debe ser aditivo; pendiente localizar el contrato vigente |
| HTTP/DB crear y editar período académico (fin = inicio + 111 días; hoy solo `inicio < fin`). Work item previo: [HU096-HU098](../HU096-HU097-HU098-periodos-academicos/CLOSURE.md) (solo Administrador) | Frontend del administrador (selector de fechas), `GET /coordinador/periodos-academicos`, cierre masivo | Cambio de comportamiento: períodos nuevos/editados con otra duración dejan de aceptarse; los existentes se conservan |
| HTTP previsualización (nuevo, propuesto) | Frontend coordinador | Ruta nueva; PDP +1 ruta |
| PERSISTENCE: SP y dato de horas | Backend | Dueño DB; aprobación versionada |
| Mensajes: códigos nuevos (p. ej. horas no cuadran) | Backend/Frontend | Catálogo, no texto en código |

## Riesgos y dependencias

- Sin dato de horas en la DB no hay "total esperado": depende de la decisión 1 y del dueño DB.
- El conteo real depende del período y de los festivos; prometer "32" siempre sería falso.
- La precarga reemplaza sesiones futuras generadas: editar horario con asistencia ya tomada requiere B10 antes de tocar nada.
- El hueco B9 existe hoy y puede producir cruces de estudiantes ya inscritos.
- Build base y E2E de esta rama no verificados en esta sesión.

## Test plan

Pendiente `TEST_PLAN.md`. Borrador de casos RED, derivados de los requisitos y no de la implementación: 1 día/semana y 2 días/semana con período de 16 semanas, empezando en lunes y empezando a mitad de semana (16 y 32 en ambos); período creado con fin distinto de inicio + 111 días rechazado; fin calculado desde el inicio; período histórico de otra duración conserva su cálculo; `numeroSemana` nunca pasa de 16; período más corto; festivo que cancela; horas semanales distintas de las de la materia; cruce con otro grupo del docente; cambio de franja con estudiante inscrito que se cruza; edición con sesiones ya dictadas; grupo creado a mitad de período; creación sin franjas; ejecutor ajeno (`SEC_002`); falla intermedia sin escrituras parciales; idempotencia al repetir. Secuencia RED → GREEN → VALIDATE; integración real `NOT_RUN` no es PASS.

## Rollback y stop conditions

Ver "Clase de cambio". `CONTRACT_CONFLICT` y `TEST_CONTRACT_CONFLICT` se registran en este work item antes de continuar.

## Deuda conocida y validación manual

- **Implementado el 2026-11-19 por orden directa del responsable** («empieza a implementar ya, SQL, backend y front, sobre los proyectos oficiales, sin laboratorio»): se implementó sin esperar el TEST_PLAN RED previo ni la entrega por fases a John; las pruebas se escribieron junto con el código y están listadas en [VALIDATION.md](VALIDATION.md). El contrato implementado está en [CONTRACT.md](CONTRACT.md).
- Deuda: **DR-03** — editar el inicio de un período con grupos/sesiones ya generados no tiene guarda (las sesiones existentes no se reprecargan). **CC-04** — `usp_generar_sesiones_grupo` (congelado, solo DOCENTE) conserva el cálculo `DATEDIFF(WEEK…)`: puede dar `numeroSemana` 17 en períodos que no empiecen en lunes. **CC-05** — texto de `VAL_002`. **B6/B12** — sin dato que los defina. Los SP congelados y las 7 filas de `Grupo` existentes no se tocaron.
- MV pendiente: recorrido manual del coordinador en navegador (crear materia con horas → crear período → crear grupo con previsualización → editar horario).
- Tras la entrega: aprovisionar el PDP con el recurso nuevo (`provision_asistencias.py pdp` + `verify`).

## Definition of Ready

- **CONTRACT_ANALYSIS: READY**.
- **Implementación: ejecutada con autorización explícita del responsable** (omite el flujo TEST_PLAN-RED-primero; la brecha queda registrada arriba). Contrato implementado: [CONTRACT.md](CONTRACT.md); documentos de análisis históricos: [CONTRACT_PROPOSAL](CONTRACT_PROPOSAL.md), [CONTRACT_MATRIX](CONTRACT_MATRIX.md), [CONTRACT_CONFLICTS](CONTRACT_CONFLICTS.md), [HANDOFF_DB](HANDOFF_DB.md) (ya no se enviará a John: los cambios se escribieron directamente en el repo DB).

## Siguiente paso propuesto

1. Revisión del PR de los tres repos (DB, Backend, Frontend) y despliegue del paquete DB (migración 57 + objetos) en los demás ambientes.
2. Aprovisionar el PDP (ruta nueva) y ejecutar `verify`/`verify-decisions`.
3. Validación manual MV del recorrido del coordinador.
4. Decidir DR-03 (guarda al editar períodos con grupos) y CC-04 (SP congelado) con el dueño DB.

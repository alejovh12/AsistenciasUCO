---
status: implemented
type: work-item
scope: backend-db-frontend
owner: backend-team
last-reviewed: 2026-11-19
---

# CONTRACT — SES-GEN-01: calendario de sesiones del semestre al definir el grupo

Contrato **tal como quedó implementado** (2026-11-19), a partir de [CONTRACT_PROPOSAL](CONTRACT_PROPOSAL.md) y de las decisiones del responsable en chat. Donde difiere de la propuesta, manda este documento (ver §9). Fuera del OpenAPI de Golden Path (`NON_GOLDEN_PATH_EXTENSION`); se congela aquí, en `HTTP_AS_IS_MATRIX.md` y en `infra/security-baseline/access-catalog.json`.

## 1. Previsualización del horario (ruta nueva, solo lectura)

`POST /api/v1/coordinador/grupos/previsualizacion-horario` — rol `COORDINADOR`. `BUSINESS_COMMAND` de lectura: **200** (no 201), no escribe estado de negocio. Un recurso nuevo en el PDP (`asistencias-uco`, concesión a `COORDINADOR`).

Petición:

```json
{ "idGrupo": null, "idAsignatura": "<uuid>", "idPeriodoAcademico": "<uuid>", "idDocente": "<uuid>",
  "franjas": [ { "dia": "LU", "horaInicio": "08:00", "horaFin": "10:00" } ] }
```

- `idGrupo` ausente = grupo nuevo: `idAsignatura`, `idPeriodoAcademico` e `idDocente` obligatorios (400 si falta alguno). Con `idGrupo` se toman del grupo (si se envían y difieren: 400 `VAL_001`); `idDocente` ausente = titular actual.
- `franjas`: obligatorio, de 1 a 21 elementos (tope técnico), mismas reglas de forma que crear grupo (`dia` = código `LU…DO` o nombre; horas `HH:mm`).
- Sin alias legados (`dias`, `horaInicio/horaFin` sueltos, `asignaturaId`, …).

Respuesta `200` (`ApiDataResponse<PrevisualizacionHorarioGrupoDTO>`). **Todos los tiempos de duración van en minutos enteros**; una hora de la materia son **60 minutos de reloj**:

| Campo | Contenido |
|---|---|
| `idGrupo` | Eco; `null` si es grupo nuevo |
| `asignatura` | `{id, codigo, nombre, horasSemanales}`; `horasSemanales` `null` = sin cargar |
| `periodo` | `{id, nombre, fechaInicio, fechaFin, diasPeriodo, semanasPeriodo, estado}` (`yyyy-MM-dd`; `estado` PLANEACION/ACTIVO/CERRADO) |
| `zonaHoraria` | Zona IANA institucional |
| `franjas[]` | `{codigoDia, nombreDia, horaInicio, horaFin, minutos}` |
| `resumenSemanal` | `{franjasPorSemana, minutosSemanales, minutosSemanalesMateria, diferenciaMinutos, coincideConMateria}` (los de la materia, `null` si no tiene horas) |
| `resumenPeriodo` | `{sesionesCalendario, sesionesProgramadas, sesionesEnFestivo, sesionesNoProgramables, minutosCalendario, minutosProgramados, minutosPerdidosFestivo, minutosNoProgramables, minutosEsperadosMateria, diferenciaMinutos, cubreTotalEsperado}`; esperado = `horasSemanales × 60 × diasPeriodo / 7`; **informa, no bloquea** |
| `festivos[]` | `{fecha, nombre, sesionesAfectadas}` |
| `sesiones[]` | `{numero, numeroSemana (1..16 en 112 días), fecha, horaInicio, horaFin, fechaHoraInicioUtc, fechaHoraFinUtc, minutos, estado, nombreFestivo}`; `estado` ∈ `PROGRAMADA`, `CANCELADA_POR_FESTIVO`, `NO_PROGRAMABLE_POR_FECHA` |
| `cambioSobreGrupoExistente` | Solo con `idGrupo`: `{sesionesConservadas, minutosConservados, sesionesFuturasQueSeRetiran, sesionesFuturasQueSeCrean, sesionesFuturasQueSeConservan}` |
| `conflictos[]` | `{codigo, severidad, mensaje, franja}`; `severidad` ∈ `BLOQUEANTE` (la confirmación lo rechazaría con el mismo código), `ADVERTENCIA`, `INFORMATIVO`; `mensaje` sale del catálogo de la DB |
| `puedeConfirmar` | `true` si no hay conflictos `BLOQUEANTE` (informativo: la confirmación revalida todo) |

Errores: 400 (`VALIDATION_ERROR`/`VAL_APP_076`: franjas ausentes o vacías, hora ilegible, día inexistente, `HOR_004`, `VAL_002` recurso inexistente); 401; 403 (rol, o ámbito: `SEC_001`/`SEC_002`); 409 `ASI_010` (asignatura inactiva, sin plan activo o de programas que no coordina — **todos**, DR-14) y `PER_010` (período terminado, cerrado o de otra institución). Los conflictos del **horario** (`HOR_002`, `HOR_003`, `HOR_006`, `HOR_007`, `HOR_008`, `HOR_009`, `SES_005`) van como datos con 200.

Calculado en la DB (`usp_previsualizar_horario_grupo`, un solo conjunto de resultados de 34 columnas, ver su cabecera): mismo día de semana, zona horaria y festivos que la precarga que ejecuta la confirmación; **previsualización = confirmación** (probado en `test_ses_gen_01_calendario_semestre.sql`). El backend asegura los festivos del período antes de calcular (mejor esfuerzo, como al crear/editar).

## 2. Crear y editar grupo

- `POST /api/v1/grupos`: `franjas` **obligatorias** (≥ 1, tras normalizar el formato legado). Ausentes o `[]` → **400** `VAL_APP_076` (`ERR_CAMPO_OBLIGATORIO`). Cambio de comportamiento aprobado (DR-02: Frontend primero, luego backend).
- `PUT /api/v1/grupos/{id}`: `franjas` ausente = «el horario no cambia» (sin cambio); `[]` sigue siendo 400 (`HOR_004`). **Solo `COORDINADOR`** (DR-05): se retiró `ADMINISTRADOR` de `SecurityConfig` y del catálogo de accesos.
- Respuesta de ambos: sin cambio (`{id, mensajeUsuario}`); el cliente confirma con la previsualización previa.
- Nuevas validaciones en la DB (mismos códigos en confirmación y previsualización): `usp_crear_grupo` — B1 (`ASI_010`), B2 (`PER_010`; **ya no sustituye en silencio** el período por el más reciente: `idPeriodoAcademico` es obligatorio, DR-06); `usp_actualizar_grupo` — ámbito del coordinador sobre los programas de la asignatura (`SEC_002`, cierra `SECURITY_FINDING-GRP-001`); `usp_guardar_horarios_grupo` — B5 `HOR_006` (suma semanal exacta = `horasSemanales × 60`; con NULL no valida), B7 `HOR_007` (duración 60–240 min) y `HOR_008` (ventana 06:00–22:00), B9 `HOR_009` (cruce con otros grupos de estudiantes inscritos).

## 3. Asignaturas: `horasSemanales`

- Lectura (aditivo): `GET /api/v1/coordinador/asignaturas` y `GET /api/v1/{coordinador,decano}/planes-estudio/{planId}/asignaturas` agregan `horasSemanales` (`number | null`). El SP devuelve 22 columnas (antes 21).
- Escritura (aditivo): `POST/PUT /api/v1/coordinador/planes-estudio/{id}/asignaturas…` aceptan `horasSemanales` (entero ≥ 1; la DB valida el tope, `ASI_009`). En `POST`, ausente = NULL (sin cargar); en `PUT`, ausente = no cambia.
- Decisiones: entero (sin fracciones), tabla `Asignatura`, filas existentes NULL (con NULL, B5/B8 no se evalúan y se avisa con `HOR_013`).

## 4. Período académico

- `POST/PUT /api/v1/admin/periodos-academicos`: `fechaFin` **opcional**. Falta → la DB la calcula (`fechaInicio + 111 días`); presente y distinta de la calculada → 400 `PER_011`; `inicio ≥ fin` → 400 (`PER_002`/`VAL_APP_020`).
- `PUT`: la duración solo se valida si cambian `fechaInicio` o `fechaFin` (un período histórico que solo cambia de nombre se conserva).
- `POST` responde 201 con la `fechaFin` **persistida** (la lee de `uv_periodo_academico` si no se envió).
- Frontend: el modal solo pide el inicio y muestra el fin calculado de solo lectura; en edición, si no cambia el inicio, conserva el fin guardado.

## 5. Códigos de error (públicos = códigos de la DB)

| Código | HTTP | Regla |
|---|---|---|
| `HOR_006` | 409 | B5: horas semanales de las franjas ≠ las de la materia |
| `HOR_007` / `HOR_008` | 400 | B7: duración / ventana horaria de la franja |
| `HOR_009` | 409 | B9: el cambio cruza con otros grupos de estudiantes inscritos |
| `ASI_009` | 400 | `horasSemanales` fuera de 1..`ASIGNATURA/HORAS_SEMANALES_MAXIMO` |
| `ASI_010` | 409 | B1: asignatura no disponible para crear grupos |
| `PER_010` | 409 | B2: período no vigente o de otra institución |
| `PER_011` | 400 | Duración del período ≠ 16 semanas |
| `SES_005` | 409 | Hora local inexistente por cambio de horario (antes `ERR_DB_UNCLASSIFIED`) |
| `VAL_APP_076` | 400 | `franjas` obligatorias al crear grupo (backend; `code` público `ERR_CAMPO_OBLIGATORIO`) |
| `HOR_010`–`HOR_014` | 200 `INFORMATIVO`/`ADVERTENCIA` | Solo previsualización (total < esperado, festivos, inicio tardío, materia sin horas, sesiones conservadas) |

Los textos y marcadores viven en `CatalogoMensajeUsuario` (migración 57 / seeds 04 y 62); el backend conserva solo código, clase y respaldo.

## 6. Parámetros de la DB (`CatalogoParametro`)

`PERIODO/SEMANAS_DURACION = 16`; `ASIGNATURA/HORAS_SEMANALES_MAXIMO = 40`; `HORARIO/DURACION_MINIMA_MINUTOS = 60`; `HORARIO/DURACION_MAXIMA_MINUTOS = 240`; `HORARIO/VENTANA_INICIO = 06:00`; `HORARIO/VENTANA_FIN = 22:00`. Valores fijados por el responsable (2026-11-19). Parámetro de horario ausente: la regla B7 no se evalúa; `SEMANAS_DURACION` ausente: fail-closed (`PER_011`).

## 7. Seguridad y PDP

- Catálogo de accesos: +1 entrada (`POST /api/v1/coordinador/grupos/previsualizacion-horario`, `COORDINADOR`) y `PUT /api/v1/grupos/{id}` pasa a `["COORDINADOR"]`. `provision_asistencias.py catalog`: 198 endpoints, 0 sin regla, 0 obsoletos.
- **El PDP debe aprovisionarse** con el recurso nuevo (`provision_asistencias.py pdp`, luego `verify`); sin eso el PEP responde 403. La concesión de `ADMINISTRADOR` sobre `PUT /grupos/{id}` que ya exista en el PDP aparece como deriva (`SOBRA`) y solo se revoca con `--prune`.
- Grant SQL: `rol_asistencias_runtime_ses_gen_01.sql` (`EXECUTE` sobre `usp_previsualizar_horario_grupo`).

## 8. Eventos y observabilidad

Sin eventos ni SSE nuevos. La previsualización no se audita (no escribe), no registra el cuerpo ni datos personales y conserva `X-Correlation-Id`.

## 9. Diferencias con la propuesta (decisiones del responsable en chat, 2026-11-19)

| Tema | Propuesta | Implementado |
|---|---|---|
| Unidad de la hora | Horas con decimales por definir | Entero, 60 min de reloj; totales en **minutos** en la API |
| Valores B7 | Por definir | 60–240 min por franja, ventana 06:00–22:00, parámetros en la DB |
| Códigos públicos | `ERR_HORAS_SEMANALES_NO_COINCIDEN`, … | Los códigos de la DB (`HOR_006`, `ASI_009`, …), como `PER_007`/`ASI_008` |
| Aplicación de la DB | Entrega por fases con handoff a John | Cambios escritos en el repo DB y aplicados a la DB de desarrollo en una sola entrega |
| SP congelado `usp_generar_sesiones_grupo` | A decidir (CC-04) | **No se toca**; solo se corrigió `numeroSemana` en la precarga |
| Lectura tolerante de columnas | Secuencia expandir/contraer | Backend y DB se entregan juntos: el backend lee 22 columnas |
| B6 (franjas por semana) | Sin definición | Se reduce a B5 + B7 + ≥ 1 franja |
| Guarda al editar períodos con grupos (DR-03) | A decidir | **Sin guarda nueva** (deuda conocida) |

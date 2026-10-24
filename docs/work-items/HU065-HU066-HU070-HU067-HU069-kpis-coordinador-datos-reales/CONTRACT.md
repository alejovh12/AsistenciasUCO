---
status: frozen-implemented
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# CONTRACT — HU065 / HU066 / HU070 / HU067 / HU069: KPIs y alertas del Coordinador con datos reales

> **Work Item:** `HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales`
> **Estado:** `FROZEN-IMPLEMENTED`: aprobado por el owner con D1–D10 del [PLAN](PLAN.md); evidencia en [VALIDATION](VALIDATION.md).
> **Supuestos del borrador:** se toman las recomendaciones del PLAN: D1(a) denominador = sesiones vencidas con registro, D2(a) falta = `SJC`, D3(a) cierre masivo fuera de alcance, D4(a) sesión ejecutada = vencida con registro, D5(a) sin teléfono, D6(a) peor clasificación entre matrículas activas, D7(a) `uv_sesion_v2`, D8(b) período opcional, D9(a) sin SSE, D10(a) ramas desde `develop`.
> **Conformidad:** `constitution.md`, `AGENTS.md` §3.3, `GUIA_METODICA_IMPLEMENTACION_HU.md`.

---

## 1. Definiciones normativas (SSOT de los cálculos)

Todas se implementan **una sola vez, en DB**. El backend y el frontend no recalculan.

| Término | Definición |
|---|---|
| **Matrícula activa** | Fila de `uv_estudiante_grupo` con `codigoEstadoEstudiante = 'A'` en un grupo del programa. |
| **Sesión vencida** | Sesión de `uv_sesion_v2` con `fechaHoraFin` ≤ ahora **y** `procedenciaTemporal` determinada (D7). Las de procedencia indeterminada no se cuentan como vencidas y se reportan aparte. |
| **Sesión con registro** | Sesión con ≥ 1 fila en `uv_asistencia`. |
| **Sesión ejecutada** | Vencida **y** con registro (D4). |
| **Sesión vencida sin registro** | Vencida y sin registro: incumplimiento de calendario. |
| **Falta** | `DetalleAsistencia.asistio = 0` con `RazonCausa.codigo = 'SJC'`. La excusa (`EX`) no cuenta (D2). |
| **Registro de la matrícula** | Una sesión vencida **con registro** del grupo de la matrícula (D1). |
| **% de inasistencia** (por matrícula) | `faltas / sesionesConRegistroVencidas × 100`, redondeado a 2 decimales; `0` si no hay sesiones ejecutadas en el grupo. |
| **Clasificación** | `ufn_clasificar_permanencia(%)`: `PERDIDA` si `% ≥ ASISTENCIA/UMBRAL_ALERTA_PERDIDA`; `RIESGO` si `% ≥ ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA`; si no, `REGULAR`. Una matrícula con estado `CI` (cancelado por inasistencia) se clasifica `PERDIDA` sin importar el %. |
| **Semáforo** | `VERDE` = `REGULAR`, `AMBAR` = `RIESGO`, `ROJO` = `PERDIDA`. |
| **Clasificación de la persona** | La peor de sus matrículas activas del programa (`PERDIDA` > `RIESGO` > `REGULAR`); % de la persona = el mayor de sus matrículas activas (D6). |
| **Tasa de permanencia** | `(estudiantesRegulares + estudiantesRiesgo) / totalEstudiantes × 100`; `null` si `totalEstudiantes = 0`. |
| **% de cumplimiento** | `sesionesEjecutadas / sesionesVencidas × 100`; `null` si no hay sesiones vencidas. |
| **Progreso del calendario** | `sesionesVencidas / sesionesProgramadas × 100`; `null` si no hay sesiones programadas. |

Todo valor que no pueda calcularse (divisor cero, dato inexistente) se devuelve `NULL`/ausente. **Nunca** un valor por defecto "creíble".

---

## 2. Contrato de Base de Datos

### 2.1 Reglas comunes a los SP de este WI

1. Solo lectura, **sin** `SET XACT_ABORT ON` ni transacciones. `SET NOCOUNT ON`.
2. Contexto de ejecutor (`usp_establecer_contexto_usuario_ejecutor`): se captura el `SESSION_CONTEXT` previo y **siempre** se restaura (éxito y error), como `usp_consultar_grupos_paginado`.
3. `@idUsuarioEjecutor` y `@idCorrelacion` obligatorios; validación de correlación con `usp_validar_id_correlacion_esta_presente_interno`; fallas con `THROW 51000, <mensajeTecnico>, 1` (`DBCODE=…|…`).
4. **Ámbito dentro del SP:** el programa se resuelve desde el ejecutor (`Coordinador` → `Programa.coordinador`). Si el ejecutor no es coordinador de un programa → `SEC_001`/`KPI_001` (ver §2.7). Las filas de estudiantes salen de `uv_auth_estudiante`/`uv_auth_sesion` (fail-closed sin contexto).
5. Resultsets `READ_QUERY_RESULTSET`; se agregan a la excepción de 4 columnas de `test/test_contract_closure.sql` (archivo caliente, al final).
6. Paginación: `@numeroPagina ≥ 1`, `@tamanoPagina` entre 1 y `PAGINACION/TAMANO_MAXIMO`; si no, `PAG_001` (código ya existente).
7. Sin números mágicos: umbrales y tamaño máximo salen de `CatalogoParametro`; códigos de razón/estado de sus catálogos.

### 2.2 Vistas y función

**`dbo.uv_cumplimiento_sesion_grupo`** (una fila por sesión):

| Columna | Tipo | Origen |
|---|---|---|
| `idSesion`, `idGrupo`, `idPeriodoAcademico` | `UNIQUEIDENTIFIER` | `uv_sesion_v2` / `uv_grupo` |
| `idPrograma` | `UNIQUEIDENTIFIER` | `uv_grupo_programa` |
| `fechaHoraInicio`, `fechaHoraFin` | `DATETIME2` | `uv_sesion_v2` |
| `estaVencida` | `BIT` | `fechaHoraFin ≤ SYSUTCDATETIME()` **y** `procedenciaTemporal IS NOT NULL` |
| `procedenciaIndeterminada` | `BIT` | `procedenciaTemporal IS NULL` |
| `tieneRegistroAsistencia` | `BIT` | `EXISTS` en `uv_asistencia` |

**`dbo.uv_resumen_asistencia_estudiante_grupo`** (una fila por `EstudianteGrupo`):

| Columna | Tipo |
|---|---|
| `idEstudianteGrupo`, `idEstudiante`, `idGrupo`, `idAsignatura`, `idPrograma`, `idPeriodoAcademico` | `UNIQUEIDENTIFIER` |
| `codigoEstadoEstudiante` | `NVARCHAR` |
| `sesionesEjecutadas` | `INT` (sesiones vencidas con registro del grupo) |
| `asistencias`, `faltasSinJustaCausa`, `excusas` | `INT` |
| `porcentajeInasistencia` | `DECIMAL(5,2)` |
| `codigoPermanencia` | `NVARCHAR(10)` (`ufn_clasificar_permanencia`) |

**`dbo.ufn_clasificar_permanencia(@porcentaje DECIMAL(5,2), @codigoEstadoEstudiante NVARCHAR(10))`** → `NVARCHAR(10)` (`REGULAR`/`RIESGO`/`PERDIDA`). Lee los dos parámetros del catálogo; si faltan, `RETURN NULL` (los SP lo traducen en un error técnico, no se asume un umbral).

> Ambas vistas son aditivas. No alteran `uv_sesion`, `uv_asistencia`, `uv_detalle_asistencia` (congeladas).

### 2.3 `dbo.usp_consultar_kpis_coordinador` (HU065, HU070)

| Parámetro | Tipo | Req. | Regla |
|---|---|:-:|---|
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER` | Sí | Debe ser coordinador de un programa. |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | Sí | |
| `@idPeriodoAcademico` | `UNIQUEIDENTIFIER = NULL` | No | NULL = período vigente (`uv_grupo.grupoEstaHablitado`). Si viene, debe existir (`PER_001`). |

**Resultset (1 fila):**

| Columna | Tipo | Nota |
|---|---|---|
| `idPrograma`, `nombrePrograma` | `UNIQUEIDENTIFIER`, `NVARCHAR` | |
| `idPeriodoAcademico`, `nombrePeriodoAcademico` | idem | `NULL` si no hay período vigente |
| `totalEstudiantes` | `INT` | personas distintas con ≥ 1 matrícula activa |
| `estudiantesRegulares`, `estudiantesRiesgo`, `estudiantesPerdida` | `INT` | clasificación de la persona (D6) |
| `tasaPermanencia` | `DECIMAL(5,2) NULL` | §1 |
| `sesionesProgramadas`, `sesionesVencidas`, `sesionesEjecutadas`, `sesionesVencidasSinRegistro`, `sesionesSinProcedenciaTemporal` | `INT` | §1 |
| `porcentajeCumplimiento`, `porcentajeProgresoCalendario` | `DECIMAL(5,2) NULL` | §1 |
| `umbralAdvertencia`, `umbralPerdida` | `DECIMAL(5,2)` | eco de los parámetros aplicados (transparencia para la UI) |

Sin período vigente: una fila con contadores en 0 y porcentajes `NULL` (no es un error).

### 2.4 `dbo.usp_consultar_alerta_temprana_programa` (HU066, HU070)

| Parámetro | Tipo | Req. | Regla |
|---|---|:-:|---|
| `@idUsuarioEjecutor`, `@idCorrelacion` | | Sí | |
| `@idPeriodoAcademico` | `UNIQUEIDENTIFIER = NULL` | No | como §2.3 |
| `@semaforo` | `NVARCHAR(5) = NULL` | No | `VERDE` / `AMBAR` / `ROJO`; otro valor → `GEN_002`. |
| `@filtroTexto` | `NVARCHAR(100) = NULL` | No | `CHARINDEX` sobre nombre, identificación y correo (sin comodines del usuario). |
| `@numeroPagina`, `@tamanoPagina` | `INT` | Sí | §2.1 (6) |

**Resultset:** una fila por persona (D6), orden `porcentajeInasistencia DESC, nombreCompleto ASC, idEstudiante ASC`:
`idEstudiante`, `nombreCompleto`, `numeroIdentificacion`, `correo`, `totalMatriculasActivas`, `porcentajeInasistencia`, `faltasSinJustaCausa`, `codigoPermanencia`, `semaforo`, `totalRegistros = COUNT(*) OVER()`.

### 2.5 `dbo.usp_consultar_bitacora_estudiante_programa` (HU067)

Parámetros: `@idUsuarioEjecutor`, `@idCorrelacion`, `@idEstudiante`, `@idPeriodoAcademico = NULL`, `@numeroPagina`, `@tamanoPagina`.

- El estudiante debe tener matrícula en un grupo del programa del ejecutor; si no → `GEN_001` con `'Estudiante'` (mismo código para inexistente y ajeno, CA-06).
- **Resultset** (orden `fechaHoraInicio DESC, idSesion DESC`): `idSesion`, `numeroSesion`, `nombreSesion`, `fechaHoraInicio`, `fechaHoraFin`, `idGrupo`, `codigoGrupo`, `idAsignatura`, `nombreAsignatura`, `asistio` (`BIT NULL`: `NULL` si la sesión vencida no tiene registro de ese estudiante), `codigoRazonCausa`, `nombreRazonCausa`, `totalRegistros`.

### 2.6 `dbo.usp_consultar_ficha_estudiante_programa` y `dbo.usp_consultar_resumen_asistencia_estudiante_programa` (HU069)

Ficha (1 fila): `@idUsuarioEjecutor`, `@idCorrelacion`, `@idEstudiante` → `idEstudiante`, `tipoIdentificacion`, `numeroIdentificacion`, `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `nombreCompleto`, `correo`, `estaActivoUsuario`, `idPrograma`, `nombrePrograma`. (Sin teléfono, D5.) Ajeno/inexistente → `GEN_001`.

Resumen (N filas, una por matrícula del estudiante en el programa y período): `@idUsuarioEjecutor`, `@idCorrelacion`, `@idEstudiante`, `@idPeriodoAcademico = NULL` → `idGrupo`, `codigoGrupo`, `idAsignatura`, `nombreAsignatura`, `codigoEstadoEstudiante`, `nombreEstadoEstudiante`, `sesionesEjecutadas`, `asistencias`, `faltasSinJustaCausa`, `excusas`, `porcentajeInasistencia`, `codigoPermanencia`.

### 2.7 Mensajes (seed propio `18_mensajes_kpis_coordinador.sql`, no `04_mensaje.sql`)

| Código | Uso | HTTP |
|---|---|:-:|
| `KPI_001` | El ejecutor no es coordinador de un programa. | 403 |
| `KPI_002` | Falta un parámetro de umbral en el catálogo (`ASISTENCIA/UMBRAL_*`). | 500 |
| `GEN_001`, `GEN_002`, `PER_001`, `PAG_001`, `SEC_001`, `CORR_001` | ya existentes | 404 / 400 / 404 / 400 / 403 / 400 |

---

## 3. Contrato HTTP (Backend)

Comunes: base `/api/v1`; autorización por el comodín existente `/api/v1/coordinador/**` (`COORDINADOR` | `ADMINISTRADOR`). El caso de uso resuelve el programa con `InstitutionalScopePort.findProgramaIdByCoordinadorUsuario`; sin programa → `403`. Errores con `ApiErrorResponse` (mensaje del catálogo). Validación de forma (UUID, paginación ≥ 1, `semaforo` ∈ {`VERDE`,`AMBAR`,`ROJO`}) → `400` sin llamar a la DB. `X-Correlation-Id` propagado.

| Método y ruta | HU | Query | 200 |
|---|---|---|---|
| `GET /api/v1/coordinador/indicadores` | 065, 070 | `idPeriodoAcademico?` | `ApiDataResponse<KpisCoordinadorResponse>` |
| `GET /api/v1/coordinador/alertas-tempranas` | 066, 070 | `pagina` (def. 1), `tamano` (def. 10), `semaforo?`, `nombre?`, `idPeriodoAcademico?` | `ApiListResponse<AlertaTempranaResponse>` (`total = totalRegistros`) |
| `GET /api/v1/coordinador/estudiantes/{idEstudiante}/bitacora` | 067 | `pagina`, `tamano`, `idPeriodoAcademico?` | `ApiListResponse<BitacoraAsistenciaResponse>` |
| `GET /api/v1/coordinador/estudiantes/{idEstudiante}/ficha` | 069 | `idPeriodoAcademico?` | `ApiDataResponse<FichaEstudianteResponse>` (cabecera + `resumenPorAsignatura[]`) |

Los nombres de campo de cada respuesta son exactamente las columnas de §2 (camelCase). `KpisCoordinadorResponse` incluye `umbralAdvertencia` y `umbralPerdida`. Los campos que pueden ser `null` (porcentajes, `asistio`) se serializan como `null`.

Errores: `400` (forma/`PAG_001`/`GEN_002`), `401`, `403` (`KPI_001`/`SEC_001`/administrador sin programa), `404` (`GEN_001` estudiante, `PER_001` período), `500` (`KPI_002`).

### 3.1 Capas

`CoordinadorIndicadoresController` / `CoordinadorFichaEstudianteController` → `Consultar*InputPort` → `*Interactor` → `*UseCaseImpl` (valida forma, resuelve el programa, llama al puerto) → `*QueryPort` → adapter JPA (`EXEC dbo.usp_consultar_…` nativo mapeado a `Sp*Entity`, un entity por resultset). Wiring con `@Bean` explícitos en `CoordinadorIndicadoresWiringConfiguration` (POJOs sin estereotipos). El adapter traduce `DBCODE=PAG_001|` a `ValidationException` (patrón de `ProgramaAcademicoJpaRepository`); el resto de códigos por `DbFailureClassifier` (`KPI_001` → 403, `KPI_002` → 500).

---

## 4. Contrato Frontend (Angular 19)

| Elemento | Contrato |
|---|---|
| `core/models/coordinator-indicators.model.ts` | Tipos espejo de las respuestas (sin `any`); porcentajes `number \| null`. |
| `core/services/coordinator-indicators.service.ts` | Signals `kpis`, `alertas`, `totalAlertas`, `cargando`; métodos `consultarKpis(periodo?)`, `consultarAlertas(filtro)`, `consultarBitacora(id, pagina)`, `consultarFicha(id)`. Errores propagados, sin `catchError(() => of(...))`. |
| `features/coordinator/coordinator-indicators/` | Tarjetas de cumplimiento y permanencia, tabla de alertas con `PaginationComponent` y filtro semafórico. OnPush, `input()`/`output()`. Los `null` se muestran como "Sin datos", nunca como 0 ni como un valor por defecto. |
| Ficha (modal) | Abre desde `coordinator-students` (solo se agrega el botón); línea de tiempo paginada (bitácora) y tabla de resumen por asignatura; el mensaje de error es el del backend. |
| Reglas | Sin cálculo de porcentajes ni clasificaciones en el cliente; sin `setInterval`; sin ids de negocio generados en cliente; el interceptor por rol existente ya evita llamadas de otros roles. |

---

## 5. Realtime y observabilidad

- **Realtime:** sin evento SSE en esta ola (D9).
- **Observabilidad:** `CorrelationIdContext.get()` → `@idCorrelacion`; logs INFO del caso de uso con `correlationId`, `idPrograma` y la operación, **sin PII** (sin nombres ni correos del estudiante).

---

## 6. Aprobación

| Rol | Estado | Fecha |
|---|---|---|
| Owner del producto | ⏳ Pendiente (D1–D10) | — |
| Congelamiento (`CONTRACT_FROZEN`) | ⏳ | — |

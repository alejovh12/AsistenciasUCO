---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# CONTRACT — HU133 / HU134 / HU135 / HU136 / HU138 / HU139: KPIs institucionales del Administrador con datos reales

> **Work Item:** `HU133-HU134-HU135-HU136-HU138-HU139-kpis-admin-datos-reales`
> **Estado:** `DRAFT`: no está congelado ni tiene `.sha256`. Pasa a `CONTRACT_FROZEN` cuando el owner apruebe y resuelva D1–D10 del [PLAN](PLAN.md).
> Hereda las definiciones de las olas [1](../HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales/CONTRACT.md) y [2](../HU102-HU103-HU106-HU144-kpis-decano-datos-reales/CONTRACT.md) (falta, sesión ejecutada, clasificación, retención, ausentismo ponderado, nulos) y sus aclaraciones: `sesionesEjecutadas` agregado es la **suma por matrícula activa**; período inexistente → 404; listados con `ApiListResponse`.

---

## 1. Definiciones normativas (SSOT de los cálculos)

Se implementan **una sola vez, en DB**, sobre `uv_resumen_asistencia_estudiante_grupo` y `uv_cumplimiento_sesion_grupo` (sin modificarlas). Backend y frontend no recalculan.

| Término | Definición |
|---|---|
| **Ámbito** | Facultades de la institución `Administrador.institucion` del ejecutor (D1). |
| **Matrícula activa** | Fila de la vista de resumen con `codigoEstadoEstudiante = 'A'` en un grupo de un programa del ámbito y del período resuelto. |
| **Estudiante (persona)** | Cuenta una vez, con la peor clasificación de sus matrículas activas en el ámbito (`PERDIDA` > `RIESGO` > `REGULAR`). |
| **% de ausentismo global** (HU133) | `Σ faltasSinJustaCausa / Σ sesionesEjecutadas × 100` sobre las matrículas activas del ámbito, 2 decimales; `null` si `Σ sesionesEjecutadas = 0` (D2). |
| **% de asistencia global** | `100 − porcentajeAusentismo`, calculado **en DB**; `null` si el ausentismo es `null`. |
| **Sesión vencida / con registro** | Las de `uv_cumplimiento_sesion_grupo`: vencida = `fechaHoraFin ≤ ahora` con procedencia temporal; con registro = existe ≥ 1 fila en `uv_asistencia`. |
| **Ratio de cumplimiento** (HU138) | `sesionesConRegistro / sesionesVencidas × 100` (solo sesiones vencidas con registro sobre vencidas) en grupos del ámbito y período, 2 decimales; `null` si `sesionesVencidas = 0` (D3). |
| **Tasa de retención** (por facultad, HU139) | `(estudiantesRegulares + estudiantesRiesgo) / totalEstudiantes × 100`; `null` si `totalEstudiantes = 0`. |
| **Intervalo temporal** (HU134) | `SEMANA`: semana calendario que inicia el lunes; `MES`: mes calendario; ambos según `Sesion.fechaHoraInicio` en la zona horaria institucional del catálogo (D5). |
| **Ausentismo del intervalo** | `Σ faltasSJC / Σ sesiones-estudiante ejecutadas` de las sesiones **vencidas con registro** cuyo inicio cae en el intervalo; el intervalo se emite solo si tiene ≥ 1 sesión ejecutada. |
| **Estudiantes con matrícula activa** (HU135) | Personas distintas con ≥ 1 matrícula activa en el período en el ámbito. |
| **Estudiantes registrados** | Estudiantes cuyo usuario está activo en el ámbito (vinculados por `EstudiantePrograma` a un programa de la institución). |
| **Docentes con grupo activo** (HU136) | Docentes titulares de ≥ 1 grupo habilitado del período en el ámbito. |
| **Docentes registrados** | Docentes con usuario activo y vínculo activo (`DocentePrograma.estado`) a un programa del ámbito. |

Todo valor que no pueda calcularse se devuelve `NULL`/ausente (los conteos sin datos son `0`, los porcentajes `NULL`). **Nunca** un valor por defecto «creíble».

---

## 2. Contrato de base de datos

Solo lectura (sin `XACT_ABORT` ni transacción). Errores con `THROW 51000` y `DBCODE=<código>|<mensaje>`; mensajes vía `usp_obtener_mensaje_catalogo`.

### 2.1 Internos

| Objeto | Parámetros | Resultado | Errores |
|---|---|---|---|
| `usp_resolver_institucion_administrador_interno` | `@idUsuarioEjecutor`, `@idCorrelacion` | `@idInstitucion OUTPUT` | `CORR_001`, `VAL_001` (ejecutor nulo), `KPI_004` (no es administrador de una institución), `KPI_002` (umbral ausente, cuando `@validarUmbrales = 1`) |
| `usp_resolver_periodo_kpi_institucion_interno` | `@idInstitucion`, `@idPeriodoSolicitado = NULL` | `@idPeriodo OUTPUT`: el pedido, o el más reciente con grupos habilitados en la institución; `NULL` si no hay | `PER_001` (período inexistente) |

### 2.2 Públicos

**`usp_consultar_kpis_institucion(@idUsuarioEjecutor, @idCorrelacion, @idPeriodoAcademico = NULL)`** — una fila:

`idInstitucion, nombreInstitucion, idPeriodoAcademico, nombrePeriodoAcademico, totalFacultades, totalProgramas, totalEstudiantes, estudiantesRegulares, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo, porcentajeAsistencia, sesionesVencidas, sesionesConRegistro, porcentajeCumplimiento, estudiantesConMatriculaActiva, estudiantesRegistrados, docentesConGrupoActivo, docentesRegistrados, umbralAdvertencia, umbralPerdida`.

Invariantes: `regulares + riesgo + perdida = totalEstudiantes = estudiantesConMatriculaActiva`; `sesionesConRegistro ≤ sesionesVencidas`.

**`usp_consultar_comparativa_facultades(@idUsuarioEjecutor, @idCorrelacion, @idPeriodoAcademico = NULL)`** — una fila por facultad de la institución (D4), orden `porcentajeAusentismo` desc (nulos al final), luego `nombreFacultad`:

`idFacultad, nombreFacultad, estaActivaFacultad, totalProgramas, totalEstudiantes, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo`.

**`usp_consultar_tendencia_ausentismo(@idUsuarioEjecutor, @idCorrelacion, @idPeriodoAcademico = NULL, @granularidad NVARCHAR(6) = N'SEMANA')`** — una fila por intervalo con datos, orden cronológico ascendente:

`inicioIntervalo (DATE), finIntervalo (DATE), sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo`.

`@granularidad` válida: `SEMANA`, `MES`; otro valor → `GEN_002`.

### 2.3 Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `KPI_004` (nuevo, `20_mensajes_kpis_admin.sql`) | 403 | El ejecutor no es administrador de una institución |
| `KPI_002` (ola 1) | 500 | Falta un umbral en el catálogo |
| `PER_001` (ola 1) | 404 | Período inexistente |
| `GEN_002` | 400 | `granularidad` no válida |
| `VAL_001`, `CORR_001` | 400 | Ejecutor o correlación nulos |

---

## 3. Contrato HTTP (todas bajo `/api/v1/admin`, rol `ADMINISTRADOR`)

| Método y ruta | Parámetros | Respuesta 200 |
|---|---|---|
| `GET /indicadores` | `idPeriodoAcademico?` | `ApiDataResponse<KpisInstitucionDTO>` |
| `GET /comparativa-facultades` | `idPeriodoAcademico?` | `ApiDataResponse<List<ComparativaFacultadDTO>>` (sin paginar: acotada por facultades) |
| `GET /tendencia-ausentismo` | `idPeriodoAcademico?`, `granularidad?` (`SEMANA` por defecto, `MES`) | `ApiDataResponse<List<PuntoTendenciaDTO>>` (sin paginar: acotada por intervalos del período) |

Los DTO tienen exactamente los campos de §2.2 (camelCase; `BigDecimal` con 2 decimales para porcentajes; `LocalDate` en ISO-8601; `null` donde §1 lo indica). Errores: `ApiErrorResponse` con `message` traducido por el backend; `KPI_004` viaja con su código para que el catálogo resuelva el mensaje.

Sin `Authorization` → 401; `DECANO`/`COORDINADOR`/`DOCENTE`/`ESTUDIANTE` → 403 por RBAC.

---

## 4. Contrato de frontend

- `core/models/admin-indicators.model.ts`: espejo de los DTO (porcentajes `number | null`; fechas `string`).
- `core/services/admin-indicators.service.ts` (Signals `kpis`, `comparativa`, `tendencia`, `cargando`): `consultarKpis(idPeriodo?)`, `consultarComparativa(idPeriodo?)`, `consultarTendencia(granularidad?, idPeriodo?)`; propaga el error sin tocar el estado.
- `features/admin/admin-indicators/admin-indicators.component.ts` (`/app/admin/indicadores`, roles `['ADMINISTRADOR','ADMIN']`, `OnPush`): tarjetas de KPI y de censo (la tarjeta destaca «con matrícula/grupo activo» y muestra «registrados» como dato secundario); tarjetas comparativas por facultad; gráfica de línea SVG de la tendencia con selector `SEMANA`/`MES`. `null` → «Sin datos», nunca `0%`. Errores con `getApiErrorMessage`/`ToastService`.
- **SSE (D7):** el componente se suscribe al evento `ASISTENCIAS_SESION_ACTUALIZADAS` del `RealtimeService` existente y, al recibirlo, vuelve a consultar KPIs y tendencia; se desuscribe al destruirse. Sin `setInterval`.
- `overview-admin`: la tarjeta «Programas Académicos» muestra `totalProgramas` del backend o «Sin datos» (entrega el dato el padre `overview.component.ts`).
- Prohibido: cálculos de porcentajes, ratios, buckets o semáforos en el cliente; `setInterval`; datos estáticos.

---

## 5. Compatibilidad y trazabilidad

- Aditivo: no cambia ningún contrato congelado ni objeto de las olas 1 y 2.
- `HTTP_AS_IS_MATRIX.md`: 3 filas nuevas (al cierre, commit aparte).
- Mapeo HU → objeto: HU133 y HU138 → `usp_consultar_kpis_institucion` (ausentismo/asistencia y ratio); HU135/HU136 → censo en `usp_consultar_kpis_institucion`; HU139 → `usp_consultar_comparativa_facultades`; HU134 → `usp_consultar_tendencia_ausentismo`.
- **No contratadas (bloqueadas por datos):** HU140, HU141, HU129 (PLAN §8).

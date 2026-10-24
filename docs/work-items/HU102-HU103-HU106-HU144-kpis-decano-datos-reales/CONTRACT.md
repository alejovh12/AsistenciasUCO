---
status: frozen-approved
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# CONTRACT — HU102 / HU103 / HU106 / HU144: KPIs de facultad del Decano con datos reales

> **Work Item:** `HU102-HU103-HU106-HU144-kpis-decano-datos-reales`
> **Estado:** `FROZEN-APPROVED`: aprobado por el owner con las recomendaciones D1–D10 del [PLAN](PLAN.md).
> Hereda las definiciones de la [ola 1](../HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales/CONTRACT.md) (falta, sesión ejecutada, clasificación, semáforo `VERDE/AMBAR/ROJO`, nulos).

---

## 1. Definiciones normativas (SSOT de los cálculos)

Se implementan **una sola vez, en DB**, sobre `uv_resumen_asistencia_estudiante_grupo` (sin modificarla). Backend y frontend no recalculan.

| Término | Definición |
|---|---|
| **Ámbito** | Programas de la facultad cuyo decano es el ejecutor (`Facultad.decano → Decano.usuario`). |
| **Matrícula activa** | Fila de la vista de resumen con `codigoEstadoEstudiante = 'A'` en un grupo de un programa de la ámbito y del período resuelto. |
| **Estudiante (persona)** | Cuenta una vez, con la **peor** clasificación de sus matrículas activas en la facultad (`PERDIDA` > `RIESGO` > `REGULAR`) (D3). |
| **Tasa de retención** (HU106) | `(estudiantesRegulares + estudiantesRiesgo) / totalEstudiantes × 100`, 2 decimales; `null` si `totalEstudiantes = 0` (D2). |
| **% de ausentismo** (HU102/HU103) | `Σ faltasSinJustaCausa / Σ sesionesEjecutadas × 100` sobre las matrículas activas, 2 decimales; `null` si `Σ sesionesEjecutadas = 0` (D1). |
| **% de asistencia** | `100 − porcentajeAusentismo` calculado **en DB**; `null` si el ausentismo es `null` (D10). |
| **Grupo activo** (HU144) | Grupo del período con `grupoEstaHablitado = 1` (D4). |
| **Cupo máximo** | `uv_estadistica_grupo.cupoMaximoConfigurado`. |
| **Matriculados activos** | `uv_estadistica_grupo.cantidadActivos` (estado `A`). |
| **Cupos disponibles** | `cupoMaximo − matriculadosActivos` (puede ser negativo si el dato lo es; no se recorta). |
| **Tasa de aforo** | `matriculadosActivos / cupoMaximo × 100`, 2 decimales; `null` si `cupoMaximo` es nulo o 0 (D4). En el KPI: `Σ matriculadosActivos / Σ cupoMaximo` de los grupos considerados. |

Todo valor que no pueda calcularse se devuelve `NULL`/ausente. **Nunca** un valor por defecto «creíble».

---

## 2. Contrato de base de datos

Solo lectura (sin `XACT_ABORT` ni transacción). Errores con `THROW 51000` y `DBCODE=<código>|<mensaje>`; mensajes vía `usp_obtener_mensaje_catalogo`.

### 2.1 Internos

| Objeto | Parámetros | Resultado | Errores |
|---|---|---|---|
| `usp_resolver_facultad_decano_interno` | `@idUsuarioEjecutor`, `@idCorrelacion`, `@validarUmbrales BIT = 1`, `@idFacultad OUTPUT` | Facultad del decano | `VAL_001` (ejecutor nulo), `KPI_003` (no es decano de una facultad), `KPI_002` (umbral ausente) |
| `usp_resolver_periodo_kpi_facultad_interno` | `@idFacultad`, `@idPeriodoSolicitado = NULL`, `@idPeriodo OUTPUT` | Período pedido, o el más reciente con grupos habilitados de la facultad; `NULL` si no hay | `PER_001` (período inexistente) |
| `usp_validar_programa_en_facultad_interno` | `@idFacultad`, `@idPrograma` | — | `GEN_001` «Programa» (no existe o es de otra facultad) |

### 2.2 Públicos

**`usp_consultar_kpis_decano(@idUsuarioEjecutor, @idCorrelacion, @idPeriodoAcademico = NULL)`** — una fila:

`idFacultad, nombreFacultad, idPeriodoAcademico, nombrePeriodoAcademico, totalProgramas, totalEstudiantes, estudiantesRegulares, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo, porcentajeAsistencia, gruposActivos, cupoTotal, matriculadosActivos, tasaAforo, umbralAdvertencia, umbralPerdida`.

Invariante: `estudiantesRegulares + estudiantesRiesgo + estudiantesPerdida = totalEstudiantes`.

**`usp_consultar_comparativa_programas_facultad(@idUsuarioEjecutor, @idCorrelacion, @idPeriodoAcademico = NULL)`** — una fila por programa de la facultad (D5), orden `porcentajeAusentismo` desc (nulos al final), luego `nombrePrograma`:

`idPrograma, nombrePrograma, estaActivoPrograma, totalEstudiantes, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo`.

**`usp_consultar_aforo_grupos_facultad(@idUsuarioEjecutor, @idCorrelacion, @numeroPagina, @tamanoPagina, @idPeriodoAcademico = NULL, @idPrograma = NULL, @soloHabilitados BIT = 1)`** — paginado (`OFFSET/FETCH` + `COUNT(*) OVER() AS totalRegistros`), orden `tasaAforo` desc (nulos al final), `codigoGrupo`:

`idGrupo, codigoGrupo, idAsignatura, nombreAsignatura, idPrograma, nombrePrograma, cupoMaximo, matriculadosActivos, cuposDisponibles, tasaAforo, grupoEstaHabilitado, totalRegistros`.

### 2.3 Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `KPI_003` (nuevo, `19_mensajes_kpis_decano.sql`) | 403 | El ejecutor no es decano de una facultad |
| `KPI_002` (ola 1) | 500 | Falta un umbral en el catálogo |
| `PER_001` (ola 1) | 400 | Período inexistente |
| `PAG_001` (ola 1) | 400 | Página/tamaño fuera de rango |
| `GEN_001` | 404 | Programa inexistente o de otra facultad |
| `VAL_001` | 400 | Ejecutor nulo |

---

## 3. Contrato HTTP (todas bajo `/api/v1/decano`, rol `DECANO`; `ADMINISTRADOR` sin facultad → 403)

| Método y ruta | Parámetros | Respuesta 200 |
|---|---|---|
| `GET /indicadores` | `idPeriodoAcademico?` | `ApiDataResponse<KpisDecanoDTO>` |
| `GET /comparativa-programas` | `idPeriodoAcademico?` | `ApiDataResponse<List<ComparativaProgramaDTO>>` (sin paginar, D6) |
| `GET /aforo-grupos` | `pagina` (≥1), `tamano` (≤ máximo del catálogo), `idPeriodoAcademico?`, `idPrograma?`, `soloHabilitados?` (defecto `true`) | `ApiPageResponse<AforoGrupoDTO>` |

`KpisDecanoDTO`, `ComparativaProgramaDTO` y `AforoGrupoDTO` tienen exactamente los campos de §2.2 (camelCase; `BigDecimal` con 2 decimales para porcentajes; `null` donde §1 lo indica). Errores: `ApiErrorResponse` con `message` ya traducido por el backend (la UI lo muestra tal cual).

Sin `Authorization` → 401; `COORDINADOR`/`DOCENTE`/`ESTUDIANTE` → 403 por RBAC.

---

## 4. Contrato de frontend

- `core/models/dean-indicators.model.ts`: espejo de los DTO (porcentajes `number | null`).
- `core/services/dean-indicators.service.ts` (Signals `kpis`, `comparativa`, `aforo`, `totalAforo`, `cargando`): `consultarKpis(idPeriodo?)`, `consultarComparativa(idPeriodo?)`, `consultarAforo(filtro)`; propaga el error sin tocar el estado.
- `features/dean/dean-indicators/dean-indicators.component.ts` (`/app/decano/indicadores`, roles `['DECANO']`, `OnPush`): tarjetas de KPI; barras de comparación (ancho = valor recibido, sin recalcular); tabla de aforo con `PaginationComponent` y filtro por programa (opciones tomadas de la propia comparativa). `null` → «Sin datos», nunca `0%`. Errores con `getApiErrorMessage`/`ToastService`.
- `overview-dean`: la tarjeta «Asistencia Promedio» muestra `porcentajeAsistencia` del backend o «Sin datos»; se elimina el `94%` y su texto «Nivel óptimo institucional» (no es una afirmación derivada de datos).
- Prohibido: cálculos de porcentajes, semáforos o estados en el cliente; `setInterval`; datos estáticos.

---

## 5. Compatibilidad y trazabilidad

- Aditivo: no cambia ningún contrato congelado ni objeto de la ola 1.
- `HTTP_AS_IS_MATRIX.md`: 3 filas nuevas (al cierre, commit aparte).
- Mapeo HU → objeto: HU102 → `usp_consultar_kpis_decano` (+ comparativa); HU103 → `usp_consultar_comparativa_programas_facultad`; HU106 → `tasaRetencion` de `usp_consultar_kpis_decano`; HU144 → `usp_consultar_aforo_grupos_facultad` y los campos de aforo del KPI.

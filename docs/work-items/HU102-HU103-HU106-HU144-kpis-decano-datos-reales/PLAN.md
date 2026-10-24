---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# PLAN — KPIs de facultad con datos reales (Ola 2: Decano) — HU102 / HU103 / HU106 / HU144

Fase 01: Planificación y Definition of Ready (DoR), según `GUIA_METODICA_IMPLEMENTACION_HU.md`, `HOJA_DE_RUTA_JERARQUIA_ROLES.md`, `docs/governance/DEFINITION_OF_READY.md` y `AGENTS.md`. Continúa la [ola 1](../HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales/PLAN.md) (§8 de ese plan).

---

## 1. Identidad y objetivo

- **Work Item ID:** `HU102-HU103-HU106-HU144-kpis-decano-datos-reales`
- **Base Git:** rama `feature/HU065-kpis-coordinador-datos-reales` de cada repo (la ola 2 **reutiliza** objetos de la ola 1, que aún no está en `develop`). Si la ola 1 se integra antes, se rebasa sobre `develop`.
- **Actor:** Decano de facultad (`DECANO`). El `ADMINISTRADOR` pasa el filtro de rutas (`/api/v1/decano/**`) pero el caso de uso lo rechaza con 403 si no es decano de una facultad (mismo criterio que D9 de HU076 y que la ola 1).
- **Historias de usuario:**
  - **HU102** Tablero analítico de ausentismo de la facultad.
  - **HU103** Comparativa de inasistencias entre los programas de la facultad.
  - **HU106** Indicador macro de retención estudiantil de la facultad.
  - **HU144** Censo de grupos activos y tasa de aforo de la facultad.
- **Objetivo:** que toda cifra que ve el Decano salga de la DB (SSOT), con las **mismas definiciones** de la ola 1 (falta = `SJC`, sesión ejecutada, umbrales del catálogo) y el ámbito (facultad del decano) forzado dentro de los SP. **Cero valores simulados**. Elimina el `94%` quemado de la tarjeta «Asistencia Promedio» de `overview-dean`.

### Criterios de aceptación

| ID | Criterio |
|---|---|
| CA-01 | `GET /api/v1/decano/indicadores` devuelve, para la facultad del decano y el período vigente: programas, estudiantes con matrícula activa, conteo Regular/Riesgo/Pérdida, **tasa de retención** (HU106), **% de ausentismo** (HU102), sesiones ejecutadas, faltas, grupos activos, cupo total, matriculados activos y **tasa de aforo** (HU144). Todo calculado en DB. |
| CA-02 | `GET /api/v1/decano/comparativa-programas` devuelve una fila por programa de la facultad con sus estudiantes, riesgo, pérdida, retención y % de ausentismo (HU103), ordenada por ausentismo descendente (los `null` al final). |
| CA-03 | `GET /api/v1/decano/aforo-grupos` lista paginada (`pagina`, `tamano`) de grupos de la facultad con cupo máximo, matriculados activos, cupos disponibles y tasa de aforo; filtra por `idPrograma` y `soloHabilitados` (HU144). |
| CA-04 | Las definiciones de falta, sesión ejecutada, clasificación y umbrales son **las de la ola 1** (misma vista y misma función); no se redefinen. |
| CA-05 | Un `idPrograma` que no pertenece a la facultad del decano responde **404** (`GEN_001` «Programa»): no se revela que existe. |
| CA-06 | Un usuario que no es decano de ninguna facultad no obtiene datos: 403 (`KPI_003`) o 0 filas fail-closed; nunca datos de otra facultad. |
| CA-07 | Lo que no puede calcularse (divisor cero, sin período vigente) se devuelve `null`; nunca `0` ni un valor «creíble». |
| CA-08 | La UI no calcula ni estima: muestra lo que entrega el backend (tarjetas, barras de comparación y aforo), con estados vacío/carga/error y mensajes del servidor. Sin `setInterval`, sin datos estáticos, sin el `94%`. |
| CA-09 | Las cifras de la API coinciden con una consulta de control directa sobre tablas (conciliación en el E2E). |

---

## 2. AS-IS verificado (rama de la ola 1, 2026-10-24)

| Capa | Hallazgo |
|---|---|
| DB | Existen `uv_resumen_asistencia_estudiante_grupo` (una fila por matrícula con faltas, sesiones y `codigoPermanencia`), `ufn_clasificar_permanencia`, `uv_cumplimiento_sesion_grupo`, `uv_estadistica_grupo` (cupo máximo y matriculados por estado), `uv_grupo_programa` (grupo→programa y `grupoEstaHablitado`), `uv_programa` (con `idFacultad`) y `uv_facultad` (con `idDecano`). `Facultad.decano → Decano.usuario` identifica al decano. |
| DB | **No existe** SP que resuelva «la facultad del decano» (la ola 1 resuelve el programa del coordinador con `usp_resolver_programa_coordinador_interno`) ni SP agregado por facultad. |
| Backend | `DecanoPortalController` (`/api/v1/decano`) sirve coordinadores; no hay endpoint de indicadores. La ola 1 dejó `DbCodeTranslator`, `JpaQueryExecutor`, el patrón `Sp*Entity` y `IndicadoresRules` reutilizables. |
| Frontend | `overview-dean.component.ts` muestra **`94%` quemado** en «Asistencia Promedio» (único dato simulado de esa vista). No existe pantalla de indicadores del Decano. |
| Datos locales | Facultad de Ingeniería: 1 programa y 2 grupos; «Test Facultad»: 0 programas. Las pruebas DB usan fixture propio (≥ 2 facultades y ≥ 2 programas) para probar el aislamiento y la comparativa. |

### Hallazgo en la ola 1 (corregido hoy, fuera de esta fase)

El filtro de semáforo del frontend enviaba `AMARILLO` y el contrato/SP define `AMBAR` (el backend respondía 400). El E2E de la ola 1 no probaba ese filtro. Corregido en `cf20635` con un spec que fija las opciones. **Lección aplicada a esta ola:** el E2E probará cada valor de cada filtro.

---

## 3. TARGET

### 3.1 Base de datos (SQL Server)

- **Reutiliza sin modificar:** `uv_resumen_asistencia_estudiante_grupo`, `ufn_clasificar_permanencia`, `uv_cumplimiento_sesion_grupo`, `uv_estadistica_grupo`, `uv_grupo_programa`, `uv_programa`, `uv_facultad`.
- **Internos nuevos (`usp_*_interno`):** `usp_resolver_facultad_decano_interno` (valida ejecutor y correlación, resuelve la facultad; `KPI_003` si no es decano) · `usp_resolver_periodo_kpi_facultad_interno` (período solicitado o vigente de la facultad) · `usp_validar_programa_en_facultad_interno` (`GEN_001` si el programa no es de la facultad).
- **SP públicos de consulta (solo lectura, sin `XACT_ABORT`):** `usp_consultar_kpis_decano` · `usp_consultar_comparativa_programas_facultad` · `usp_consultar_aforo_grupos_facultad` (con `OFFSET/FETCH` y `COUNT(*) OVER()`).
- **Mensajes:** `schema/seed/19_mensajes_kpis_decano.sql` (`KPI_003`; no se toca `04_mensaje.sql`).
- **Umbrales y tamaño máximo de página:** del catálogo de parámetros (`ASISTENCIA/*`, `PAGINACION/TAMANO_MAXIMO`); nada quemado.

### 3.2 Backend (Spring Boot)

Features nuevas bajo `application/features/decano/`: `consultarkpisdecano`, `consultarcomparativaprogramas`, `consultaraforogrupos` (estructura `primaryports` / `usecase` igual que la ola 1). Puerto `DecanoIndicadoresQueryPort` + proyecciones, adaptador JPA nativo con entidades `Sp*` nuevas (el `DbCodeTranslator` ya existente se reutiliza y recibe `KPI_003`). Controller **nuevo** `DecanoIndicadoresController` (`/api/v1/decano/**`) y `DecanoIndicadoresWiringConfiguration` nueva: no se toca `DecanoPortalController`, `DecanoWiringConfiguration` ni `SecurityConfig` (verificar que la regla actual de `/api/v1/decano/**` ya admite `DECANO`).

### 3.3 Frontend (Angular)

Página `DeanIndicatorsComponent` (`/app/decano/indicadores`, entrada de menú «Indicadores» para `DECANO`): tarjetas de KPI, **comparativa de programas con barras HTML accesibles** (sin dependencia nueva de gráficos) y tabla paginada de aforo con filtro por programa. Servicio `DeanIndicatorsService` (Signals). `overview-dean`: la tarjeta «Asistencia Promedio» pasa a mostrar el `porcentajeAusentismo`-derivado que entrega el backend (o «Sin datos»); es el único cambio permitido en ese archivo.

---

## 4. Alcance y no alcance

**Alcance:** HU102, HU103, HU106, HU144 con las rutas y pantallas de §3.

**No alcance:** ola 3 (Administrador); HU104/HU105 (directorio y búsqueda de estudiantes de facultad: HU propias, sin dependencia de agregados); HU109 organigrama; cohortes entre períodos (ver D2); exportaciones; notificaciones; realtime (D9 de la ola 1); cambiar las vistas o funciones de la ola 1.

---

## 5. Clase de cambio y perímetro de rutas

- **Change class:** `BEHAVIOR_CHANGE` (funcionalidad nueva) + `CONTRACT_CHANGE` aditivo en DB y HTTP.
- **PRIMARY_VARIABLE:** definición única de «ausentismo de facultad», «retención» y «aforo» calculadas en DB.
- **ALLOWED_PATHS:**
  - `GestioAsistenciaBackend/docs/work-items/HU102-HU103-HU106-HU144-kpis-decano-datos-reales/**`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_{resolver_facultad_decano,resolver_periodo_kpi_facultad,validar_programa_en_facultad}_interno.sql`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_{kpis_decano,comparativa_programas_facultad,aforo_grupos_facultad}.sql`
  - `GestioAsistenciaDB/schema/seed/19_mensajes_kpis_decano.sql`; `GestioAsistenciaDB/test/test_usp_consultar_*_decano*.sql` (nuevos)
  - Backend: `application/features/decano/{consultarkpisdecano,consultarcomparativaprogramas,consultaraforogrupos}/**`, puertos/proyecciones/adaptador/entidades nuevos, `DecanoIndicadoresController`, `DecanoIndicadoresWiringConfiguration` y sus tests.
  - Frontend: servicio, modelos y componentes nuevos; ruta y menú; solo la tarjeta «Asistencia Promedio» en `overview-dean.component.ts`.
- **ARCHIVOS CALIENTES (solo al final, commit aparte):** `DbFailureClassifier` (si hiciera falta), `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`, `JpaRepositoryBeanUniquenessTest` (inventario), `RbacSecurityFilterChainTest`, `HU-CATALOG.md`.
- **FORBIDDEN_PATHS:** contratos WORM, vistas congeladas (`uv_sesion`, `uv_asistencia`, `uv_detalle_asistencia`), `uv_resumen_asistencia_estudiante_grupo` y `ufn_clasificar_permanencia` (se consumen), `SecurityConfig`, `DecanoPortalController`, `dean-faculty`, `dean-infraestructura`.
- **SECURITY_IMPACT:** ámbito por facultad en DB; solo agregados y nombres de programa/asignatura (sin PII de estudiantes); sin secretos.
- **OBSERVABILITY_IMPACT:** `CorrelationIdContext.get()` → `@idCorrelacion`; logs sin PII.
- **ROLLBACK:** revertir PR de Front → Back → DB; todo es aditivo.

---

## 6. Decisiones requeridas (bloquean READY)

| # | Decisión | Opciones | Recomendación |
|---|---|---|---|
| **D1** | ¿Cómo se calcula el **% de ausentismo** de la facultad/programa (HU102/HU103)? | (a) **Ponderado**: `Σ faltasSJC / Σ sesionesEjecutadas` sobre las matrículas activas. (b) Promedio simple de los % por estudiante. | **(a)**: no sobrepondera a quien tiene pocas sesiones y es consistente con la regla del 20 % (que es por matrícula). `null` si `Σ sesionesEjecutadas = 0`. |
| **D2** | ¿Qué es **retención** (HU106)? | (a) `(Regular + Riesgo) / total` de estudiantes con matrícula activa en el período (misma «tasa de permanencia» de la ola 1, a nivel facultad). (b) Cohorte entre períodos (quién sigue matriculado respecto al período anterior). | **(a)**. (b) exige histórico de cohortes que el modelo no garantiza; se etiqueta en UI «Retención por asistencia (período vigente)» para no sobre-prometer. (b) queda como HU futura si el owner la pide. |
| **D3** | Unidad de conteo de estudiantes | (a) Persona, con la **peor** clasificación de sus matrículas activas de la facultad (D6 de la ola 1). (b) Por matrícula. | **(a)** para conteos de estudiantes y retención; el ausentismo (D1) pondera por matrícula. |
| **D4** | ¿Qué es «grupo activo» y cómo se calcula el aforo (HU144)? | (a) Grupo con `grupoEstaHablitado = 1` en el período; aforo = `matriculadosActivos(A) / cupoMaximo`. (b) Contar también grupos de períodos pasados. | **(a)**; `soloHabilitados` (defecto `true`) permite ver todos. `tasaAforo = null` si `cupoMaximo` es 0/nulo; puede superar 100 % solo si el dato lo exige (no se recorta). |
| **D5** | ¿Qué programas entran en la comparativa (HU103)? | (a) Todos los de la facultad con su estado `estaActivoPrograma`. (b) Solo activos. | **(a)**, con el indicador de estado; los programas sin datos salen con `null`, no se ocultan. |
| **D6** | Paginación | (a) Comparativa **sin paginar** (acotada por programas de la facultad); aforo **paginado**. (b) Paginar todo. | **(a)**: la comparativa es un conjunto pequeño y completo para graficar; el aforo puede crecer (grupos). |
| **D7** | Período | (a) `idPeriodoAcademico` opcional; defecto = período con grupos habilitados de la facultad (más reciente). (b) Solo vigente. | **(a)** (consistente con D8 de la ola 1). Un período inexistente → 400 `PER_001`. |
| **D8** | Rutas y RBAC | Rutas nuevas bajo `/api/v1/decano/**`; sin tocar `SecurityConfig`. | Verificar con `RbacSecurityFilterChainTest` que `DECANO` y `ADMINISTRADOR` pasan el filtro y que `COORDINADOR`/`DOCENTE`/`ESTUDIANTE` reciben 403. Si la regla actual no lo permite → `BLOCKED_BY_MISSING_EVIDENCE` y se pide aprobación para el archivo caliente. |
| **D9** | Gráfica de comparación (HU103) | (a) Barras HTML/CSS accesibles, sin dependencia nueva. (b) Librería de gráficos. | **(a)**: sin dependencia nueva; el cliente solo escala anchos a partir de los valores recibidos (presentación, sin recalcular). |
| **D10** | `overview-dean`: tarjeta «Asistencia Promedio» | (a) Mostrar `100 − porcentajeAusentismo` calculado en el cliente. (b) Que el backend entregue el valor a mostrar. | **(b)**: el DTO incluye `porcentajeAsistencia` (= `100 − ausentismo`, calculado en DB) para que el cliente no calcule. Se refleja en el CONTRACT. |

---

## 7. Riesgos y dependencias

| Riesgo | Mitigación |
|---|---|
| La ola 2 depende de objetos de la ola 1 aún no integrados | Rama base = rama de la ola 1; rebase sobre `develop` cuando se integre; el PR de la ola 2 se abre después del de la ola 1. |
| Datos locales insuficientes (1 programa) | Fixture propio en los tests DB (2 facultades, 2 programas, grupos con cupos distintos); el E2E concilia contra consulta de control. |
| Ambigüedad de «retención» | D2 explícita + etiqueta honesta en UI. |
| Filtros con valores no probados (defecto de `AMBAR` en la ola 1) | El E2E recorre cada valor de cada filtro (`idPrograma`, `soloHabilitados` true/false, período válido/ inválido). |
| Aforo > 100 % por datos | No se recorta; se muestra tal cual. |
| Agregado sobre muchas matrículas | Agregación en DB con índices existentes; el tamaño máximo de página viene del catálogo. |

**Dependencias:** ola 1 (objetos reutilizados), HU076-078 (programa con facultad), HU110/HU111 (facultad con decano). No depende de IAM-001.

---

## 8. Hoja de ruta posterior

Ola 3 (Administrador) según §8 del plan de la ola 1: HU133, HU138, HU139, HU135/HU136 y HU134 derivables con ámbito institución; HU140/HU141/HU129 siguen `BLOCKED_BY_MISSING_DATA`.

---

## 9. Secuencia de ejecución (fases 02–06)

| Fase | Repo | Commits atómicos (Conventional Commits, `Refs: HU102…`) |
|---|---|---|
| 02 CONTRACT | Backend (docs) | `docs(hus): congelar contrato de kpis del decano` |
| 03 RED | DB → Backend → Frontend | `test(db/sp): …`, `test(backend/usecase): …`, `test(frontend/decano): …` |
| 04 GREEN | DB → Backend → Frontend | `feat(db/sp): …`, `feat(backend/…): …`, `feat(frontend/decano): …` |
| 05 AUDIT | los 3 | `VALIDATION.md` (E2E con JWT real, conciliación, cobertura ≥ 80 % del código nuevo, medida también en frontend) |
| 06 CLOSURE | Backend + Infra | archivos calientes en commit aparte; `HU-CATALOG.md` |

Un PR por repo, aprobado por el owner. Sin push ni PR sin orden expresa.

---

## 10. Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md). Orden RED → GREEN → VALIDATE por repo, DB → Backend → Frontend.

## 11. Rollback y stop conditions

- **Rollback:** §5.
- **TEST_CONTRACT_CONFLICT:** si una prueba existente falla por el cambio; se documenta, nunca se relaja.
- **BLOCKED_BY_MISSING_EVIDENCE:** D8 si la regla de seguridad actual no admite `DECANO`.
- **STOP:** D1–D10 sin aprobar; cualquier valor simulado detectado (CA-08/CA-09).

## 12. Definition of Ready (DoR)

| Criterio | Estado | Evidencia / pendiente |
|---|---|---|
| 1. Objetivo y alcance delimitados | ✅ | §1, §4 |
| 2. AS-IS con evidencia | ✅ | §2 |
| 3. Contratos identificados | ✅ (borrador) | [CONTRACT.md](CONTRACT.md) |
| 4. Matriz TDD inicial | ✅ | [TEST_PLAN.md](TEST_PLAN.md) |
| 5. Sin conflicto con contratos WORM | ✅ | Solo objetos y rutas nuevos |
| 6. Decisiones del owner | ❌ | D1–D10 (recomendaciones en §6) |
| 7. Archivos calientes | ✅ (con regla) | §5 |
| 8. Rollback viable | ✅ | §5 |

**Dictamen DoR:** `NOT_READY`: pendiente solo de la aprobación del owner sobre D1–D10.

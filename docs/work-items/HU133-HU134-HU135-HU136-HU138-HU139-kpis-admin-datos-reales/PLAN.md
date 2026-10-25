---
status: frozen-approved
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# PLAN — KPIs institucionales con datos reales (Ola 3: Administrador) — HU133 / HU134 / HU135 / HU136 / HU138 / HU139

Fase 01: Planificación y Definition of Ready (DoR), según `GUIA_METODICA_IMPLEMENTACION_HU.md`, `HOJA_DE_RUTA_JERARQUIA_ROLES.md`, `docs/governance/DEFINITION_OF_READY.md` y `AGENTS.md`. Continúa la [ola 1](../HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales/PLAN.md) (Coordinador) y la [ola 2](../HU102-HU103-HU106-HU144-kpis-decano-datos-reales/PLAN.md) (Decano).

---

## 1. Identidad y objetivo

- **Work Item ID:** `HU133-HU134-HU135-HU136-HU138-HU139-kpis-admin-datos-reales`
- **Base Git:** rama `feature/HU102-kpis-decano-datos-reales` de cada repo (la ola 3 **reutiliza** objetos de las olas 1 y 2, aún no integradas en `develop`). Si se integran antes, se rebasa sobre `develop`.
- **Actor:** Administrador institucional (`ADMINISTRADOR`). Su ámbito sale de `Administrador.institucion`: ve **solo su institución**. Un usuario sin fila en `Administrador` recibe 403.
- **Historias de usuario (derivables de datos existentes):**
  - **HU133** Indicador KPI global de asistencia en la universidad.
  - **HU138** Ratio de sesiones dictadas con asistencia registrada.
  - **HU139** Tarjetas comparativas de inasistencia por facultad.
  - **HU134** Tendencias temporales de ausentismo.
  - **HU135** Contador de censo estudiantil.
  - **HU136** Contador de censo docente.
- **Bloqueadas por falta de datos (no se implementan; ver §8):** HU140 (censo por sedes), HU141 (censo por escalafón), HU129 (horas contratadas).
- **Objetivo:** que toda cifra que ve el Administrador salga de la DB (SSOT) con **las mismas definiciones** de las olas 1 y 2 (falta = `SJC`, sesión ejecutada, umbrales del catálogo) y el ámbito (institución del administrador) forzado dentro de los SP. **Cero valores simulados**. Corrige además un dato inventado en `overview-admin`: la tarjeta «Programas Académicos» muestra `decanos().length > 0 ? 1 : 0`.

### Criterios de aceptación

| ID | Criterio |
|---|---|
| CA-01 | `GET /api/v1/admin/indicadores` devuelve, para la institución del administrador y el período vigente: **asistencia y ausentismo globales** (HU133), **ratio de sesiones con asistencia registrada** (HU138) con sus conteos, y el **censo** de estudiantes y docentes (HU135/HU136). Todo calculado en DB. |
| CA-02 | `GET /api/v1/admin/comparativa-facultades` devuelve una fila por facultad de la institución con estudiantes, programas, retención y ausentismo (HU139), ordenada por ausentismo descendente (nulos al final). |
| CA-03 | `GET /api/v1/admin/tendencia-ausentismo` devuelve la serie temporal de ausentismo del período (HU134) por semana o por mes, con solo los intervalos que tienen datos. |
| CA-04 | Las definiciones de falta, sesión ejecutada, retención y ausentismo son **las de las olas 1 y 2**; no se redefinen. |
| CA-05 | Un administrador sin institución, o un rol distinto, no obtiene datos: 403 (`KPI_004`) o 0 filas fail-closed; nunca datos de otra institución. |
| CA-06 | Lo que no puede calcularse (divisor cero, sin período vigente) se devuelve `null`; nunca `0` ni un valor «creíble». |
| CA-07 | La UI no calcula ni estima: muestra lo que entrega el backend (tarjetas, comparativa, gráfica de línea), con estados vacío/carga/error y mensajes del servidor. Sin `setInterval`, sin datos estáticos, sin la expresión `decanos().length > 0 ? 1 : 0`. |
| CA-08 | Los indicadores que dependen de la asistencia se **actualizan por SSE** cuando llega el evento de asistencias de sesión actualizadas ya existente; no hay polling (D7). |
| CA-09 | Las cifras de la API coinciden con una consulta de control directa sobre tablas (conciliación en el E2E). |

---

## 2. AS-IS verificado (rama de la ola 2, 2026-10-24)

| Capa | Hallazgo |
|---|---|
| DB | Reutilizables sin cambios: `uv_resumen_asistencia_estudiante_grupo` (faltas y sesiones por matrícula), `ufn_clasificar_permanencia`, `uv_cumplimiento_sesion_grupo` (sesiones vencidas con/sin registro), `uv_estadistica_grupo`, `uv_grupo_programa`, `uv_programa`, `uv_facultad`, `uv_periodo_academico`. `Administrador(usuario, institucion)` identifica el ámbito. `Sesion` tiene `fechaHoraInicio` (base de la serie temporal) y `Asistencia`/`DetalleAsistencia` el detalle por estudiante. |
| DB | **No existe** SP que resuelva «la institución del administrador» ni SP agregado por institución. **No existen** tablas de sedes, escalafón docente ni horas contratadas (`Docente` solo tiene `usuario`). `TipoVinculacion` es el tipo de contrato del docente en un programa, **no** un escalafón. |
| Backend | `AdminPortalController` sirve la administración de decanos/facultades/parámetros. Las rutas `/api/v1/admin/sedes*` son **stubs que responden 501** (`unavailable`). `SecurityConfig` ya protege `/api/v1/admin/**` con `ROLE_ADMINISTRADOR`. Existe publicación SSE (`RealtimePublisherPort`) del evento de asistencias de sesión actualizadas; **no** hay eventos de alta de estudiante/docente ni de matrícula. |
| Frontend | `overview-admin.component.ts` no calcula KPI de asistencia; su tarjeta «Programas Académicos» muestra `decanos().length > 0 ? 1 : 0` (valor inventado). `admin-sedes` consume endpoints 501. |
| Datos locales | Una institución, una facultad con datos, 7 estudiantes y un puñado de sesiones: las pruebas DB usan fixture propio (≥ 2 instituciones, ≥ 2 facultades) para probar aislamiento y comparativa. |

---

## 3. TARGET

### 3.1 Base de datos (SQL Server)

- **Reutiliza sin modificar** las vistas y la función de §2.
- **Internos nuevos (`usp_*_interno`):** `usp_resolver_institucion_administrador_interno` (valida ejecutor y correlación, resuelve la institución; `KPI_004` si no es administrador de una institución) · `usp_resolver_periodo_kpi_institucion_interno` (período solicitado o el más reciente con grupos habilitados en la institución).
- **SP públicos de consulta (solo lectura, sin `XACT_ABORT`):** `usp_consultar_kpis_institucion` · `usp_consultar_comparativa_facultades` · `usp_consultar_tendencia_ausentismo`.
- **Mensajes:** `schema/seed/20_mensajes_kpis_admin.sql` (`KPI_004`; no se toca `04_mensaje.sql`).
- **Umbrales y parámetros:** del catálogo; nada quemado.

### 3.2 Backend (Spring Boot)

Features nuevas bajo `application/features/admin/`: `consultarkpisinstitucion`, `consultarcomparativafacultades`, `consultartendenciaausentismo` (estructura `primaryports` / `usecase` como las olas 1 y 2). Puerto `AdminIndicadoresQueryPort` + proyecciones, adaptador JPA nativo con entidades `Sp*` nuevas (reutiliza `DbCodeTranslator`, que recibe `KPI_004`). Controller **nuevo** `AdminIndicadoresController` (`/api/v1/admin`) y `AdminIndicadoresWiringConfiguration` nueva: no se tocan `AdminPortalController`, `SecurityConfig` ni wirings existentes.

### 3.3 Frontend (Angular)

Página `AdminIndicatorsComponent` (`/app/admin/indicadores`, entrada de menú «Indicadores» para `ADMINISTRADOR`/`ADMIN`): tarjetas de KPI y censo, tarjetas comparativas por facultad, gráfica de línea de la tendencia (SVG propio, sin librería nueva) con selector de granularidad. Servicio `AdminIndicatorsService` (Signals). La página se **suscribe al evento SSE existente** para recargar los indicadores de asistencia (D7). `overview-admin`: la tarjeta «Programas Académicos» pasa a mostrar `totalProgramas` entregado por el backend (o «Sin datos») y se elimina la expresión inventada.

---

## 4. Alcance y no alcance

**Alcance:** HU133, HU134, HU135, HU136, HU138, HU139 con las rutas y pantallas de §3.

**No alcance:** HU140, HU141, HU129 (`BLOCKED_BY_MISSING_DATA`, §8); implementar sedes (`/admin/sedes` sigue 501); eventos SSE de alta de estudiante/docente o de matrícula (follow-up, D7); exportaciones; comparativas entre instituciones; cambiar vistas o funciones de las olas 1 y 2.

---

## 5. Clase de cambio y perímetro de rutas

- **Change class:** `BEHAVIOR_CHANGE` (funcionalidad nueva) + `CONTRACT_CHANGE` aditivo en DB y HTTP.
- **PRIMARY_VARIABLE:** definición única de «asistencia global», «ratio de cumplimiento», «censo» y «bucket temporal» calculadas en DB.
- **ALLOWED_PATHS:**
  - `GestioAsistenciaBackend/docs/work-items/HU133-HU134-HU135-HU136-HU138-HU139-kpis-admin-datos-reales/**`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_{resolver_institucion_administrador,resolver_periodo_kpi_institucion}_interno.sql`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_{kpis_institucion,comparativa_facultades,tendencia_ausentismo}.sql`
  - `GestioAsistenciaDB/schema/seed/20_mensajes_kpis_admin.sql`; `GestioAsistenciaDB/test/test_usp_*_institucion*.sql`, `test_usp_consultar_*_facultades*.sql`, `test_usp_consultar_tendencia_ausentismo.sql` (nuevos)
  - Backend: `application/features/admin/{consultarkpisinstitucion,consultarcomparativafacultades,consultartendenciaausentismo}/**`, puertos/proyecciones/adaptador/entidades nuevos, `AdminIndicadoresController`, `AdminIndicadoresWiringConfiguration` y sus tests.
  - Frontend: servicio, modelos y componentes nuevos; ruta y menú; en `overview-admin` solo la tarjeta «Programas Académicos» (y la entrada del dato en el padre `overview.component.ts`).
- **ARCHIVOS CALIENTES (solo al final, commit aparte):** `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`, `JpaRepositoryBeanUniquenessTest` (inventario), `RbacSecurityFilterChainTest`, `HU-CATALOG.md`.
- **FORBIDDEN_PATHS:** contratos WORM, vistas congeladas (`uv_sesion`, `uv_asistencia`, `uv_detalle_asistencia`), las vistas/funciones de las olas 1 y 2 (se consumen), `SecurityConfig`, `AdminPortalController`, `admin-sedes`, `DbCodeTranslator` salvo añadir el código `KPI_004`.
- **SECURITY_IMPACT:** ámbito por institución en DB; solo agregados y nombres de facultad (sin PII de estudiantes ni docentes); sin secretos.
- **OBSERVABILITY_IMPACT:** `CorrelationIdContext.get()` → `@idCorrelacion`; logs sin PII.
- **ROLLBACK:** revertir PR de Front → Back → DB; todo es aditivo.

---

## 6. Decisiones requeridas (bloquean READY)

| # | Decisión | Opciones | Recomendación |
|---|---|---|---|
| **D1** | Ámbito del administrador | (a) Su institución (`Administrador.institucion`). (b) Todo el sistema. | **(a)**: es lo que modela la DB; sin fila en `Administrador` → 403 `KPI_004`. |
| **D2** | HU133: ¿qué es «asistencia global»? | (a) Ponderada: `100 − Σ faltasSJC / Σ sesionesEjecutadas` sobre matrículas activas del período (igual que D1 de la ola 2). (b) Promedio de porcentajes por facultad. | **(a)**, con `porcentajeAusentismo` y `porcentajeAsistencia` (calculado en DB). `null` si no hay sesiones ejecutadas. |
| **D3** | HU138: ¿qué es el «ratio»? | (a) `sesionesEjecutadas / sesionesVencidas × 100` (igual que `porcentajeCumplimiento` de la ola 1, a nivel institución), entregando también ambos conteos. (b) Contar solo sesiones con ≥ 1 estudiante presente. | **(a)**: «con asistencia registrada» = existe ≥ 1 registro en `uv_asistencia`. `null` si no hay sesiones vencidas. |
| **D4** | HU139: ¿qué facultades entran? | (a) Todas las de la institución con su estado (activa/inactiva), sin datos → `null`. (b) Solo activas. | **(a)** (igual que D5 de la ola 2). Orden por ausentismo descendente, nulos al final. |
| **D5** | HU134: granularidad y forma de la serie | (a) `granularidad` = `SEMANA` (defecto, semana calendario iniciada en lunes) o `MES`, sobre `Sesion.fechaHoraInicio`; solo intervalos con datos; ausentismo ponderado por intervalo. (b) Semana académica (`numeroSemana`). (c) Solo mensual. | **(a)**: el calendario real es independiente de cómo se numeren las sesiones. No se rellenan intervalos vacíos con 0. |
| **D6** | HU135/HU136: definición del censo | (a) Estudiantes: personas con ≥ 1 matrícula activa en un grupo del período (titular del KPI) **y** total de estudiantes registrados activos. Docentes: con titularidad de ≥ 1 grupo habilitado **y** total de docentes registrados activos. (b) Solo registrados. | **(a)**: devuelve los dos conteos; la tarjeta destaca «con matrícula/grupo activo» y muestra «registrados» como dato secundario. |
| **D7** | «Tiempo real» (HU135/HU136 y KPIs de asistencia) sin polling | (a) KPIs de asistencia (HU133/HU138/HU134) se recargan al recibir el evento SSE de asistencias actualizadas que ya existe; el censo se carga al entrar. El push real del censo requiere publicar eventos de alta/matrícula, que hoy no existen → follow-up. (b) Crear ya los eventos de alta y matrícula (amplía el alcance a casos de uso ajenos). (c) Solo carga al entrar. | **(a)**: respeta «sin polling», no toca casos de uso ajenos y declara con honestidad que el censo no se actualiza en vivo todavía. |
| **D8** | Rutas y RBAC | Rutas nuevas bajo `/api/v1/admin/**` (ya exige `ADMINISTRADOR`); sin tocar `SecurityConfig`. | Confirmado en el AS-IS. `RbacSecurityFilterChainTest` verificará 401/403 y 200 para `ADMINISTRADOR`. |
| **D9** | Gráfica de tendencia | (a) SVG propio sin dependencia nueva. (b) Librería de gráficos. | **(a)**: el cliente solo mapea los valores recibidos a coordenadas (presentación, sin recalcular). |
| **D10** | `overview-admin`: tarjeta «Programas Académicos» | (a) Que el backend entregue `totalProgramas` (de `usp_consultar_kpis_institucion`). (b) Quitar la tarjeta. | **(a)**; si la consulta falla o no hay dato → «Sin datos». Se elimina `decanos().length > 0 ? 1 : 0`. |

---

## 7. Riesgos y dependencias

| Riesgo | Mitigación |
|---|---|
| La ola 3 depende de las olas 1 y 2 (aún sin integrar) | Rama base = rama de la ola 2; el PR de la ola 3 se abre después de los anteriores. |
| Datos locales mínimos | Fixture propio en los tests DB (2 instituciones, 2 facultades, sesiones en varias semanas y meses); el E2E concilia contra consultas de control. |
| Ambigüedad del bucket temporal (zona horaria) | Las fechas de `Sesion` se agrupan con la zona institucional del catálogo (`uv_grupo` ya la usa); la prueba DB fija sesiones cerca de los bordes de semana/mes. |
| «Tiempo real» del censo no cumple literalmente | D7 explícita; follow-up para eventos de alta/matrícula. |
| Valores sin probar en filtros (defecto de `AMBAR` en la ola 1) | El E2E recorre cada valor de cada parámetro (`granularidad`, período válido/inexistente). |
| Agregado sobre toda la institución | Agregación en DB sobre las vistas existentes; índices existentes; sin paginación necesaria (conjuntos acotados: facultades e intervalos). |

**Dependencias:** olas 1 y 2; HU107 (facultades); HU142 (períodos). No depende de IAM-001.

---

## 8. Historias bloqueadas por datos inexistentes (sin simulación)

| HU | Qué falta | Condición para hacerla «real» |
|---|---|---|
| HU140 Censo por sedes | No hay tabla de sedes (`/admin/sedes` son stubs 501) ni relación estudiante/grupo→sede | Una HU propia de captura de sedes y asignación sede↔grupo/estudiante. |
| HU141 Censo por escalafón | `Docente` solo tiene `usuario`; `TipoVinculacion` es contrato, no escalafón | Una HU propia que modele y capture el escalafón docente. |
| HU129 Horas contratadas | No existe el dato | Una HU propia que modele las horas contratadas. |

Hasta entonces quedan `BLOCKED_BY_MISSING_DATA`: no se muestran gráficos ni conteos inventados para ellas.

---

## 9. Secuencia de ejecución (fases 02–06)

| Fase | Repo | Commits atómicos (Conventional Commits, `Refs: HU133…`) |
|---|---|---|
| 02 CONTRACT | Backend (docs) | `docs(hus): congelar contrato de kpis del administrador` |
| 03 RED | DB → Backend → Frontend | `test(db/sp): …`, `test(backend/usecase): …`, `test(frontend/admin): …` |
| 04 GREEN | DB → Backend → Frontend | `feat(db/sp): …`, `feat(backend/…): …`, `feat(frontend/admin): …` |
| 05 AUDIT | los 3 | `VALIDATION.md` (E2E con JWT real recorriendo cada valor de cada filtro, conciliación, cobertura ≥ 80 % medida en backend **y** frontend) |
| 06 CLOSURE | Backend + Infra | archivos calientes en commit aparte; `HU-CATALOG.md` (las tres HU bloqueadas se anotan como bloqueadas, no como pendientes) |

Un PR por repo, aprobado por el owner. Sin push ni PR sin orden expresa.

---

## 10. Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md). Orden RED → GREEN → VALIDATE por repo, DB → Backend → Frontend.

## 11. Rollback y stop conditions

- **Rollback:** §5.
- **TEST_CONTRACT_CONFLICT:** si una prueba existente falla por el cambio; se documenta, nunca se relaja.
- **STOP:** D1–D10 sin aprobar; cualquier valor simulado detectado (CA-07/CA-09); intento de implementar HU140/HU141/HU129 sin datos reales.

## 12. Definition of Ready (DoR)

| Criterio | Estado | Evidencia / pendiente |
|---|---|---|
| 1. Objetivo y alcance delimitados | ✅ | §1, §4, §8 |
| 2. AS-IS con evidencia | ✅ | §2 |
| 3. Contratos identificados | ✅ (borrador) | [CONTRACT.md](CONTRACT.md) |
| 4. Matriz TDD inicial | ✅ | [TEST_PLAN.md](TEST_PLAN.md) |
| 5. Sin conflicto con contratos WORM | ✅ | Solo objetos y rutas nuevos |
| 6. Decisiones del owner | ✅ | D1–D10 aprobadas con las recomendaciones de §6 |
| 7. Archivos calientes | ✅ (con regla) | §5 |
| 8. Rollback viable | ✅ | §5 |

**Dictamen DoR:** `READY`: D1–D10 aprobadas por el owner con las recomendaciones.

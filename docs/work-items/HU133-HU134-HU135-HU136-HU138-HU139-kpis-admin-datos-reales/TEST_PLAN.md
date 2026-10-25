---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# TEST_PLAN — HU133 / HU134 / HU135 / HU136 / HU138 / HU139: KPIs institucionales del Administrador

**Estado:** `RED_PLANNED` (borrador). Orden **DB → Backend → Frontend**; RED → GREEN → VALIDATE por repo. Referencias: [PLAN](PLAN.md) (CA-01…CA-09, D1–D10) y [CONTRACT](CONTRACT.md) (`DRAFT`; las pruebas se derivan de él y se congelan con él).

Regla: ninguna prueba se relaja para pasar; solo se modifica con `TEST_CONTRACT_CONFLICT` documentado.

---

## 1. Matriz de pruebas

### 1.1 Base de datos (`GestioAsistenciaDB/test/`)

**Fixture determinista** (transacción con `ROLLBACK`, derivado del de las olas 1 y 2): 2 instituciones **I1** (administrador A1) e **I2** (administrador A2); I1 con facultades **F1** (programas P1, P2) y **F2** (programa P3) y una facultad inactiva **F3** sin programas; I2 con **F4**/P4; periodo vigente y uno pasado; sesiones vencidas con registro, vencidas sin registro, de procedencia indeterminada y futuras, repartidas en **al menos 3 semanas y 2 meses** (incluidas sesiones en los bordes domingo/lunes y fin/inicio de mes); matrículas `A`, `CVP`, `CI`, `F`; faltas `SJC` y excusas `EX`; docentes con grupo habilitado, con vínculo sin grupo y sin vínculo. Todas las cifras esperadas son **números exactos calculados a mano en el propio test**.

| ID | Objeto | Caso | Esperado |
|---|---|---|---|
| T-DB-01 | `usp_resolver_institucion_administrador_interno` | A1 → I1; A2 → I2 | institución correcta |
| T-DB-02 | idem | Decano / coordinador / docente / estudiante / sin perfil | `KPI_004` |
| T-DB-03 | idem | Ejecutor nulo; correlación nula; umbral ausente (con validación) | `VAL_001`; `CORR_001`; `KPI_002` |
| T-DB-04 | `usp_resolver_periodo_kpi_institucion_interno` | Sin período → más reciente con grupos habilitados; período dado; inexistente; institución sin grupos | período; el dado; `PER_001`; `NULL` |
| T-DB-05 | `usp_consultar_kpis_institucion` | A1, período por defecto | Todas las cifras de §2.2 del CONTRACT = valores exactos del fixture (facultades, programas, conteos Regular/Riesgo/Pérdida, retención, sesiones-estudiante, faltas, ausentismo ponderado, asistencia, sesiones vencidas/con registro, cumplimiento, censos) |
| T-DB-06 | idem | Invariantes: `regulares+riesgo+perdida = total = estudiantesConMatriculaActiva`; `sesionesConRegistro ≤ sesionesVencidas`; `EX` no suma faltas; sesión sin registro y de procedencia indeterminada no entran en el denominador de ausentismo ni en el cumplimiento | cumple |
| T-DB-07 | idem | Aislamiento: A2 ve solo I2 (cifras distintas) | cifras de su institución |
| T-DB-08 | idem | Institución sin matrículas/sesiones | conteos `0`, porcentajes `NULL` (no `0`) |
| T-DB-09 | idem | Censo: estudiante con matrícula activa vs estudiante registrado sin matrícula; docente con grupo habilitado vs docente solo vinculado | conteos exactos, distintos entre sí |
| T-DB-10 | idem | Conciliación contra consulta directa sobre tablas (faltas SJC, programas, facultades de la institución) | igual a los SP |
| T-DB-11 | `usp_consultar_comparativa_facultades` | A1 | Filas F1, F2, F3 con cifras exactas; F3 (inactiva, sin datos) con conteos 0 y `NULL`; F4 ausente; orden por ausentismo desc, nulos al final |
| T-DB-12 | idem | Suma de `totalEstudiantes` por facultad ≥ `totalEstudiantes` de la institución (persona en 2 facultades) y cada fila coincide con su propio cálculo | cumple |
| T-DB-13 | `usp_consultar_tendencia_ausentismo` | `SEMANA` | Intervalos (lunes a domingo) con cifras exactas; orden cronológico; solo intervalos con datos; sesiones de borde en el intervalo correcto |
| T-DB-14 | idem | `MES` | Intervalos mensuales con cifras exactas; la suma de faltas/sesiones de los intervalos = la del KPI |
| T-DB-15 | idem | `granularidad` inválida; en minúsculas | `GEN_002`; se acepta/normaliza según CONTRACT (probar el caso real) |
| T-DB-16 | idem | Período pasado explícito; sin datos | intervalos del período pasado; lista vacía |
| T-DB-17 | los 3 públicos | `@@TRANCOUNT = 0` antes/después; sin `XACT_ABORT`; ejecutor ajeno no ve filas | cumple |
| T-DB-18 | Regresión | Tests de las olas 1 y 2 + `test_contract_closure.sql` | PASS |

Suites existentes que deben seguir en PASS: `test_reactive_academic`, `test_public_objects`, `test_transaction_ownership`, `test_authorized_views_rbac`.

### 1.2 Backend (JUnit 5 + Mockito + ArchUnit + MockMvc)

| ID | Prueba | Esperado |
|---|---|---|
| T-BE-01 | Casos de uso `ConsultarKpisInstitucion`, `ConsultarComparativaFacultades`, `ConsultarTendenciaAusentismo`: happy path, actor nulo, ámbito (sin institución → `ForbiddenException` con código `KPI_004`), granularidad inválida/por defecto | Sin cálculo en Java |
| T-BE-02 | Regla anti-simulación sobre las clases nuevas (mismo patrón que las olas 1 y 2) | Sin literales numéricos de negocio |
| T-BE-03 | Adaptador JPA: mapeo de filas `Sp*`; `NULL` se conserva; fechas `DATE` → `LocalDate` | Fidelidad |
| T-BE-04 | `DbCodeTranslator`: `KPI_004`→403 (con código), `GEN_002`→400, `PER_001`→404 | Mapeo correcto |
| T-BE-05 | Controller (MockMvc): 200; parámetros opcionales delegan `null`; `granularidad` por defecto y explícita; 403/404/400 propagados | Contrato HTTP |
| T-BE-06 | `RbacSecurityFilterChainTest` (caliente, al cierre): 401 sin token; `DOCENTE`/`ESTUDIANTE`/`COORDINADOR` 403; `ADMINISTRADOR` pasa | RBAC |
| T-BE-07 | ArchUnit existente; `JpaRepositoryBeanUniquenessTest` (inventario +1) | PASS |
| T-BE-08 | Cobertura de las clases nuevas (JaCoCo), **re-medida al final** | ≥ 80 % líneas (objetivo 100 % en casos de uso y mapeos) |
| T-BE-09 | Suite completa `mvnw -o test`, **repetida tras el último commit de tests** | 0 fallos |

### 1.3 Frontend (Jasmine/Karma, ChromeHeadless)

| ID | Prueba | Esperado |
|---|---|---|
| T-FE-01 | `AdminIndicatorsService`: URLs y parámetros (solo los presentes, incluida `granularidad`); publica lo recibido sin transformar; propaga errores sin tocar estado | Contrato |
| T-FE-02 | Tarjetas de KPI y censo: exactamente lo recibido; `null` → «Sin datos», nunca `0%`; «con matrícula/grupo activo» destacado y «registrados» secundario | CA-06/CA-07 |
| T-FE-03 | Comparativa de facultades: una tarjeta por facultad con las cifras recibidas; facultad inactiva señalada; sin fila inventada | HU139 |
| T-FE-04 | Gráfica de tendencia: un punto por intervalo recibido, ancho/alto proporcionales a los valores recibidos (sin recalcular); lista vacía → estado vacío; cambiar `SEMANA`/`MES` reconsulta con ese valor | HU134 |
| T-FE-05 | SSE: al emitirse `ASISTENCIAS_SESION_ACTUALIZADAS` se reconsultan KPIs y tendencia; otros eventos no; al destruir el componente se cancela la suscripción; no hay `setInterval` | CA-08 |
| T-FE-06 | Valores de cada filtro/selector fijados por spec (lección de `AMBAR`): `granularidad` envía exactamente `SEMANA`/`MES` | Sin defectos de código |
| T-FE-07 | Error HTTP 403/404/400: se muestra el `message` del servidor | CA-07 |
| T-FE-08 | `overview-admin`: tarjeta «Programas Académicos» = `totalProgramas` del backend o «Sin datos»; la expresión `decanos().length > 0 ? 1 : 0` no existe; falla de la consulta no rompe el resto de la vista | CA-07 |
| T-FE-09 | Ruta `/app/admin/indicadores` protegida por `roleGuard` (`ADMINISTRADOR`/`ADMIN`) y entrada de menú solo para esos roles | RBAC UI |
| T-FE-10 | Specs existentes de `overview`, `overview-admin`, `admin-*` y `dashboard-layout` siguen en verde (solo se admite añadir proveedores espía por cambio de constructor, documentado) | Sin regresión |
| T-FE-11 | `ng test` completo y `ng build`; cobertura del código nuevo del frontend **medida** y reportada | 0 fallos; ≥ 80 % |

### 1.4 E2E con JWT real (`scratch/test_hu133_hu134_hu135_hu136_hu138_hu139.ps1`, solo lectura)

Backend de la rama en un puerto distinto de 8080; Keycloak :8081; SQL en Docker. Credenciales canónicas (`admin.institucional@uco.edu.co`).

| ID | Verificación |
|---|---|
| T-E2E-01 | Sin token 401; `decano`, `coordinador`, `docente`, `estudiante` 403 en las 3 rutas |
| T-E2E-02 | `admin.institucional` obtiene 200 en las 3 rutas |
| T-E2E-03 | **Conciliación:** cada campo de `/indicadores` coincide con la ejecución directa de `usp_consultar_kpis_institucion` y con consultas de control sobre tablas (facultades, programas, matrículas activas, estudiantes y docentes registrados) |
| T-E2E-04 | `/comparativa-facultades`: una fila por facultad de la institución; cada fila coincide con el SP |
| T-E2E-05 | `/tendencia-ausentismo`: la suma de faltas y sesiones de los puntos = la del KPI (`SEMANA` y `MES`); orden cronológico |
| T-E2E-06 | **Cada valor de cada parámetro:** `granularidad` = `SEMANA`, `MES`, ausente (= `SEMANA`), inválida (400); `idPeriodoAcademico` válido (200) e inexistente (404) en las 3 rutas |
| T-E2E-07 | Un usuario administrador sin fila en `Administrador` → 403 con mensaje del servidor (si la semilla no lo permite: SKIP explícito, cubierto por DB) |
| T-E2E-08 | SSE: con el canal `/realtime/stream` abierto, registrar una asistencia dispara el evento (smoke; marcar SKIP explícito si no es automatizable) |

### 1.5 Seguridad, gobernanza y observabilidad

| ID | Verificación |
|---|---|
| T-SEC-01 | Cero SQL/`EntityManager`/`JdbcTemplate` en controller o `application` (ArchUnit) |
| T-SEC-02 | Respuestas sin PII (solo agregados y nombres de facultad/institución) |
| T-SEC-03 | `X-Correlation-Id` propagado a `@idCorrelacion` (test del adaptador) |
| T-GOV-01 | `infra.ps1 verify` PASS; contratos WORM intactos |
| T-GOV-02 | Ningún archivo de `FORBIDDEN_PATHS` modificado (revisión del diff); HU140/HU141/HU129 sin implementación ni datos simulados |

---

## 2. Orden de ejecución

1. **RED DB** → **GREEN DB** (cada SP con su test) → regresión de las olas 1 y 2 → `test_contract_closure.sql`.
2. **RED Backend** → **GREEN Backend** → `mvnw -o test`.
3. **RED Frontend** → **GREEN Frontend** → `ng test` + `ng build`.
4. **E2E** con JWT real y conciliación.
5. **Archivos calientes** (commit aparte): `RbacSecurityFilterChainTest`, `JpaRepositoryBeanUniquenessTest`, `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`, `HU-CATALOG.md`.
6. Suite completa del backend **repetida al final**, `infra.ps1 verify` y `VALIDATION.md`.

## 3. Criterio de salida

Todos los IDs en PASS (o SKIP explícito justificado); cobertura del código nuevo ≥ 80 % en backend **y frontend**, re-medida al final; E2E sin `FAIL`; conciliación exacta; `decanos().length > 0 ? 1 : 0` eliminado; sin push ni PR hasta orden del owner.

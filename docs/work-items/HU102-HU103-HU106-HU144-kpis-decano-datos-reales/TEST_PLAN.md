---
status: frozen-approved
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# TEST_PLAN — HU102 / HU103 / HU106 / HU144: KPIs de facultad del Decano

**Estado:** `RED_PLANNED` (borrador). Orden **DB → Backend → Frontend**; RED → GREEN → VALIDATE por repo. Referencias: [PLAN](PLAN.md) (CA-01…CA-09, D1–D10) y [CONTRACT](CONTRACT.md) (`frozen-approved`; las pruebas se derivan de él y se congelan con él).

Regla: ninguna prueba se relaja para pasar; solo se modifica con `TEST_CONTRACT_CONFLICT` documentado.

---

## 1. Matriz de pruebas

### 1.1 Base de datos (`GestioAsistenciaDB/test/`)

**Fixture determinista** (transacción con `ROLLBACK`): 2 facultades **F1** (decano D1) y **F2** (decano D2); F1 con programas **P1** y **P2** (P2 inactivo o sin matrículas), F2 con **P3**; grupos con cupos distintos (p. ej. 10, 5, 0), matrículas en estados `A`, `CVP`, `CI`, `F`; sesiones vencidas con registro, sin registro y futuras; faltas `SJC` y excusas `EX`. Los resultados esperados son **números exactos** calculados a mano en el propio test.

| ID | Objeto | Caso | Esperado |
|---|---|---|---|
| T-DB-01 | `usp_resolver_facultad_decano_interno` | Decano D1 | F1 |
| T-DB-02 | idem | Usuario que no es decano / coordinador / admin | `KPI_003` |
| T-DB-03 | idem | Ejecutor nulo; umbral ausente | `VAL_001`; `KPI_002` |
| T-DB-04 | `usp_resolver_periodo_kpi_facultad_interno` | Sin período → más reciente con grupos habilitados; período dado; inexistente | período; el dado; `PER_001`; facultad sin grupos → `NULL` |
| T-DB-05 | `usp_validar_programa_en_facultad_interno` | P1 con F1; P3 con F1; inexistente | OK; `GEN_001`; `GEN_001` |
| T-DB-06 | `usp_consultar_kpis_decano` | D1 | Conteos Regular/Riesgo/Pérdida por **persona** (peor matrícula), `tasaRetencion`, `porcentajeAusentismo` ponderado, sesiones, faltas, grupos activos, cupo, matriculados, `tasaAforo` = valores exactos del fixture |
| T-DB-07 | idem | Invariante `regulares+riesgo+perdida = total`; la excusa `EX` no suma faltas; la sesión sin registro no entra al denominador | cumple |
| T-DB-08 | idem | Aislamiento: D2 no ve datos de F1 y viceversa (mismos SP, cifras distintas) | cifras de su facultad |
| T-DB-09 | idem | Facultad sin matrículas | `totalEstudiantes = 0`, `tasaRetencion`, `porcentajeAusentismo`, `porcentajeAsistencia`, `tasaAforo` = `NULL` (no `0`) |
| T-DB-10 | idem | `porcentajeAsistencia = 100 − porcentajeAusentismo` | cumple; `NULL` si el ausentismo es `NULL` |
| T-DB-11 | `usp_consultar_comparativa_programas_facultad` | D1 | Filas P1 y P2 con cifras exactas; orden por ausentismo desc, nulos al final; P3 ausente |
| T-DB-12 | idem | Programa inactivo se lista con `estaActivoPrograma = 0` (D5) | cumple |
| T-DB-13 | `usp_consultar_aforo_grupos_facultad` | Página 1/2, `COUNT(*) OVER()` | `totalRegistros` correcto; páginas disjuntas |
| T-DB-14 | idem | Filtro `idPrograma` válido / de otra facultad | filas del programa / `GEN_001` |
| T-DB-15 | idem | `soloHabilitados` 1 vs 0 | 1 excluye grupos no habilitados; 0 los incluye |
| T-DB-16 | idem | `cupoMaximo = 0` o nulo → `tasaAforo = NULL`; aforo > 100 % no se recorta | cumple |
| T-DB-17 | idem | `@tamanoPagina` > máximo del catálogo; página 0 | `PAG_001` |
| T-DB-18 | los 3 públicos | `@@TRANCOUNT = 0` antes/después; no usan `XACT_ABORT`; ejecutor ajeno no ve filas | cumple |
| T-DB-19 | Regresión | Los 6 tests de la ola 1 + `test_contract_closure.sql` | PASS |

Suites existentes que deben seguir en PASS: `test_reactive_academic`, `test_public_objects`, `test_transaction_ownership`, `test_authorized_views_rbac`, y las de la ola 1.

### 1.2 Backend (JUnit 5 + Mockito + ArchUnit + MockMvc)

| ID | Prueba | Esperado |
|---|---|---|
| T-BE-01 | Casos de uso: `ConsultarKpisDecanoUseCaseImpl`, `ConsultarComparativaProgramasUseCaseImpl`, `ConsultarAforoGruposUseCaseImpl` — happy path, actor nulo, validaciones de página/tamaño | Mensajes de catálogo; sin cálculo en Java |
| T-BE-02 | Regla anti-simulación (extender `KpisCoordinadorSinSimulacionTest` o equivalente) sobre las clases nuevas | Sin literales numéricos de negocio ni listas estáticas |
| T-BE-03 | Adaptador JPA: mapeo de filas `Sp*` a proyecciones; `NULL` se conserva (no se convierte en `0`) | `null` se mantiene |
| T-BE-04 | `DbCodeTranslator`: `KPI_003`→403, `GEN_001`→404, `PER_001`/`PAG_001`/`VAL_001`→400 | Mapeo correcto |
| T-BE-05 | Controller (`@WebMvcTest`/MockMvc): 200 con `ApiDataResponse`/`ApiPageResponse`; 400 con parámetros inválidos; 403/404 propagados con `message` del servidor | Contrato HTTP |
| T-BE-06 | `RbacSecurityFilterChainTest` (archivo caliente, al cierre): sin token 401; `COORDINADOR`/`DOCENTE`/`ESTUDIANTE` 403; `DECANO` y `ADMINISTRADOR` pasan el filtro (D8) | RBAC |
| T-BE-07 | ArchUnit existente (capas, sin `JdbcTemplate`/SQL en controller/application, `Sp*` sin `@Immutable`); `JpaRepositoryBeanUniquenessTest` (inventario +1) | PASS |
| T-BE-08 | Cobertura de las clases nuevas (JaCoCo) | ≥ 80 % líneas (objetivo 100 % en casos de uso y mapeos) |
| T-BE-09 | Suite completa `mvnw -o test` | 0 fallos |

### 1.3 Frontend (Jasmine/Karma, ChromeHeadless)

| ID | Prueba | Esperado |
|---|---|---|
| T-FE-01 | `DeanIndicatorsService`: URLs y parámetros (solo los presentes); publica lo recibido sin transformar; propaga errores sin tocar estado | Contrato |
| T-FE-02 | Tarjetas: muestran exactamente lo recibido; `null` → «Sin datos», nunca `0%` | CA-07/CA-08 |
| T-FE-03 | Comparativa: una barra por programa con ancho proporcional al valor **recibido**; sin fila inventada; programa inactivo señalado | HU103 |
| T-FE-04 | Aforo: tabla + `PaginationComponent`; cambio de página reconsulta; filtro por programa reinicia a la página 1 y envía `idPrograma` | HU144 |
| T-FE-05 | Las opciones/valores de cada filtro coinciden con el contrato (lección de `AMBAR`): spec que fija los valores enviados | Sin defectos de código |
| T-FE-06 | Error HTTP 403/404/400: se muestra el `message` del servidor; no se interpreta el texto | CA-08 |
| T-FE-07 | `overview-dean`: la tarjeta «Asistencia Promedio» refleja `porcentajeAsistencia` o «Sin datos»; el texto `94%` no existe en el componente; sin `setInterval` | CA-08 |
| T-FE-08 | Ruta `/app/decano/indicadores` protegida por `roleGuard` (`DECANO`) y entrada de menú solo para `DECANO` | RBAC UI |
| T-FE-09 | Specs de `overview-dean`, `dean-faculty`, `dean-coordinadores` y `dashboard-layout` existentes siguen en verde | Sin regresión |
| T-FE-10 | `ng test` completo y `ng build`; cobertura del código nuevo del frontend **medida** y reportada | 0 fallos; ≥ 80 % |

### 1.4 E2E con JWT real (`scratch/test_hu102_hu103_hu106_hu144.ps1`, solo lectura)

Backend de la rama en un puerto distinto de 8080; Keycloak :8081; SQL en Docker.

| ID | Verificación |
|---|---|
| T-E2E-01 | Sin token 401; `coordinador`, `docente`, `estudiante` 403 en las 3 rutas |
| T-E2E-02 | `decano.ingenieria` obtiene 200 en las 3 rutas |
| T-E2E-03 | **Conciliación:** cada campo de `/indicadores` coincide con la ejecución directa de `usp_consultar_kpis_decano` y con una consulta de control sobre tablas (conteos de matrículas activas, cupos) |
| T-E2E-04 | `/comparativa-programas`: una fila por programa de la facultad; la suma de estudiantes por programa ≥ `totalEstudiantes` de la facultad (una persona puede estar en dos programas) y cada fila coincide con el SP |
| T-E2E-05 | `/aforo-grupos`: `tamano` respetado; `totalElementos` coincide con el conteo de control; páginas disjuntas |
| T-E2E-06 | **Cada valor de cada filtro:** `idPrograma` propio (200), de otra facultad (404), inexistente (404); `soloHabilitados` `true` y `false`; `idPeriodoAcademico` válido (200) e inexistente (400); `pagina=0` y `tamano` excesivo (400) |
| T-E2E-07 | Administrador sin facultad → 403 con mensaje del servidor |

### 1.5 Seguridad, gobernanza y observabilidad

| ID | Verificación |
|---|---|
| T-SEC-01 | Cero SQL/`EntityManager`/`JdbcTemplate` en controller o `application` (ArchUnit) |
| T-SEC-02 | Respuestas sin PII de estudiantes (solo agregados y nombres de programa/asignatura) |
| T-SEC-03 | `X-Correlation-Id` propagado a `@idCorrelacion` (test del adaptador) |
| T-GOV-01 | `infra.ps1 verify` PASS; contratos WORM intactos |
| T-GOV-02 | Ningún archivo de `FORBIDDEN_PATHS` modificado (revisión del diff) |

---

## 2. Orden de ejecución

1. **RED DB** → **GREEN DB** (cada SP con su test) → `test_contract_closure.sql`.
2. **RED Backend** → **GREEN Backend** → `mvnw -o test`.
3. **RED Frontend** → **GREEN Frontend** → `ng test` + `ng build`.
4. **E2E** con JWT real y conciliación.
5. **Archivos calientes** (commit aparte): `RbacSecurityFilterChainTest`, `JpaRepositoryBeanUniquenessTest`, `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`, `HU-CATALOG.md`.
6. `infra.ps1 verify` y `VALIDATION.md`.

## 3. Criterio de salida

Todos los IDs en PASS; cobertura del código nuevo ≥ 80 % en backend **y frontend**; E2E sin `FAIL`; conciliación exacta; `94%` eliminado; sin push ni PR hasta orden del owner.

---
status: validated-pending-pr
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-24
---

# VALIDATION — HU133 / HU134 / HU135 / HU136 / HU138 / HU139: KPIs institucionales del Administrador (datos reales)

Ola 3 del [PLAN](PLAN.md). Ramas `feature/HU133-kpis-admin-datos-reales` en DB, Backend, Frontend e Infra (worktrees `.worktrees/{db,backend,frontend,infra}-kpis-admin`), basadas en la rama de la ola 2; sin push ni PR. D1–D10 aprobadas con las recomendaciones del PLAN. **D7 no se pudo cumplir tal como se aprobó (ver §4).**

## 1. Resumen

| Capa | Evidencia | Resultado |
|---|---|---|
| DB | `test_usp_resolvers_institucion_admin`, `test_usp_consultar_kpis_institucion`, `test_usp_consultar_comparativa_facultades`, `test_usp_consultar_tendencia_ausentismo` (fixture con 2 instituciones, facultad inactiva, periodo pasado, 2.º docente y sesiones en fechas absolutas con bordes de semana y mes en hora local; cifras calculadas a mano) | 4/4 PASS en la primera ejecución del GREEN |
| DB | Regresión: tests de las olas 1 y 2, `test/test_contract_closure.sql` | PASS, 0 `TEST_FAIL` |
| Backend | `mvnw -o test` completo, **ejecutado después del último commit de tests** (incluye ArchUnit y `KpisAdminSinSimulacionTest`) | 2195 tests, 0 fallos |
| Backend | JaCoCo sobre las 37 clases nuevas | líneas 100 %, ramas 94,4 % |
| Frontend | `ng test` completo (ChromeHeadless) con cobertura | 1101 SUCCESS |
| Frontend | `ng build --configuration=development` | sin errores |
| Frontend | Cobertura del código nuevo (`admin-indicators.*`, `overview-admin`) medida con lcov | líneas 100 %, ramas 87,5 % |
| E2E | `scratch/test_hu133_hu134_hu135_hu136_hu138_hu139.ps1` con JWT reales (backend de la rama en `:8090`) | 69 PASS, 2 SKIP, 0 FAIL |
| Infra | `infra.ps1 verify` (checkout principal) | PASS (18 comprobaciones WORM, 0 secretos) |

## 2. Qué verifica el E2E

401 sin token y 403 para decano, coordinador, docente y estudiante en las 3 rutas; KPIs conciliados campo a campo con la ejecución directa de `usp_consultar_kpis_institucion` y con consultas de control sobre tablas (facultades, programas, estudiantes registrados, faltas SJC de sesiones vencidas con procedencia determinada); comparativa con una fila por facultad y misma cifra que el SP; tendencia `SEMANA` y `MES` con suma de sesiones y faltas igual a la del KPI y orden cronológico; **cada valor de cada parámetro** (`granularidad` `SEMANA`/`MES`/ausente/inválida; `idPeriodoAcademico` vigente e inexistente en las 3 rutas).

**SKIP (2):**
- Administrador sin fila en `Administrador` → 403: la semilla no tiene ese usuario; lo cubre el test DB (`KPI_004`) y la traducción a 403 con código el test del adaptador.
- SSE para el administrador: no existe canal institucional (ver §4).

**Hallazgos del propio E2E durante la ejecución (errores de mi script, no del producto):** la primera corrida dio 4 FAIL — un error de sintaxis de PowerShell (`$g:`), un comodín (`-like '*?*'`) que armaba mal las URLs del período (lo que además hacía «pasar» en falso el 404 de período inexistente) y una consulta de control sin el filtro de sesiones vencidas con procedencia determinada (10 faltas en tablas vs 2 del SP: 8 están en sesiones de procedencia indeterminada, que la definición excluye). Se corrigieron el script y la consulta de control, no el producto, y el 404 de período inexistente ahora se verifica con la URL correcta.

## 3. TEST_CONTRACT_CONFLICT

Dos cambios a pruebas existentes, ninguno relaja una aserción:
- `overview.component.spec.ts` añade un proveedor espía de `AdminIndicatorsService` (el componente ahora lo inyecta para la tarjeta de programas); se agregaron 3 pruebas nuevas del rol ADMINISTRADOR.
- **Pruebas DB de las olas 1 y 2** (`test_usp_consultar_kpis_coordinador`, `test_usp_consultar_kpis_decano`): simulan «periodo vencido» moviendo `fechaFin` a `hoy − 1` (UTC), pero con la zona institucional (UTC−5) entre las 19:00 y las 24:00 locales el periodo seguía vigente y las pruebas **fallaban según la hora del día**. Se cambió el margen a `hoy − 3`; no cambia ninguna aserción ni cifra esperada. Commits `fix(db/test)` en las ramas de las olas 1, 2 y 3.

## 4. Hallazgos y cambios respecto al CONTRACT/PLAN

- **D7 (SSE) no cumplido:** `GET /api/v1/realtime/stream` exige `grupoId` y verifica la titularidad docente; no hay canal institucional, así que el administrador no puede suscribirse. La página ofrece el botón «Actualizar» (acción del usuario, no sondeo). Consecuencia: **HU135 y HU136 quedan `[PARCIAL]`** (contadores reales, sin actualización push) y los KPIs de asistencia tampoco se actualizan solos. Follow-up propuesto: canal SSE institucional y eventos de alta de estudiante/docente y matrícula.
- **Docentes registrados:** definición distinta a la del CONTRACT original (no hay `DocentePrograma` en el esquema desplegado); ver aclaraciones del CONTRACT.
- **Sin `InstitutionalScopePort`:** el ámbito lo fuerza solo la DB (no hay método para la institución del administrador); `KPI_004` viaja con su código.
- **Alcance de frontend ampliado en un archivo:** además de `overview-admin.component.ts`, se tocó `overview.component.ts` (padre) para inyectar `AdminIndicatorsService` y pasar `totalProgramas` a la tarjeta; sin esto el componente de presentación no puede dejar de mostrar `decanos().length > 0 ? 1 : 0`.
- `DbCodeTranslator`: se añadió `KPI_004` (comportamiento anterior intacto).
- Archivos calientes en commits aparte: `RbacSecurityFilterChainTest` (generador de tokens sin `DECANO`; se prueba 401/403/200 con los roles soportados), `JpaRepositoryBeanUniquenessTest` (inventario 39→40), `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`. No se tocaron `SecurityConfig`, `AdminPortalController` ni wirings existentes. Mensaje nuevo `KPI_004` en `schema/seed/20_mensajes_kpis_admin.sql`.

## 5. Pendiente / fuera de alcance

- La ola 3 depende de las olas 1 y 2: su PR se abre después de los de esas olas (o se rebasa sobre `develop`).
- No se ejecutó `test_suite.sql` completo, solo los tests nuevos, la regresión de las olas 1 y 2 y el cierre de contrato.
- HU140 (sedes), HU141 (escalafón) y HU129 (horas contratadas) siguen `BLOCKED_BY_MISSING_DATA`; en `HU-CATALOG` HU140 quedó `[BLOQUEADO 501]` (las rutas de sedes responden 501) y HU141/HU129 no se modificaron.
- `Gestión de Decanos`/`admin-sedes` no se tocaron.
- Sin push ni PR hasta orden explícita.

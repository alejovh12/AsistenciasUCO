---
status: validated-pending-pr
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-24
---

# VALIDATION — HU065 / HU066 / HU070 / HU067 / HU069: KPIs, alerta temprana, permanencia, bitácora y ficha del Coordinador (datos reales)

Ola 1 del [PLAN](PLAN.md). Ramas `feature/HU065-kpis-coordinador-datos-reales` en DB, Backend y Frontend (worktrees `.worktrees/db-kpis-coord`, `backend-kpis-plan`, `frontend-kpis-coord`), sin push ni PR. Decisiones D1–D10 aprobadas con las recomendaciones del PLAN. Reemplaza a las ramas locales descartadas `feature/kpis-coordinador-p1-p2` y `feature/ficha-bitacora-estudiante-coordinador-p3` (datos simulados), que no se modificaron.

## 1. Resumen

| Capa | Evidencia | Resultado |
|---|---|---|
| DB | `test_ufn_clasificar_permanencia`, `test_uv_resumen_asistencia_estudiante_grupo`, `test_usp_consultar_kpis_coordinador`, `test_usp_consultar_alerta_temprana_programa`, `test_usp_consultar_bitacora_estudiante_programa`, `test_usp_consultar_ficha_estudiante_programa` (cifras exactas sobre un fixture determinista) | 6/6 PASS |
| DB | `test/test_contract_closure.sql` con las 5 lecturas nuevas exentas de la regla de SP transaccional | PASS, 0 `TEST_FAIL` |
| Backend | `mvnw -o test` completo (incluye ArchUnit y `KpisCoordinadorSinSimulacionTest`) | 2097 tests; falló 1 (ver §3), corregido y re-ejecutado `RbacSecurityFilterChainTest` + `JpaRepositoryBeanUniquenessTest`: 54/54. Después se añadieron 5 tests que pasan; la suite completa no se repitió tras ellos |
| Backend | JaCoCo sobre las 61 clases nuevas | líneas 100 %, ramas 94,6 % |
| Frontend | `ng test` (ChromeHeadless) | 1062 SUCCESS (19 nuevos) |
| Frontend | `ng build --configuration=development` | sin errores |
| E2E | `scratch/test_hu065_hu066_hu070_hu067_hu069.ps1` con JWT reales (backend de la rama en `:8090`) | 21/21 |
| Infra | `infra.ps1 verify` (checkout principal) | PASS (18 comprobaciones WORM, 0 secretos) |

## 2. Qué verifica el E2E

401 sin token; 403 para estudiante (indicadores) y docente (alertas); KPIs de la API conciliados campo a campo contra la ejecución directa de `usp_consultar_kpis_coordinador` (total 4, regulares 3, riesgo 0, pérdida 1, programadas 6, ejecutadas 2); regulares + riesgo + pérdida = total; umbrales 15/20 desde catálogo; paginación (`tamano`), 400 con `pagina=0`; filtro `ROJO` solo devuelve `ROJO`; ficha y bitácora de un estudiante real; 404 para un estudiante inexistente o ajeno.

## 3. TEST_CONTRACT_CONFLICT

Ninguno sobre pruebas existentes. Un fallo en una prueba nueva propia: `coordinador_indicadores_solo_coordinador_y_administrador_pasan_el_filtro` incluía el rol `DECANO`, que el generador de tokens de `RbacSecurityFilterChainTest` no soporta (responde 401 por token inválido, no 403). Se retiró `DECANO` de esa lista; no se relajó ninguna aserción sobre el comportamiento de las rutas.

## 4. Hallazgos y cambios respecto al CONTRACT

- Lecturas puras: los SP de consulta no usan `XACT_ABORT` ni transacción; los auxiliares son `usp_*_interno`.
- `JpaQueryExecutor` no clasifica errores de SP: el adaptador traduce `KPI_001`/`GEN_001`/`PER_001`/`PAG_001`/`GEN_002`/`VAL_001` con `DbCodeTranslator` (403/404/400).
- Definiciones: falta = `asistio = 0` con razón `SJC` (la excusa no cuenta); sesión vencida = `fechaHoraFin ≤ ahora` con procedencia temporal; la persona toma la peor clasificación entre sus matrículas activas (D6). Los nulos nunca se sustituyen por cero (el frontend muestra "Sin datos").
- Archivos calientes en commits aparte al final: `RbacSecurityFilterChainTest`, `JpaRepositoryBeanUniquenessTest` (inventario 37→38), `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`. No se tocaron `SecurityConfig`, wirings existentes, `04_mensaje.sql` ni `DbFailureClassifier`. Mensajes nuevos en `schema/seed/18_mensajes_kpis_coordinador.sql`.

## 5. Pendiente / fuera de alcance

- Botón «Ficha» dentro de «Estudiantes del Programa» (HU068): no se añadió para no alterar esa pantalla; la ficha se abre desde la tabla de alertas.
- No se midió cobertura del código nuevo del frontend (solo pasan los 19 specs y el build).
- No se ejecutó `test_suite.sql` completo, solo los 6 tests nuevos y el cierre de contrato.
- `HU-CATALOG` se actualiza en el worktree de Infra (ver commit correspondiente).
- Olas 2 (Decano) y 3 (Admin) según el PLAN; HU140/141/129 siguen `BLOCKED_BY_MISSING_DATA`.
- Sin push ni PR hasta orden explícita.

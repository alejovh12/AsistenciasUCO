---
status: validated-pending-pr
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-24
---

# VALIDATION — HU102 / HU103 / HU106 / HU144: KPIs de facultad del Decano (datos reales)

Ola 2 del [PLAN](PLAN.md). Ramas `feature/HU102-kpis-decano-datos-reales` en DB, Backend, Frontend e Infra (worktrees `.worktrees/{db,backend,frontend,infra}-kpis-decano`), basadas en la rama de la ola 1; sin push ni PR. D1–D10 aprobadas con las recomendaciones del PLAN.

## 1. Resumen

| Capa | Evidencia | Resultado |
|---|---|---|
| DB | `test_usp_resolvers_decano`, `test_usp_consultar_kpis_decano`, `test_usp_consultar_comparativa_programas_facultad`, `test_usp_consultar_aforo_grupos_facultad` (fixture con 2 facultades, programa inactivo, periodo pasado y cupos distintos; cifras calculadas a mano) | 4/4 PASS en la primera ejecución del GREEN |
| DB | Regresión: los 6 tests de la ola 1 y `test/test_contract_closure.sql` | PASS, 0 `TEST_FAIL` |
| Backend | `mvnw -o test` completo (incluye ArchUnit y `KpisDecanoSinSimulacionTest`) | 2148 tests; falló 1 (inventario de repositorios, esperado: 38→39, archivo caliente). Corregido; re-ejecutados `RbacSecurityFilterChainTest` + `JpaRepositoryBeanUniquenessTest`: 56/56. Después se añadió `SpIndicadoresDecanoEntitiesTest` (pasa); la suite completa no se repitió tras ese último test |
| Backend | JaCoCo sobre las 41 clases nuevas (re-medido tras añadir `SpIndicadoresDecanoEntitiesTest`) | líneas 100 %, ramas 96,2 % |
| Frontend | `ng test` completo (ChromeHeadless) | 1083 SUCCESS |
| Frontend | `ng build --configuration=development` | sin errores |
| Frontend | Cobertura del código nuevo (`dean-indicators.*`, `overview-dean`) medida con lcov | líneas 100 %, ramas 92,9 % |
| E2E | `scratch/test_hu102_hu103_hu106_hu144.ps1` con JWT reales (backend de la rama en `:8090`) | 60 PASS, 1 SKIP, 0 FAIL |
| Infra | `infra.ps1 verify` (checkout principal) | PASS (18 comprobaciones WORM, 0 secretos) |

## 2. Qué verifica el E2E

401 sin token y 403 para coordinador, docente y estudiante en las tres rutas; administrador sin facultad → 403; KPIs conciliados campo a campo con la ejecución directa de `usp_consultar_kpis_decano` y con consultas de control sobre tablas (programas, grupos habilitados); comparativa con una fila por programa y misma cifra que el SP; aforo paginado (tamaño, páginas disjuntas, total contra control); **cada valor de cada filtro** (`idPrograma` propio e inexistente, `soloHabilitados` true/false, período vigente e inexistente en las tres rutas, `pagina=0`, `tamano` excesivo y `tamano=0`).

**SKIP (1):** `idPrograma` de otra facultad → 404: la semilla local no tiene programas de otra facultad. El caso está cubierto por el test DB con fixture (T-DB-05 y T-DB-14) y no se creó ningún dato para forzarlo.

## 3. TEST_CONTRACT_CONFLICT

Un cambio a una prueba existente, justificado por un cambio de constructor y no por relajar aserciones: `overview.component.spec.ts` añade un proveedor espía de `DeanIndicatorsService` (el componente ahora lo inyecta para la tarjeta de asistencia). Ninguna aserción existente se modificó; se agregaron 3 pruebas nuevas del rol DECANO.

## 4. Hallazgos y cambios respecto al CONTRACT/PLAN

- Aclaraciones al CONTRACT (ver su §1): `sesionesEjecutadas` = suma por matrícula; `PER_001` → 404; `ApiListResponse` en aforo; mensajes `KPI_*` con código.
- **Alcance de frontend ampliado en un archivo:** además de `overview-dean.component.ts`, se tocó `overview.component.ts` (padre) para inyectar `DeanIndicatorsService` y pasar `porcentajeAsistencia` a la tarjeta. Es la única forma de que `overview-dean` (componente de presentación por `input`) deje de mostrar el `94%` sin que el cliente calcule.
- `DbCodeTranslator` ganó una sobrecarga (`entidadNoEncontrada`) y `KPI_003`; el comportamiento de la ola 1 no cambia (sus pruebas siguen en verde). `KPI_001` ahora viaja con su código.
- Archivos calientes en commits aparte: `RbacSecurityFilterChainTest` (el generador de tokens de ese test no soporta `DECANO`; se prueba 401/403 y que `ADMINISTRADOR` pasa el filtro), `JpaRepositoryBeanUniquenessTest` (inventario 38→39), `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql`. No se tocaron `SecurityConfig` (ya admite `DECANO` y `ADMINISTRADOR` en `/api/v1/decano/**`), `DecanoPortalController` ni wirings existentes. Mensaje nuevo `KPI_003` en `schema/seed/19_mensajes_kpis_decano.sql`.

## 5. Pendiente / fuera de alcance

- La ola 2 depende de la ola 1: el PR de esta debe abrirse después del de la ola 1 (o rebasar sobre `develop` cuando esta se integre).
- No se ejecutó `test_suite.sql` completo, solo los tests nuevos, la regresión de la ola 1 y el cierre de contrato.
- Mensajes específicos de `GEN_001`/`PER_001`/`PAG_001` hacia el usuario siguen siendo genéricos (limitación del resolvedor del catálogo con placeholders, común a la ola 1).
- El texto «Nivel óptimo institucional» de la tarjeta se reemplazó por «Período vigente de la facultad».
- Ola 3 (Administrador) según el PLAN de la ola 1; HU140/141/129 siguen `BLOCKED_BY_MISSING_DATA`.
- Sin push ni PR hasta orden explícita.

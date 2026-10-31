---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-10-30
---

# VALIDATION — FIX-002: titularidad y acceso al reporte Excel

## Resultado

**READY_FOR_LOCAL_COMMIT.** La autorización llega desde el principal autenticado hasta Application, y la decisión se aplica antes de leer las filas. El contrato URL/XLSX no cambió. La revalidación local posterior al cierre de hallazgos terminó con `BUILD SUCCESS`; integración real sigue `NOT_RUN` porque no hay un SQL Server aislado y autorizado identificado. Esto no es aprobación de producción. No se hizo commit, push, merge ni operación de rama.

SHA actual de `develop` al validar: `e231088217e44b43633e64ba45bd078a7b98b53e`.

Hallazgos cerrados: roles activos vacíos rechazan a docente/coordinador aunque existan identidad/scope residuales; el controller genera un XLSX legible con los once campos nulos; se concilió 108 entradas Git visibles con 116 archivos físicos protegidos. Hallazgo pendiente: faltan en `AuthorizationReportJpaParityIT` comparaciones JPA/JDBC para roles activos, administrador activo y scope de coordinador por grupo, y no se ejecutó integración real.

## Auditoría y adaptación histórica

- `SecurityConfig`: el GET existente requiere autenticación y conserva `DOCENTE`, `COORDINADOR`, `ADMINISTRADOR`; ESTUDIANTE sigue excluido. Sin token/JWT inválido permanecen en 401.
- `ReporteAsistenciaController`: recibe UUID de `AuthenticatedUserResolver`; el cliente no aporta el actor. Construye el DTO de solicitud antes de llamar al InputPort.
- `GenerarReporteAsistenciaInteractor` / mapper / DTO / domain / use case: mantienen la cadena hexagonal neutral. Application no importa Spring.
- `GenerarReporteAsistenciaUseCaseImpl`: contrasta roles activos institucionales y exige autorización por scope según cada rol; lanza 403 antes de `ReporteAsistenciaQueryPort` si ninguna vía aplica.
- `ReporteWiringConfiguration`: inyecta los puertos de consulta y scope existentes.
- `InstitutionalScopePort` y `InstitutionalScopeJpaRepository`: ya exponían roles activos, identidad docente, scope docente/coordinador y administrador activo. No se cambiaron puerto, query, adaptador ni DB.
- Usuarios multirrol pasan por cualquier vía propia que tenga rol activo y scope válido. Una identidad docente ausente nunca concede acceso; un scope válido de coordinador puede autorizar por sí mismo.
- Comparación antigua: `git merge-base develop jose-valencia/fix-002-reporte-asistencia-titularidad` = `72312d7c60449cd0118b8d32b8da02ad1eb0074d`; `git rev-list --count develop..jose-valencia/fix-002-reporte-asistencia-titularidad` = 6. Revisados commits `6a014ab0`, `ac39358f`, `d8bbc8d3`, `d66f5480`, `2821ccee` y `f033407f`, así como el diff desde ese merge-base. No se fusionó la rama. La lógica histórica no exigía rol activo ni cerraba el caso `Optional.empty`; solo se recuperó el paso actor/grupo por DTO/domain y se adaptó la autorización al scope vigente.

## Pruebas y controles

| Ejecución | Resultado | Alcance |
|---|---|---|
| RED previo a la corrección: `mvnw test -Dtest=ReporteAsistenciaTitularidadHttpTest,ReporteAsistenciaAdministradorAccessHttpTest -Djacoco.skip=true` | **FAIL esperado** — exit 1; 12 tests, 8 failures, 0 errors, 0 skipped | Reprodujo DOCENTE/COORDINADOR ajeno e identidad faltante con 200, ausencia de consultas de scope y falta de rechazo para ADMINISTRADOR inactivo. Hashes y detalle: `TEST_PLAN.md`. |
| Focal post-hallazgos: `.\mvnw.cmd -B -ntp test '-Dtest=ReporteAsistenciaTitularidadHttpTest,ReporteAsistenciaAdministradorAccessHttpTest,GenerarReporteAsistenciaUseCaseImplTest,ReporteAsistenciaControllerTest' '-Djacoco.skip=true'` (JDK 25.0.2, 2026-10-30 19:24 -05:00) | **PASS** — exit 0; 28 tests, 0 failures, 0 errors, 0 skipped | 13 UseCase, 2 administrador HTTP, 3 controller/XLSX y 10 titularidad HTTP. Incluye roles vacíos y fila con once valores nulos. |
| Primer `.\mvnw.cmd -B -ntp verify` sandboxizado (JDK 25; 2026-10-30 19:33 -05:00) | **FAIL por restricción del sandbox** — 2709 tests, 0 failures, 121 errors, 0 skipped | `SecurityBaselinePepWiringTest` no pudo abrir loopback (`SocketException: Permission denied: connect`). No se usa como PASS ni se generó cobertura válida de esta corrida. |
| Repetición autorizada: `.\mvnw.cmd -B -ntp verify` (JDK 25.0.2; terminó 2026-10-30 19:40:03 -05:00) | **PASS** — exit 0; 2708 tests, 0 failures, 0 errors, 0 skipped; 452 suites XML; ArchUnit PASS; `BUILD SUCCESS` en 6:33 | Suite backend completa en la corrida nueva; enforcer JDK 25 y controles JaCoCo completados. Los reportes Surefire actuales son de esta ejecución. |
| JaCoCo `target/site/jacoco/jacoco.xml` de la corrida exitosa | **PASS** — LINE 13,247/14,379 = **92.13 %**; BRANCH 3,493/4,219 = **82.79 %** | Gates LINE ≥80 % / BRANCH ≥70 % cumplidos; Maven registró “All coverage checks have been met” sobre 1314 clases. |
| `git diff --check` (posterior a los cambios de prueba) | **PASS**, exit 0 | Sin errores de whitespace; Git emite únicamente aviso de normalización CRLF/LF de `LINEA_BASE.md`. |

El RED inicial fue una falla esperada que acredita el defecto antes de producción; no forma parte del resultado GREEN. Los valores 2705/92.12 %/82.70 % del registro anterior fueron reemplazados por los de la corrida nueva; no se reutilizan como revalidación. La primera corrida completa sandboxizada sí se registra como FAIL de entorno, y la repetición con loopback autorizado es el resultado PASS.

## Contrato, archivos y entorno

- Sin cambio a `SecurityConfig`, OpenAPI canónico, SHA del OpenAPI, URL, nombre/contenido XLSX, esquema/SQL, `InstitutionalScopePort`, adaptador JPA, frontend, CI o `pom.xml`.
- Archivos de producción: `GenerarReporteAsistenciaInputPort.java`, `GenerarReporteAsistenciaDTO.java`, `GenerarReporteAsistenciaInteractor.java`, `ReporteAsistenciaMapper.java`, `GenerarReporteAsistenciaUseCase.java`, `GenerarReporteAsistenciaDomain.java`, `GenerarReporteAsistenciaUseCaseImpl.java`, `ReporteAsistenciaController.java`, `ReporteWiringConfiguration.java`.
- Pruebas: `GenerarReporteAsistenciaUseCaseImplTest.java`, `ReporteAsistenciaControllerTest.java`, `ReporteAsistenciaTitularidadHttpTest.java`, `ReporteAsistenciaAdministradorAccessHttpTest.java`.
- Documentación: `PLAN.md`, `TEST_PLAN.md`, este `VALIDATION.md`, `HTTP_AS_IS_MATRIX.md`, `docs/work-items/README.md`, `docs/baseline/LINEA_BASE.md`.
- Conteo actual FIX-002: 11 archivos ya versionados modificados y 8 archivos nuevos sin seguimiento = **19 archivos**. No se añadieron archivos fuera del alcance. La edición de cierre añadió métodos a los tests existentes y actualizó solo este `TEST_PLAN.md` y `VALIDATION.md` dentro del work item.
- Work item permanece `IN_PROGRESS`, pendiente de revisión/commit e integración. Sonar, CodeQL, CI remoto, `-Pintegration`, SQL Server real, IdP real y validación manual: **NOT_RUN**. Los HTTP tests usan decoder JWT controlado y mocks para los puertos secundarios; no certifican la firma real del IdP ni el comportamiento SQL desplegado.
- Evidencia FIX-003 y `.codex` — solo conteos, sin leer ni imprimir contenido: `git ls-files --others --exclude-standard` reporta 108 archivos; el inventario físico suma 116: 100 en `.codex/` y 16 en `evidence/`. De esos 16, ocho son archivos visibles para Git y ocho pertenecen al metadata de un `.git` interno, que Git no enumera como archivos sin seguimiento normales; dos de esos ocho también coinciden con reglas ignore (subconjunto, no cantidad adicional). No se modificaron. La diferencia histórica 108 vs. 116 era mezclar listado Git con recorrido físico que incluyó metadata interna.
- No se operaron contenedores, bases, datos ni volúmenes. La primera corrida completa fue bloqueada por loopback; la revalidación PASS usó autorización local para el mismo `verify`, sin activar integración SQL.
- La rama histórica conserva 6 commits exclusivos y no se eliminó. **No es elegible para eliminación** bajo la regla del repo hasta que la corrección reemplazante se integre a `develop`; puede permanecer como referencia histórica por ahora.

## Riesgos pendientes

`AuthorizationReportJpaParityIT` cubre `canDocenteAccessGrupo` y filas del reporte en paridad JDBC/JPA; no cubre `findRolesActivosByUsuario`, `isAdministradorActivo` ni `canCoordinadorAccessGrupo` (solo compara scope de coordinador por programa). El perfil requiere datasource externo y las variables `SPRING_DATASOURCE_URL`, usuario y clave no están configuradas en el entorno de esta corrida. No se identificó ni autorizó un SQL Server aislado; además, la documentación apunta a `gestionasistenciadb`, expresamente prohibida como disposable. Por seguridad, integración permanece **NOT_RUN**.

Los consumidores aún no se validaron en un entorno HTTP externo y faltan Sonar, CodeQL y CI remoto. Estos límites no cambian el resultado de los tests locales, pero deben resolverse mediante los gates normales antes de integración/producción.

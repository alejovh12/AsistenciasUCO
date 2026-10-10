---
status: BACKEND_ONLY_REVIEW_IN_PROGRESS
date: 2026-10-10
base_branch: jose-valencia/maint-003i-utc-v1-read-timezone-fix
scope: BACKEND_ONLY
---
# MAINT-003K — Cierre estricto del backend (sin frontend, sin limpieza DB)

## Alcance innegociable
Esta fase realiza exclusivamente **revisión, corrección y certificación del código de backend**. No tocar frontend, Redis, MinIO, despliegue DB principal, limpieza de datos, PR merge, ni cerrar ramas. Mantener provider de seguridad vigente hasta futura migración; lo que debe perdurar es la validación JWT, roles institucionales, ownership y RBAC, no refactorización específica de Keycloak.

## Evidencia actual
- Backend rama publicada `jose-valencia/maint-003h-provider-contract-impl` con HEAD `4a52b36` y MAINT-003I lectura v1 `jose-valencia/maint-003i-utc-v1-read-timezone-fix` con propuesta no certificada.
- Codex reportó Java25 Surefire 1499 PASS, Failsafe 196 PASS (0 skipped), JaCoCo 91,31% líneas/78,87% ramas y HTTP real UTC JWT 20/20,21/21. Fueron ejecutados sobre artefactos ANTERIORES al cambio MAINT-003I; NO certifican esta nueva corrección.
- GitHub remoto tiene PR #18/#19/#20 Draft y #21 abierto. No mezclarlos ahora; la revisión de branches/merges será fase posterior.
- En MAINT-003I, `UvSesionEntity` pasa `Date` → `LocalDateTime` con `@JdbcType(LocalDateTimeJdbcType.class)`, el mapper retorna el reloj literal y `CoreViewQueriesJpaParityIT` lee datetime2 como `LocalDateTime`. Hay test `SesionLocalClockProjectionTest`; MAINT-003K incorpora otro para preservar el tipo JDBC en el mapeo. **La corrección no ha sido ejecutada aquí**.
- En SQL Server `datetime2(7)` no contiene offset. Por eso el mapper de consulta nunca debe aplicar `Date.toInstant()` ni `ZoneId.systemDefault()`. El registro v1 carece de confirmación de procedencia, que es distinto de que la consulta deba alterar sus campos.

## Hipótesis de defecto prioritario
En JVM Bogotá, el modelo v1 `java.util.Date` + `toInstant().atZone(UTC)` transformaba 14:00 SQL en 19:00. V2 no debe verse afectada. Confirmar con SQL Server 2022 real y JDK25 en `UTC`, `America/Bogota` y `Europe/Berlin`, idealmente invocando REST y comparando con SELECT de la misma fila. Si el proveedor JDBC exige un mapping alternativo, corrígelo en misma rama, **sin volver a java.util.Date**, y conserva el contrato SQL literal.

## Gates de cierre (todos obligatorios)
1. Inventario `git status`, HEAD, JDK, perfiles Maven, `app.sesiones.v2.enabled` y conector SQL real. No reset/clean destructivo.
2. Unit Maven `./mvnw -B -ntp clean verify` con JaCoCo ON; ArchUnit, OpenAPI checks y hashes; línea >=80%, ramas >=70%. Reportar test names, total, failure/error/skip.
3. Integration Java `./mvnw -B -ntp -Pintegration verify` contra **DB D06+CC-003G-01** en contenedor aislado; 196 tests (o número real actualizado), 0 failures/errors y 0 skips focales; mantener `SqlStoredProcedureContractIT`, `UsuarioPasswordHashSqlServerIT`, `UsuarioPlanEstudioProvidersSqlServerIT`, sin tests desactivados.
4. Repetir lectura v1 (por sesión/grupo, misma SQL) con 3 zonas JVM y 100ns exactos; JDBC vs JPA; v2 GET Z confirmado/NULL indeterminado; POST/PATCH v2 y equivalencia Bogotá/Berlín.
5. Seguridad HTTP JWT real: 401 sin bearer, 403 rol incorrecto, 403 docente sin titularidad, docente dueño 2xx, actor solo de JWT, login SQL mínimo. No exigir mejoras del proveedor de identidad: revisar solo los contratos de seguridad independientes de él y corregir defectos existentes de seguridad que afecten el backend.
6. Revisar el flag `app.sesiones.v2.enabled`: OFF por defecto, comportamiento con token y sin token, startup fail-fast cuando DB incompatible, sin abrir endpoints v2 indebidamente. Verificar que OFF y filtro Spring Security devuelvan el status documentado o actualizar el contrato si 401 ocurre antes de MVC.
7. Detectar residuos UTC `Date/Timestamp` en *otros lectores de Sesion* y reportes/consultas. Solo corregir bugs demostrados mediante test, conservando v1 wire.
8. Actualizar matriz de contratos OpenAPI: 9 operaciones v1 invariantes + 4 v2; protección 405 PUT v2; guardar checksum correcto.
9. SonarCloud, CodeQL, Trivy/CI de la rama HEAD si hay PR/runner; estado `NOT_RUN` si no disponibles. No declarar GREEN remoto sin checks.
10. Preparar `VALIDATION.md` y `TEST_PLAN.md` con SHA exacto, evidencia reproducible, comandos, cifras reales, cambios, caveats, decisión `BACKEND_READY_TO_REVIEW` o `BLOCKED`. No merge, no cierre de ramas.

## Criterio de éxito
`BACKEND_LOCAL_GREEN` requiere Maven clean verify y integration completos, timezone regressions, seguridad, JaCoCo y OpenAPI con resultados reales. `BACKEND_CI_GREEN` solo tras ejecutar CI sobre SHA final, con checks requeridos y aprobación correspondiente. El siguiente módulo se considera fuera de alcance hasta que el backend cumpla esos gates.

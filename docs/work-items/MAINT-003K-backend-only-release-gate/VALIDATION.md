# MAINT-003K — Evidencia disponible

Fecha: 2026-10-10

## Verificación independiente efectuada
- GitHub: se comprobó que la rama `jose-valencia/maint-003i-utc-v1-read-timezone-fix` contiene `UvSesionEntity` con `LocalDateTime` y `@JdbcType(LocalDateTimeJdbcType.class)`, mapper que conserva la fecha, y corrección del oráculo de `CoreViewQueriesJpaParityIT`.
- MAINT-003K agregó una aserción unitaria de los tipos de mapeo JPA a `SesionLocalClockProjectionTest` para que una futura regresión a `java.util.Date` sea detectable. Los scripts/documentos de otros dominios no se cambiaron.
- `SesionJpaRepository` continúa diferenciando v1 y v2 por `ContratoHorarioSesion`; solo la ruta v2 formatea literal UTC exacto a SQL y selecciona `usp_*_sesion_v2`. El flujo v1 conserva sus llamadas y payloads.
- Seguridad HTTP usa `JwtDecoder` y `JwtClaimsExtractor` abstraídos en SecurityConfig, RBAC DOCENTE y comprobación contextual; no se propuso migrar Keycloak en esta fase.

## NO ejecutado / NO certificado
- Maven JDK 25: NOT_RUN en esta revisión.
- SQL Server 2022 real + Keycloak: NOT_RUN en esta revisión.
- Cobertura JaCoCo del nuevo HEAD: NOT_RUN.
- CI remoto, SonarCloud, CodeQL y Trivy para el nuevo HEAD: NOT_RUN.
- PR abierto/merge a develop/deploy: NO.

## Resultados de una rama anterior (reportados por Codex, no certificados para MAINT-003K)
Surefire 1499 PASS; Failsafe 196 PASS sin skips; JaCoCo 91.31% líneas y 78.87% ramas; E2E UTC JWT 20/20 o 21/21 según timezone. Al cambiar el mapper v1, **estos resultados deben repetirse** y no deben etiquetarse GREEN de la rama actual.

## Resultado
`MAINT_003K_STATUS=CODE_REVIEW_AND_REGRESSION_TEST_ADDED_NOT_VERIFIED`.
Siguiente acción exclusivamente backend: Codex ejecuta todos los gates de `PLAN.md` y `TEST_PLAN.md`, corrige fallos concretos sin debilitar seguridad ni borrar tests y sustituye esta sección con evidencias de las ejecuciones reales. Nada de frontend, Redis, MinIO o DB reset en esta fase.

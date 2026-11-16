---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# VAL-oleada8-backend - PLAN

Fecha: 2026-11-15. Rama `fix/VAL-oleada8-backend` desde `repo-antiguo/develop` (5b1a9cd9), en un worktree aparte porque los
checkouts principales tienen trabajo en curso (`feat/activacion-cuentas`). Origen: pendientes del backend en
`informes/LISTA_CHEQUEO_VALIDACION_2026-11-12` (estado al 2026-11-13) que no dependen de Azure, del PDP compartido ni de staging.
No se cruza con `fix/VAL-oleada7-backend` (PEP y cachés de catálogos). Sin cambios de contratos, de BD ni de frontend.

## Pasos

| Ítem | Hallazgo | Cambio | Archivos |
| :--- | :--- | :--- | :--- |
| EST-02 | Sonar `javabugs:S2259` en `SecurityErrorResponseWriter.java:49`: `storeErrorCode` tolera una petición nula y luego `safePath` la desreferencia | RED con petición nula (NPE); `safePath` devuelve `""` si la petición es nula | `SecurityErrorResponseWriter`, `SecurityErrorResponseWriterTest` |
| F12-07 | `server.port: 8080` literal | RED en `OperationalReadinessConfigurationTest`; `server.port: ${SERVER_PORT:8080}` | `application.yml`, `OperationalReadinessConfigurationTest` |
| PRU-17 | Sin prueba de token expirado, `nbf` futuro ni firma alterada en el modo baseline (el de por defecto; Keycloak ya la tenía) | Prueba de composición real del `JwtDecoder` baseline con clave RSA local: válido, expirado, `nbf` futuro, carga alterada, firma truncada, otra clave, `alg: none`, otro emisor y otra audiencia. El 401 de `BadJwtException` ya lo cubre `SecurityConfigTest` | `BaselineJwtDecoderCompositionTest` (nueva) |
| OBS-06 | `SensitiveDataSanitizer` no oculta correo, documento ni nombre | RED; usuario del correo enmascarado (`j***@dominio`), valor de llaves exactas de dato personal (`correo`, `email`, `documento`, `numeroDocumento`, `identificacion`, `cedula`, `nombre(s)`, `apellido(s)`, `nombreCompleto`) como `[DATO_PERSONAL]` en texto, JSON y metadatos de auditoría. Llaves como `nombreMateria` no se tocan. Cuantificadores posesivos (sin ReDoS) y prueba de 90 000 caracteres | `SensitiveDataSanitizer`, `SensitiveDataSanitizerTest` |
| PRU-11 | Ninguna prueba del backend cita el tope de 20 tomas | IT sobre SQL real por los puertos reales: la toma 20 abre, la 21 da `TOM_004` (409) sin crear fila | `TomaAsistenciaBordesSqlServerIT` (nueva) |
| PRU-12 | Sin prueba de carrera de tomas en el backend | Misma IT: dos cierres simultáneos (barrera de dos hilos) cierran una vez; lote y cierre simultáneos sin excepción técnica ni detalles duplicados | `TomaAsistenciaBordesSqlServerIT` |
| F12-02 | Imágenes base del Dockerfile sin digest | RED en `OperationalReadinessConfigurationTest` (cada `FROM` con etiqueta y `@sha256`); `eclipse-temurin:25-jdk-alpine` y `25-jre-alpine` fijadas por digest (Dependabot actualiza ambos). Credencial de GitHub Packages documentada | `Dockerfile`, `OperationalReadinessConfigurationTest`, `docs/security/SECRETS_ROTATION_RUNBOOK.md` §6 |
| HEX-09 | La regla "no invocar `usp_*_interno`" estaba escrita pero nada la hacía cumplir | Prueba de fuente sobre `src/main/java` y `src/main/resources` (con aserción positiva de más de 100 referencias públicas) y referencia en el documento | `ProduccionSoloInvocaProcedimientosPublicosTest`, `docs/architecture/adapter-composition-standard.md` |
| F12-01 | Copia de `access-catalog.json` sin documentar | Se documenta que Infra no lo copia: genera `rutas-por-rol.md` y lo valida con `-Check` | `infra/security-baseline/README.md` |
| EST-14 | Herramientas de análisis estático sin declarar | Declaración de Sonar, CodeQL, ArchUnit, JaCoCo, Trivy, Gitleaks y Dependency Review; decisión de no agregar SpotBugs (propuesta, requiere visto bueno) | `docs/testing/STATIC_ANALYSIS.md` |
| ROL-12 | E2E de 5 roles en 17/18 y atado a cuentas `uco-*`, desactivadas desde la recarga de datos del 2026-11-15 | `E2E_USUARIO_<ROL>` y `E2E_USER_PASSWORD` opcionales; prueba offline | `infra/security-baseline/scripts/e2e_baseline.py`, `test_e2e_baseline_usuarios.py` |
| OBS-10 | Sin indicadores de Keycloak ni SMTP | `smtp` (`testConnection`) y `emisorIdentidad` (documento OIDC) en `/actuator/health`, fuera de `readiness` | `config/health/*HealthIndicator`, `CorreoTransport`, `JakartaMailCorreoTransport`, configuraciones SMTP, baseline y Keycloak |
| OBS-07 | Pocas métricas de negocio | `asistencias_negocio_eventos_total{tipo}` con lista cerrada de 11 hechos (tomas, QR, reclamos, sesiones, alertas) y panel "Negocio" en el dashboard | `ReactorRealtimeAdapter`, `MetricasNegocioRealtimeTest` |
| OBS-09, OBS-11, OBS-13 | Muestreo de trazas sin guía por ambiente; Grafana solo con el dashboard de Loki; README de observabilidad sin fecha nueva | Tabla de muestreo por ambiente; dashboard de métricas `asistencias-uco-metricas.json` (20 paneles); README fechado 2026-11-15 | `docs/observability/README.md`, `.env.example`, `infra/observability/grafana/provisioning/dashboards/asistencias-uco-metricas.json` |

## Fuera de alcance (con motivo)

| Ítem | Motivo |
| :--- | :--- |
| PRU-13 (backend) | El plazo del reclamo lo calcula la BD (`REV_020`); el backend solo traduce el código a 409 y eso ya está probado (`DbFailureClassifierReclamoRevisionTest`). Los bordes exactos están en las pruebas de BD y del frontend (oleada 4). |
| PRU-15 | `BLOCKED_BY_MISSING_EVIDENCE`: no existe una regla de "periodo cerrado" que rechace tomas, lotes o reclamos. `usp_ejecutar_cierre_masivo_periodo` solo pasa matrículas de A a F y audita; tampoco hay definición de qué debe pasar en un segundo cierre. Escribir la prueba sería inventar la regla: John debe decidirla. |
| OBS-04 | Fijar `APP_ADAPTERS_AUDIT_PROVIDER=sqlserver` es configuración de cada ambiente real, no un cambio de código. |
| REA-01, F12-06, REA-03, REA-04, F12-08, F12-05 | Trabajo grande o de otro frente (caché del PDP, bus distribuido del SSE, publicación de imágenes). |
| SEC-11 | Depende de Azure. |

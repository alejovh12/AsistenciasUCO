---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---

# SEC-002 — VALIDATION (precommit)

| Gate | Estado |
|---|---|
| Fuente CVE: SARIF PR16 | PASS (lectura de artefactos 11588914959 / 11590120454) |
| Confirmación Spring Boot 4.0.8 y dependencias administradas | PASS documental (documentación oficial Spring) |
| POM versión parent + Tomcat | PREPARED |
| Java25 Maven verify | NOT_RUN |
| Dependency tree actualizado | NOT_RUN |
| GitHub PR Backend CI | NOT_RUN |
| Trivy nueva imagen / repositorio | NOT_RUN |
| Integration SQL/MinIO/ClamAV | NOT_RUN |
| Resultado CVE final | NOT_CERTIFIED |

No declarar CVEs resueltos sin Trivy post-fix. No bajar policy de escaneo.

## Nueva adenda post-Trivy (2026-10-08)

Se completaron **Trivy FS e imagen** en HEAD `6d2d4b3`, run 37872449097. Conteos imagen: 24 (CRITICAL 1/HIGH 12/MEDIUM 11); `DS-0002` por root en Dockerfile del filesystem. Se proponen BOM Jackson 2/3 + BC 1.85.2 + USER no root. Ejecución de Maven/Java25/Docker tras este cambio: **NOT_RUN**; eficacia de CVE: **NOT_CERTIFIED** hasta SARIF nueva, startup y dependency tree. El scan no usa política fail-on-vulnerabilities aún; green no demuestra cero CVE.

## Tercer lote (CVE MEDIUM) — pendiente de CI del nuevo SHA

**Antes del cambio:** [run 37873166385](https://github.com/alejovh12/AsistenciasUCO/actions/runs/37873166385) PR #17 SHA `f9a08f4`: Backend CI, CodeQL/Dependency Review, Trivy Repo/Image y ArchUnit PASS; image SARIF **8 findings (CRITICAL 0, HIGH 1, MEDIUM 7)**. FS SARIF 7 findings. El único HIGH es MSSQL JDBC con versión de JAR aparente corregida, marcado INVESTIGATE, no como falso positivo confirmado.

**Cambio preparado (sin Java/SQL):** POI `5.4.1`, Commons Compress `1.28.0` managed, Alpine libpng/zlib upgrade en imagen runtime, guard Docker USER `10001:10001` en CI. `OpenTelemetry 1.55.0` pendiente de microfase, `HEALTHCHECK` LOW decisión requerida. **Post-change Maven/Trivy:** NOT_RUN al redactar, validar por nuevo GitHub SHA. Nunca declarar CVE CLOSED basándose solamente en un POM modificado.

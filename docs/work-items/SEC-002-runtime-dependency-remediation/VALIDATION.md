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

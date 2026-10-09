---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---
# SEC-001 — evidencia y estado

PREPARACIÓN: workflow y documentación redactados desde merge develop `551594179c2de582cbe62b4490276879d1ffc886`. Java25/Trivy/Docker **NO EJECUTADOS** por esta herramienta, aún no se dispone de resultados SARIF/CVE. Si el PR genera runs, registrar:
| SHA | Run | ArchUnit clases/tests/skips | Trivy FS findings | Trivy image findings | Verdict |
|---|---|---|---|---|---|
| PENDIENTE | PENDIENTE | NOT_RUN | NOT_RUN | NOT_RUN | NOT_CERTIFIED |

Criterio: no afirmar `TRIVY_PASS` solo porque los jobs estén verdes; salida `exit-code:0` significa inventario inicial. Registrar CVEs con ID, severidad, pkg/version, fixed version, incidencia y resolución. Si falla setup/download, no convertirlo en "0 hallazgos". La integración SQL/MinIO/ClamAV no se sustituye por Trivy/ArchUnit.

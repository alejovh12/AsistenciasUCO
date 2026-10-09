---
name: uco-quality-gate
description: Diagnosticar y recuperar Quality Gates de PRs con SonarCloud, JaCoCo, GitHub Actions, CodeQL y pruebas RED trazables.
---
# uco-quality-gate

Leer AGENTS.md, docs/governance/SOURCE_OF_TRUTH.md, docs/testing/TESTING_STANDARD.md, .github/CI.md y, para PR #15, docs/work-items/QUALITY-PR15-recovery/{PLAN,SONAR_TRIAGE,COVERAGE_MATRIX,TEST_PLAN,VALIDATION}.md.

## Obligaciones
- Anclar métricas a SHA, PR, run, fuente y fecha. Sonar New Code Coverage NO equivale a JaCoCo bundle; CodeQL PASS NO implica Sonar Security A.
- Recuperar issue key, rule key, type, severity, fichero/línea, causa y flujo antes de atribuir Vulnerability/Bug. Si ausente: BLOCKED_BY_MISSING_EVIDENCE.
- Java25 clean verify / Surefire unit no ejecuta automáticamente Failsafe *IT; los mocks sólo verifican lógica, no proveedor real ni SQL Server.
- Pruebas primero: requirement/contrato → test RED causal + snapshot → implementación sin modificar RED → GREEN → integración → CI remoto. Priorizar negativos y side effects por encima de cobertura cosmética.
- Gate PR #15 del 2026-10-08: Sonar new coverage >=80 %, Security A, Reliability A; JaCoCo global LINE >=80% BRANCH >=70%; revalidar thresholds en cada ejecución.
- PROHIBIDO bajar umbrales, exclusions, suprimir tests, cambios de quality profile o clasificar issues falsos sin análisis. No declarar PASS por reports viejos, skips, mocks ni jobs skipped.
- Auditor no corrige durante auditoría; si misma herramienta desempeña roles declarar independencia limitada.
- Q0 DOCUMENTATION_ONLY no autoriza cambios src/main, src/test, pom, CI ni contratos.

Salida: triage issues, matriz cobertura/tests, RED/Green vinculados, sha de fix, métricas reales, skips, evidencia local/remota y estado explícito PASS/FAIL/NOT_RUN.

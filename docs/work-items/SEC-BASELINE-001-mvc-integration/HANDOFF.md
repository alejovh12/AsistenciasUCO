# Handoff

## Entregables

- Rama experimental: `jose-valencia/experiment-security-baseline-mvc` (repositorio `alejovh12/AsistenciasUCO`).
- Historia sobre `develop` (`4082736`):
  1. `a74b288` — experiment(security): add opt-in MVC PEP enforcement.
  2. `83ab487` — docs(security): record experimental handoff blockers.
  3. `16621c7` — fix(security): close PEP bypass and harden MVC Security Baseline client (código validado).
  4. Commit de documentación que acompaña a este archivo (registro de la integración real).
- Worktree local: `.w/sec-mvc`. Laboratorio versionado: `infra/security-baseline-lab/`; runbook: `LAB_RUNBOOK.md`.
- La copia sin versionar `.workspace/security-baseline-local/release-assets/` queda como estado local histórico.

## Resultado

- MVC: filtro registrado una sola vez dentro de la cadena, fail-closed, deny-by-default (PASS real + simulado).
- PEP/PDP reales v0.1.6: decisiones ALLOW 204 / DENY 403 / 401 comprobadas con identidades sintéticas.
- E2E académico: `BLOCKED_BY_IDENTITY_CONTRACT` (issuer/claims incompatibles, ver `IDENTITY_MAPPING.md`).
- `verify` 1565/1565, ArchUnit PASS, JaCoCo LINE 90.74 % / BRANCH 80.64 %.

## No afirmar

- No afirmar compatibilidad E2E académica con Security Baseline.
- No afirmar que `sub` es `idUsuario`.
- No afirmar PASS de PDP caído ni de timeout del PEP real (simulados), CI remoto ni merge.
- No activar el flag en `develop` ni en ambientes compartidos.

## Próximo desbloqueo

1. Owner de identidad/seguridad: elegir y versionar una opción de `IDENTITY_MAPPING.md` (IdP común, claim institucional con procedencia o token exchange).
2. Equipo Security Baseline: recibir los `SECURITY_FINDING` de `RISKS.md` (200 sin decisión, secretos literales, plantillas 500, `environment` no vinculado).
3. Contratos: decidir `ACCESS_DECISION_UNAVAILABLE` (D-007) antes de cualquier merge.
4. Frontend / Security UI: ver `PLAN.md`, sección de pendientes.

No hay PR. Si se decide abrirlo como draft para revisión: `https://github.com/alejovh12/AsistenciasUCO/compare/develop...jose-valencia/experiment-security-baseline-mvc` (no fusionar).

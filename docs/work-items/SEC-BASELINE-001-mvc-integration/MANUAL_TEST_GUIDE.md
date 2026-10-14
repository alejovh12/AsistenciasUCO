# Guía de prueba manual

Procedimiento completo de laboratorio en `LAB_RUNBOOK.md`. Resultados de esta sesión en `VALIDATION.md` y `SECURITY_CONTRACT_MATRIX.md`.

## Preparación segura

1. `new-lab-env.ps1` genera secretos aleatorios en `%USERPROFILE%\.asistencias-security-lab\lab.env`; nunca copiar el `.env` institucional al laboratorio.
2. `docker compose ... config --quiet`, `pull`, `up -d --wait keycloak`, `rotate-lab-client-secrets.ps1`, `up -d --wait`.
3. `provision_lab_pdp.py` (idempotente).
4. `.\mvnw.cmd -B -ntp -DskipTests package` y `start-backend-pep-lab.ps1` (8083).

## Pruebas observables (solo GET)

| # | Prueba | Herramienta | Resultado de esta sesión |
|---|---|---|---|
| 1 | Decisiones reales PEP/PDP con identidades sintéticas | `lab_matrix.py pep` | 13/14; PEP-03 = hallazgo upstream |
| 2 | Backend con PEP ON: 401/503/públicas/preflight/SSE | `lab_matrix.py backend` | 10/10 |
| 3 | PEP detenido | `docker stop asistencias-security-lab-pep-1`; `lab_matrix.py backend-pep-down`; `docker start ...` | 4/4 |
| 4 | Flag OFF | arrancar sin `SECURITY_BASELINE_PEP_ENABLED` | cubierto por test de contexto |
| 5 | Allow + rol local / ownership | — | `BLOCKED_BY_IDENTITY_CONTRACT` |

Registrar URL, timestamp, correlation ID, status, código de error y decisión. Nunca token, Cookie, client secret, credencial de aplicación ni contraseña: los scripts los mantienen en memoria y en `lab.env`.

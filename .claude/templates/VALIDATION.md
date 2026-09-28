# VALIDATION — {ID}: {título}

## Dictamen

PASS / FAIL. Si el ambiente impide un gate: VALIDATION_BLOCKED_BY_ENVIRONMENT con causa exacta; no convertirlo en PASS. Identificar auditor y grado real de independencia.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | | |
| Arquitectura | ArchUnit/dependencias | | |
| Contratos | HTTP/DB/security/eventos | | |
| Tests | RED → GREEN, negativos | | |
| Coverage | Líneas ≥80 %, ramas ≥70 %; contadores | | |
| Integración | DB real, fixtures, skips | | |
| Build | mvn verify | | |
| Seguridad | Secretos, RBAC, scanners aplicables | | |
| Documentos | Enlaces, deuda, manuales, ADR | | |

## Integridad de RED

Hashes/commit comparados, cambios autorizados del tester y resolución de conflictos; ninguno si no aplica.

## Bloqueantes y limitaciones

## Evidencias remotas/manuales pendientes

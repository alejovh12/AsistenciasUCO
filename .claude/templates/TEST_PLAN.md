# TEST_PLAN — {ID}: {título}

## Fuentes

Requisito, PLAN, contrato aprobado/versionado, DB, seguridad y eventos aplicables.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|

Cubrir caso feliz, bordes, errores, seguridad, persistencia y contrato; marcar NO APLICA con motivo.

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES / NO
- ENVIRONMENT:
- MOCK_SUFFICIENT: YES / NO
- WHY:

## Negative / Boundary Coverage

## Side Effects / Rollback

## Pruebas RED requeridas

Archivo/test, fallo esperado por requisito, comando y resultado previo. No usar fallo ambiental como RED funcional.

## RED_SNAPSHOT

Obligatoria cuando aplica RED (NO APLICA justificado en documentación pura). El auditor compara este snapshot contra la versión usada para aprobar GREEN.

| Campo | Valor |
|---|---|
| Base commit | |
| Archivos de test | |
| SHA-256 o commit del estado RED | |
| Comando | |
| Exit code | |
| Fallo esperado | |

## Congelación

Revisor/decisión de aprobación, archivos + SHA-256 o commit de RED. Cualquier cambio posterior requiere TEST_CONTRACT_CONFLICT y resolución trazable del tester/contratos.

## Integración y E2E

Motor/fixture/versión, autorización del ambiente, rollback/cleanup y control de skips.

## Falsos positivos que deben evitarse

Negativos que refuten implementación incorrecta; no mock del SUT, assertions cosméticos ni expectativas copiadas de una solución terminada.

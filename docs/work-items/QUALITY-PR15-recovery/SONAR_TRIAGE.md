---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---
# QUALITY-PR15 — issues Sonar y security hardening

Fuente: https://sonarcloud.io/dashboard?id=alejovh12_AsistenciasUCO&pullRequest=15
Snapshot: New Code Coverage 48.4% (mínimo 80%), Security C (requerido A), Reliability C (requerido A). ISSUE_KEYS/RULES/LINES: NOT_RETRIEVED. No adjudicar issues por suposición. CodeQL Java PASS histórico no corrige Sonar.

## Procedimiento Q1
1. Filtrar issues abiertos de New Code en el PR y confirmar SHA, Bug, Vulnerability y Security Hotspot (proceso separado).
2. Guardar issue key, rule key, type/severity, path, line, mensaje, flujo de datos, causa reproducible y referencia a código.
3. Separar issue Sonar real de riesgo de code review. Si UI/API no devuelve detalle, registrar permisos/error como BLOCKED_BY_MISSING_EVIDENCE; no inventar.
4. Asociar RED, fix SHA, GREEN e issue resultante del siguiente análisis. No declarar C→A hasta nuevo Sonar PASS.
5. No cambiar exclusions/quality profiles, desactivar reglas/tests, bajar umbral ni marcar false positives sin evaluación legítima.

| Issue key | Regla | Tipo | File:line | Test RED | Fix | Verificación |
|---|---|---|---|---|---|---|
| PENDIENTE | PENDIENTE | Security C | no recuperado | NOT_RUN | ninguna | FAIL snapshot |
| PENDIENTE | PENDIENTE | Reliability C | no recuperado | NOT_RUN | ninguna | FAIL snapshot |

## Riesgos a investigar (NO Sonar issues confirmados)
- ClamAvMalwareScanAdapter: endsWith("OK") puede aceptar respuesta inesperada; validar protocolo exacto y fail closed.
- MinioFileStorageAdapter: transferTo(buffer) sin límite explícito al leer objeto; validar máximo real.
- CompressionPolicy.inflate: falta de límite de expansión; probar zip bomb sintético seguro.
- ArchivoController: Content-Disposition inline, cabeceras nosniff, nombre, content-type verificado.
- IT con valores dev por defecto: verificar si Sonar reporta secretos, sin imprimirlos.
- CoreViewJpaProjectionMapper String.valueOf(null) puede formar "null".
- ParameterCatalogJpaRepository Boolean.valueOf texto inválido transforma en false sin fallo: confirmar contrato.

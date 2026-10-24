---
status: active
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-24
---

# VALIDATION — HU154 + HU023 + HU151 + HU152

## Estado por capa (2026-10-24)

| Capa | Resultado | Evidencia |
| :--- | :---: | :--- |
| DB: pruebas T-SQL | PASS | 8 tests de catalogos (3 SP x 2 entidades + 2 contratos de vista) en `sql_integracion`; regresion `test_subject_catalog`, `test_reactive_academic`, `test_public_objects` PASS. Migracion `06` aplicada dos veces (idempotente). |
| Backend: `mvnw test` | PASS | Suite completa: 2096 tests, 0 fallos. |
| Frontend: `ng test` / `ng build` | PASS | 1064 tests, build OK. |
| E2E con JWT real | PASS | `scratch/test_catalogos_admin.ps1`: 57 verificaciones contra backend en :18082, Keycloak :8081, SQL Server :14332. Cubre RBAC (403/401), ciclo crear-renombrar-inactivar-reactivar, 400/404/409 con codigo y mensaje propios, idempotencia y `GET /tipos-programa` transversal. |
| `infra.ps1 verify` | No concluyente | Falla por `CONTRATO_FALTANTE` al ejecutarse desde el worktree (espera el layout canonico del workspace); no por cambios de este work item. |
| Cobertura de codigo nuevo (Sonar >= 80 %) | Pendiente medir | Medir antes del PR. |

## Aplicado a la BD compartida
En `sql_integracion` (:14332) se aplicaron solo objetos de este work item: mensajes del seed, migracion `06`, `uv_area`, `uv_tipo_programa` y los SP de tipos de programa y areas. Los registros `E2E *` creados por la prueba se eliminaron.

## Pendiente
1. Medir cobertura de codigo nuevo (Sonar) antes del PR.
2. Re-ejecutar `infra.ps1 verify` desde el workspace canonico tras integrar.
3. Deuda tecnica (D7): `usp_crear_asignatura` toma `TOP 1` area si no resuelve el nombre y no filtra por `estaActivaArea`.

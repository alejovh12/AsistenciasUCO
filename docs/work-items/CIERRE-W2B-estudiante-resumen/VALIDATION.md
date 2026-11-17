---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W2B - VALIDATION

Entorno desechable (sin tocar el laboratorio): SQL Server `sql_gate_w2b` con el esquema de la rama de BD `feat/cierre-w2b-estudiante-resumen` desplegado desde cero, MinIO y ClamAV propios sin volumenes con nombre.

| Compuerta | Resultado |
| :--- | :--- |
| BD: `deploy_schema.ps1` + fixture LB-004D + `test_summary.ps1` + `freeze-manifest.ps1 -Mode Verify` | DB GATE PASS: 917 aprobadas, 0 fallidas, 1 omitida permitida, 0 criticas faltantes |
| Backend: `mvnw clean verify -Pintegration` (con el merge de `develop` incluido) | BUILD SUCCESS: 4778 pruebas unitarias, 0 fallos; 365 de integracion, 0 fallos, 17 omitidas (las `assumeTrue` previas) |
| Contrato de SP (`SqlStoredProcedureContractIT`) y paridad JPA (`AcademicQueryJpaParityIT`) | verdes contra la BD de la rama |

Una corrida previa fallo en `JpaRepositoryBeanUniquenessTest` (69 esperados, 70 reales): el repositorio nuevo suma uno; el conteo documentado se actualizo a 70.

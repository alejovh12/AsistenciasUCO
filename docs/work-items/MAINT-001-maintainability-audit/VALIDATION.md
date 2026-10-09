---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-001 / PR #18 — validación

## Previo
PR #17 SEC-002 fusionado correctamente a `develop` en `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69`.
PR #18 `f86cb74e1abf033850cf7550b2152d8023949d80` anterior a esta adenda: Backend CI, Backend Security y Backend Deep Security Scan **SUCCESS** (GitHub Actions). Los tests nuevos del presente cambio **no** están incluidos en esos runs.

## Alcance de esta revisión
Se contrastó con `arquisoft-backend-develop(1).zip` proporcionado por el profesor, los contratos actuales JPA y las vistas SQL físicas de `johnjduque/gestion-asistencia-db` (develop). Lectura estática; no se ejecutó SQL Server, Docker ni Java 25 aquí.

- Adaptador paginado ya disponible sobre vistas: PASS lectura de código.
- HTTP overflow guard: CODE_PREPARED / TEST_NOT_RUN.
- Nuevos tests unit HTTP validator (3) + SQL Server IT (1 con 3 páginas): TEST_SOURCE_PREPARED, NOT_RUN.
- SQL test con fixture de identidad >10 y multigrupo: PENDIENTE evidencia real.
- Sonar, JUnit, ArchUnit, CodeQL, Trivy para SHA nuevo: NOT_RUN hasta Actions.
- DB migrations: NONE; contrato físico existente intacto.
- Riesgo detectado: test IT anterior compara todo un conjunto con página de 100; si fixture >100 requiere actualizar oráculo del test de forma contractual, no limitar los datos de producción.

## Cierre
No declarar nuevos tests GREEN antes de GitHub Actions. No hacer merge automático. La fase funcional DB para soportes LB-004 permanece independiente.

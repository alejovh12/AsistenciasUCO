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

## Ajuste adicional al oráculo SQL Server

En `CoreViewQueriesJpaParityIT.estudiantes_conservan_paginacion_detalle_contextos_y_not_found`, comparar los primeros 100 registros de la vista con la página `size=100`, conservando `assertEquals(before.size(), page.totalItems())`. El assert anterior comparaba erróneamente **todos** los registros con una única página y fallaría al superar 100 estudiantes. Se trata de un ajuste en la prueba que preserva la semántica, sin modificación de producción. La nueva prueba de páginas 0/1/2 cubre slicing por SQL Server real. Estado de nueva ejecución: NOT_RUN hasta nuevo run GitHub.

## 2026-10-09 — corrección P1: paridad no vacía y tres páginas reales

Revisión Codex detectó que `estudiantes_paginados_desde_vistas_sql_server_conservan_conteo_y_orden_en_tres_paginas` podía quedar verde con 0 filas y páginas vacías (oráculo SQL y JPA coincidían vacíamente). Se añadió **prerrequisito explícito >=11 identidades distintas** en la vista, tamaños esperados por página 0/1/2, recuento observado y no duplicación entre las tres páginas. Esto convierte la ausencia de fixture en fallo accionable, NO skip ni éxito falso. Requiere SQL Server aislado con al menos 11 estudiantes, uno multigrupo, antes de `-Pintegration`; no ejecutar sobre DB de producción, no añadir seeds a migraciones compartidas.

**Evidencia anterior suministrada por Codex**: clon `gestionasistenciadb_fase1` con 11 identidades, 12 contextos, una identidad en dos grupos; pruebas SQL PR18 192 IT / 0 errores / 0 skips, páginas 5/5/1 y 11 identidades únicas. La evidencia corresponde al SHA anterior `39e34c49` y no acredita todavía el commit P1. Ejecutar nueva suite en clon luego de revalidar fixture; mantener PR draft y sin merge hasta reportar nuevo SHA/GREEN.

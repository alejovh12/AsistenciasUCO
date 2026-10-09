---
name: uco-backend-pagination-utc
description: Workflow for SQL-view paging and UTC session contract validation against real SQL Server.
---

# Paginación por vistas + UTC: skill de integración real
1. Source truth DB en `johnjduque/gestion-asistencia-db`; no simular proveedor real con mocks para cierre E2E. 
2. Query estudiantes identidad 1:1 desde `uv_estudiante_identidad`, filtros `EXISTS uv_estudiante`, `COUNT`, `ORDER BY` estable, `OFFSET/FETCH` ejecutado en SQL Server.
3. Datos: fixture controlado >=11, multigrupo; no ejecutar DML en producción. Validar 3 páginas, resultados, conteo, RBAC, negativos, overflows y ausencia de duplicados.
4. SQL `Sesion.datetime2` = UTC por contrato, no contiene offset. En API legacy v1 `LocalDateTime` sin offset => ambigüedad; **no reinterpretar** sin ADR. Target v2 explicit RFC3339 offset + respuesta Z, sin reescritura de históricos.
5. Horarios académicos semanales (`dia` + `LocalTime`) son wall-clock locales y no instantes; requieren TZ institucional si se materializan en fechas.
6. CI Java25/ArchUnit/Sonar/Trivy ≠ prueba SQL real; `-Pintegration` debe mostrar executed/fail/skip y conexión de proveedor.
7. Documento final handoff requerido y aprobado antes de comenzar adaptación frontend. Ver `docs/work-items/MAINT-003-backend-utc-pagination-readiness`.

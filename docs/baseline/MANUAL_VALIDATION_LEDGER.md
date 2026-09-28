---
status: active
type: ledger
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Ledger de validaciones manuales

Único registro de validaciones externas/manuales pendientes. Ninguna fila afirma ejecución. Cuando se automatice, marcar AUTOMATIZADA y enlazar el test y su corrida; cuando pase, conservar evidencia sanitizada y fecha.

| ID | Fecha de registro | Escenario | Motivo/ambiente requerido | Evidencia y condición de cierre | Responsable por rol | Estado |
|---|---|---|---|---|---|---|
| MV-001 | 2026-09-20 | Golden Path frontend + Keycloak + SQL Server + SSE | Validación externa reportada en la autorización LB-001C.1; artefacto/log externo no está en este checkout | PASS reportado: Bearer, F5, horarios/grupos/sesiones/estudiantes, consulta+lote, AN/SJC/EX, SSE entre navegadores, offline→online y reconciliación HTTP; fixture temporal retirado. Límite: no hay enlace/log reproducible local, ver [ENTRY_EVIDENCE](../work-items/LB-001C-openapi-contract-first/ENTRY_EVIDENCE.md) | backend-team + frontend/DB | PASS_REPORTED_EXTERNAL (2026-09-24) |
| MV-002 | 2026-09-20 | Provisioning y roles con IdP real | Fake HTTP no certifica Keycloak real | DB-first, UUID/correo canónicos, roles, compensación interna y fallo tras commit; no exponer secretos | backend-team + identidad | PENDIENTE |
| MV-003 | 2026-09-20 | Logs/metrics/traces y Azure runtime del ambiente | Config/adapters y mocks no demuestran operación cloud | Key Vault real; parámetros y mensajes de App Configuration reales; Event Grid; invalidación y refresh observable de caches; correlationId/traceId/spanId y métricas aplicables; cero exposición de secretos; ambiente/fecha y evidencia sanitizada. Evidencia parcial LB-001D.2 (2026-09-26, `-Pazure-integration`, solo lectura vía Ports): Key Vault, parámetros y mensajes reales PASS; **no** cubre Event Grid real, invalidación/refresh, observabilidad completa ni evidencia operacional | backend-team + operaciones | PENDIENTE |
| MV-004 | 2026-09-20 | CI remoto y protección de ramas | Workflows no prueban Rulesets ni estado Sonar | Enlace a corrida, Quality Gate, CodeQL, Dependency Review y Rulesets aplicables | mantenedor del repositorio | PENDIENTE |
| MV-005 | 2026-09-24 | Swagger UI runtime sobre OpenAPI canónico | Render real del JAR local en navegador, sin depender de DB/IdP externos | PASS local: `/swagger-ui/` cargó 9 operaciones; PATCH expandible con parámetros/body/Execute, PUT deprecated, `operationId` y Authorize visibles; assets solo localhost; consola 0 warnings/errores; YAML HTTP `application/yaml` con SHA canónico MATCH. Swagger UI muestra un aviso no bloqueante por `jsonSchemaDialect` distinto del dialecto base; el contrato no se alteró. Servidor y mock OIDC efímeros retirados. Ver [LB-001C.3 VALIDATION](../work-items/LB-001C-openapi-contract-first/LB-001C.3-VALIDATION.md) | backend-team | PASS |

La integración SQL automatizada se registra en VALIDATION; solo una verificación humana inevitable va aquí. Una validación obligatoria pendiente impide DONE del alcance correspondiente.

---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Alineación del consumidor frontend

Referencia AS-IS del backend; el frontend no está incluido en este checkout. La comparación anterior se conserva como [histórica](../archive/frontend-contract-alignment.md) y no demuestra el estado actual del consumidor.

- Errores: `message` y `details`, según [contrato HTTP](../contracts/OPENAPI_STANDARD.md); no asumir `mensajeUsuario` como envelope de error.
- Identificación: requests existentes usan número compatible con Integer; migración futura TD-006 no autoriza cambiar JSON ahora.
- Asistencia: distinguir individual de lote. El [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) usa `sesionId`, `registros[{estudianteId, estado}]`; SQL espera otra forma serializada interna. No exponer `asistenciaJSON` por deducción de parámetros DB.
- Revisión: [SolicitarRevisionAsistenciaRequest](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/request/SolicitarRevisionAsistenciaRequest.java) tiene sesionId (alias sesion), categoria, justificacion (fallback motivo), soporteNombre y soporteUrl. La afirmación anterior `asistencia/motivo` estaba desactualizada.
- Queries GET y POST legacy: inventario en [HTTP_AS_IS_MATRIX](../contracts/HTTP_AS_IS_MATRIX.md). No retirar legacy sin consumidor/versionado y acuerdo.
- SSE: Bearer por header, grupoId, reconexión y recuperación de estado según [contrato realtime](../contracts/REALTIME_EVENT_STANDARD.md). No certificar actualización UI a partir de tests backend.

LB-001B debe obtener el repositorio/versión del consumidor y registrar compatibilidad, casos de error y tiempos. Sin ello: `BLOCKED_BY_MISSING_EVIDENCE` ME-002; seguimiento [TD-017](../baseline/TECHNICAL_DEBT.md#td-017) y [MV-001](../baseline/MANUAL_VALIDATION_LEDGER.md).

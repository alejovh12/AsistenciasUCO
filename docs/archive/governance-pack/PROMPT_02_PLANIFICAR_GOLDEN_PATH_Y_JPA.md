---
status: superseded
type: historical
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

> Histórico sustituido por [AGENTS](../../../AGENTS.md), [LINEA_BASE](../../baseline/LINEA_BASE.md) y [work-items](../../work-items/README.md). Sus prompts no se ejecutan; no autorizan JPA ni estado oficial en .workspace.

# Prompt — Planificar Golden Path + piloto JPA

Actúa como `1-planificador`. **No escribas código.**

Objetivo: producir el plan ejecutable para la primera migración JDBC -> JPA usando el Golden Path de
asistencia, preservando contratos.

## Alcance

1. Confirmar AS-IS de:
   - `POST /api/v1/asistencias/lote`;
   - `GET /api/v1/grupos/{grupoId}/asistencias`;
   - `GET /api/v1/realtime/stream`;
   - `AsistenciaRepositoryPort`;
   - `AsistenciaRepositorySqlServerAdapter`;
   - `usp_registrar_asistencias_sesion`;
   - vistas usadas por la consulta;
   - autorización docente/grupo;
   - frontend consumidor.
2. Crear inventario de contrato HTTP y DB.
3. Detectar qué parte del OpenAPI debe formalizarse antes de JPA.
4. Diseñar piloto JPA:
   - query primero;
   - SP por lote conservado;
   - entidades/proyecciones solo Infrastructure;
   - `ddl-auto=validate|none`;
   - `open-in-view=false`;
   - sin cambio de schema;
   - sin dual-write productivo.
5. Definir matriz de tests RED antes de implementación.
6. Definir parity tests JDBC/JPA.
7. Definir rollback.

## Restricciones

- no Big Bang;
- no reescribir SP en Java;
- no WebFlux en esta tarea;
- no cambiar SSE/provider realtime en esta tarea;
- no introducir Redis/Blob/IaC en esta tarea;
- no cambiar reglas de asistencia;
- cualquier dato no comprobable => `BLOCKED_BY_MISSING_EVIDENCE`.

## Salida

`.workspace/plans/PLAN-LB-001-JPA-GOLDEN-PATH.md` siguiendo `.claude/templates/PLAN.md`.

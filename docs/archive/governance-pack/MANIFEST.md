---
status: superseded
type: historical
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

> Histórico sustituido por [AGENTS](../../../AGENTS.md), [LINEA_BASE](../../baseline/LINEA_BASE.md) y [work-items](../../work-items/README.md). Sus prompts no se ejecutan; no autorizan JPA ni estado oficial en .workspace.

# Manifest

Paquete de arranque de gobernanza para AsistenciasUCO. Diseñado a partir del backend auditado y del
patrón de agentes/skills del backend de referencia Arquisoft, sin copiar su arquitectura de bounded
contexts ni reglas que no aplican a AsistenciasUCO.

## Archivos principales

- `AGENTS.md`
- `CLAUDE.md`
- `docs/ai/README.md`
- `docs/baseline/*`
- `docs/persistence/JDBC_TO_JPA.md`
- `docs/contracts/OPENAPI_STANDARD.md`
- `docs/testing/TESTING_STANDARD.md`
- `docs/adr/ADR-001*`, `ADR-002*`
- `.claude/skills/*`
- `.claude/agents/*`
- `.claude/templates/*`
- `PROMPT_01_INSTALAR_GOBERNANZA.md`
- `PROMPT_02_PLANIFICAR_GOLDEN_PATH_Y_JPA.md`

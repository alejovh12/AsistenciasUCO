---
status: superseded
type: historical
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

> Histórico sustituido por [AGENTS](../../../AGENTS.md), [LINEA_BASE](../../baseline/LINEA_BASE.md) y [work-items](../../work-items/README.md). Sus prompts no se ejecutan; no autorizan JPA ni estado oficial en .workspace.

# Prompt — Instalar gobernanza de agentes en AsistenciasUCO

Quiero que trabajes **solo sobre documentación y configuración de agentes**, sin modificar código
funcional Java, frontend ni base de datos.

## Objetivo

Integrar el paquete de gobernanza de IA de AsistenciasUCO al repositorio real para que futuras
tareas usen reglas versionadas en vez de prompts largos.

## Instrucciones

1. Lee `AGENTS.md` del paquete y el `docs/ai/README.md`.
2. Audita la documentación ya existente del backend antes de copiar nada.
3. Conserva como fuente técnica existente:
   - `docs/architecture/*`
   - `docs/security/*`
   - `docs/integration/*`
   - `docs/testing/*`
   - `docs/backend-baseline-contract.md`
4. Integra los archivos nuevos evitando duplicar reglas. Los nuevos archivos deben **referenciar**
   documentación existente, no reemplazarla sin razón.
5. Si ya existe `CLAUDE.md` o `AGENTS.md`, fusiona conservando una sola fuente de verdad y reporta
   conflictos.
6. No cambies endpoints, dependencias Maven, clases, tests, yml runtime ni CI en esta tarea.
7. No inventes una convención si el código/documentación actual contradice el paquete; registra el
   conflicto para revisión.
8. Crea las carpetas `.workspace/plans`, `.workspace/tests`, `.workspace/validation` con `.gitkeep`
   solo si el equipo decide versionarlas; de lo contrario añádelas al `.gitignore`. No decidas por
   tu cuenta: reporta ambas opciones.
9. Entrega un diff resumido con:
   - archivos añadidos;
   - archivos fusionados;
   - conflictos detectados;
   - reglas que quedaron como fuente de verdad;
   - próximos pasos.

## Gate

Esta tarea termina cuando un agente nuevo, leyendo únicamente `AGENTS.md` + una skill, puede saber:

- dónde va JPA;
- dónde no puede ir JPA;
- cómo se define un endpoint contract-first;
- cómo se escriben tests RED;
- cuál es el Golden Path;
- cuándo debe detenerse por falta de evidencia.

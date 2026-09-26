---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Política documental

Una regla tiene una fuente mantenida. Los índices y skills enlazan esa fuente; los work items registran evidencia, resultados y decisiones puntuales. No copiar arquitectura extensa a AGENTS, CLAUDE o skills.

## Clasificación

Frontmatter en documentos donde distinga autoridad: `status`, `type`, `scope: backend`, `owner: backend-team`, `last-reviewed: AAAA-MM-DD`. Owner identifica el rol responsable, no una aprobación humana ya obtenida. La revisión documental no certifica un despliegue.

- Tipos permitidos: `normative`, `active` (referencia AS-IS/índice), `runbook`, `historical`, `adr`, `ledger`, `work-item`.
- Estados: `active`, `draft`, `superseded`, `archived`.
- En ADR, el cuerpo distingue decisión aceptada/propuesta. En work items, READY/DONE/PASS son resultados de trabajo, no estados del frontmatter.
- Skills y definiciones de agentes usan su metadata `name`/`description`; no necesitan repetir el frontmatter documental. Plantillas son formularios, no resultados.

## Actualizaciones

Conservar documentación buena. Corregir AS-IS solo con ruta/símbolo/test comprobable. Si se mueve un documento, actualizar referencias y el [índice](../README.md). Trasladar deuda vigente al [ledger único](../baseline/TECHNICAL_DEBT.md), conservar IDs y enlazarlos desde el tema; los detalles técnicos del mecanismo siguen en su norma.

Roadmaps sustituidos y mapas de refactor van a [archive](../archive/README.md), marcados históricos y enlazados a su reemplazo. Ningún documento histórico cambia arquitectura activa. No mantener paquetes de instalación duplicados.

## Evidencias y trabajo local

Los planes, matrices RED, validaciones y cierres que justifican una tarea se versionan en [work-items](../work-items/README.md). Registrar fecha, commit/base, archivo/símbolo o comando, exit code, entorno, resultado y límite de la afirmación. Rutas a `target/` o logs locales pueden acompañar un resumen versionado, nunca ser su único contenido. No versionar tokens, `.env`, PII, dumps ni logs completos sin sanitizar.

`.workspace/` puede guardar borradores locales sin autoridad. `.claude/worktrees/`, checkpoints, mailbox, locks y memorias locales no son documentación ni evidencia normativa. Se ignoran; nunca se trasladan a docs.

## Separación de responsabilidades

Las [seis funciones](../../AGENTS.md) separan plan, contrato, pruebas, implementación, auditoría y cierre. El auditor emite hallazgos sin corregir. Si una misma herramienta ejecuta varias funciones, declarar esa limitación y separar sus pases; no simular un revisor humano o una auditoría externa. Se admite delegación solo dentro del alcance autorizado por la tarea.

Arquisoft aporta metodología, no arquitectura: ver [adaptación](REFERENCE_ADAPTATION.md).

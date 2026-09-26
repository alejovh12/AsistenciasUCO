---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Higiene del repositorio

- No versionar secretos (`.env`, tokens, claves, cadenas de conexión), `target/`, worktrees, logs ni dependencias temporales.
- No dejar scripts de auditoría descartables dentro de la entrega.
- `.workspace/` es scratch local sin autoridad. La evidencia permanente se traslada, resumida y sanitizada, a [work-items](../work-items/README.md).
- Los logs crudos se resumen/sanitizan y después se eliminan.

## ZIP o snapshot para compartir

Excluir como mínimo: `.git/`, `target/`, `.env`, `.idea/`, `.claude/worktrees/`, `.workspace/`, `.tmp-log-metadata/`, `logs/`, `uploads/`.

No excluir: `.claude/agents/`, `.claude/skills/`, `.claude/templates/`, `docs/`.

Ver también la [política documental](DOCUMENTATION_POLICY.md).

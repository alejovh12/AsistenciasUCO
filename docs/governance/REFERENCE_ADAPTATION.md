---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Adaptación del backend de referencia Arquisoft

El backend de referencia aporta una idea especialmente valiosa: **separar contexto estable de la
tarea puntual**. En vez de enviar un prompt enorme, usa un índice corto, skills especializadas,
agentes con responsabilidades separadas y plantillas verificables.

## Qué se adopta

- `CLAUDE.md`/archivo raíz como índice, no como tratado gigante.
- `.claude/skills/` como fuente operativa de reglas.
- agentes separados para planificar, implementar, probar y validar.
- templates de plan/validación.
- documentación humana más extensa fuera del prompt.
- validación explícita de arquitectura y convenciones.

## Qué se cambia para AsistenciasUCO

### Tests antes de implementación

En el referente el tester trabaja principalmente sobre una implementación ya creada. Para
AsistenciasUCO se endurece el flujo:

`PLAN -> CONTRATO -> TEST RED -> IMPLEMENTACIÓN -> VALIDACIÓN`.

El implementador no puede editar los tests RED. Esto evita diseñar tests retrospectivos que solo
confirman la solución escogida.

### No copiar bounded contexts ni estructura ajena

AsistenciasUCO es un backend único con estructura `application / infrastructure / crosscutting /
observability` y Composition Root propio. No se copian módulos Gradle, CQRS, catálogos, RabbitMQ ni
naming de Arquisoft si no existe una necesidad real.

### Persistencia condicionada por una BD no administrada por backend

La migración JPA respeta vistas/SP actuales y no introduce migraciones de schema desde la
aplicación.

### Golden Path único antes de expandir

La referencia tiene múltiples contextos maduros. AsistenciasUCO primero consolida una vertical:
registro de asistencia por sesión + consulta + realtime. Solo después replica el patrón.

## Principio final

Copiar el **método de gobierno** del referente, no su arquitectura de dominio. Toda regla incluida en
este paquete debe poder justificarse con el proyecto AsistenciasUCO o con un objetivo explícito de la
línea base.

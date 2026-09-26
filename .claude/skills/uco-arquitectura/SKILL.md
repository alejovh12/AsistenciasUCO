---
name: uco-arquitectura
description: Aplicar las reglas de capas, puertos, adapters, providers y Composition Root de AsistenciasUCO.
---

# uco-arquitectura

## Cuándo usar

Aplicar las reglas de capas, puertos, adapters, providers y Composition Root de AsistenciasUCO.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [backend-package-structure.md](../../../docs/architecture/backend-package-structure.md)
- [infrastructure-structure.md](../../../docs/architecture/infrastructure-structure.md)
- [adapter-composition-standard.md](../../../docs/architecture/adapter-composition-standard.md)

## Reglas obligatorias

Preserva la dirección de dependencias, separa capability/provider y audit/observability. Reutiliza puertos con consumidores reales. La selección tecnológica vive en Composition Root, nunca en Application, perfiles tecnológicos o Service Locator. No generar shared/helpers por una sola vertical.

## Archivos y cambios prohibidos

No introducir imports de Spring/JPA/JDBC/Reactor/Keycloak/Azure en Domain/Application; no editar reglas ArchUnit para ocultar una violación. Fuera del PLAN no tocar controllers, contratos, SQL ni pom.xml.

## Quality gates

ArchUnit, consistencia path/package y tests de wiring por provider; verify/coverage según DoD.

## Evidencia esperada

PLAN con paths existentes/propuestos, grafo de dependencias afectado, provider/config/fail-fast y VALIDATION de wiring/arquitectura.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.

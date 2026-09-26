---
name: uco-persistencia
description: Revisar persistencia JDBC y planificar o ejecutar un piloto JDBC a JPA cuando esté autorizado.
---

# uco-persistencia

## Cuándo usar

Revisar persistencia JDBC y planificar o ejecutar un piloto JDBC a JPA cuando esté autorizado.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [JDBC_TO_JPA.md](../../../docs/persistence/JDBC_TO_JPA.md)
- [ADR-002-jpa-incremental.md](../../../docs/adr/ADR-002-jpa-incremental.md)
- [repository-mock-inventory.md](../../../docs/integration/repository-mock-inventory.md)
- [GOLDEN_PATH_ASISTENCIA.md](../../../docs/baseline/GOLDEN_PATH_ASISTENCIA.md)
- [sqlserver-connection.md](../../../docs/integration/sqlserver-connection.md)
- [CONTRACT_ALIGNMENT_PROTOCOL.md](../../../docs/integration/CONTRACT_ALIGNMENT_PROTOCOL.md)

## Modos de trabajo

**A. CONTRACT ANALYSIS.** Puede inspeccionar `RepositoryPort`, adapter JDBC, SQL embebido, IT y schema/vistas/SP del repositorio DB, siguiendo el protocolo de alineación. No migra a JPA ni modifica código, tests ni SQL.

**B. PERSISTENCE MIGRATION.** Solo con contrato DB congelado, DoR READY, work item `PERSISTENCE_MIGRATION` autorizado y tests definidos.

Regla obligatoria: no iniciar JDBC → JPA sobre un contrato DB con `MISMATCH`, `DECISION_REQUIRED` o `BLOCKED_BY_MISSING_EVIDENCE` en la parte migrada.

## Reglas obligatorias

Carga puerto y adapter JDBC afectados, contrato DB liberado e IT. Migración incremental, query primero y SP complejo conservado inicialmente. RepositoryPort estable, @Entity solo Infrastructure; JDBC/JPA pueden coexistir temporalmente. Sin dual-write; paridad antes de retiro. Hibernate validate/none y open-in-view=false. Evitar N+1, EAGER como parche y cascadas generales; justificar native query y fronteras transaccionales.

## Archivos y cambios prohibidos

No modificar schema/SQL DB; no @Entity/JpaRepository/EntityManager en Domain/Application; no exponer entidades por HTTP; no cambiar contrato/RED ni agregar JPA/pom.xml fuera de un work item `PERSISTENCE_MIGRATION` READY.

## Quality gates

Integración real SQL Server, paridad query/command/errores/rollback y correlation; ArchUnit/verify/coverage. Mocks no certifican JPA real.

## Evidencia esperada

Firma DB con versión, fixtures, resultados observables equivalentes, configuración segura, rollback y selección por Composition Root.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.

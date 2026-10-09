---
status: active
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-10-05
---

# ADR-004 — Repositorios JPA directos con `@Repository`

- **Estado:** accepted
- **Fecha:** 2026-10-05
- **Work item:** [LB-008 / JPA-06A](../work-items/LB-008-jpa-only-persistence-migration/PLAN.md#jpa-06a--jpa-repository-architecture-simplification)
- **Refina:** [ADR-003](ADR-003-jpa-only-persistence.md)

## Contexto

La migración JPA-only dejó una cadena funcional pero sobrearquitecturada:

```text
Application Port -> XxxSqlServerAdapter -> XxxJpaCommandPersistence/XxxJpaQueryPersistence -> EntityManager
```

Los adapters son delegadores y cinco configuraciones registran manualmente 27 beans de repositorio.
La indirección no aporta traducción de protocolo ni selección tecnológica adicional.

## Decisión

Para persistencia JPA productiva de SQL Server, la implementación concreta del secondary port es un
`@Repository XxxJpaRepository` de Infrastructure. El repository recibe `EntityManager` por constructor,
contiene commands y queries de su capability e implementa directamente uno o varios ports cohesionados.

```text
Application Port -> @Repository XxxJpaRepository -> EntityManager -> Hibernate -> SQL Server
```

Spring descubre estos repositories mediante el component scanning estándar. La selección del provider se
conserva con condiciones declarativas cuando exista una alternativa real; no se crean beans manuales de
repositories. Configuración técnica de Hibernate/JPA y providers no persistentes permanece en Composition Root.

Los nombres `XxxSqlServerAdapter`, `XxxJpaCommandPersistence` y `XxxJpaQueryPersistence` se retiran cuando solo
representan la cadena anterior. `QueryRow` queda reservado para resultados compuestos que no corresponden 1:1 a
una entity de vista.

## Consecuencias y límites

- Domain/Application conservan puertos neutrales y no importan Spring/JPA/Hibernate.
- `@Repository` es obligatorio solo para repositories JPA productivos de Infrastructure.
- No cambian DB, SP, views, contratos, transacciones, UTC, nulls, ordering, seguridad ni HTTP.
- `TransactionOperations` de Grupo se conserva.
- `ProcedureResult`, mapper y validator permanecen.
- Auditoría/TD-010 y JPA-07 quedan fuera de JPA-06A.
- Adapters de otros providers/capabilities siguen las reglas generales de Composition Root; esta decisión no los
  auto-registra ni elimina su selección tecnológica.

## Enmienda de implementación (2026-10-06): repositories no `final`

El diseño original mostraba `public final class XxxJpaRepository`. La validación en Spring Boot real
(paridad SQL Server, `DecanoJpaRepository` primero en el arranque) demostró que no es viable: `@Repository`
activa la traducción de excepciones de persistencia, que envuelve el bean en un proxy, y Spring Boot usa CGLIB
por defecto (`spring.aop.proxy-target-class=true`). CGLIB no puede subclasear una clase `final` y el contexto
fallaba con `Cannot subclass final class ...XxxJpaRepository`.

- Decisión tomada: los 27 `XxxJpaRepository` son `public class` (no `final`). Sus métodos no se sobrescriben
  fuera del repository; la cohesión la garantiza el paquete y la ArchUnit.
- Descartado: `spring.aop.proxy-target-class=false` global. Cambiaría el proxying de toda la aplicación para
  resolver un caso local.
- Evidencia de regresión: `JpaRepositoryBeanUniquenessTest#repositories_jpa_son_proxiables_con_traduccion_de_excepciones_de_persistencia_y_cglib`
  (RED antes del cambio, GREEN después) y la paridad SQL Server real.

## Configuraciones de selector (JPA-07)

- `app.adapters.persistence.provider` / `APP_ADAPTERS_PERSISTENCE_PROVIDER` **RETIRADO** en JPA-07 (2026-10-06): era un
  enum de un solo valor (`SQLSERVER`) sin alternativa. Los repositories SQL Server JPA se descubren como `@Repository`
  normales, sin `@ConditionalOnProperty` de persistencia. No se mantiene una abstracción para una alternativa inexistente.
- `AuditEventJpaRepository` se descubre igual (`@Repository`); el selector de auditoría es `app.adapters.audit.provider`.
- `MessageCatalogJpaRepository` y `ParameterCatalogJpaRepository` conservan condiciones por provider
  (`AZURE`, `AZURE_APPCONFIG` son alternativas reales).


---
status: active
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-10-03
---

# ADR-003 — Persistencia JPA-only como TARGET

- **Estado:** accepted
- **Fecha:** 2026-10-03
- **STATUS:** accepted
- **SUPERSEDES_TARGET_OF:** [ADR-002](ADR-002-jpa-incremental.md)
- **Work item:** [LB-008](../work-items/LB-008-jpa-only-persistence-migration/PLAN.md)

## Contexto

[ADR-002](ADR-002-jpa-incremental.md) aceptó una migración incremental JDBC → JPA con convivencia temporal, selectores por provider y fallback JDBC. Esa decisión fue correcta para el piloto LB-002 y queda como evidencia histórica.

Tras reunión con el profesor se decidió que el TARGET ya no es la convivencia permanente. El backend debe persistir únicamente mediante JPA/Hibernate.

## Decisión

1. **TARGET:** JPA/Hibernate es la única API de persistencia que usa directamente el código productivo del backend.
2. **JDBC directo sale del código productivo.** No deben quedar `JdbcTemplate`, `NamedParameterJdbcTemplate`, `NamedParameterJdbcOperations`, `RowMapper`, `ResultSet`, `MapSqlParameterSource`, `org.springframework.jdbc` ni `java.sql.*` en los adapters/repositories, salvo excepción técnica demostrada y aprobada.
3. **Driver interno permitido.** `mssql-jdbc` permanece como transporte interno de Hibernate hacia SQL Server. Eliminar JDBC significa eliminar su uso directo por nuestro código, no el driver.
4. **Datasource.** `spring.datasource.*` se conserva porque JPA requiere un `DataSource`.
5. **Dependencias.** `spring-boot-starter-data-jpa` es la API principal. `spring-boot-starter-jdbc` se retira como dependencia directa cuando ningún código productivo lo requiera.
6. **Commands de stored procedures.** El patrón único TARGET es `EntityManager` + `createNativeQuery("EXEC dbo.usp_xxx …")` + parámetros nombrados + `getResultList()` + `ProcedureResultMapper` + `ProcedureResultValidator`. `StoredProcedureQuery` se conserva como historia del piloto LB-002 y no se usa como segundo patrón regular nuevo, salvo bloqueo técnico demostrado y una nueva decisión explícita.
7. **Vistas.** Se modelan como entidades de persistencia `@Entity @Immutable` solo en Infrastructure, consultadas con `EntityManager`.
8. **Schema.** El backend no administra schema: `ddl-auto` solo `none` o `validate`; `open-in-view=false`.
9. **Sin dual-write.** Un solo provider por operación en ejecución. La paridad se certifica antes de cualquier cambio de provider, y el retiro de JDBC ocurre solo después de certificar paridad.
10. **Arquitectura de repository.** [ADR-004](ADR-004-jpa-repository-architecture.md) refina la forma interna:
    los repositories JPA de Infrastructure implementan directamente los secondary ports con `@Repository`.

## Qué NO cambia

Esta decisión solo afecta la tecnología de persistencia. No modifica:

- Domain ni Application (puertos neutrales y casos de uso se mantienen).
- HTTP, OpenAPI, DTOs HTTP, roles, seguridad, ownership ni Keycloak.
- Contratos realtime y SSE.
- Semántica de los stored procedures, schema, vistas, funciones y seeds de la DB. La DB es de solo lectura para esta migración.
- Reglas de negocio.

## Consecuencias

- LB-002 permanece como evidencia histórica del piloto. No se reescribe.
- LB-004 queda pausada ([PAUSE](../work-items/LB-004-stateless-serverless-readiness/PAUSE.md)) sin revertirse.
- Los selectores `jdbc|jpa`, las exclusiones de auto-configuración del piloto y el fallback JDBC productivo son deuda de migración que se retira capacidad por capacidad y, como máximo, en JPA-07.
- Un SP que no pueda certificar paridad o atomicidad queda `BLOCKED_BY_MISSING_EVIDENCE` para ese procedimiento concreto, sin bloquear los demás.

## Criterio de salida

LB-008 se cierra solo con `DIRECT_JDBC_IN_SRC_MAIN = 0`, `HYBRID_JDBC_JPA_RUNTIME = 0`, `JDBC_FALLBACK_RUNTIME = 0` y `JDBC_PROVIDER_SELECTOR = 0`, con paridad certificada contra SQL Server real.

## Alcance de la aceptación

Decisión humana registrada en la sesión del 2026-10-03, posterior a LB-002. Autoriza planificar y ejecutar LB-008 por microfases. No autoriza cambios de DB, de contrato ni de Domain/Application.

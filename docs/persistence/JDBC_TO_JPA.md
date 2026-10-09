---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Estándar de migración JDBC -> JPA

## 0. Estado de la estrategia: AS-IS histórico/piloto vs TARGET actual

| Ámbito | Documento | Vigencia |
|---|---|---|
| **AS-IS histórico / piloto** (JDBC + JPA coexistiendo, fallback JDBC, migración incremental) | [ADR-002](../adr/ADR-002-jpa-incremental.md), secciones 2–4 y 6 de este documento | Evidencia de LB-002 (CLOSED / FROZEN). No se reescribe. |
| **TARGET actual: JPA-only** | [ADR-003](../adr/ADR-003-jpa-only-persistence.md) (`SUPERSEDES_TARGET_OF: ADR-002`), [LB-008](../work-items/LB-008-jpa-only-persistence-migration/PLAN.md) | Vigente. Reemplaza la convivencia permanente y el fallback JDBC como objetivo. |

Las reglas de convivencia, selectores, fallback y «JDBC aún no retirado» de este documento describen el piloto histórico. No son el TARGET. El TARGET actual es:

- JDBC directo fuera de `src/main` (`DIRECT_JDBC_IN_SRC_MAIN = 0`); el driver `mssql-jdbc` queda como transporte interno de Hibernate.
- Commands de SP, patrón único TARGET: `EntityManager` + `createNativeQuery("EXEC …")` + `setParameter(...)` + `getResultList()` + `ProcedureResultMapper` + `ProcedureResultValidator`.
- `StoredProcedureQuery` fue utilizado en el piloto LB-002. Es historia, no un segundo patrón regular nuevo; solo puede reconsiderarse por bloqueo técnico demostrado y nueva decisión explícita.
- Vistas: `@Entity @Immutable` solo en Infrastructure, consultadas con `EntityManager`.
- Sin selectores `jdbc|jpa`, sin hybrid adapters, sin fallback JDBC productivo.
- Configuración JPA estándar de Spring Boot, `open-in-view=false`, `ddl-auto` `none` o `validate`.

Mientras LB-008 no esté cerrada, el código real no está terminado: Asistencia ya es JPA-only (commands y queries, sin selectores ni híbrido) y el bootstrap JPA es el estándar de Spring Boot (JPA-02A); el resto del JDBC directo (44 archivos) sigue pendiente de su microfase.

Estrategia original del piloto aprobada en [ADR-002](../adr/ADR-002-jpa-incremental.md). No ejecutada en su totalidad; su implementación pertenece a LB-002, después de cerrar el contrato de persistencia del Golden Path (LB-001A).

## 1. Objetivo

Migrar persistencia sin reescribir reglas de negocio ni contaminar Clean Architecture. La primera
vertical es el Golden Path de asistencia.

## 2. Estado histórico auditado y AS-IS heredado

- Spring Boot 4.x / Java 25.
- Spring MVC + virtual threads.
- `spring-boot-starter-jdbc` activo.
- El estado auditado original no tenía `@Entity`, `JpaRepository` ni `EntityManager`. LB-002 incorporó posteriormente entidades JPA de Asistencia, `EntityManager` y un command piloto con `StoredProcedureQuery`; LB-008 debe normalizar ese piloto al TARGET actual.
- Los puertos de Application ya aíslan buena parte del acceso a datos.

## 3. Reglas obligatorias

### 3.1 Dependencias

Permitido:

```text
Infrastructure -> Spring Data JPA / Hibernate / jakarta.persistence
```

Prohibido:

```text
Application -> JpaRepository
Application -> EntityManager
Domain -> @Entity
Domain -> jakarta.persistence
```

### 3.2 Modelo JPA

Las entidades JPA son **modelos de persistencia**, no entidades de dominio. Deben vivir debajo de
`infrastructure/adapter/secondary/persistence/sqlserver/`.

No reutilizar objetos de Application como `@Entity` para ahorrar mapeo.

### 3.3 Schema

El backend no crea ni altera schema.

Configuración admitida:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

o `none` cuando `validate` no sea viable por vistas/SPs.

Prohibido: `update`, `create`, `create-drop`.

Obligatorio para el piloto aprobado (evitar Open Session in View):

```properties
spring.jpa.open-in-view=false
```

### 3.4 Commands

Un command ya encapsulado y validado en un stored procedure NO se reescribe en Java. En LB-008 se
invoca desde Infrastructure con el único patrón TARGET: `EntityManager`,
`createNativeQuery("EXEC dbo.usp_xxx …")`, binding nombrado, `getResultList()`,
`ProcedureResultMapper` y `ProcedureResultValidator`. No se permite SQL/SP en el Use Case.

`StoredProcedureQuery` pertenece al piloto histórico LB-002. No se incorpora en migraciones nuevas
ni se conserva en una capacidad cerrada, salvo bloqueo técnico demostrado y nueva decisión explícita.

### 3.5 Queries

Para listados:

- proyecciones cuando no se necesita materializar todo el grafo;
- `Pageable` para paginación;
- `Specification` solo para filtros dinámicos que lo justifiquen;
- evitar `EAGER` como solución a `LazyInitializationException`;
- detectar N+1 con integration tests o conteo de consultas cuando sea relevante.

### 3.6 Transacciones

Application no importa Spring Transaction. La frontera transaccional concreta pertenece a
Infrastructure. No introducir una transacción distribuida ficticia entre SQL y realtime.

El evento realtime se publica **después** de una persistencia exitosa. Si se requiere garantía
atómica SQL -> broker durable, eso es una decisión posterior (Outbox), no se improvisa en el piloto.

## 4. Estrategia de migración del Golden Path

### Paso A — Preparación

1. congelar contrato HTTP/OpenAPI;
2. congelar firma de `AsistenciaRepositoryPort` salvo hallazgo;
3. identificar todas las vistas/SP usados;
4. capturar tests de comportamiento JDBC actuales;
5. agregar dependencia JPA y config segura.

### Paso B — Query primero

Migrar `consultarAsistenciasPorGrupo` a una implementación JPA. La consulta debe producir exactamente
los mismos datos funcionales que la versión JDBC antes de añadir mejoras de filtros/paginación.

Si el contrato target incorpora paginación, se aprueba primero en OpenAPI y luego se implementa.

### Paso C — Command por lote

Migrar `registrarAsistenciasSesion` conservando `dbo.usp_registrar_asistencias_sesion`.

La serialización del payload JSON y los parámetros `idCorrelacion`/`idUsuarioEjecutor` deben mantener
la semántica actual. No cambiar firma del SP.

### Paso D — Paridad

Ejecutar ambos caminos en tests controlados sobre fixtures equivalentes, nunca dual-write sobre el
mismo request productivo.

El `PARITY TEST` es obligatorio antes de cambiar el provider: mismo fixture, misma semántica y misma proyección; baseline JDBC y candidato JPA se comparan field-by-field. Repository mocks, `EntityManager` mocks y H2 no certifican mappings/queries dependientes de SQL Server.

Comparar:

- status/errores funcionales;
- filas/resultados observables;
- rollback;
- correlation/audit;
- evento realtime solo después de éxito.

### Paso E — Retiro JDBC de la vertical

Solo después de paridad y gates verdes. El resto del backend NO puede permanecer en JDBC por estar fuera
del Golden Path: el Golden Path prioriza y valida, pero toda persistencia productiva converge a JPA
(LB-008, ADR-003). Lo no migrado todavía tiene microfase asignada en
[JDBC_RESIDUAL_INVENTORY](../work-items/LB-008-jpa-only-persistence-migration/JDBC_RESIDUAL_INVENTORY.md).

En el piloto histórico LB-002, commands y queries pudieron seleccionarse por separado y el selector
permitió rollback al provider JDBC. En LB-008 no se crean selectores ni fallback nuevos: el baseline
JDBC solo permanece durante la comparación controlada y se retira de la capacidad al certificarla.

### Alcance del piloto de query (LB-002.0, [DECISION](../work-items/LB-002-jpa-incremental/LB-002.0-DECISION.md))

LB-002.1 es una migración de tecnología de persistencia, no de arquitectura de autorización. Contra el freeze final de la DB (`develop`, code freeze `dcc69f19...`, contrato `45e48c5a...`):

- El piloto usa las vistas base congeladas `dbo.uv_detalle_asistencia`, `dbo.uv_asistencia` y `dbo.uv_estudiante_grupo`; **no** `uv_auth_*`.
- **No** establece `SESSION_CONTEXT` (estado por conexión física; su adopción exige work item propio con set/use/restore, limpieza ante excepción, pool, transacciones y tests de fuga).
- **No** consume `usp_consultar_grupos_paginado` ni añade paginación (DR-010 intacta).
- Autorización AS-IS: HTTP Security → Use Case → `InstitutionalScopePort` → Repository.
- Paridad en SQL Server real (`sql_server_asistencias`, `gestionasistenciadb`, con el freeze desplegado y `test_summary.ps1` PASS): mismo fixture, baseline JDBC vs candidato JPA.

## 5. Ejemplo TARGET, no archivos existentes ni nombres definitivos

```text
infrastructure/adapter/secondary/persistence/sqlserver/core/
  AsistenciaRepositorySqlServerJpaAdapter.java
  jpa/
    entity/
    repository/
    projection/
    mapper/
```

No crear un nuevo `shared:jpa` hasta tener al menos dos consumidores reales de una abstracción.

## 6. Prohibiciones

- Big Bang de todos los adapters.
- Cambiar JDBC -> JPA y SP -> Java en el mismo paso.
- `nativeQuery=true` para todo por comodidad.
- relaciones bidireccionales JPA sin necesidad.
- cascadas `CascadeType.ALL` por defecto.
- exponer entidad JPA desde controller.
- usar `Optional`/JPA types en Application Port si el contrato actual no lo requiere.
- modificar DB para "hacer que JPA funcione".

## Evidencia AS-IS y límites

[pom.xml](../../pom.xml) declara JDBC/SQL Server, Java 25, Spring Boot 4.0.6 y
`spring-boot-starter-data-jpa` (Hibernate administrado por Boot). Desde JPA-02A el bootstrap es el
estándar de Spring Boot, con un único `EntityManagerFactory` y `JpaTransactionManager`; Asistencia
usa JPA-only en commands y queries. JDBC directo permanece temporalmente en 44 archivos de otras
verticales, todos asignados a una microfase de LB-008. [Inventario](../integration/repository-mock-inventory.md)
y [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) enlazan puertos/adapters reales. Las firmas SQL
consumidas no prueban por sí solas el schema/SP liberado; sin ese contrato se aplica
`BLOCKED_BY_MISSING_EVIDENCE` a la capacidad concreta.

`OUTSIDE_GOLDEN_PATH != OUTSIDE_JPA_MIGRATION`: el Golden Path determina prioridad, evidencia y
orden, no qué código puede permanecer en JDBC. Todo acceso JDBC productivo debe migrar a JPA antes
del cierre de LB-008. La coexistencia solo es temporal durante la comparación controlada de una
microfase y nunca implica dual-write productivo.

---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Estándar de migración JDBC -> JPA

Estrategia aprobada en [ADR-002](../adr/ADR-002-jpa-incremental.md). No ejecutada; su implementación pertenece a LB-002, después de cerrar el contrato de persistencia del Golden Path (LB-001A).

## 1. Objetivo

Migrar persistencia sin reescribir reglas de negocio ni contaminar Clean Architecture. La primera
vertical es el Golden Path de asistencia.

## 2. Estado actual

- Spring Boot 4.x / Java 25.
- Spring MVC + virtual threads.
- `spring-boot-starter-jdbc` activo.
- No hay `@Entity`, `JpaRepository` ni `EntityManager` en el estado auditado.
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

Un command ya encapsulado y validado en un stored procedure NO se reescribe en Java durante el
piloto. Se puede invocar desde Infrastructure usando Spring Data `@Procedure` cuando la firma es
simple o `EntityManager`/`StoredProcedureQuery` cuando se necesita control explícito.

La elección debe justificarse en el plan. No se permite SQL/SP en el Use Case.

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

Comparar:

- status/errores funcionales;
- filas/resultados observables;
- rollback;
- correlation/audit;
- evento realtime solo después de éxito.

### Paso E — Retiro JDBC de la vertical

Solo después de paridad y gates verdes. El resto del backend puede seguir en JDBC mientras se migra
vertical por vertical.

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

[pom.xml](../../pom.xml) declara JDBC/SQL Server, Java 25 y Spring Boot 4.0.6; no declara JPA. [Inventario](../integration/repository-mock-inventory.md) y [Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md) enlazan puertos/adapters reales. Las firmas SQL consumidas no prueban schema/SP liberados; sin ese contrato: `BLOCKED_BY_MISSING_EVIDENCE`. Coexistencia temporal significa una implementación seleccionada por operación/vertical, nunca dos escrituras productivas.

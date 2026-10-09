---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Inventario de adapters de persistencia

## Linea base actual

SQL Server es el provider de persistencia. Asistencia usa JPA/Hibernate exclusivamente; las demás
verticales conservan JDBC de forma transitoria mientras avanzan las microfases de LB-008. Los
commands siguen ejecutando Stored Procedures y las queries siguen consumiendo Views. JPA-02A usa
el bootstrap estándar de Spring Boot y `JpaTransactionManager`.

## REAL / DEFAULT

| Feature | Puerto | Repository JPA (SQL Server, JPA-06A) | Contrato SQL |
|---------|--------|--------------------------------------|--------------|
| TipoIdentificacion | `TipoIdentificacionRepositoryPort` | `TipoIdentificacionJpaRepository` | `dbo.uv_tipo_identificacion` |
| Usuario | `UsuarioRepositoryPort` | `UsuarioJpaRepository` | `dbo.usp_sincronizar_usuario` (contrato consumido por Java) |
| Docente | `DocenteRepositoryPort` | `DocenteJpaRepository` | `dbo.uv_docente_identidad`, `dbo.uv_docente`; commands no disponibles sin contrato público |
| Grupo | `GrupoRepositoryPort` | `GrupoJpaRepository` | `dbo.uv_grupo`, `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` |
| Sesion | `SesionRepositoryPort` | `SesionJpaRepository` | Views/SPs de sesion disponibles estaticamente, pendiente validacion E2E |
| Asistencia | `AsistenciaRepositoryPort` | `AsistenciaJpaRepository` (LB-008, JPA-only) | SPs de asistencia disponibles estaticamente, pendiente validacion E2E |

## Mocks para testing

| Feature | Mock adapter | Registro esperado |
|---------|--------------|-------------------|
| Sesion | `SesionRepositoryMockAdapter` | `@TestConfiguration` o creacion directa en tests |
| Asistencia | `AsistenciaRepositoryMockAdapter` | `@TestConfiguration` o creacion directa en tests |
| Grupo | `GrupoRepositoryMockAdapter` | `@TestConfiguration` o creacion directa en tests |
| Usuario | `UsuarioRepositoryMockAdapter` | `@TestConfiguration` o creacion directa en tests |
| TipoIdentificacion | `TipoIdentificacionRepositoryMockAdapter` | `@TestConfiguration` o creacion directa en tests |

Los mocks ya no se seleccionan mediante un spring profile tecnologico. No deben tener
`@Component`, `@Repository`, `@Service` ni `@Profile`.

## Deuda pendiente

Estado único: [TD-008](../baseline/TECHNICAL_DEBT.md#td-008). Los mocks ya viven en `src/test/java/.../persistence/sqlserver/testdouble`; la antigua tarea de moverlos está superada. Esta inspección no certifica la DB desplegada.

Evidencia: [adapters reales](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core) y [test doubles](../../src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/testdouble).

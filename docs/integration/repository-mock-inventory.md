# Inventario de adapters de persistencia

## Linea base actual

La persistencia productiva/default usa SQL Server mediante Spring JDBC. Los commands se ejecutan por Stored Procedures y las queries por Views. No hay JPA, Hibernate ORM ni R2DBC en la linea base.

## REAL / DEFAULT

| Feature | Puerto | Adapter SQL Server | Contrato SQL |
|---------|--------|--------------------|--------------|
| TipoIdentificacion | `TipoIdentificacionRepositoryPort` | `TipoIdentificacionRepositorySqlServerAdapter` | `dbo.uv_tipo_identificacion` |
| Usuario | `UsuarioRepositoryPort` | `UsuarioRepositorySqlServerAdapter` | `dbo.usp_sincronizar_usuario_interno` |
| Docente | `DocenteRepositoryPort` | `DocenteRepositorySqlServerAdapter` | `dbo.uv_docente_identidad`, `dbo.uv_docente`, SPs internos confirmados |
| Grupo | `GrupoRepositoryPort` | `GrupoRepositorySqlServerAdapter` | `dbo.uv_grupo`, `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` |
| Sesion | `SesionRepositoryPort` | `SesionRepositorySqlServerAdapter` | Views/SPs de sesion disponibles estaticamente, pendiente validacion E2E |
| Asistencia | `AsistenciaRepositoryPort` | `AsistenciaRepositorySqlServerAdapter` | SPs de asistencia disponibles estaticamente, pendiente validacion E2E |

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

- Validar ejecucion/E2E de Sesion y Asistencia contra SQL Server.
- Mover mocks a `src/test/java` si se decide retirar definitivamente su presencia en `src/main/java`.

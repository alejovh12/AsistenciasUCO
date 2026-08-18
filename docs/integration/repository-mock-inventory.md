# Inventario de adapters de persistencia

## Linea base actual

La persistencia productiva/default usa SQL Server mediante Spring JDBC. Los commands se ejecutan por Stored Procedures y las queries por Views. No hay JPA, Hibernate ORM ni R2DBC en la linea base.

## REAL / DEFAULT

| Feature | Puerto | Adapter default | Contrato SQL |
|---------|--------|-----------------|--------------|
| TipoIdentificacion | `TipoIdentificacionRepositoryPort` | `TipoIdentificacionRepositorySqlServerAdapter` | `dbo.uv_tipo_identificacion` |
| Usuario | `UsuarioRepositoryPort` | `UsuarioRepositorySqlServerAdapter` | `dbo.usp_sincronizar_usuario_interno` |
| Docente | `DocenteRepositoryPort` | `DocenteRepositorySqlServerAdapter` | `dbo.uv_docente_identidad`, `dbo.uv_docente`, SPs internos confirmados |
| Grupo | `GrupoRepositoryPort` | `GrupoRepositorySqlServerAdapter` | `dbo.uv_grupo`, `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` |

## TEMPORARY / MOCK PROFILE

| Feature | Mock adapter | Profile | Motivo |
|---------|--------------|---------|--------|
| Sesion | `SesionRepositoryMockAdapter` | `mock` | No existe adapter SQL real validado todavia. |
| Asistencia | `AsistenciaRepositoryMockAdapter` | `mock` | No existe adapter SQL real validado todavia. |

Con profile normal/default, Sesion y Asistencia no exponen endpoints respaldados por memoria. Sus controllers y beans quedan disponibles solo con `spring.profiles.active=mock`.

## Mocks no productivos

`GrupoRepositoryMockAdapter`, `TipoIdentificacionRepositoryMockAdapter` y `UsuarioRepositoryMockAdapter` no estan registrados como beans productivos. Se conservan solo como apoyo de pruebas aisladas o desarrollo puntual.

## Deuda pendiente

- Implementar adapters SQL reales para Sesion en una tarea funcional separada.
- Implementar adapters SQL reales para Asistencia en una tarea funcional separada.
- No desplazar `GrupoRepositorySqlServerAdapter` con mocks en ningun profile productivo.

# Pendientes de arquitectura dependientes de DB

Este documento registra deuda tecnica real que requiere cambios coordinados con base de datos, Stored Procedures, Views o contratos persistentes.

## Identificacion

`numeroIdentificacion` permanece temporalmente como `Integer` en Java y `INT` en SQL. La migracion futura debe llevarlo a `String` / `VARCHAR` para soportar correctamente identificadores y posibles tipos alfanumericos.

## Codigos de resultado DB

Los Stored Procedures todavia no entregan un codigo de error estable y machine-readable. `DbFailureClassifier` puede necesitar clasificar algunos errores mediante mensajes hasta que los SP expongan algo equivalente a `codigoResultado`, ademas de `estadoResultado`, `mensajeUsuarioResultado` y `mensajeTecnicoResultado`.

Caso conocido: `usp_validar_tipo_identificacion_exista_por_id_interno` puede producir `GEN_001` con informacion que Java no puede clasificar semanticamente y termina en `ERR_DB_UNCLASSIFIED`. Debe existir un codigo DB estable para ese caso.

## Sesion

Sesion dispone de adapter SQL Server productivo implementado estaticamente
(`SesionRepositorySqlServerAdapter`) y configurado desde el Composition Root de persistencia.
Los mocks ya no se seleccionan mediante un spring profile tecnologico; quedan destinados a
testing/test configuration. Pendiente: validacion de ejecucion/E2E.

## Asistencia

Asistencia dispone de adapter SQL Server productivo implementado estaticamente
(`AsistenciaRepositorySqlServerAdapter`) y configurado desde el Composition Root de persistencia.
Los mocks ya no se seleccionan mediante un spring profile tecnologico; quedan destinados a
testing/test configuration. Pendiente: validacion de ejecucion/E2E.

## Estados de asistencia

Existe una incompatibilidad pendiente entre estados reales actuales `AN`, `SJC`, `EX` y logica/SP que historicamente esperaba `A`, `T`. Debe resolverse cuando se pueda modificar DB.

## Transactional Outbox

Transactional Outbox puede requerir nueva estructura DB. Queda documentado como evolucion dependiente de DB y no se implementa todavia.

## Auditoria: DML directo en AuditEventJdbcRepository

`infrastructure.audit.adapter.sqlserver.AuditEventJdbcRepository` ejecuta `INSERT`/`SELECT`
directos contra `dbo.AuditoriaEvento` en lugar de pasar por un stored procedure `usp_*` publico,
violando la regla "nunca DML directo" que respeta el resto de `persistence.sqlserver`. Requiere
crear `usp_RegistrarEventoAuditoria` / `usp_ConsultarEventoAuditoriaPorCorrelationId` (o
equivalente) y migrar la clase a `CanonicalStoredProcedureExecutor`. Ver detalle en
`docs/architecture/infrastructure-structure.md` (seccion "KNOWN ARCHITECTURAL DEBT").

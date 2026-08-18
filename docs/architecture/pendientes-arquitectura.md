# Foundation backend actual

## Arquitectura congelada

```text
Domain
   ^
Application
   ^
Infrastructure
```

El core actual es imperativo:

- Spring MVC.
- Virtual threads.
- Spring JDBC.
- SQL Server JDBC.
- Stored Procedures para commands.
- Views para queries.

WebFlux/Reactor quedan disponibles para adapters futuros de borde, como WebClient, WebSocket, SSE, eventos, notificaciones o streams. No se usan en domain/application ni en persistencia JDBC.

## Persistencia real default

| Feature | Estado default | Contrato |
|---------|----------------|----------|
| TipoIdentificacion | Real SQL | `dbo.uv_tipo_identificacion` |
| Usuario | Real SQL | `dbo.usp_sincronizar_usuario_interno` |
| Docente | Real SQL | `dbo.uv_docente_identidad`, `dbo.uv_docente`, SPs internos confirmados |
| Grupo | Real SQL | `dbo.uv_grupo`, `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente` |
| Sesion | Sin mock productivo | Pendiente adapter SQL real |
| Asistencia | Sin mock productivo | Pendiente adapter SQL real |

Sesion y Asistencia solo cargan mocks con `spring.profiles.active=mock`.

## Correlation ID

El correlation ID es infraestructura:

```text
HTTP -> CorrelationIdFilter -> CorrelationIdContext/MDC -> SQL Adapter -> @idCorrelacion
```

No existe `IdCorrelacion` en application/domain. Los DTOs, mappers, use cases y repository DTOs de application no transportan correlacion tecnica. `ApiErrorResponse.correlationId` se llena desde infraestructura.

## Registro de estudiante en grupo

Contrato HTTP:

```http
POST /api/v1/grupos/{grupoId}/estudiantes
```

El body no contiene `grupoId`; el controller une path + request mediante mapper de infraestructura hacia el input de application.

Flujo:

```text
Controller
-> Primary Port
-> Interactor
-> Use Case
-> GrupoRepositoryPort
-> GrupoRepositorySqlServerAdapter
-> TransactionTemplate
-> dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente
```

El typo historico `grupoEstaHablitado` solo vive en el adapter SQL y se publica en Java como `grupoHabilitado`.

## Excepciones

El flujo objetivo es:

```text
SQL Adapter -> ApplicationException -> GlobalExceptionHandler
```

Los use cases no interpretan `estadoResultado`, `mensajeUsuarioResultado`, `mensajeTecnicoResultado`, SQL, JDBC ni Stored Procedures.

`GlobalExceptionHandler` es el unico punto que traduce excepciones semanticas a HTTP. Los 500 usan mensaje seguro y no exponen mensajes tecnicos.

## Contrato DB de errores

El contrato real confirmado para SPs es:

```text
estadoResultado
mensajeUsuarioResultado
mensajeTecnicoResultado
```

No existe `codigoResultado`. La clasificacion por mensajes conocidos vive solo en `DbFailureClassifier`, dentro de infraestructura, y convierte fallos SQL a codigos internos `ERR_*`.

## Security Debt - Alta Prioridad

El contrato inspeccionado define `dbo.Usuario.password` como `nvarchar(500) NOT NULL`; no hay evidencia suficiente de hashing robusto en la frontera DB/backend actual.

No se corrige unilateralmente en esta foundation. Requiere coordinacion:

- Backend.
- DB.
- Autenticacion.

## Pendientes funcionales

- Implementar Sesion SQL real en tarea separada.
- Implementar Asistencia SQL real en tarea separada.

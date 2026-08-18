# Guia de validacion del backend

## Modelo actual

Las validaciones se distribuyen asi:

- HTTP request: Bean Validation para forma de entrada publica (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, tipos JSON).
- Application/domain: invariantes funcionales y normalizacion.
- Infrastructure adapter: contrato JDBC/SQL Server, transaccion y traduccion SQL -> `ApplicationException`.
- `GlobalExceptionHandler`: traduccion semantica a HTTP y respuestas seguras.

Los errores tecnicos de request, como JSON roto, UUID invalido, tipo JSON incorrecto, binding invalido o Bean Validation fallida, deben responder 400 con `ApiErrorResponse` seguro.

## Commands y queries

- Commands -> Stored Procedures.
- Queries -> Views.
- Grupo consulta `dbo.uv_grupo`.
- Registro de estudiante en grupo ejecuta `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente`.

## Registro de estudiante en grupo

Endpoint publico:

```http
POST /api/v1/grupos/{grupoId}/estudiantes
```

El `grupoId` viene solo por path. El body publico contiene datos personales:

```json
{
  "tipoIdentificacionId": "...",
  "numeroIdentificacion": 123,
  "primerApellido": "...",
  "segundoApellido": "...",
  "primerNombre": "...",
  "segundoNombre": "...",
  "correo": "...",
  "password": "..."
}
```

El request HTTP se mapea en infraestructura a `RegistrarEstudianteDTO`, que si contiene `grupoId` porque application necesita ese dato funcional. No se transporta `idCorrelacion` en application.

## Correlation ID

El flujo correcto es:

```text
HTTP -> CorrelationIdFilter -> CorrelationIdContext/MDC -> SQL Adapter -> @idCorrelacion
```

`CorrelationIdContext` vive en `infrastructure/correlation` y no se mueve a application. `getOrCreate()` cubre integration tests, jobs futuros y ejecuciones internas sin request HTTP.

## Mocks

Con profile default:

- TipoIdentificacion: SQL real.
- Usuario: SQL real.
- Docente: SQL real.
- Grupo: SQL real.
- Sesion: sin mock productivo.
- Asistencia: sin mock productivo.

Con `spring.profiles.active=mock`, Sesion y Asistencia pueden cargar sus mocks temporales para desarrollo/pruebas.

## Cobertura automatica clave

- Arquitectura: anti-Reactor en application/domain, anti-SQL-protocol en use cases, anti-correlation tecnica en application.
- HTTP Grupo: request valido 201, UUID invalido 400, JSON roto 400, tipo incorrecto 400, email invalido 400, requerido ausente 400.
- Correlation: header valido, header ausente, header invalido, MDC/context/header iguales durante request y limpieza final incluso con excepcion.
- SQL Adapter Grupo: SP correcto, parametros correctos, `@idCorrelacion` oficial, `estadoResultado`, clasificacion centralizada a `ERR_*`, `DataAccessException`.
- Integracion Grupo: rollback real sin Usuario/Estudiante/EstudianteGrupo/EstudiantePrograma parciales.

## Mensajes seguros

`ApiErrorResponse` no debe exponer:

- `mensajeTecnicoResultado`
- `technicalMessage`
- JDBC / SQL Server
- stacktrace
- password
- connection string

Los detalles tecnicos quedan solo en logs controlados de infraestructura.

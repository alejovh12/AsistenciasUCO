# Backend Package Structure

`primary` contiene entrada externa hacia el backend: controllers, filtros HTTP y seguridad de entrada.

`secondary` contiene salidas del backend hacia persistencia o sistemas externos.

`config` es el composition root: wiring de beans y configuracion de infraestructura.

`application` contiene casos de uso, primary ports, secondary ports, excepciones funcionales y catalogos de feature.

`crosscutting` contiene contratos y utilidades tecnicas realmente transversales.

`observability` contiene correlation, tracing y el modelo tecnico compartido de auditoria.

`repository` se organiza asi:

```text
application/secondaryports/repository/
    *RepositoryPort.java
    dto/
    projection/

infrastructure/adapter/secondary/repository/
    adapter/
    diagnostics/
    error/
    mapper/
```

`externalservice` existe solo cuando el sistema consume activamente un servicio externo de negocio.

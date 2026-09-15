# Backend Package Structure

Ver `docs/architecture/infrastructure-structure.md` para el árbol completo y las reglas de
organización (capability -> provider -> implementación, Application Port vs contrato interno de
Infrastructure, Composition Root, config/wiring vs config/adapters, audit vs observability). Este
documento resume solo la vista de alto nivel.

`primary` contiene entrada externa hacia el backend: controllers, filtros HTTP, la vertical
realtime SSE y seguridad de entrada (contract/handler/jwt).

`secondary` contiene salidas del backend hacia persistencia o sistemas externos, agrupadas por
capability (`persistence`, `identity`, `realtime`, `cryptography`) y luego por provider
(`sqlserver`, `keycloak`, `localsse`, `spring`).

`audit` contiene el registro de auditoría (quién hizo qué, cuándo, con qué resultado) como
concepto de negocio/cumplimiento independiente de la telemetría: `model`, `contract`,
`adapter/{logging,sqlserver}` y `web` (interceptor y advices HTTP).

`config` es el composition root: `config/adapters/*` selecciona la tecnología concreta por
capability (SQL Server, Keycloak, local-sse, password encoder, audit sink) vía
`app.adapters.<capability>.provider`; `config/wiring` ensambla UseCase + Interactor a partir de
Application Ports, sin conocer ninguna tecnología.

`application` contiene casos de uso, primary ports, secondary ports, excepciones funcionales y
catálogos de feature. No conoce Infrastructure.

`crosscutting` contiene contratos y utilidades técnicas realmente transversales:
`exception/{,catalog}`, `validation`, `sanitization`, `util`. No conoce Infrastructure.

`observability` contiene correlation y tracing (OpenTelemetry) - telemetría de plataforma, no
auditoría de negocio.

`persistence` (bajo `infrastructure/adapter/secondary`) se organiza así:

```text
application/secondaryports/repository/
    *RepositoryPort.java
    dto/
    projection/

infrastructure/adapter/secondary/persistence/sqlserver/
    core/           (*RepositoryPort SQL Server: asistencia, docente, estudiante, grupo, sesion,
                     tipoidentificacion, usuario)
    academic/       (adapters de solo-lectura sobre el catálogo académico)
    reporting/      (modelos de lectura especializados, p.ej. ReporteAsistenciaSqlServerAdapter)
    authorization/  (InstitutionalScopeSqlServerAdapter)
    support/
        error/      (traducción de excepciones SQL a errores de dominio)
        mapping/    (row mappers)
        procedure/  (ejecución canónica de stored procedures usp_*)
```

`externalservice` existe solo cuando el sistema consume activamente un servicio externo de
negocio.

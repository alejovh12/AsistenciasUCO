---
status: superseded
type: historical
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Documento sustituido

Fuente histórica: `docs/architecture/infrastructure-refactor-mapping.md`. No rige nuevas tareas. Sustituido por [infrastructure-structure.md](../architecture/infrastructure-structure.md).

# Mapa de migración: infrastructure-structure

Tabla ruta-anterior -> ruta-nueva de cada movimiento significativo del refactor estructural en
`refactor/infrastructure-structure`, con justificación. Útil para arqueología de commits /
cherry-pick futuro. No incluye renombres cosméticos de archivos de test que simplemente
acompañan a su clase de producción (se infieren de la fila correspondiente).

## Persistencia SQL Server (commit "consolidate SQL Server persistence adapters")

| Ruta anterior | Ruta nueva | Justificación |
|---|---|---|
| `infrastructure.adapter.secondary.academic.*SqlServerAdapter` | `infrastructure.adapter.secondary.persistence.sqlserver.academic.*` | Explicita `persistence` (capability) y `sqlserver` (provider); antes el provider solo aparecía en el nombre de clase, no en la ruta. |
| `infrastructure.adapter.secondary.report.ReporteAsistenciaSqlServerAdapter` | `infrastructure.adapter.secondary.persistence.sqlserver.reporting.ReporteAsistenciaSqlServerAdapter` | Es un modelo de lectura especializado sobre SQL Server, no una tecnología de "reporting" independiente. |
| `infrastructure.adapter.secondary.repository.adapter.{Asistencia,Docente,Estudiante,Grupo,Sesion,TipoIdentificacion,Usuario}RepositorySqlServerAdapter` | `infrastructure.adapter.secondary.persistence.sqlserver.core.*` | Elimina el anidamiento redundante `repository.adapter` (repository = Port, adapter = rol; ninguno de los dos decía la tecnología). |
| `infrastructure.adapter.secondary.repository.adapter.{Asistencia,Grupo,Sesion,TipoIdentificacion,Usuario}RepositoryMockAdapter` | `src/test/java/...infrastructure.adapter.secondary.persistence.sqlserver.testdouble.*` | Eran test doubles viviendo en `src/main/java`; solo se seleccionan desde `MockRepositoryTestConfiguration` (`@TestConfiguration`, `src/test/java`). Nunca fueron seleccionables desde producción, pero vivir en `src/main` los hacía parte del artefacto productivo igualmente. |
| `infrastructure.adapter.secondary.repository.error.*` | `infrastructure.adapter.secondary.persistence.sqlserver.support.error.*` | Utilidad de soporte específica de la capability persistence/sqlserver. |
| `infrastructure.adapter.secondary.repository.mapper.*` | `infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.*` | Idem. |
| `infrastructure.adapter.secondary.repository.procedure.*` | `infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.*` | Idem; ejecución canónica de `usp_*`. |
| `infrastructure.adapter.secondary.repository.{contract,diagnostics,integration}` (tests) | `infrastructure.adapter.secondary.persistence.sqlserver.{contract,diagnostics,integration}` (tests) | Mismo criterio, en el árbol de test. |
| `infrastructure.adapter.secondary.security.InstitutionalScopeSqlServerAdapter` | `infrastructure.adapter.secondary.persistence.sqlserver.authorization.InstitutionalScopeSqlServerAdapter` | Es SQL Server-backed (consulta `uv_*`), pertenece a persistence/sqlserver, no a un paquete `security` genérico que mezclaba tecnologías distintas. |
| `infrastructure.adapter.secondary.security.SpringPasswordEncoderAdapter` | `infrastructure.adapter.secondary.cryptography.password.spring.SpringPasswordEncoderAdapter` | Es criptografía (BCrypt vía Spring Security Crypto), no SQL ni HTTP; capability propia `cryptography.password`, provider `spring`. |
| `infrastructure.adapter.secondary.identity.KeycloakIdentityProviderAdapter` | `infrastructure.adapter.secondary.identity.keycloak.KeycloakIdentityProviderAdapter` | Explicita el provider (`keycloak`) en la ruta, igual que el resto del árbol. |
| `infrastructure.adapter.secondary.realtime.ReactorRealtimeAdapter` | `infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter` | Explicita el provider (`localsse`), consistente con `config.adapters.realtime.localsse` que ya existía. |

## Seguridad y realtime primarios (commit "split primary security ... and realtime into sse")

| Ruta anterior | Ruta nueva | Justificación |
|---|---|---|
| `infrastructure.adapter.primary.security.{ApiAccessDeniedHandler,ApiAuthenticationEntryPoint,SecurityErrorResponseWriter}` | `infrastructure.adapter.primary.security.handler.*` | Agrupa los `AuthenticationEntryPoint`/`AccessDeniedHandler`/escritor de respuesta de error, que son un sub-rol distinto de "resolver quién es el usuario" o "interpretar el JWT". |
| `infrastructure.adapter.primary.security.AuthenticatedUserProvider` (interfaz) | `infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver` | Renombrado: la interfaz **resuelve** el usuario autenticado desde `SecurityContext`, no "provee" ni selecciona una tecnología - "Provider" sugería (incorrectamente) un rol de selección de proveedor. |
| `infrastructure.adapter.primary.security.JwtAuthenticatedUserProvider` | `infrastructure.adapter.primary.security.jwt.SecurityContextAuthenticatedUserResolver` | Renombrado a juego con el Port; explicita que resuelve desde `SecurityContextHolder`, sin leer JWT directamente (lee el principal ya normalizado por el converter). |
| `infrastructure.adapter.primary.security.InstitutionalJwtAuthenticationConverter` | `infrastructure.adapter.primary.security.jwt.InstitutionalJwtAuthenticationConverter` | Movido (sin renombrar) al paquete `jwt`, que agrupa todo lo que interpreta/valida un `Jwt`. |
| `infrastructure.adapter.primary.security.spi.JwtClaimsAdapter` (interfaz) | `infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor` | Renombrado: "Adapter" no debe usarse como sufijo de interfaz (ver `InfrastructureStructureRulesTest`); el rol real es extraer claims. |
| `infrastructure.adapter.primary.security.keycloak.KeycloakJwtClaimsAdapter` | `infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor` | Renombrado a juego con el contrato; movido bajo `jwt.keycloak`. |
| `infrastructure.adapter.primary.security.validation.{AudienceValidator,RequiredUuidClaimValidator}` | `infrastructure.adapter.primary.security.jwt.validation.*` | Validadores de claims JWT, agrupados bajo `jwt`. |
| `infrastructure.adapter.primary.realtime.RealtimeStreamGateway` (interfaz) | `infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway` | Movido bajo `sse.contract`; sin renombrar (sigue siendo el contrato interno correcto). |
| `infrastructure.adapter.primary.realtime.RealtimeEventResponse` | `infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse` | DTO de respuesta SSE, agrupado en `sse.response`. |
| `infrastructure.adapter.primary.realtime.RealtimeStreamGatewayImpl` | `infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway` | Renombrado para explicitar el provider (`localsse`) en el propio nombre de la clase, igual que `ReactorRealtimeAdapter` en secondary. |
| `infrastructure.adapter.primary.controller.realtime.RealtimeEventsController` | `infrastructure.adapter.primary.realtime.sse.controller.RealtimeEventsController` | Saca el controller SSE del paquete genérico `controller` (compartido por todos los features REST) y lo agrupa con el resto de la vertical realtime. |

## Auditoría (commit "separate audit trail from observability")

| Ruta anterior | Ruta nueva | Justificación |
|---|---|---|
| `infrastructure.observability.audit.{AuditEvent,AuditActorType,AuditOutcome,RequestActor}` | `infrastructure.audit.model.*` | Audit es negocio/cumplimiento, no telemetría de plataforma (ver `infrastructure-structure.md` §7). |
| `infrastructure.observability.audit.AuditEventPublisher` (interfaz) | `infrastructure.audit.contract.AuditEventPublisher` | Idem; es el contrato que el resto de Infrastructure usa para publicar auditoría. |
| `infrastructure.adapter.secondary.audit.LoggingAuditEventPublisher` | `infrastructure.audit.adapter.logging.LoggingAuditEventPublisher` | Capability `audit`, provider `logging` (siempre activo). |
| `infrastructure.adapter.secondary.audit.AuditEventJdbcRepository` | `infrastructure.audit.adapter.sqlserver.AuditEventJdbcRepository` | Capability `audit`, provider `sqlserver` (sink durable opcional). Ver deuda arquitectónica documentada (DML directo, no corregido en este refactor). |
| `infrastructure.adapter.primary.controller.audit.{AuditableOperation,AuditInterceptor,AuditRequestAttributes,AuditRequestBodyCaptureAdvice,AuditResponseBodyCaptureAdvice}` | `infrastructure.audit.web.*` | La captura HTTP de auditoría es parte del mismo dominio `audit`, no del paquete genérico de controllers. |

## Crosscutting (commit "consolidate crosscutting helpers ... move tracing under opentelemetry")

| Ruta anterior | Ruta nueva | Justificación |
|---|---|---|
| `crosscutting.helpers.validation.{ValidationHelper,ValidationPatterns}` | `crosscutting.validation.*` | Misma responsabilidad que `ValidationErrorType`/`ValidationIssue`/`ValidationResult(Builder)`/`Validator`, que ya vivían en `crosscutting.validation`; existían dos paquetes de validación por accidente histórico. |
| `crosscutting.helpers.{NumberHelper,ObjectHelper,TextHelper}` | `crosscutting.util.*` | Utilidades genéricas sin responsabilidad de dominio ni técnica específica (a diferencia de `SensitiveDataSanitizer`, que sí tiene una responsabilidad técnica concreta y se queda en `sanitization`). |
| `infrastructure.observability.tracing.TraceContextSnapshot` | `infrastructure.observability.tracing.opentelemetry.TraceContextSnapshot` | Explicita el estándar (`opentelemetry`) igual que `audit.adapter.sqlserver` o `realtime.localsse`. |

## Wiring de features (commit "normalize feature wiring configuration naming")

| Ruta anterior | Ruta nueva | Justificación |
|---|---|---|
| `infrastructure.config.features.*` | `infrastructure.config.wiring.*` | `wiring` es el nombre que refleja "ensamblaje de UseCase+Interactor+Port", distinto de `config.adapters` (selección de tecnología); evita el nombre genérico `config.application` que sugeriría (incorrectamente) que Application vive ahí. |
| `AdminFeatureConfiguration` | `AdminWiringConfiguration` | Normaliza a `<Feature>WiringConfiguration`. |
| `AsistenciaBeansConfig` | `AsistenciaWiringConfiguration` | Idem. |
| `CoordinadorFeatureConfiguration` | `CoordinadorWiringConfiguration` | Idem. |
| `DecanoFeatureConfiguration` | `DecanoWiringConfiguration` | Idem. |
| `DocenteBeansConfig` + `DocenteFeatureConfiguration` (dos clases, beans disjuntos) | `DocenteWiringConfiguration` (una clase, mismos `@Bean`) | Consolidadas tras verificar cero solapamiento de beans entre ambas; Spring no distingue si los beans están en una o dos clases `@Configuration`. |
| `EstudianteBeansConfig` + `EstudianteFeatureConfiguration` | `EstudianteWiringConfiguration` | Idem. |
| `GrupoBeansConfig` | `GrupoWiringConfiguration` | Normaliza. |
| `ReporteFeatureConfiguration` | `ReporteWiringConfiguration` | Normaliza. |
| `SesionBeansConfig` | `SesionWiringConfiguration` | Normaliza. |
| `TipoIdentificacionBeansConfig` | `TipoIdentificacionWiringConfiguration` | Normaliza. |
| `UsuarioBeansConfig` | `UsuarioWiringConfiguration` | Normaliza. |

## No movido (evaluado y descartado deliberadamente)

| Candidato | Decisión | Razón |
|---|---|---|
| `infrastructure.config.security.SecurityConfig` | Se queda donde está | Es la composición del filter chain de Spring Security en sí (no selección de provider ni wiring de UseCase); no encaja en `config.adapters` ni en `config.wiring`. |
| `infrastructure.config.AuditWebConfig` | Se queda en `infrastructure.config` (solo se actualizó su import de `AuditInterceptor`) | Es wiring genérico de Spring MVC (registra un interceptor), no selección de tecnología de audit (eso ya lo hace `config.adapters.audit.AuditAdapterConfiguration`). |
| `infrastructure.config.adapters.persistence.sqlserver.SqlServerAuditSupportConfiguration` | Se queda donde está | Su condición de activación es `app.adapters.persistence.provider=sqlserver` (no `app.adapters.audit.provider`): pertenece a la capability `persistence`, no a `audit`, aunque construya un colaborador (`AuditEventJdbcRepository`) que ahora vive en `infrastructure.audit.adapter.sqlserver`. |
| `infra/observability/{compose.yaml,prometheus,loki,tempo,alloy,grafana}` (raíz del repo) | Sin cambios | Ya estaba razonablemente organizado; no había mezcla ni churn que justificara moverlo. |
| `infrastructure.config.FeaturesBeansConfigTest` | Sin renombrar | Prueba `SqlServerCoreRepositoryAdapterConfiguration` (Composition Root de adapters), no un wiring de feature; su nombre de archivo ya era engañoso antes de este refactor y no forma parte del mapeo de este PR (ver deuda arquitectónica documentada). |

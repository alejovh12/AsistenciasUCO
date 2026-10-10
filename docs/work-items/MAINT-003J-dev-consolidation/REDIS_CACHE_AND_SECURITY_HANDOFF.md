---
date: 2026-10-10
status: ARCHITECTURE_HANDOFF_DEFERRED
---
# Security provider neutrality + Redis catalog cache — próxima fase

## Seguridad (no invertir en migrar Keycloak ahora)
Fuente de autorización: rol institucional (`ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE, ESTUDIANTE`) y ownership real por `Usuario.id` UUID en DB. Mantener `Spring Security`, `JwtDecoder`, validación estricta `iss/aud/exp/firma`, `AuthenticatedUserResolver`, `InstitutionalScopePort`, `Resource Server` stateless. Keycloak hoy es adaptador de extracción de claims `KeycloakJwtClaimsExtractor`, y provisioning `IdentityProviderPort`; **no** alterar el modelo de seguridad al intercambiar provider. Congelar pruebas contractuales neutrales para roles, scopes, 401/403, sin usar claims de Keycloak en Application. Migración futura tendrá test E2E con nuevo issuer e IdP, sin aflojar las validaciones.

## Redis como caché distribuida
NO mover directamente `CatalogoMensajeUsuario`/`CatalogoMensajeTecnico`/`CatalogoParametro` de SQL a Redis como única fuente. Redis es almacén efímero/cache hasta ADR de durabilidad y recuperación aprobada. `uv_mensaje_usuario`, `uv_mensaje_tecnico`, `uv_parametro` conservan contrato; port `CatalogCachePort` no sabe de Redis y se implementa en Infrastructure.
- Keys: incluir namespace ambiente/versión de contrato/tipo/código/grupo, sin colisiones; revisar PR #19, que arregla colisiones de key en cache de parámetros.
- Loader: cache-aside con TTL explícito, `MISS`→lectura SQL→put, solo datos no sensibles; proteger stampede y evitar escritura inconsistente. Fail-soft **solo para cache**: si Redis falla, consultar SQL (los catálogos siguen operativos). Nunca devolver mensaje stale como autoridad de permisos.
- Invalidation: incorporar `fechaModificacion`/versión catálogo al payload o invalidación después de despliegues; cache preheat controlado; métricas hit/miss/evictions/latency; límites de memoria.
- Serialización versionada, datos serializados validados, no usar deserialización de clases arbitrarias ni almacenar JWT/contraseñas. TLS/ACL/secret externo para Redis; no permisos `FLUSHALL` para runtime.
- RED tests antes de integración: paridad SQL/Redis, TTL, invalidación, restart, dos instancias, caída de Redis y recuperación, carga concurrente, separación de ambientes/tenants y cambios de mensaje en caliente. No desplegar en MAINT-003J.

## MinIO (es provider vigente, no Azure Blob)
Se confirma `LB-004B.2` APPROVED: `FileStoragePort` + `MinioFileStorageAdapter` + `MinioStorageAdapterConfiguration` + `infra/files/compose.yaml`. Bucket privado `asistencias-soportes`; access keys de servicio separadas de root; prueba de policy; descarga vía backend con Bearer y ownership, sin URL pública ni `window.open` sin token.

Pendiente fundamental: `REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT` — referencia durable `fileId` a reclamo/revisión con owner, estado, tamaño/hash y auditoría. Validar upload→restart→download y dos instancias, permisos 403, antivirus/size/ZIP guardrails, fallo MinIO y rollback del provider. No eliminar volumen ni rehacer el storage provider. Documentación antigua sobre Azure Blob bajo `LB-004B.1` es **histórica superseded**, no target actual. MinIO Community del compose usa build del upstream archivado y está calificado `LOCAL_DEV_TEMPORARY`; no asumir proveedor productivo mantenido sin revisión.

## Frontend v2
Paquete de código separado debido a 403 de escritura al frontend. Integrar con flags OFF en DEV, ejecutar `npm run verify`, validar DST overlap con selectores de offset en formularios, fechas cross-midnight y GET after reload, 403 de owner, compatibilidad asistencia/reclamos. No mezclar rediseño de IdP/Redis con el PR frontend UTC.

---
date: 2026-10-10
status: DECISIONS_CONFIRMED_IMPLEMENTATION_PARTIAL
base: jose-valencia/maint-003i-utc-v1-read-timezone-fix
---
# MAINT-003J — Decisiones actualizadas: DEV DB, frontend UTC, seguridad, MinIO y Redis

## Nuevas decisiones explícitas del proyecto
1. Los datos actuales de SQL Server de desarrollo son desechables: **se permite limpiar y regenerar fixtures**, incluidas sesiones no confirmadas. Esto NO autoriza borrar recursos de staging, producción ni una DB de terceros. El inventario de FKs y el preflight son obligatorios; ver DEV_DB_RESET_AND_DEPLOY_RUNBOOK.
2. Persistencia temporal: **sesiones nuevas exclusivamente como instantes UTC normalizados en DATETIME2(7) con procedencia confirmada**, por rutas de v2 o generador institucional. NO reetiquetar un timestamp legacy como UTC_V2. La tabla sigue siendo `dbo.Sesion`, con columna `procedenciaTemporal`.
3. Los contenedores SQL de tests no contienen información que se deba copiar al servidor oficial: **desplegar mismos scripts sobre `sql_server_asistencias / gestionasistenciadb`** y crear fixtures directamente en DEV; mantener los clones hasta cierre y evidencia. No mezclar backups entre bases ni copiar volúmenes.
4. El proveedor de storage **es MINIO, no Azure Blob**. LB-004B.2 `PROVIDER_DECISION.md`, `MINIO_STORAGE_CONTRACT.md` y `infra/files/compose.yaml` ya lo señalan como APPPROVED. `MinioFileStorageAdapter`, `FileStoragePort` y tests existen. NO volver a implementar desde cero ni eliminar `asistencias-minio-data`. Se mantiene bucket privado, acceso mediado por backend y autorización.
5. Seguridad: futuro reemplazo del proveedor de identidad. Congelar **modelo institucional** `ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE, ESTUDIANTE`, principal `Usuario.id` UUID, JWT validado (issuer/audience/firma/tiempos), `InstitutionalScopePort` y RBAC/ownership de DB. No invertir en optimizaciones específicas de Keycloak hoy; su proveedor actual sigue protegido hasta el reemplazo. `JwtClaimsExtractor`/`IdentityProviderPort` son las fronteras que se reemplazarán sin alterar Application. Nunca desactivar JWT, RBAC o permisos para acelerar la migración.
6. Redis: se adopta como **caché distribuida futura** de parámetros y mensajes, no como sustituto automático de la fuente durable SQL. Decidir la fuente permanente mediante ADR posterior. Mantener catálogos SQL operativos hasta lograr pruebas de parity, invalidación, TTL y cold start; no activar Redis en MAINT-003J.
7. Frontend: migrar vistas docentes de sesiones a `/api/v2/sesiones` con feature flag OFF por defecto; browser IANA, DST gap/overlap y cross-midnight. V1 de estudiantes y Excel permanece hasta migración y pruebas funcionales.
8. LB-004: retomarlo **después de smoke DEV** de sesiones, asistencia, permisos y horario (v2), aprovechando MinIO existente. Binding persistente de metadata de reclamos en DB sigue siendo un gate independiente.

## Estado realmente observado en GitHub
- Backend `jose-valencia/maint-003h-provider-contract-impl@4a52b36` y DB `feat/cc-003g-01-public-user-plan-providers@3f6afec` publicados.
- Corrección v1 UTC de `maint-003i-utc-v1-read-timezone-fix` añadida como propuesta, **no ejecutada con Maven ni SQL** en esta auditoría.
- PR #18, #19 y #20 Draft; #21 abierto; no PR DB. No se hizo merge ni deploy a DB original.
- No se tuvo acceso a Docker/SQL Server/Windows del usuario desde esta revisión.
- Frontend `AsistenciasUCO/AsistenciasUCO-Frontend` permite lectura pero el intento de crear rama respondió 403; el código UTC v2 se entrega como zip independiente para aplicación vía Codex.

## Target final DEV
```text
Angular (timezone IANA) => Spring API (/api/v2/sesiones)
                        => puertos Application/JPA => dbo.Sesion (DATETIME2(7) UTC + procedencia)
                                          |         => RBAC y catálogos de SQL Server
                                          |
                                          +=> MinioFileStorageAdapter => MinIO privado
                                          
Futuro: CatalogCachePort => Redis distribuido (cache), SQL catálogos como fuente durable
Futuro: JwtClaimsExtractor nuevo provider, mismo principal y roles institucionales
```

No hay dos aplicaciones ni DB principales. Los contenedores de prueba se retiran **después** de certificar DEV (no en este PR), protegiendo volúmenes de MinIO, Keycloak y monitoreo.

---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# Inventario de secretos de ejecución

Origen: SEC-02 de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`, work item
[VAL-oleada3-baul-secretos](../work-items/VAL-oleada3-baul-secretos/PLAN.md). Este documento lista **nombres**,
nunca valores. La rotación de cada secreto está en el [runbook de rotación](SECRETS_ROTATION_RUNBOOK.md).

## Cómo se resuelven

| Proveedor del baúl (`app.adapters.vault.provider`) | Dónde se lee cada secreto |
|---|---|
| `azure_keyvault` (predeterminado de `application.yml`) | Al arrancar, `VaultRuntimeSecretsEnvironmentPostProcessor` lee del Key Vault (`AZURE_KEYVAULT_ENDPOINT`) cada secreto de la tabla, a través de `SecretVaultPort`, y lo antepone a las demás fuentes. **Si el secreto no existe en el baúl**, la propiedad cae a su variable de entorno o al secreto del orquestador (fallback). Si el baúl falla técnicamente (red, identidad, permiso), el arranque falla. |
| `local_env` (perfiles `local` y `dev` sin variables, perfil `integration`, CI) | Solo variables de entorno o secretos del orquestador, como antes. No se consulta Azure. |
| `azure_keyvault` con `APP_SECRETS_RUNTIME_FROM_VAULT_ENABLED=false` | Solo variables de entorno; el puerto del baúl sigue activo para los secretos que se leen en caliente (festivos). Uso: contingencia mientras se corrige la identidad o los permisos del baúl. |

La tabla de correspondencia vive en el código (`RuntimeSecretCatalog`) y la prueba `RuntimeSecretCatalogTest` falla
si este inventario no lista cada propiedad y cada nombre de secreto.

## Secretos que se leen al arrancar

Los nombres siguen la regla de Key Vault (letras, dígitos y guiones). Los dueños son roles del equipo; la persona
responsable hoy aparece entre paréntesis.

| Propiedad Spring | Variable de entorno (fallback) | Secreto en Key Vault | Dueño | Rotación |
|---|---|---|---|---|
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `asistencias-db-username` | Dueño de BD (repositorio DB) | Solo si cambia el login; va con la contraseña |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `asistencias-db-password` | Dueño de BD (repositorio DB) | 90 días, o de inmediato si se expone |
| `app.providers.keycloak-identity.admin-client-secret` | `KEYCLOAK_ADMIN_CLIENT_SECRET` | `asistencias-keycloak-admin-client-secret` | Dueño de seguridad (José Alejandro) | 90 días |
| `app.providers.security-baseline-identity.admin-client-secret` | `SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET` | `asistencias-baseline-identity-admin-client-secret` | Dueño de seguridad, con el equipo de Security Baseline (tercero) | 90 días |
| `app.providers.security-baseline-pdp-admin.client-secret` | `SECURITY_BASELINE_PDP_ADMIN_CLIENT_SECRET` | `asistencias-pdp-admin-client-secret` | Dueño de seguridad, con el equipo de Security Baseline | 90 días |
| `app.providers.security-baseline-pep.application-credential` | `SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL` | `asistencias-pep-application-credential` | Dueño de seguridad; la emite el PDP | 90 días, o según la política del PDP |
| `app.providers.minio.access-key` | `MINIO_ACCESS_KEY` | `asistencias-minio-access-key` | Dueño de Infra | 90 días, en pareja con la clave secreta |
| `app.providers.minio.secret-key` | `MINIO_SECRET_KEY` | `asistencias-minio-secret-key` | Dueño de Infra | 90 días |
| `app.providers.smtp-notification.username` | `SMTP_NOTIFICATION_USERNAME` | `asistencias-smtp-username` | Dueño de notificaciones (Sergio) | Solo si cambia la cuenta |
| `app.providers.smtp-notification.password` | `SMTP_NOTIFICATION_PASSWORD` | `asistencias-smtp-password` | Dueño de notificaciones (Sergio) | 180 días o la política del proveedor de correo |
| `app.security.azure-events.webhook-token` | `AZURE_EVENTGRID_WEBHOOK_TOKEN` | `asistencias-eventgrid-webhook-token` | Dueño de Azure (José Alejandro) | 90 días |
| `app.providers.catalogo-parametros.secreto` | `CATALOGO_PARAMETROS_SECRETO` | `asistencias-catalogo-parametros-secreto` | Dueño de la integración del Catálogo de Parámetros (Sergio), con el equipo del Catálogo (tercero) | 90 días o la política del Catálogo; solo se usa con `APP_ADAPTERS_PARAMETER_CATALOG_PROVIDER=catalogoparametros` |

## Secretos que se leen en caliente

| Consumidor | Secreto en Key Vault | Dueño | Rotación |
|---|---|---|---|
| `FeriadosIoFestivosAdapter` (API de festivos) | `feriados-io-api-key` (el parámetro `NOMBRE_SECRETO_FERIADOS_IO` puede cambiar el nombre) | Dueño de Azure (José Alejandro) | Según el proveedor de la API; sin reinicio |

Este secreto pasa por la caché del adaptador (50 entradas, 5 minutos) y se invalida con Event Grid.

## Valores que no son secretos

No van al baúl: los endpoints `AZURE_KEYVAULT_ENDPOINT` y `AZURE_APPCONFIG_ENDPOINT` (nombres de recursos, sin valor
por defecto desde SEC-12), los ids de cliente, las URL y los identificadores de MessageUcoLab. Se configuran por
variables de entorno del ambiente.

## Identidad para leer el baúl

`DefaultAzureCredential`: Azure CLI (`az login`) en desarrollo e identidad administrada en ambientes administrados.
Permiso mínimo: lectura de secretos (rol *Key Vault Secrets User* o política `get`) sobre ese baúl. No hay client
secret de Azure en el repositorio.

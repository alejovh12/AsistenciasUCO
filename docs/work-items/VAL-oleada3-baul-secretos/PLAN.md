---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# PLAN — VAL-oleada3-baul-secretos (backend)

Origen: sección de secretos de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md` (SEC-02, SEC-10, SEC-12; SEC-07 es de
Infra y ya estaba en CUMPLE). Rama local `fix/VAL-oleada3-baul-secretos` desde `origin/develop`. Sin Azure real: SEC-11
sigue pendiente. Skills: `uco-azure`, `uco-seguridad`, `uco-arquitectura`.

## SEC-02 — secretos de ejecución desde el baúl

| Qué | Cambio |
|---|---|
| Inventario en código | `infrastructure/config/adapters/vault/runtime/RuntimeSecretCatalog`: propiedad Spring → nombre en Key Vault para BD, Keycloak (legacy y baseline), PDP, PEP, MinIO, SMTP y token del webhook de Event Grid. Solo nombres. |
| Carga | `VaultRuntimeSecretsEnvironmentPostProcessor` (Composition Root, registrado en `META-INF/spring.factories`, corre después de `ConfigDataEnvironmentPostProcessor`). Con `app.adapters.vault.provider=azure_keyvault` crea el puerto con `AzureKeyVaultClientFactory` y antepone `VaultSecretPropertySource` (foto al arrancar). |
| Fallback | Secreto ausente en el baúl → la propiedad sigue con su variable de entorno o secreto del orquestador. Proveedor `local_env` → no hace nada (local, test, integración, CI). `APP_SECRETS_RUNTIME_FROM_VAULT_ENABLED=false` desactiva solo esta carga. |
| Fallo | Fallo técnico del baúl → falla el arranque con un mensaje que nombra la propiedad y el secreto, nunca el valor (fail-closed). |
| Arquitectura | Lectura por `SecretVaultPort` (Application); SDK solo en `config/adapters/vault/azure/AzureKeyVaultClientFactory`, que ahora comparte `AzureKeyVaultAdapterConfiguration`. Nada cambia en Domain/Application. La selección sigue siendo el selector `app.adapters.vault.provider`. |
| Documento | `docs/security/SECRETS_INVENTORY.md` (secreto, propiedad, variable, nombre en Key Vault, dueño, rotación). `RuntimeSecretCatalogTest` exige que coincida con el código. |

Grafo: `EnvironmentPostProcessor (config) → AzureKeyVaultClientFactory (config, SDK) → AzureKeyVaultAdapter (adapter secundario) ⇢ SecretVaultPort (Application)`; `VaultSecretPropertySource → SecretVaultPort`.

## SEC-10 — rotación

- `docs/security/SECRETS_ROTATION_RUNBOOK.md`: calendario, responsables, pasos, rollback, particularidades por secreto y
  la cadena Event Grid → webhook → `ProcesarEventoAzureInputPort` → `CatalogInvalidationPort` → caché de
  `AzureKeyVaultAdapter`. Deja claro qué se toma en caliente (API key de festivos) y qué exige reinicio escalonado.
- Prueba `RotacionSecretoInvalidacionCacheTest`: con `SecretClient` simulado (v1 y luego v2) y la cadena real
  caso de uso → compuesto → adaptador, un `SecretNewVersionCreated` invalida la caché y la lectura siguiente trae v2.

## SEC-12 — sin nombres de recursos reales por defecto

- `application.yml`: `AZURE_KEYVAULT_ENDPOINT` y `AZURE_APPCONFIG_ENDPOINT` sin valor por defecto.
- Fallo temprano y claro: `AzureKeyVaultProviderProperties` / `AzureAppConfigProviderProperties` con mensaje que nombra
  la variable y la salida local; el post-procesador lo lanza aún antes cuando el baúl Azure está activo.
- `application-local.yml` y `application-dev.yml`: el valor por defecto del selector pasa a `local_env` (baúl) y
  `sqlserver` (catálogos); las variables de entorno siguen mandando. Es un valor del selector, no un `@Profile`.
- `MessageUcoLabMessageCatalogAdapterConfiguration`: el endpoint de App Configuration solo se exige si la contingencia
  es `azure` (con `fallback=none` ya no se enlaza).
- Frontend (`40-env-js.sh`): ver el work item del repositorio Frontend.

## Fuera de alcance

Azure real (SEC-11), credenciales y recuperación de cuentas (Keycloak/terceros), cambios de RBAC, controllers,
contratos, SQL y `pom.xml`.

## Riesgos y rollback

- Un ambiente con `azure_keyvault` sin endpoint deja de arrancar (intencional). Rollback: revertir el commit o definir
  la variable.
- Ambientes `local`/`dev` sin variables pasan a `local_env` y catálogos SQL Server. Para seguir con Azure de desarrollo,
  definir los selectores y endpoints (Infra `init-env.ps1` ya los escribe).

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VAL-oleada3-baul-secretos - VALIDATION (backend)

Fecha: 2026-11-13. JDK 25.0.5, Spring Boot 4.0.8, rama local `fix/VAL-oleada3-baul-secretos`.

## Corridas

| Corrida | Comando | Resultado |
|---|---|---|
| 1 | `.\mvnw.cmd -B -o -q test -Dtest=VaultRuntimeSecretsEnvironmentPostProcessorTest,RuntimeSecretCatalogTest,RotacionSecretoInvalidacionCacheTest,MessageUcoLabMessageCatalogWiringTest,VaultAdapterConfigurationTest,AzureAndInvalidationBeansTest,ProviderAndAdapterPropertiesTest,AzureKeyVaultAdapterTest,CloudIntegrationClassificationTest,OperationalReadinessConfigurationTest -Djacoco.skip=true` | 62 pruebas, 2 errores en `RotacionSecretoInvalidacionCacheTest` (stubbing anidado de Mockito en la propia prueba; se usó `KeyVaultSecret` real). Repetida esa clase: PASS. |
| 2 | `.\mvnw.cmd -B -o verify` (suite completa, ArchUnit, JaCoCo) | **4468 pruebas, 0 fallos, 0 errores, 0 omitidas. "All coverage checks have been met". BUILD SUCCESS** (20:53 min). |
| 3 | `-Pintegration verify` | **NOT_RUN.** SQL Server de Docker estaba arriba, pero el perfil necesita las credenciales de BD del `.env` del backend, que este trabajo no debía abrir. El cambio no toca ese perfil: usa `local_env`, así que el post-procesador no actúa. |
| 4 | `-Pazure-integration verify` (SEC-11) | **NOT_RUN** (requiere Azure real y `az login`). |

## Pruebas nuevas

- `config/adapters/vault/runtime/VaultRuntimeSecretsEnvironmentPostProcessorTest` (SEC-02, SEC-12): con puerto falso,
  los secretos del baúl tienen prioridad y los ausentes caen a la variable de entorno; `local_env` y selector ausente no
  consultan el baúl; la bandera lo desactiva; Azure sin endpoint falla con un mensaje que nombra
  `AZURE_KEYVAULT_ENDPOINT` y `local_env`; un fallo técnico detiene el arranque nombrando propiedad y secreto (no el
  valor); secreto en blanco = ausente; orden posterior a ConfigData; registro en `spring.factories`.
- `config/adapters/vault/runtime/RuntimeSecretCatalogTest` (SEC-02): cubre BD, Keycloak, PDP, PEP, MinIO, SMTP y
  webhook; nombres válidos de Key Vault y únicos; catálogo inmutable; `docs/security/SECRETS_INVENTORY.md` lista cada
  propiedad y secreto.
- `adapter/secondary/vault/azure/RotacionSecretoInvalidacionCacheTest` (SEC-10): `SecretClient` simulado devuelve v1 y
  luego v2; antes del evento la caché sirve v1 sin volver al baúl; tras `Microsoft.KeyVault.SecretNewVersionCreated`
  procesado por `ProcesarEventoAzureUseCaseImpl` → `CompositeCatalogInvalidationAdapter` → `AzureKeyVaultAdapter`, la
  lectura siguiente trae v2 (TTL de 1 h en la prueba, así que no es por expiración). Evento de otro secreto no invalida;
  `SecretExpired` sí.
- `MessageUcoLabMessageCatalogWiringTest` (SEC-12): con `fallback=none` arranca sin endpoint de App Configuration; con
  `fallback=azure` y sin endpoint falla y el mensaje nombra `AZURE_APPCONFIG_ENDPOINT`.

## Arquitectura

ArchUnit verde dentro de la corrida 2. El SDK de Azure sigue solo en `infrastructure/config/adapters/vault/azure`
(fábrica) y `infrastructure/adapter/secondary/vault/azure` (adaptador). Domain/Application sin cambios.
`CloudIntegrationClassificationTest` verde: ninguna prueba nueva lee Azure real (endpoints `.invalid`).

## Revisión de secretos

Sin gitleaks local (no está instalado en el equipo). Revisión manual de `git diff`: solo nombres de secretos, nombres
de variables y valores ficticios de prueba (`valor-version-1`, `valor-del-baul`, `test-only-*`). Sin `.env` ni valores
reales. Los nombres reales de los recursos Azure de desarrollo salen de `application.yml`.

## Estado de los ítems

| Ítem | Estado propuesto | Evidencia |
|---|---|---|
| SEC-02 | CUMPLE (código y pruebas con dobles; Azure real en SEC-11) | `VaultRuntimeSecretsEnvironmentPostProcessor`, `RuntimeSecretCatalog`, `docs/security/SECRETS_INVENTORY.md`, pruebas de arriba |
| SEC-10 | CUMPLE | `docs/security/SECRETS_ROTATION_RUNBOOK.md`, `RotacionSecretoInvalidacionCacheTest` |
| SEC-12 | CUMPLE | `application.yml` sin nombres reales, fallo temprano con mensaje, perfiles `local`/`dev` sin Azure, Frontend `40-env-js.sh` sin id real |
| SEC-11 | Sigue PENDIENTE EXTERNO | Requiere Azure real |

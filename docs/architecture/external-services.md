---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# External Services

Los adapters de salida se organizan por capacidad/proveedor según [infrastructure-structure](infrastructure-structure.md). No crear un paquete externo o puerto sin consumidor funcional real.

Identity provisioning usa `IdentityProviderPort` y `infrastructure.adapter.secondary.identity.keycloak.KeycloakIdentityProviderAdapter`, elegido por `KeycloakIdentityAdapterConfiguration`. Véase [contrato de identidad](../security/keycloak-identity-provider.md).

Runtime security es capacidad distinta: `JwtClaimsExtractor` / `KeycloakJwtClaimsExtractor`, `JwtDecoder` y `InstitutionalJwtAuthenticationConverter`. `SecurityConfig` ya es neutral respecto al IdP; la referencia antigua a `KeycloakGrantedAuthoritiesConverter` estaba desactualizada. Véase [seguridad runtime](../security/runtime-security-provider-architecture.md).

Correo y APIs institucionales son ejemplos futuros, sin implementación declarada por este documento. El recurso OIDC/JWT pertenece al adapter primario de seguridad, no a un servicio externo de negocio.

## Azure runtime AS-IS

Azure ya implementa capabilities concretas y no se modela como una capability genérica:

| Capability | Port | Provider / adapter | Entrada o selector |
|---|---|---|---|
| Secret Vault | `SecretVaultPort` | Azure Key Vault / `AzureKeyVaultAdapter` | `app.adapters.vault.provider=azure_keyvault` |
| Parameter Catalog | `ParameterCatalogPort` | Azure App Configuration / `AzureAppConfigParameterCatalogAdapter` | `app.adapters.parameter-catalog.provider=azure_appconfig` |
| Message Catalog | `MessageCatalogPort` | Azure App Configuration / `AzureAppConfigMessageCatalogAdapter` | `app.adapters.message-catalog.provider=azure` |
| Invalidation | `CatalogInvalidationPort` | `CompositeCatalogInvalidationAdapter` | Azure Event Grid por `POST /api/v1/internal/azure-events` |

Los clientes Azure se construyen en Composition Root con `DefaultAzureCredential`. Las caches son Caffeine locales al proceso, con TTL e invalidación explícitos; no son cache distribuida. Event Grid es entrada operacional, no Identity ni autenticación Bearer de la API de negocio. Detalle, fallos, seguridad y operación: [Azure Runtime Integration](../integration/azure-runtime-integration.md).

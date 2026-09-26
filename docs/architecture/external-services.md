---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# External Services

Los adapters de salida se organizan por capacidad/proveedor según [infrastructure-structure](infrastructure-structure.md). No crear un paquete externo o puerto sin consumidor funcional real.

Identity provisioning usa `IdentityProviderPort` y `infrastructure.adapter.secondary.identity.keycloak.KeycloakIdentityProviderAdapter`, elegido por `KeycloakIdentityAdapterConfiguration`. Véase [contrato de identidad](../security/keycloak-identity-provider.md).

Runtime security es capacidad distinta: `JwtClaimsExtractor` / `KeycloakJwtClaimsExtractor`, `JwtDecoder` y `InstitutionalJwtAuthenticationConverter`. `SecurityConfig` ya es neutral respecto al IdP; la referencia antigua a `KeycloakGrantedAuthoritiesConverter` estaba desactualizada. Véase [seguridad runtime](../security/runtime-security-provider-architecture.md).

Correo y APIs institucionales son ejemplos futuros, sin implementación declarada por este documento. El recurso OIDC/JWT pertenece al adapter primario de seguridad, no a un servicio externo de negocio.

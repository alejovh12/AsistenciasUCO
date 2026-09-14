# External Services

`externalservice` agrupa comunicaciones iniciadas por AsistenciasUCO hacia sistemas externos
de negocio o infraestructura institucional.

Estado actual:

- Identity provisioning existe mediante `IdentityProviderPort` y
  `KeycloakIdentityProviderAdapter`.
- El adapter de provisioning se selecciona desde `KeycloakIdentityAdapterConfiguration` con
  `app.adapters.identity.provider=keycloak`.
- Runtime authentication continua parcialmente acoplado a Keycloak en `SecurityConfig` y
  `KeycloakGrantedAuthoritiesConverter`. Ese desacoplamiento pertenece a la siguiente fase.

Ejemplos futuros o pendientes:

- Correo.
- APIs institucionales.
- Servicios externos.

Spring Security Resource Server y la validacion JWT no son un adapter de external service de negocio. Permanecen en `infrastructure/adapter/primary/security` y `infrastructure/config/security`.

Estructura actual de provisioning:

```text
application/secondaryports/identity/
    IdentityProviderPort.java

infrastructure/adapter/secondary/identity/
    KeycloakIdentityProviderAdapter.java
```

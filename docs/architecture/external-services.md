# External Services

`externalservice` se usara cuando AsistenciasUCO tenga que iniciar comunicacion con sistemas externos de negocio.

Ejemplos futuros:

- Keycloak Admin API para aprovisionamiento de identidades.
- Correo.
- APIs institucionales.
- Servicios externos.

Spring Security Resource Server y la validacion JWT no son un adapter de external service de negocio. Permanecen en `infrastructure/adapter/primary/security` y `infrastructure/config/security`.

Estructura futura, solo cuando exista implementacion real:

```text
application/secondaryports/externalservice/identity/
    IdentityProvisioningPort.java

infrastructure/adapter/secondary/externalservice/keycloak/
    KeycloakIdentityProvisioningAdapter.java
    dto/
    mapper/
```

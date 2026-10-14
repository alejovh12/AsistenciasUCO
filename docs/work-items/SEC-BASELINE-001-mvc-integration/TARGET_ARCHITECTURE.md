# Arquitectura TARGET del experimento

```text
HTTP request
  -> CorrelationIdFilter
  -> AzureEventGridAuthFilter (si aplica)
  -> BearerTokenAuthenticationFilter / JwtDecoder actual
  -> SecurityBaselinePepFilter (solo flag ON; único registro, servlet registration deshabilitado)
       -> SecurityBaselinePepClient (HTTP, Infrastructure)
       -> PEP embedded endpoint
  -> AuthorizationFilter (roles actuales)
  -> controller / Application / ownership actual
```

## Reglas

1. El filtro experimental no sustituye `JwtDecoder`, `JwtClaimsExtractor`, roles ni ownership.
2. Una request sin Bearer sigue siendo responsabilidad del resource server y conserva el `401` existente.
3. Una autenticación JWT válida no basta para continuar: el PEP debe responder `204` con decisión.
4. Solo `204` con `X-Decision-Id` válido es ALLOW. `401` del PEP se traduce a `401`; `403` a `403`; timeout, excepción, redirect, cualquier otro 2xx o estado se traduce a `503 ACCESS_DECISION_UNAVAILABLE`.
5. Solo una decisión permitida continúa hacia las reglas de Spring Security; el PEP no concede roles ni ownership.
6. El filtro no reenvía cookies ni cuerpos de negocio. Envía únicamente la información definida por el contrato raw del PEP, un único Bearer bien formado y los headers de correlación saneada/credencial, por HTTP/1.1 y sin seguir redirects.
7. El adaptador es secundario y vive en Infrastructure. Domain y Application no conocen Security Baseline.
8. Deny-by-default: toda ruta exige decisión salvo `SecurityPublicEndpoints` (misma fuente que los `permitAll`) y el preflight CORS. Los dispatches ASYNC/ERROR heredan la decisión del REQUEST.

## Estado de activación

`app.providers.security-baseline-pep.enabled=false` por defecto. La activación exige URL, nombre de aplicación, ambiente y credencial proporcionada externamente. No se guarda ningún secreto en YAML, tests, logs o documentación.

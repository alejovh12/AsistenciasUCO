# AS-IS comprobado

## Backend

- Aplicación Spring Boot servlet/Spring MVC; `src/main/resources/application.yml` fija `spring.main.web-application-type: servlet`.
- `pom.xml` usa `spring-boot-starter-webmvc`, `spring-boot-starter-security` y resource server OAuth2. No se agrega WebFlux.
- `SecurityConfig` mantiene CORS sin credenciales de navegador, sesión stateless, JWT Bearer, reglas de rol y el filtro de Azure antes del Bearer filter.
- `KeycloakSecurityAdapterConfiguration` valida issuer, timestamp, audience y el claim UUID configurable `userIdClaim`, cuyo valor local actual es `idUsuario`.
- `InstitutionalJwtAuthenticationConverter` toma el principal institucional del claim `idUsuario`; no usa `sub` como sustituto.
- `CorrelationIdFilter` establece o sanea `X-Correlation-Id` y el contexto de correlación.
- Los errores de autenticación/autorización existentes usan el envelope `ApiErrorResponse` con códigos `UNAUTHORIZED` y `FORBIDDEN`.

## Security Baseline v0.1.6

- Tag verificado: `v0.1.6` → `272c8e3168aeee6934194e2278b3537dc3c5dd78`.
- El starter oficial inspeccionado está basado en WebFlux y describe el enforcement como filtro web reactivo.
- El endpoint implementado por el PEP embebido es `POST /internal/v1/embedded-access-decisions`.
- El request raw del PEP contiene `applicationName`, `environment`, `path` y `method`; la credencial de aplicación viaja por `X-Application-Credential`.
- La respuesta operacional del PEP usa `204` + `X-Decision-Id` para permitir (el starter acepta cualquier 2xx), `401` para token/credencial no válidos, `403` para denegación y otros estados/fallos para indisponibilidad.
- Comprobado en el laboratorio (2026-10-14): sin Bearer, el endpoint embebido responde `200` vacío sin `X-Decision-Id`; con `Upgrade: h2c` responde `501`; el PDP valida un único issuer (`security-baseline`) y audiencia `security-baseline-pdp`; la administración del PDP (perfil `keycloak`) solo acepta sesión BFF; los recursos con plantilla `{x}` devuelven `500`.
- El starter oficial no exige que este backend implemente el endpoint interno PEP→PDP; hacerlo aquí sería mezclar responsabilidades.

## Discrepancias y evidencia faltante

- El README del starter describe una protección embebida; otra guía del release describe PEP externo y registro. Se toma el código/endpoint como AS-IS operativo y se conserva la discrepancia para decisión posterior.
- El release clonado no contiene `Manual-integracion-aplicaciones.pdf` ni `Manual-funcional-plataforma-seguridad.pdf`; fueron buscados en los archivos versionados y en los assets publicados del release. Estado: `BLOCKED_BY_MISSING_EVIDENCE` para validar esas reglas documentales.
- El compose oficial, el realm importado y `pep/src/main/resources/application.properties` contienen credenciales literales. Se reportan como `SECURITY_FINDING` por archivo/tipo (`RISKS.md`); el laboratorio no las reutiliza.
- Filtro MVC en `a74b288`: era un bean `Filter` sin registro servlet deshabilitado → doble registro y bypass total del PEP reproducido en MockMvc. Corregido en `16621c7`.

# Decisiones

## D-001 — no incorporar WebFlux

El backend AS-IS es Spring MVC y el starter oficial es WebFlux. Copiarlo directamente cambiaría el runtime y el contrato de infraestructura. Se implementa un filtro servlet mínimo con el mismo comportamiento observable y sin dependencia WebFlux.

## D-002 — PEP embebido, no PDP directo

El código oficial expone `/internal/v1/embedded-access-decisions` para aplicaciones protegidas. El contrato PEP→PDP es interno al Security Baseline y no se implementa en este backend.

## D-003 — conservar JWT institucional

El flag agrega una barrera de autorización externa después de la autenticación local. No cambia issuer, audience, `idUsuario`, roles ni ownership. Esto permite rollback apagando una propiedad y evita una migración de identidad implícita.

## D-004 — fail-closed

La indisponibilidad o una respuesta inválida del PEP no debe abrir rutas protegidas. Se expone como `503` (ver D-007 para el código).

## D-005 — identidad bloqueada

No hay evidencia que pruebe que el `sub` de Security Baseline sea el UUID institucional `idUsuario`. En el laboratorio se comprobó además que el token de la plataforma no trae `idUsuario` y que el PDP solo confía en su propio issuer. Estado: `BLOCKED_BY_IDENTITY_CONTRACT` (ver `IDENTITY_MAPPING.md`).

## D-006 — laboratorio separado

El laboratorio usa `asistencias-security-lab`, loopback, red y volúmenes propios. No ejecuta `down -v`, no comparte recursos con el stack estable y no recibe credenciales desde archivos versionados.

## D-007 — código específico para 503 (cambio solicitado por el usuario)

`a74b288` respondía `503` con `INTERNAL_ERROR`, semántica engañosa para una dependencia externa caída. Por instrucción explícita del usuario se añade `SecurityErrorCode.ACCESS_DECISION_UNAVAILABLE`. Solo se emite con el flag experimental activo; el contrato público por defecto no cambia. El test unitario existente que exigía `INTERNAL_ERROR` se actualizó por esta decisión (no para acomodar la implementación). **Antes de cualquier merge**, el código debe incorporarse al contrato (`BACKEND_GOLDEN_PATH_CONTRACT.md` / OpenAPI) por decisión contractual.

## D-008 — un solo registro del filtro

Todo bean `Filter` se registra además como filtro servlet global. Con el wiring de `a74b288` esa instancia podía ejecutarse antes de la cadena de seguridad, sin autenticación, y marcar el atributo *already-filtered*, de modo que la instancia de la cadena se saltaba: bypass total del PEP, reproducido en el slice MockMvc. Se añade un `FilterRegistrationBean` con `enabled=false`; el único registro efectivo es `addFilterAfter(BearerTokenAuthenticationFilter)`.

## D-009 — deny-by-default con rutas públicas explícitas

En lugar de una lista de prefijos protegidos (`/api/v1/**`, `/api/v2/sesiones/**`), toda ruta exige decisión salvo las de `SecurityPublicEndpoints` (actuator, webhook Azure, Swagger GET, `/error`), que es también la fuente de los `permitAll` de `SecurityConfig`. El preflight CORS se excluye con `CorsUtils.isPreFlightRequest`, no por método.

## D-010 — ALLOW solo con `204` + `X-Decision-Id`

El controlador oficial responde `204` con `X-Decision-Id`. En el laboratorio, el PEP v0.1.6 respondió `200` vacío sin decisión a peticiones sin Bearer (incluso para rutas no registradas). Cualquier 2xx distinto, o un `X-Decision-Id` ausente o con formato inválido, es respuesta anómala → `503`.

## D-011 — solo un Bearer bien formado

El cliente reenvía `Authorization` únicamente si hay un solo header `Bearer <token68>`; en otro caso devuelve `401` sin llamar al PEP. Nunca reenvía `Cookie`, aunque el starter oficial reenvíe la cookie de sesión BFF: este backend es Bearer-only y no usa esa sesión.

## D-012 — transporte

HTTP/1.1 explícito (el JDK con HTTP/2 envía `Upgrade: h2c` y el PEP responde `501`), redirects `NEVER`, connect/request timeout positivos y ≤ 30 s, URL de origen fijo (sin userinfo, query, fragment ni path) y HTTP solo hacia loopback; cualquier otro host exige HTTPS. `toString()` de las propiedades no expone la credencial. La correlación enviada es la del `CorrelationIdContext` saneado; si no cumple `[A-Za-z0-9_.:-]{1,128}` se usa el request id.

## D-013 — laboratorio derivado del release, con desviaciones explícitas

`infra/security-baseline-lab/compose.lab.yaml` difiere del `compose.yaml` oficial en: proyecto/red/volúmenes propios; publicación solo en `127.0.0.1`; secretos por variable (incluido el del cliente técnico del PEP, literal en la imagen); volumen de datos de Keycloak; Keycloak escuchando en 19090 con IP fija y `extra_hosts` en PDP/PEP (el upstream usa `host-gateway` con publicación en `0.0.0.0`). Los secretos literales del realm importado se sustituyen en Keycloak tras el arranque y el usuario demo de credencial pública se deshabilita.

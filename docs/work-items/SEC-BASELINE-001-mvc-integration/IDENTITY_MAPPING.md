# Identidad `sub` vs `idUsuario`

Estado: **`BLOCKED_BY_IDENTITY_CONTRACT`** para el E2E académico (2xx, roles locales, ownership).

## Hechos comprobados en el laboratorio (2026-10-14)

Claims observados (sin imprimir tokens):

| Emisor | `iss` | `aud` | `idUsuario` | Roles `asistencias-api` | Otros |
|---|---|---|---|---|---|
| Keycloak institucional (8081) | `http://127.0.0.1:8081/realms/asistencias-uco` | `asistencias-api`, `account` | presente | `DOCENTE` (+ legado) | `azp=asistencias-uco-frontend` |
| Keycloak del laboratorio (19090) | `http://keycloak.localhost:19090/realms/security-baseline` | `security-baseline-pdp`, `security-baseline-bff` | **ausente** | **ausentes** | `tenant=universidad-uco` (hardcoded), `sub` = id Keycloak |

- El PEP embebido no valida el Bearer: lo reenvía al PDP. El PDP acepta un único issuer
  (`PDP_KEYCLOAK_ISSUER`) y la audiencia `security-baseline-pdp`.
- El PDP mantiene su propio `userId` interno (p. ej. `lab-docente-permitido` → `944dba4a-…`), distinto
  del `sub` de Keycloak (`fca023fc-…`); se enlaza en el primer login BFF por issuer + `sub` + email.
- El backend exige issuer institucional, `aud=asistencias-api`, claim `idUsuario` y roles de cliente.

## Escenario A — JWT institucional

Resultado real: 401 en el PEP (`TOKEN_INVALID`) y, a través del backend, 401 con el mismo correlation
ID registrado por el PEP. Hacerlo pasar exigiría reconfigurar el PDP para confiar en el realm
institucional (cambiar el IdP de la plataforma, que también gobierna su administración). No es una
configuración soportada por el release ni se relajó ninguna validación. **No compatible.**

## Escenario B — JWT del laboratorio

Resultado real: el backend lo rechaza con 401 (issuer incorrecto) antes de llegar al PEP. El PEP/PDP sí
lo autoriza o deniega correctamente (204/403 reales). No existe en v0.1.6 un mecanismo verificable que
emita `idUsuario` o resuelva el actor académico a partir del sujeto de la plataforma.

## Decisión

No se agrega mapper `sub → idUsuario`, no se añade un atributo `idUsuario` arbitrario a usuarios
sintéticos del laboratorio, no se asocia por correo/nombre, no se cambia `KEYCLOAK_USER_ID_CLAIM`, no
se relajan validadores y no se modifica SQL Server.

## Evidencia requerida para desbloquear

Una de estas decisiones, versionada por el owner de identidad/seguridad:

1. **IdP común**: el PDP/PEP confían en el realm institucional (issuer `asistencias-uco`) y ese realm
   emite la audiencia que espera el PDP (`security-baseline-pdp` o la que se acuerde), manteniendo
   `idUsuario`; o
2. **Claim institucional en la plataforma**: el realm de Security Baseline emite `idUsuario` con
   procedencia verificable (federación/aprovisionamiento desde la fuente institucional) y el backend
   acepta ese issuer mediante configuración explícita; o
3. **Token exchange** documentado entre ambos emisores.

Hasta entonces solo es válido probar el PEP/PDP con identidades sintéticas y el backend con su JWT
institucional por separado, como se hizo.

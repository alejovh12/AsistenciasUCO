# Decisión de identidad (laboratorio)

Estado: `BLOCKED_BY_IDENTITY_CONTRACT` **resuelto solo en laboratorio**. No es una decisión institucional.

## Estrategia elegida: IdP común de laboratorio (opción 2 de SEC-BASELINE-001)

El realm `security-baseline` del Keycloak de laboratorio (19090) emite **un único JWT** aceptado por las dos capas:

| Requisito | Cómo se cumple | Verificado |
|---|---|---|
| `iss` = `http://keycloak.localhost:19090/realms/security-baseline` | `KC_HOSTNAME` del laboratorio (sin cambios) | sí |
| `aud` ⊇ `security-baseline-pdp` | client scope existente `security-baseline-claims` (no modificado) como scope por defecto del cliente nuevo | sí |
| `aud` ⊇ `asistencias-api` | mapper oficial `oidc-audience-mapper` en el cliente `asistencias-lab-e2e` | sí |
| `resource_access.asistencias-api.roles = [DOCENTE]` | client role de `asistencias-api` + mapper `oidc-usermodel-client-role-mapper`; `fullScopeAllowed=false` y scope mapping solo de `DOCENTE`/`ESTUDIANTE` | sí |
| `idUsuario` = UUID institucional verificado | atributo de User Profile (UUID, editable solo por admin) + mapper `oidc-usermodel-attribute-mapper`; valor asignado **después** de `verify-academic-identity.ps1` | sí |
| `sub` propio e independiente | `sub` = id Keycloak (`c05297af-…`), ≠ `idUsuario` (`de000003-…-0001`) | sí |
| Firma RS256, `kid` del JWKS, vida 300 s | realm por defecto | sí |

Evidencia sin tokens: [evidence/provisioning.json](evidence/provisioning.json) (`claims[*].verdict = PASS`).

El backend 8083 confía en ese issuer **solo por configuración de proceso** (`-IdentityMode SharedLab`):

```text
KEYCLOAK_ISSUER_URI=http://keycloak.localhost:19090/realms/security-baseline
KEYCLOAK_EXPECTED_AUDIENCE=asistencias-api
KEYCLOAK_API_CLIENT_ID=asistencias-api
KEYCLOAK_USER_ID_CLAIM=idUsuario
```

No se cambió código Java: `JwtDecoders.fromIssuerLocation` sigue descubriendo el JWKS desde el issuer
(el JDK resuelve `keycloak.localhost` → 127.0.0.1, donde Keycloak publica 19090), y los validadores de
timestamps, issuer exacto, audiencia y UUID siguen activos (casos E2E-05…08 lo demuestran con 401).

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| PDP confiando en el realm institucional 8081 | Cambia el IdP de la plataforma (también gobierna su administración BFF); no soportado por v0.1.6 |
| Mapper `sub → idUsuario` o asociación por correo/nombre | Prohibido: confunde identidad de plataforma con actor académico |
| JWT fabricado o firmado manualmente | Prohibido: no demuestra nada del proveedor |
| Reescribir `iss`, fijar un JWKS distinto o desactivar validadores | Prohibido |
| Token exchange 8081 → 19090 | No disponible en el laboratorio sin configurar federación entre realms |

## Procedencia del UUID

1. `verify-academic-identity.ps1 -Mode Verify` (solo SELECT, `ApplicationIntent=ReadOnly`) comprobó para cada UUID:
   usuario existe y está activo, exactamente un `Docente`, el grupo FIX-PAG esperado existe con nombre único y su
   titular es ese docente.
2. `provision_shared_identity_e2e.py` se niega a asociar un UUID que no esté en esa evidencia o cuya verificación
   tenga más de 24 h.
3. La asociación (atributo `idUsuario`) la hace un administrador del laboratorio; el usuario no puede editarla.

Identidades del laboratorio:

| Usuario lab (Keycloak 19090) | `idUsuario` | Grupo del que es titular | Client role | Rol PDP | Propósito |
|---|---|---|---|---|---|
| `lab-e2e-docente-fixpag300` | `de000003-…-0001` | FIX-PAG-300 | DOCENTE | `DOCENTE_E2E_FIXPAG300_LAB` | positivo / ownership |
| `lab-e2e-rol-incorrecto` | `de000003-…-0002` | FIX-PAG-1000 | ESTUDIANTE | `DOCENTE_E2E_FIXPAG300_LAB` | rol local incorrecto |
| `lab-e2e-docente-sin-pdp` | `de000003-…-0003` | FIX-PAG-5000 | DOCENTE | ninguno | sin permiso PDP |
| `lab-e2e-sin-idusuario` | — | — | DOCENTE | `DOCENTE_E2E_FIXPAG300_LAB` | JWT sin `idUsuario` |

Las identidades institucionales E2E del Keycloak 8081 (`docente.paginacion300`, etc.) **no se modificaron**; solo
se usó `docente.paginacion300` para emitir un token como caso negativo de issuer (E2E-07).

## Qué es exclusivo del laboratorio

- Todo el realm `security-baseline` del 19090, sus clientes `asistencias-api`, `asistencias-lab-e2e`,
  `asistencias-lab-e2e-wrong-aud` y los usuarios `lab-e2e-*`.
- La asociación manual `usuario lab → idUsuario`.
- Direct Access Grants (ROPC) en `asistencias-lab-e2e`: solo para automatizar la matriz; prohibido en producción.
- El issuer HTTP `keycloak.localhost` y `-IdentityMode SharedLab` del proceso 8083.
- Recursos PDP con rutas exactas por UUID de grupo.

## Decisiones que requieren aprobación institucional

1. **IdP de producción**: ¿el PDP confiará en el realm institucional, o la plataforma federará/aprovisionará desde
   la fuente institucional? Debe existir un único issuer de confianza por ambiente.
2. **Fuente y ciclo de vida de `idUsuario`**: quién lo escribe (aprovisionamiento automatizado desde SQL/SIS, no
   manual), cómo se revoca y cómo se audita.
3. **Audiencias**: si el token de usuario llevará `security-baseline-pdp` + `asistencias-api` simultáneamente o
   habrá token exchange por audiencia.
4. **Autorización de recursos dinámicos** (`/grupos/{grupoId}/…`): el PDP v0.1.6 no admite plantillas; hace falta
   una corrección upstream o un modelo de recursos distinto. Las rutas exactas no escalan.
5. **Contrato `ACCESS_DECISION_UNAVAILABLE` (503)**: aprobación contractual antes de cualquier merge.
6. **Flujo de login**: Authorization Code + PKCE para el frontend; ROPC no se admite.

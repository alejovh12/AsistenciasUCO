---
status: draft
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-10-14
---

# ADR-SEC-BASELINE-003 — Migración de AsistenciasUCO a Security Baseline como mecanismo central de autenticación y autorización

## Estado

**Propuesto** (2026-10-14). No aprobado. Ámbito: arquitectura objetivo y estrategia de convivencia. No autoriza
merge, despliegue ni retiro del Keycloak 8081. Requiere aprobación explícita del usuario y acuerdos con Seguridad-UCO
en los puntos marcados "Seguridad-UCO".

## Contexto

- AS-IS en `develop`: el backend es un resource server JWT que confía en el realm `asistencias-uco` del Keycloak 8081
  (issuer exacto, JWKS, `aud=asistencias-api`, claim UUID `idUsuario`, roles de cliente); RBAC por rutas en
  `SecurityConfig`; titularidad académica en Application (`InstitutionalScopePort`) y en SP de la DB.
- Experimento (SEC-BASELINE-001…003): filtro PEP servlet opt-in, fail-closed, después de la autenticación local y antes
  del RBAC. Certificado **solo en local** con una identidad de laboratorio (issuer 19090) y una DB E2E aislada:
  291/296 casos PASS; los 5 restantes son 1 defecto upstream, 2 defectos AS-IS de AsistenciasUCO y 2 límites de
  diseño de revocación ([SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md)).
- El laboratorio no es un IdP productivo: realm HTTP en `keycloak.localhost`, asociación manual de `idUsuario`, ROPC en
  clientes de prueba, recursos PDP por ruta exacta, imágenes sin firma institucional.

## Decisión propuesta

1. **Separación de responsabilidades (se conserva).** Autenticación: un IdP OIDC institucional. Autorización de
   capacidad (¿puede este actor invocar esta operación?): PDP de Security Baseline a través del PEP. Autorización
   académica (¿es titular de este grupo/sesión/soporte?): AsistenciasUCO (`InstitutionalScopePort` + DB). El PDP nunca
   sustituye la titularidad; la titularidad nunca se delega a rutas exactas en el PDP.
2. **Issuer institucional definitivo — Seguridad-UCO.** Un único issuer HTTPS por ambiente, en el que confíen a la vez
   el PDP y el backend. Opciones a acordar: (a) Security Baseline adopta el realm institucional como su IdP; (b)
   federación (identity brokering) desde el IdP institucional hacia el realm de Security Baseline, con `idUsuario`
   propagado por mapper; (c) token exchange por audiencia. Recomendación: (b) si Seguridad-UCO opera la plataforma de
   forma centralizada para varias aplicaciones; (a) si AsistenciasUCO es el único consumidor. Se descarta mantener dos
   issuers en producción de forma indefinida.
3. **Source of truth de `idUsuario` — contrato compartido.** La fuente es el registro académico (`dbo.Usuario`, vía
   `usp_sincronizar_usuario` del owner DB) o el SIS institucional; el IdP solo lo **replica** como atributo de solo
   administración. Alta/cambio/baja automatizados y auditados; prohibido asociar por correo, nombre o `sub`.
   Desaprovisionamiento: deshabilitar en el IdP + revocar asignaciones PDP + marcar inactivo en la DB en la misma
   operación orquestada.
4. **Audiencias y scopes.** Token de usuario con `aud` mínimo: `asistencias-api` (validado por el backend) y la
   audiencia que exija el PEP/PDP. Sin audiencias de BFF en tokens de API (SB3-16). Scopes de cliente sin
   `fullScopeAllowed`; roles de cliente `asistencias-api` = {ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE, ESTUDIANTE}.
5. **Clientes Keycloak.** `asistencias-api` (bearer-only, namespace de roles); `asistencias-uco-frontend` público con
   **Authorization Code + PKCE** (S256), sin ROPC ni implicit (SB3-10); cliente confidencial de aprovisionamiento con
   `client_credentials` (ya documentado en `keycloak-service-account.md`); credencial de aplicación del PEP en secret
   manager (SB3-07). Clientes de laboratorio (`asistencias-lab-e2e*`) nunca se promueven.
6. **Acceso administrativo y Security UI.** La consola del PDP (BFF) y la consola de Keycloak solo para roles de
   plataforma, separados de los roles académicos, con **MFA obligatorio** (SB3-11) y auditoría. Ningún rol académico
   (ni ADMINISTRADOR de AsistenciasUCO) administra el PDP por defecto.
7. **Roles y permisos.** Roles académicos en el IdP (fuente: aprovisionamiento); permisos de capacidad en el PDP por
   **operación/plantilla** (requiere SB3-05 upstream). Mientras no existan plantillas, el PEP no puede usarse para rutas
   con `{id}` en producción.
8. **Frontend Angular / BFF.** Opción A (recomendada a corto plazo): SPA con PKCE y Bearer en memoria, API sigue
   Bearer-only y stateless (CSRF no aplica, como hoy). Opción B: BFF con cookie de sesión `HttpOnly`, `Secure`,
   `SameSite=Lax/Strict`; entonces **CSRF debe reactivarse** para las rutas con cookie (comentario vigente en
   `SecurityConfig`) y CORS se restringe al origen del BFF. El frontend estable no se modifica en esta fase.
9. **Endpoints y SSE.** Deny-by-default (D-009); el PEP evalúa la ruta sin query string, por lo que la titularidad SSE
   permanece en el gateway (E2E-13b). El frontend debe consumir SSE con `fetch` + Bearer (EventSource no envía
   `Authorization`); con BFF, por cookie. Errores SSE en JSON (FIX-001).
10. **Validación JWT y JWKS.** Se mantienen issuer exacto, firma por JWKS del discovery, timestamps, audiencia y UUID
    `idUsuario` obligatorios; rotación de claves del IdP soportada por `kid` + caché JWKS. No se relaja ningún
    validador para compatibilidad.
11. **Revocación.** Access token ≤ 5 min (como el laboratorio) y una de: estado de usuario/sesión consultado por el PDP,
    introspección para operaciones sensibles, o back-channel logout. Se documenta la ventana máxima aceptada (SB3-06).
12. **Rotación de secretos.** Credencial del PEP, secretos de clientes confidenciales y claves del realm con rotación
    programada y doble credencial durante la transición; nunca en YAML, logs ni evidencias.
13. **Observabilidad.** `X-Correlation-Id` extremo a extremo (backend ↔ PEP ↔ PDP), `X-Decision-Id` también en
    denegaciones (SB3-18), métricas de latencia y disponibilidad del PEP, alertas de 503 `ACCESS_DECISION_UNAVAILABLE`.

## Estrategia reversible de convivencia temporal

| Etapa | Qué cambia | Criterio de avance | Rollback |
|---|---|---|---|
| M0 (actual) | Rama experimental, flag OFF por defecto | — | — |
| M1 Contratos | Acuerdos 2, 3, 4, 6, 7, 11 con Seguridad-UCO; correcciones upstream SB3-04/05/15/16 | Acuerdo versionado; release upstream con plantillas y sin PEP-03 | — |
| M2 Integración en `develop` (flag OFF) | PR de integración del filtro + overlay 503 aprobado; FIX-002 previo | Revisión + CI verdes; comportamiento estable idéntico (`ST-*`) | revert del PR |
| M3 Modo observación | Nuevo modo "audit-only" del PEP (registra la decisión, no la aplica) en un ambiente no compartido — **requiere work item propio** | 0 discrepancias inesperadas PEP vs RBAC durante la ventana | flag OFF |
| M4 Doble issuer controlado | Backend acepta temporalmente 8081 **y** el issuer nuevo (resolver por `iss`, cada uno con su JWKS y audiencia); PEP ON solo para el issuer nuevo | Usuarios piloto en el issuer nuevo sin regresiones | quitar el issuer nuevo de la lista |
| M5 Frontend PKCE con el nuevo IdP | Configuración del frontend tras feature flag; usuarios re-autentican | Ventana de observación (p. ej. 2–4 semanas) con tasas 401/403/503 y latencia p95 del PEP dentro de umbral | volver la configuración del frontend al 8081 (sigue arriba) |
| M6 Retiro gradual de 8081 | 8081 en solo lectura (sin altas), exportación del realm, después apagado **solo con aprobación explícita** | Fin de la ventana + 0 tokens 8081 válidos (TTL/refresh vencidos) | reactivar 8081 desde la exportación |

**Migración de sesiones activas:** no se migran tokens ni refresh tokens entre IdP. Durante M4–M5 ambos issuers son
válidos; los usuarios pasan al nuevo IdP al re-autenticarse. El drenaje dura como máximo la vida del refresh token del
realm 8081; después se retira el issuer antiguo del backend.

## Responsabilidades

| Tema | AsistenciasUCO | Seguridad-UCO |
|---|---|---|
| Filtro PEP, fail-closed, contrato 503, titularidad, FIX-002, revocación del lado backend, observabilidad backend | ✔ | |
| Issuer, realm/federación, MFA, clientes, rotación de claves del IdP | consume | ✔ |
| Plantillas de recursos, PEP-03, `environment`, audiencias del scope upstream, imágenes firmadas | reporta | ✔ |
| Fuente y aprovisionamiento de `idUsuario` y roles | contrato compartido + owner DB | contrato compartido |
| Frontend PKCE / BFF | frontend AsistenciasUCO | define el flujo soportado |

## Alternativas consideradas

| Alternativa | Motivo de descarte |
|---|---|
| Convertir el laboratorio 19090 en IdP productivo | HTTP, `keycloak.localhost`, ROPC, asociación manual, imágenes sin firma institucional |
| Mapear `sub → idUsuario` o asociar por correo | Confunde identidad de plataforma con actor académico (prohibido desde SEC-BASELINE-001) |
| Delegar la titularidad al PDP con rutas exactas | No escala (un recurso por grupo/sesión) y el PDP no ve la query string |
| Cortar de golpe (big bang) apagando 8081 | Irreversible y sin ventana de observación; no autorizado |

## Consecuencias

Positivas: autorización centralizada auditable, fail-closed probado, rollback por flag. Negativas: dependencia
operativa de la disponibilidad del PEP/PDP (cada petición protegida añade una llamada; caída = 503), doble issuer
temporal que aumenta la superficie de configuración, y bloqueo hasta resolver SB3-01/02/05.

## Evidencia y referencias

[SEC-BASELINE-001](../SEC-BASELINE-001-mvc-integration/DECISIONS.md), [SEC-BASELINE-002](../SEC-BASELINE-002-shared-identity-e2e/IDENTITY_DECISION.md),
[SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md), [TECHNICAL_DEBT](TECHNICAL_DEBT.md), [DB_ISOLATION](DB_ISOLATION.md),
`docs/security/runtime-security-provider-architecture.md`, `docs/security/keycloak-service-account.md`.

# SEC-BASELINE-001 — integración experimental Security Baseline v0.1.6 en Spring MVC

## Estado

`EXPERIMENTAL / PARTIAL_WITH_BLOCKERS / NO APTO PARA MERGE FUNCIONAL`

La rama es `jose-valencia/experiment-security-baseline-mvc`, aislada desde `origin/develop` en `4082736b3607a7ee17770892764f1dcc0af381bb`; código validado en `16621c7` (ver `HANDOFF.md`). El flag permanece apagado por defecto.

Bloqueo vigente: `BLOCKED_BY_IDENTITY_CONTRACT` (ver `IDENTITY_MAPPING.md`).

## Objetivo

Probar, sin alterar el proveedor JWT institucional vigente, un enforcement PEP fail-closed para el backend Spring MVC. La integración solo puede entrar al flujo cuando el flag experimental está explícitamente habilitado y el PEP responde `2xx`.

## Alcance autorizado

- Adaptador secundario HTTP para el contrato oficial `POST /internal/v1/embedded-access-decisions`.
- Filtro servlet en Infrastructure, antes de autorización, después de la autenticación Bearer existente.
- Flag, configuración externa sin secretos y límites de timeout.
- Respuestas observables `401`, `403`, `503` y continuación solo ante decisión permitida.
- Pruebas unitarias y de slice MVC del comportamiento.
- Laboratorio local aislado versionado en `infra/security-baseline-lab/` (compose y scripts sin secretos) y documentación de handoff.

## Fuera de alcance

- Cambiar issuer, decoder, claims, roles o ownership de la API.
- Mapear `sub` a `idUsuario` sin evidencia contractual.
- Implementar el contrato PEP→PDP dentro del backend.
- Añadir WebFlux, cambiar Spring MVC, cambiar frontend estable, Keycloak estable o esquema SQL.
- Crear/ejecutar seeds, DML, migraciones o datos institucionales.
- Activar el flag en ambientes existentes, publicar un secreto, desplegar contenedores o hacer merge.

## Definition of Ready

`READY` para el adaptador aislado, sus pruebas y el laboratorio local (credencial de aplicación emitida por el PDP y custodiada fuera del repositorio). `NOT_READY` para E2E académico: el contrato de identidad entre la plataforma y el backend no existe (`BLOCKED_BY_IDENTITY_CONTRACT`) y siguen ausentes los manuales PDF. El trabajo bloqueado se documenta como tal; no se sustituye por suposición.

## Evidencia y rollback

- AS-IS: `AS_IS.md`, contrato oficial inspeccionado en el tag `v0.1.6` (`272c8e3168aeee6934194e2278b3537dc3c5dd78`) y código actual del backend.
- Redacción de decisiones: `DECISIONS.md`.
- Rollback: apagar `app.providers.security-baseline-pep.enabled`; el camino JWT/roles/ownership existente permanece intacto.
- El laboratorio usa un proyecto Compose y volúmenes con nombre propio; no comparte volúmenes ni puertos con la aplicación estable.

## Criterios de cierre del experimento

El experimento solo puede proponerse para revisión cuando las pruebas focales estén `PASS`, `.\mvnw.cmd verify` termine con gates verdes, no haya secretos en diff/logs, el mapeo de identidad tenga evidencia versionada y un reviewer confirme el contrato. Cumplido: focales, `verify`, higiene de secretos. Pendiente: identidad, decisión contractual D-007, revisión. No se declara cierre.

## Pendientes frontend y Security UI

- Frontend (Angular): hoy no cambia; envía el Bearer institucional. Con el flag ON debe tratar `503 ACCESS_DECISION_UNAVAILABLE` como error transitorio (reintento/mensaje), distinto de `403`, y no reintentar en bucle. Requiere decisión contractual D-007 y el contrato de identidad.
- El starter oficial reenvía la cookie `SECURITY_BASELINE_SESSION` del BFF; este backend es Bearer-only y no la acepta. Un frontend que use el BFF de la plataforma necesitaría otra decisión (CSRF, cookies) que está fuera de alcance.
- Security UI: el release v0.1.6 no incluye SPA de administración; la administración se hizo por API con sesión BFF. Para operación real hace falta la UI del PDP o un procedimiento operativo aprobado.
- Modelado de recursos con plantillas (`/grupos/{id}/...`) bloqueado por el `500` del PDP v0.1.6.

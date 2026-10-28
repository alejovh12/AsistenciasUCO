---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU006-perfil-usuario

## Estado

**DONE local:** los gates locales están en verde ([VALIDATION.md](VALIDATION.md)). Los cambios están en commits locales de la rama `feature/HU006-perfil-usuario` en DB, backend y frontend, sin subir hasta que John confirme.

Pendiente:
- push y PR hacia `develop` (DB → backend → frontend);
- CI remoto;
- MV-008.

## Entregado

- `GET /api/v1/usuarios/perfil` deja de responder 501. Devuelve el perfil propio leído de la DB, sin contraseña:
  - datos personales;
  - vínculos por rol (estudiante, docente, coordinador, decano, administrador), cada uno con su estado.
- "Mi perfil" muestra la sección "Expediente y vínculos institucionales".
- **No alcance:** `PUT /usuarios/perfil` sigue en 501. TD-049 queda PARCIAL.

## Documentación

Completa y actualizada:
- **Work items:** PLAN, CONTRACT, TEST_PLAN, VALIDATION, CONTRACT_MATRIX y CLOSURE en backend; PLAN, TEST_PLAN y VALIDATION en DB y frontend.
- **Backend:** `TECHNICAL_DEBT` (TD-049 PARCIAL), `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, `docs/work-items/README.md` y `MANUAL_VALIDATION_LEDGER`.
- **DB:** `DOCUMENTACION_PROCEDIMIENTOS_PUBLICOS` (addendum) y `especificacion_sp_endpoints`.
- **Frontend:** `frontend-backend-contract.md` y `frontend-features-pending.md`.

## Riesgos residuales

- El flag `USER_PROFILE_ENDPOINT_ENABLED` (Infra) sigue apagado. Mientras siga así, `AuthService` no sincroniza los nombres desde la DB. La sección nueva no depende del flag.
- No hay commit RED separado.

## Siguiente paso

Que John confirme el push y los PR. Después, MV-008.

---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT MATRIX — HU006-perfil-usuario

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-006-DB | Perfil | READ_QUERY_RESULTSET de 17 columnas, una fila por vínculo; sin vínculos devuelve 1 fila con el vínculo en NULL; si el usuario no existe, 0 filas | Repositorio que agrupa los vínculos | MATCH | P-01..P-03, PR-01..05 | — | — |
| C-006-HTTP | `GET /usuarios/perfil` | `ApiDataResponse<PerfilUsuarioDTO>` con los campos que `AuthService` ya leía y `vinculos`; responde 404 | `PerfilService` y `ProfileLinksComponent` | MATCH | PU-01..05, PC-01..03, PL-01..05 | — | — |
| C-006-CAT | Autorización | Fila existente `AUTHENTICATED`; solo cambia el texto de `ownership` | PEP del baseline | MATCH | `AccessCatalogConsistencyTest` | La ruta ya estaba provisionada | — |
| C-006-PUT | Edición | Sigue en 501 | Edición deshabilitada | MATCH (pendiente) | `UsuarioControllerTest` | TD-049 parcial | Coordinar con Sergio |

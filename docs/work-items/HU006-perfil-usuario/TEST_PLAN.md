---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# TEST_PLAN — HU006: perfil propio

## Fuentes

[PLAN.md](PLAN.md), [CONTRACT.md](CONTRACT.md), `infra/security-baseline/access-catalog.json`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| P-01 | Datos y vínculos | U1 con los 5 roles: estudiante (P1), docente en P1 (activo) y P2 (inactivo), coordinador de P2, decano de F, administrador | — | EXEC | filas | 6 filas en orden estudiante, docente ×2, coordinador, decano, administrador; datos personales y detalle correctos | vínculos de otro usuario u orden inestable | DB real |
| P-02 | Sin vínculos / ámbito | U2 sin roles; U3 con otros roles | — | EXEC con U2 | filas | 1 fila con el vínculo en NULL; no aparece nada de U3 | perfil vacío o fuga de otro usuario | DB real |
| P-03 | Validación | id inexistente; ejecutor nulo | — | EXEC | filas o error | 0 filas; `VAL_001` | error técnico | DB real |
| PU-01..05 | Caso de uso | puerto mock | execute | dominio | mapeo de datos y vínculos; lista vacía si no hay vínculos; inexistente ⇒ 404; actor nulo ⇒ 400 sin DB; constructores; interactor a DTO | NPE o 500 | Unit |
| PR-01..05 | Repositorio | EM mock | consultarPerfil | SQL, parámetros, proyección | `EXEC` con parámetros nombrados y correlación; vínculos agrupados; fila sin vínculo da lista vacía; inexistente da Optional vacío; ejecutor nulo sin DB; `DatabaseOperationException` | vínculo nulo agregado | Unit |
| PC-01..03 | HTTP | puerto mock | GET | status y JSON | 200 con datos y vínculos y sin `password`; 404; constructores | — | MockMvc |
| UC-REG | Regresión | `UsuarioControllerTest` | — | PUT y POST | — | el PUT sigue en 501; la creación de usuario sin cambios | romper la provisión | MockMvc |
| CAT | Seguridad | catálogo vs Spring | — | `AccessCatalogConsistencyTest` | — | consistente (la ruta ya existía) | — | Unit |
| PL-01..05, MK-06 | UI | HttpTestingController | — | carga | DOM y HTTP | GET sin toast global; documento, correo, nombre y estado; lista de vínculos con etiqueta por rol y estado; correo sin confirmar; error visible; el mock responde | datos de claims en lugar de la DB | Frontend unit |

## Integration Requirement

DB real para el SP (SQL Server 2022 desechable). Mocks para backend y frontend.

## Negative / Boundary Coverage

- Ejecutor nulo.
- Usuario inexistente.
- Usuario sin vínculos.
- Vínculo inactivo.
- Correo sin confirmar.
- Cuenta inactiva.
- Error HTTP.
- Rol desconocido.

## Pruebas RED requeridas

- **DB:** `test_usp_consultar_perfil_usuario.sql`. Fallo esperado: SP inexistente.
- **Backend:** `ConsultarPerfilUseCaseImplTest`, `PerfilUsuarioJpaRepositoryTest`, `UsuarioPerfilControllerTest`. Fallo esperado: clases inexistentes; antes, el GET respondía 501.
- **Frontend:** `profile-links.component.spec.ts`.

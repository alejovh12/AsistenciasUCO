---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# VALIDATION — HU006: perfil propio

| Capa | Comando | Resultado | Evidencia |
|---|---|---|---|
| DB | `freeze-manifest -Mode Verify`, `deploy_schema.ps1` y `test_summary.ps1` sobre SQL Server 2022 limpio | PASS | `HU006_PERFIL_*` (3); `TOTAL_EXECUTED=343`, `FAILED=0`, `DB GATE PASS` |
| Backend | `mvnw verify` (unitarias, ArchUnit, `AccessCatalogConsistencyTest`, `JpaRepositoryBeanUniquenessTest` con 48, JaCoCo) | PASS | 2609 pruebas, 0 fallos, 0 errores, exit 0. Incluye `UsuarioControllerTest`: el PUT sigue en 501 y la creación de usuario no cambia. |
| Backend | Cobertura de líneas de las clases nuevas (JaCoCo) | PASS | 65/68 = 95,6 %. Las 3 líneas sin cubrir están en `PerfilUsuarioWiringConfiguration`. |
| Frontend | `npm run test:ci`, cobertura del código nuevo, `coverage:realtime:check`, `ng build --configuration=production` | PASS | 1263 SUCCESS; 90,9 %; PASSED; exit 0 |
| Documentos | WI de DB, backend y frontend; TD-049 marcado PARCIAL; `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, índice, MV-008, addendum de SP, tabla SP↔endpoint, contrato frontend y `frontend-features-pending` | PASS | ver [CLOSURE](CLOSURE.md) |

## Desviaciones

- No hay commits RED separados.
- Dos fallas intermedias, ya corregidas:
  - la primera corrida de DB falló por el fixture: NULL en columnas NOT NULL;
  - la plantilla de Angular usaba `as` en un `@else if`.

## Pendiente

- GitHub Actions y SonarCloud: la rama está en commits locales, sin subir, hasta que John confirme.
- MV-008: "Mi perfil" con un usuario de cada rol.
- PUT del perfil (TD-049): se coordina con Sergio.

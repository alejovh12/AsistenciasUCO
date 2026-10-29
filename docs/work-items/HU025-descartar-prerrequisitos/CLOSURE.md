---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-29
---

# CLOSURE — HU025: descartar prerrequisitos y correquisitos

## Decisión

John (2026-10-29): **los prerrequisitos y correquisitos no existen y no están incorporados en la aplicación.** La HU025 se descarta y se elimina todo lo referente. En la BD tampoco existían: solo había un stub, que se elimina en la rama de BD del mismo nombre.

## Cambio en el backend (rama `chore/HU025-descartar-prerrequisitos`)

- **`EstudiantePortalController`:** se elimina `GET /api/v1/estudiante/materias/{materiaId}/prerrequisitos`, que respondía 501 (`FeatureUnavailableException`).
- **`EstudiantePortalControllerTest`:** se quita ese caso de la prueba de "endpoints no implementados".
- **`infra/security-baseline/access-catalog.json`:** se elimina la entrada de la ruta. `AccessCatalogConsistencyTest` sigue alineado entre el catálogo y los controllers.
- **Documentación:**
  - `docs/contracts/HTTP_AS_IS_MATRIX.md`: se eliminan las dos filas de la ruta;
  - `docs/baseline/TECHNICAL_DEBT.md`: se elimina "PrerrequisitoAsignatura" de los contratos de BD pendientes;
  - en los documentos de `HU176B-matricula-qr-autonoma` (CONTRACT, PLAN y TEST_PLAN), las reglas reutilizadas quedan en cupo, cruce, duplicado y programa, sin prerrequisitos.

**No se tocó:**
- los scripts de `scratch/`, donde "Prerrequisitos" se refiere al entorno necesario;
- `MAINT-001`, donde "prerrequisito" es una precondición de una prueba.

## Para José Alejandro (PDP)

Retirar del aprovisionamiento del PDP la ruta `GET /api/v1/estudiante/materias/{materiaId}/prerrequisitos`.

## Validación

`mvn verify` (2026-10-29): `Tests run: 2681, Failures: 0, Errors: 0`, `All coverage checks have been met`, `BUILD SUCCESS`. `AccessCatalogConsistencyTest` en verde (catálogo y controllers alineados sin la ruta).

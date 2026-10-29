# FIX-003 — Validation

Fecha de ejecución: 2026-10-28
Rama de trabajo: `develop`
Alcance: backend y frontend; sin cambios SQL, Keycloak, PDP ni base de datos principal.

## Resultados

| Evidencia | Comando | Resultado |
|---|---|---|
| Backend dirigido | `./mvnw -Dtest=CrearCoordinadorUseCaseImplTest,CrearDecanoUseCaseImplTest test` | PASS — 26 tests, 0 fallos, 0 errores |
| Frontend dirigido | `npx ng test --watch=false --browsers=ChromeHeadless --include=src/app/features/admin/admin-decanos/admin-decanos.component.spec.ts --include=src/app/features/dean/dean-coordinadores/dean-coordinadores.component.spec.ts` | PASS — 49 tests, 0 fallos |
| Backend completo | `./mvnw verify` fuera del sandbox | PASS — 2.650 tests, 0 fallos, 0 errores; JaCoCo cumplió los gates; BUILD SUCCESS |
| Frontend build | `npx ng build --configuration=development --verbose` fuera del sandbox | PASS — generación de bundle completada, código 0 |

## Cobertura de la corrección

- Reintentos de Decano/Coordinador resuelven el usuario canónico por documento y correo antes de aprovisionar identidad.
- Se rechazan conflictos de identidad documento/correo sin ejecutar el comando de rol.
- El Coordinador ya no omite silenciosamente el aprovisionamiento si el reread canónico no aparece.
- El frontend evita doble envío, ignora respuestas GET antiguas y comprueba que la identidad creada aparezca en el listado antes de considerar cerrado el flujo.
- La ausencia de persona en PDP sigue mostrándose como estado pendiente; no se añadió sincronización privilegiada automática.
- Se eliminó el fallback inventado `Facultad Asignada`; ahora se informa `Sin Facultad Asignada` cuando el backend no entrega facultad.

## Limitaciones y no ejecutado

- Los servicios locales estaban detenidos durante la validación: no fue posible ejecutar E2E HTTP con la sesión legítima ni probar Keycloak/PDP/BFF en vivo.
- La primera corrida de `verify` dentro del sandbox produjo errores de socket local (`Permission denied: getsockopt`) en tests de ClamAV/PEP/Keycloak. La corrida repetida fuera del sandbox terminó PASS.
- No se ejecutaron migraciones, seeds, `deploy_schema.ps1`, resets, grants, restauraciones ni escrituras sobre `gestionasistenciadb`.
- No se alteraron los SP existentes ni los datos, perfiles, titularidades o asignaciones manuales.
- No se creó commit/PR en esta validación. El checkout oficial del frontend devuelve `fatal: this operation must be run in a work tree` para `git status` y `git diff`; no se reparó su metadata para evitar una mutación destructiva.

Estado de cierre: `VALIDATED_LOCALLY_PENDING_E2E`.

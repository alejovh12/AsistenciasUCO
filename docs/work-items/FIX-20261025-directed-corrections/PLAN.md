---
id: FIX-20261025-directed-corrections
status: IN_PROGRESS
type: focused-correction
primary_variable: FUNCTIONAL_RUNTIME_DEFAULTS
---

# Correcciones dirigidas — 2026-10-25

## Objetivo

Corregir dos comportamientos observables del backend identificados durante la
revisión dirigida: validar un cuerpo `null` en el PATCH de estado del
coordinador como entrada HTTP inválida y mantener UTC V2 activa cuando no se
declara la propiedad de activación.

## Evidencia y contrato aplicable

- `docs/governance/API_DESIGN_RULES.md`.
- `docs/contracts/BACKEND_GOLDEN_PATH_CONTRACT.md`.
- `docs/contracts/openapi/openapi-golden-path.yaml`.
- `docs/work-items/HU121-HU122-gestion-coordinador/CONTRACT.md`.
- `docs/work-items/MAINT-003-backend-utc-pagination-readiness/PLAN.md` y
  evidencia de UTC V2 existente en el repositorio.

## Alcance autorizado

- Backend: controlador y prueba focal de HU121/HU122.
- Backend: comportamiento de ausencia de la propiedad de activación UTC V2 y
  prueba focal correspondiente.
- Frontend: retirar el selector técnico de V1 del flujo normal de sesiones y
  consumir UTC V2 de forma predeterminada en el checkout oficial existente.
- Frontend: corregir la validación de rango horario que quedó condicionada al
  antiguo selector V1.
- Documentar validación y bloqueos contractuales encontrados.

## Fuera de alcance

- No cambiar SQL, esquema, vistas ni procedimientos; el owner DB mantiene ese
  contrato.
- No cambiar la semántica de aprobación de reclamos ni la proyección
  estudiante hasta contar con una decisión contractual sobre la combinación
  `asistio=1` + razón `SJC`.
- No eliminar endpoints V1: permanecen como compatibilidad mientras se retira
  su uso normal en consumidores coordinados.
- No cambiar la semántica de métodos frontend explícitamente fuera del Golden
  Path ni los endpoints V1 de compatibilidad ajenos al flujo normal revisado.
- No hacer push, merge ni crear/modificar un PR.

## Riesgos y rollback

- Riesgo: documentación histórica de MAINT-003 todavía describe activación UTC
  V2 como pendiente. La corrección conserva el opt-out explícito existente y
  no autoriza despliegue por sí sola.
- Rollback: revertir los cambios de este work item en la rama de trabajo; las
  modificaciones locales previas del usuario se preservan y no se mezclan con
  este alcance.

## Definition of Ready

READY para las dos correcciones focales: rutas, contratos, tests y evidencia
localizados. NOT_READY para cambiar DB o cerrar funcionalmente la activación
global UTC V2, por decisión y evidencia de integración pendientes.

## Decisión técnica: UTC V2 como flujo predeterminado (2026-10-25)

El responsable del proyecto (José Alejandro Valencia Henao) autoriza técnicamente,
para este PR, que UTC V2 sea el único flujo normal de sesiones: propiedad
`app.sesiones.v2.enabled` ausente o `true` habilita V2 con contrato SQL verificado;
`false` conserva solo el bloqueo contractual de compatibilidad (404); un valor
inválido detiene el arranque. Esta decisión es del responsable del proyecto y **no
sustituye** la aprobación formal de otros owners (DB, frontend, contratos): esas
aprobaciones siguen pendientes y se tramitan en la revisión del PR antes del merge.
No se autoriza despliegue ni cambios de esquema SQL.

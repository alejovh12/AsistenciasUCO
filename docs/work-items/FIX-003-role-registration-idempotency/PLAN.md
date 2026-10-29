---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-28
---

# PLAN — FIX-003: alta de decanos y coordinadores en un solo intento

## Objetivo

Corregir el defecto de integración observado al registrar decanos y coordinadores:
la UI puede enviar dos veces el mismo formulario y una respuesta antigua de listado
puede ocultar temporalmente un alta confirmada. El backend debe resolver siempre la
persona canónica por documento y correo antes/después del comando, para que un
reintento sobre la misma identidad reprovisione de forma idempotente la cuenta
existente sin crear otra relación.

## Evidencia AS-IS

- `usp_crear_decano` y `usp_crear_coordinador` consultan la persona por correo o
  documento y reutilizan el rol existente.
- `CrearDecanoUseCaseImpl` ya valida correo y documento y vuelve a leer la persona
  canónica; `CrearCoordinadorUseCaseImpl` solo prevalidaba por correo y podía omitir
  la reprovisión de identidad si el correo recibido no resolvía después del comando.
- `AdminDecanosComponent` y `DeanCoordinadoresComponent` no tenían guardia de envío
  ni versionado de consultas de listado.
- En la evidencia de QA, el alta HTTP fue `201`; SQL y Keycloak quedaron creados y
  el PDP reportó `PENDIENTE_PERSONA_NO_REGISTRADA`. Ese estado no concede acceso ni
  autoriza una sincronización privilegiada automática.

## Alcance autorizado

- Backend: resolver identidad existente por correo + documento, conservar la
  identidad canónica por `idUsuario`, y fallar explícitamente si el comando no
  permite releerla.
- Frontend: bloquear doble envío, liberar el bloqueo en éxito/error, ignorar
  respuestas de listados obsoletas y advertir si la recarga confirmada no contiene
  la identidad recién registrada.
- Pruebas unitarias de caso de uso y componente.

## Fuera de alcance

- DDL, migraciones, seeds, resets o escritura sobre la base principal.
- Creación automática de personas/perfiles en el PDP, asignación privilegiada,
  `pdp --prune`, Keycloak/PDP/claims y titularidades académicas.
- Cambios de contraseña o eliminación de cuentas existentes.
- Nuevas rutas HTTP, cambios de método/status/envelope o cambios en los SP.

## Riesgos y rollback

- Riesgo: una identidad que ya existe por documento y llega con otro correo debe ser
  conflicto, nunca una actualización silenciosa.
- Riesgo: una consulta HTTP antigua puede llegar después de la nueva; el contador de
  consulta evita que mutile el estado actual de la pantalla.
- Rollback: revertir únicamente los archivos del perímetro de este work item; no se
  requiere rollback de DB porque no hay cambio de esquema ni de datos.

## Definition of Ready

`READY`: AS-IS, consumidores, contratos existentes, alcance, no alcance, evidencia,
pruebas y rollback están definidos. Se conserva el contrato público de los POST.

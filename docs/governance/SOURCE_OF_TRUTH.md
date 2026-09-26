---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-24
---

# Autoridad y fuentes de verdad

## Precedencia

Dentro del ámbito aplicable, la precedencia es:

1. Contratos **aprobados** y ADR vigentes.
2. Documentación `type: normative`, `status: active`.
3. Código y tests existentes para describir el **AS-IS**.
4. Work item activo: alcance y evidencia de una tarea; no cambia unilateralmente las normas.
5. Runbooks: cómo operar/verificar, no qué comportamiento inventar.
6. Documentación histórica/archivo: trazabilidad sin autoridad vigente.

Un borrador no se convierte en contrato aprobado por existir en una carpeta. Una norma expresa lo exigido; un test existente puede evidenciar una limitación y no autoriza incumplirla. Ante contradicción relevante entre fuentes autoritativas, aplicar `CONTRACT_CONFLICT`; la precedencia no permite cambiar un ámbito ajeno.

## Ámbitos independientes

| Verdad | Autoridad y evidencia | Límite |
|---|---|---|
| DOMAIN | Requisito/decisión aprobados; invariantes en `application/features/**/usecase/domain` y tests | No copiar reglas de un proveedor o de Arquisoft |
| HTTP CONTRACT | [OpenAPI canónico](../contracts/openapi/openapi-golden-path.yaml) aprobado para el Golden Path; para operaciones todavía no cubiertas, [matriz AS-IS](../contracts/HTTP_AS_IS_MATRIX.md) y código/tests como evidencia. El código del frontend solo prueba lo que consume; no redefine el contrato | SQL no autoriza renombrar JSON, cambiar estados HTTP o paginar |
| PERSISTENCE CONTRACT | Esquema/vistas/SP liberados por el equipo DB; adapters e IT describen lo que Java consume | Los SQL strings no prueban que esa versión está desplegada; backend no crea/actualiza esquema |
| SECURITY CONTRACT | [Arquitectura runtime](../security/runtime-security-provider-architecture.md), política institucional y tests | No inventar roles, claims, ownership ni permisos por conveniencia del frontend |
| REALTIME CONTRACT | [Estándar de eventos](../contracts/REALTIME_EVENT_STANDARD.md), contrato aprobado y [arquitectura SSE](../architecture/reactive-realtime.md) | API/DB conservan el estado; SSE local no garantiza entrega ni distribución |

## Repositorios dueños de contrato externo

Cuando el repositorio dueño de un contrato externo esté disponible, su evidencia versionada/aprobada prevalece **para su ámbito**:

- PERSISTENCE: schema/vistas/SP liberados por el equipo DB.
- HTTP: OpenAPI aprobado.
- Frontend: su código solo prueba lo que consume; no redefine el contrato HTTP.

La evidencia externa se incorpora como snapshot trazable según el [protocolo de alineación contractual](../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Los [ADR](../README.md#adr) aceptados no certifican runtime ni autorizan iniciar fases. El estado y las limitaciones vigentes de cada contrato se registran en el work item activo, la [línea base](../baseline/LINEA_BASE.md) y los ledgers correspondientes ([deuda técnica](../baseline/TECHNICAL_DEBT.md), [validación manual](../baseline/MANUAL_VALIDATION_LEDGER.md)).

## Protocolo de bloqueo

Registrar ID, estado (`CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT` o `BLOCKED_BY_MISSING_EVIDENCE`), fuentes exactas, alcance afectado, evidencia requerida y responsable por rol en el work item. Detener solo la implementación dependiente. Contratos/equipo dueño resuelve; el auditor verifica; el tester corrige tests si procede y vuelve a registrar RED. Nunca edita esos tests el implementador para acomodar su código.

Una discrepancia histórica comprobada se marca sustituida y se corrige citando el código; no se presenta como una nueva decisión funcional. Un conflicto no demostrable permanece abierto.

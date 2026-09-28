---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Protocolo de alineación contractual entre repositorios

Define **cómo** comparar contratos entre DB, backend y frontend. No contiene ninguna comparación; esta se registra en el work item con la plantilla [CONTRACT_MATRIX](../../.claude/templates/CONTRACT_MATRIX.md). Precedencia general en [SOURCE_OF_TRUTH](../governance/SOURCE_OF_TRUTH.md).

## Roles

| Sistema | Rol contractual |
|---|---|
| DB | **Dueño** del contrato de persistencia (schema, vistas, SP, funciones liberados) |
| Backend | **Consumidor** de persistencia y **dueño** del contrato HTTP cuando exista OpenAPI aprobado |
| Frontend | **Consumidor** del contrato HTTP/realtime; su código prueba lo que consume, no redefine el contrato |

- Una query SQL embebida en Java describe lo que el backend espera, no lo que la DB desplegada ofrece.
- Una columna existente en DB no se expone automáticamente por HTTP.

## Flujo

```
EVIDENCE → AS-IS OWNER → AS-IS CONSUMER → DIFF → CLASSIFICATION → DECISION → CONTRACT FREEZE → TEST PLAN → IMPLEMENTATION
```

1. **EVIDENCE:** registrar cada fuente con la identidad exigida abajo.
2. **AS-IS OWNER / CONSUMER:** describir cada lado por separado, solo con su evidencia.
3. **DIFF:** comparar dimensión por dimensión.
4. **CLASSIFICATION:** asignar un estado por fila.
5. **DECISION:** el dueño/aprobador resuelve cada `MISMATCH`/`DECISION_REQUIRED`, con referencia versionada.
6. **CONTRACT FREEZE:** el contrato TARGET se fija a partir del AS-IS y las decisiones; nunca se inventa un TARGET para adaptar después DB o backend.
7. **TEST PLAN → IMPLEMENTATION:** solo con [DoR](../governance/DEFINITION_OF_READY.md) READY.

## Identidad de la evidencia externa

Evidencia de otro repositorio es un **SNAPSHOT trazable**, no una fuente de verdad permanente ni una copia mantenida dentro del backend. Registrar:

- repositorio/proyecto; commit SHA y branch si están disponibles; fecha de captura;
- archivo exacto y objeto/símbolo;
- versión DB/migración si existe;
- SHA-256 del ZIP o snapshot si no hay Git confiable;
- limitaciones de la evidencia.

Nunca copiar secretos, cadenas de conexión ni datos personales.

## Estados de alineación

| Estado | Significado |
|---|---|
| MATCH | Ambos lados coinciden en la dimensión, con evidencia |
| MISMATCH | Ambos existen y difieren |
| MISSING_IN_PROVIDER | El consumidor espera algo que el dueño no ofrece |
| MISSING_IN_CONSUMER | El dueño ofrece algo que el consumidor no usa/refleja |
| DECISION_REQUIRED | Requiere decisión humana del dueño/aprobador |
| BLOCKED_BY_MISSING_EVIDENCE | Falta evidencia necesaria |
| NOT_APPLICABLE | No aplica, con justificación |

`MISMATCH` no autoriza al agente a elegir quién cambia. Cada uno registra: OWNER, CONSUMER, impacto, fuentes, acción requerida y decisión/aprobación. Prohibidas etiquetas ambiguas («más o menos compatible», «parece correcto», «probablemente funciona»).

## Dimensiones (DB ↔ backend)

Cuando apliquen: objeto DB (tabla/vista/SP/función); nombre esperado por backend; inputs; outputs; tipo DB; tipo Java; nullability; longitud/precisión/escala; identificadores; enums/códigos; semántica de estados; errores/códigos de error; orden/paginación; atomicidad/transacción; idempotencia; concurrencia; timestamps/timezone; ownership/seguridad contextual que afecte la consulta; efectos secundarios; fixtures/pruebas disponibles.

No toda fila requiere todos los campos; usar `NO APLICA` con justificación.

## Límites

- El análisis (`CONTRACT_ANALYSIS`) no modifica producción, tests, SQL ni frontend.
- Ninguna migración JDBC → JPA ni implementación parte de un contrato con `MISMATCH`, `DECISION_REQUIRED` o `BLOCKED_BY_MISSING_EVIDENCE` en la parte afectada.

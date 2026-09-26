---
status: active
type: normative
scope: backend-http-api
owner: backend-team
last-reviewed: 2026-09-24
---

# Reglas de diseño de API

## Autoridad y lectura obligatoria

Antes de crear o modificar una API se deben leer, en este orden:

1. este documento;
2. [BACKEND_GOLDEN_PATH_CONTRACT](../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md) cuando el
   cambio toque el Golden Path;
3. el [OpenAPI canónico](../contracts/openapi/openapi-golden-path.yaml) aplicable;
4. el work item activo y sus decisiones.

Los agentes no inventan endpoints, métodos, códigos, roles, campos, guarantees ni límites. Primero
inventarían AS-IS y consumidores; después clasifican el diff; solo una decisión aprobada crea el
TARGET. OpenAPI YAML versionado es la especificación. Swagger Editor/UI son tooling: annotations
Java y documentación generada no reemplazan el contrato canónico.

## Estilo obligatorio

`PRAGMATIC_RESOURCE_PLUS_BUSINESS_COMMAND`.

La URL y el método expresan semántica de recurso o intención de negocio. El SQL/SP subyacente no
decide el método HTTP: un `UPDATE` no implica PUT/PATCH y un soft-delete no implica DELETE.

## Árbol de decisión resource vs command

```text
¿Lectura de un recurso?                         → GET
¿Creación cuyo identificador controla servidor? → POST sobre colección
¿Cambio parcial de atributos representados?     → PATCH
¿Reemplazo completo e idempotente?               → PUT + METHOD_EXCEPTION
¿Transición/acción de negocio?                   → POST /recurso/{id}/{verbo-negocio}
¿Operación batch de negocio?                     → POST batch command
¿Eliminación real del recurso público?           → DELETE + METHOD_EXCEPTION
```

Si una operación encaja en más de una rama, el contrato documenta la intención, consumidores,
idempotencia y alternativas antes de elegir.

## Métodos

### GET

Lectura sin cambios de estado de negocio. No se usa GET para commands ni para “facilitar” una UI.

### POST — resource creation

Creación de un recurso cuando el servidor controla su creación/identificador. Normalmente retorna
201 cuando se crea sincrónicamente; no se impone ese status sin comportamiento real.

### POST — business command

Método preferido para acciones y transiciones que no son CRUD mecánico, con verbo de dominio
claro: `cerrar`, `cancelar`, `retirar`, `desactivar`, `matricular-estudiante`. Evitar `execute`,
`process`, `run`, `doStuff`, `doAction` o `updateStatus` salvo legado documentado.

### PATCH

Modificación parcial de atributos de la representación. El contrato define ausencia de campo,
nullability y validación; no obliga a enviar el recurso completo. No se usa para ocultar un
business command independiente.

### PUT — restringido por defecto

Solo reemplazo completo e idempotente de la representación pública. No se usa para partial update.
Todo PUT **nuevo** requiere `METHOD_EXCEPTION`. Los PUT existentes se inventarían y clasifican; no
se cambian sin fase de migración y análisis de consumidores.

### DELETE — restringido por defecto

Solo eliminación real del recurso público. Cancelar, cerrar, archivar, desactivar, retirar,
rechazar, restaurar y soft-delete son business commands. Todo DELETE **nuevo** requiere
`METHOD_EXCEPTION`. Los existentes no se rompen automáticamente.

## METHOD_EXCEPTION

Toda propuesta de PUT o DELETE nuevo incluye en el work item:

```text
METHOD_EXCEPTION: YES
RATIONALE: <por qué el método representa la semántica pública>
FULL_REPLACEMENT_OR_TRUE_DELETE: <evidencia>
ALTERNATIVES_CONSIDERED: PATCH / POST ACTION / OTHER
```

Sin los cuatro campos y su evidencia, contratos/auditor marcan `DECISION_REQUIRED`; el cambio no
se implementa. Esta regla no convierte endpoints preexistentes en excepciones aprobadas.

## Compatibilidad y clasificación

Un endpoint existente se clasifica como `MATCH`, `LEGACY_ACCEPTED`, `MIGRATION_CANDIDATE`,
`DECISION_REQUIRED`, `BLOCKED` o `NA`. No se cambia URL/método/status/schema de un consumidor
validado sin decisión de migración, compatibilidad, deprecation plan y pruebas.

`PUT /api/v1/sesiones/{sesionId}` es AS-IS publicado: permanece compatible en LB-001C.1 y se
clasifica `MIGRATION_CANDIDATE` porque modifica solo `{nombre, fechaHoraInicio, fechaHoraFin}` de
una representación más amplia. No autoriza nuevos PUT parciales.

## Batch e idempotencia

Batch de negocio usa POST. El contrato debe documentar atomicidad, partial success, idempotencia,
tamaño máximo y errores **solo si hay evidencia**. Si no están definidos: `NOT_DEFINED`.

Cada command mutante declara `IDEMPOTENT: YES | NO | CONDITIONAL | NOT_DEFINED`. No se inventa
`Idempotency-Key`; retries seguros que lo necesiten requieren decisión futura.

## URLs, idioma y versionado

- Recursos: sustantivos; comandos: verbo de dominio tras el recurso.
- Mantener español, la convención vigente; no mezclar idiomas sin decisión.
- Mantener `/api/v1` AS-IS. No crear v2 en LB-001C.1.
- Breaking changes futuros requieren decisión, consumidores, migración y deprecación.

## Status y respuestas

Cada response OpenAPI debe estar respaldada por código, test, contrato o comportamiento
demostrado. No añadir 200/201/202/204/400/401/403/404/409/422/500/501 mecánicamente. Los errores
públicos conservan `ApiErrorResponse`; clientes deciden por `code`, no por `message`.

`DBCODE`, mensajes SQL/JDBC, stack traces, detalles Keycloak, secretos y PII no son contrato
público. `DBCODE` pertenece exclusivamente a DB→backend.

## Seguridad y correlación

- Bearer: `type: http`, `scheme: bearer`, `bearerFormat: JWT`.
- Seguridad por operación según `SecurityConfig` y ownership real; distinguir 401/403.
- Roles documentados cuando son contrato. Tokens/credenciales reales nunca son examples.
- `X-Correlation-Id` conserva el comportamiento de `CorrelationIdFilter`: UUID canónico válido se
  acepta; ausente/inválido se reemplaza; el response lo expone. No es obligatorio para el cliente.

## Tiempo

- Instantes nuevos: `type: string`, `format: date-time`, UTC (`Z`) u offset explícito.
- Horario académico conserva JSON real de `horaInicio`/`horaFin` como hora local institucional.
- `Sesion.fechaHoraInicio/fechaHoraFin` AS-IS viajan como ISO local sin offset aunque la
  persistencia se interpreta en UTC. OpenAPI lo marca explícitamente como compatibilidad legacy;
  una migración a wire UTC/offset requiere fase y consumidores, no se hace silenciosamente.

## Paginación, concurrencia y orden

- DR-010: colecciones Golden Path completas, sin `page`, `size`, `offset` ni `cursor`.
- Una paginación futura exige decisión por operación.
- No se promete orden que SQL/contrato no garantiza.
- No inventar ETag/If-Match. Para riesgo real de lost update, ETag/If-Match es la estrategia
  futura preferida, fuera de alcance salvo contrato aprobado.

## Realtime/SSE

SSE es señal de notificación separada de CRUD. HTTP/DB es source of truth.
`Content-Type: text/event-stream`. Heartbeat solo liveness. No prometer replay, exactly-once,
durabilidad, orden global, `Last-Event-ID` ni pub/sub distribuido. Bearer va en header y el stream
se filtra/autoriza por grupo.

## Invariantes Golden Path

- Estados de asistencia: enum cerrado `AN | SJC | EX`; desconocido falla cerrado.
- Falta de fila de asistencia no significa `AN`.
- No crear `PRESENTE`, `AUSENTE`, `PENDING` ni `UNKNOWN` contractuales.
- Sesión leída: `sesion`, `grupo`, `nombre`, `numero`, `codigo`, `numeroSemana`, `codigoGrupo`,
  `nombreGrupo`, `fechaHoraInicio`, `fechaHoraFin`.
- Crear sesión: `grupo`, `nombre`, `fechaHoraInicio`, `fechaHoraFin`; actualizar AS-IS:
  `nombre`, `fechaHoraInicio`, `fechaHoraFin`; `nombre` 1..50.
- No revivir `tema/topic/descripcion/aula/room/tipo/status/docente`.

## Gate Contract First

Secuencia: `REQUIREMENT → CONTRACT → TEST_PLAN → RED → GREEN → VALIDATE`.
El verify debe rechazar YAML/OpenAPI inválido, `$ref` rotos, errores de schema y divergencias
críticas de controller/DTO. El implementador no cambia tests RED; el auditor ejecuta el HTTP
METHOD AUDIT y el cierre exige spec, SHA y gate verdes.

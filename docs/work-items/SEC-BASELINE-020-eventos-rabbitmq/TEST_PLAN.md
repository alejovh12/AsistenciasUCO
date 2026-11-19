# TEST_PLAN — SEC-BASELINE-020: Consumidor de eventos RabbitMQ de asignación

Trazable al PLAN.md de este work item y al contrato externo `AssignmentChangedEventV1` (manual del PDP, sección 5 y 6).

## Unit — `RegistrarEventoAsignacionUseCaseImpl`

1. Evento con `schemaVersion` distinto de 1 → lanza `EventoAsignacionInvalidoException`; el puerto secundario NO se invoca.
2. Evento con `applicationId` distinto del configurado para `asistencias-uco` → lanza `EventoAsignacionInvalidoException`; el puerto secundario NO se invoca.
3. Evento válido nuevo (`registrarSiNuevo` del puerto devuelve `true`) → el caso de uso devuelve `PROCESADO`.
4. Evento válido repetido (`registrarSiNuevo` devuelve `false`) → el caso de uso devuelve `DUPLICADO`; no lanza.
5. Falla del puerto secundario (p. ej. `RuntimeException` simulando BD caída) → se propaga sin envolver (el llamador decide NACK+requeue).

## Unit — `AssignmentChangedEventMapper`

6. JSON de ejemplo de la sección 5 del manual (con `roles[]` y campos nulos de perfil) → mapea 1:1 a
   `EventoAsignacionSeguridad` (incluye `eventId`, `schemaVersion`, `eventType`, `applicationId`, `userId`,
   `correlationId`, `assignmentId`, `roleId`, `roleName`).
7. JSON con campos adicionales desconocidos → se ignoran (no falla la deserialización).

## Unit — `AssignmentEventsRabbitListener`

8. Mensaje JSON ilegible (no es un objeto JSON) → `channel.basicReject(tag, false)` (sin requeue); no llama al caso de uso.
9. Caso de uso lanza `EventoAsignacionInvalidoException` → `channel.basicReject(tag, false)`.
10. Caso de uso devuelve `PROCESADO` o `DUPLICADO` → `channel.basicAck(tag, false)`.
11. Caso de uso lanza una `RuntimeException` genérica (BD caída) → `channel.basicNack(tag, false, true)` (requeue).

## Arquitectura (ArchUnit)

12. Ninguna clase bajo `application..` importa `org.springframework.amqp..` ni `com.rabbitmq..`.
13. `AssignmentEventsRabbitListener` no depende de `AuditEventJpaRepository` ni de ningún adapter `secondary..`
    directamente: solo del `RegistrarEventoAsignacionInputPort`.

## Integración real (`*IT.java`, perfil `-Pintegration`, RabbitMQ real del lab + BD real)

14. Se publica (vía `RabbitTemplate` de test o HTTP management API) un evento válido con un `eventId` nuevo en la cola
    `security.assignment.<applicationId>` → aparece exactamente una fila nueva en `dbo.AuditoriaEvento` con
    `resourceType=SECURITY_ASSIGNMENT_EVENT` e `id = eventId`.
15. Se publica el mismo mensaje una segunda vez (mismo `eventId`) → sigue existiendo una sola fila (no se duplica).
16. Se publica un evento con `applicationId` ajeno a `asistencias-uco` → no se audita (rejected); no aparece fila.
17. Se publica un evento con `schemaVersion=2` (no soportado) → no se audita (rejected); no aparece fila.

## RED_SNAPSHOT

**No hay snapshot RED aislado que registrar.** El contrato del evento es externo (manual del PDP) y no requirio
diseño propio, y la implementacion (puertos, caso de uso, adaptadores) y los tests unitarios/arquitectura de este
work item se escribieron en la misma sesion, sin una corrida roja capturada como paso separado antes de que las
clases de produccion existieran. Esto se declara explicitamente en vez de fabricar un snapshot: no cumple
`TESTING_STANDARD.md#red_snapshot` al pie de la letra para esta pieza.

Trazabilidad disponible en su lugar: base commit `9adf99fb` (develop); los tests unitarios y de arquitectura
corrieron en verde junto con toda la suite existente (729 clases, `mvnw test`, exit code 0; ver VALIDATION.md); la
integracion real (`AssignmentEventsRabbitListenerIT`) se verifico contra el RabbitMQ y la BD reales del lab
(`mvnw -Pintegration verify`, ver VALIDATION.md) cubriendo los 4 escenarios del manual del PDP.

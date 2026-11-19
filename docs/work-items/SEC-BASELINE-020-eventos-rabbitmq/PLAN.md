# PLAN — SEC-BASELINE-020: Consumidor de eventos RabbitMQ de asignación (PDP 0.1.14)

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-11-19 / planificador+implementador en una sesión / `develop` (9adf99fb).
- Objetivo y criterios de aceptación: el backend de AsistenciasUCO consume de forma confiable los eventos
  `assignment.changed.<applicationId>` que el PDP (Security Baseline 0.1.14) publica en RabbitMQ cuando se asigna o
  revoca un rol/perfil, y deja un registro de auditoría idempotente por `eventId` para trazabilidad. No cambia ninguna
  decisión de autorización local (el backend sigue validando en vivo contra el PDP en cada request). Aceptación:
  mensaje válido nuevo → auditado y ACK; mensaje repetido (mismo `eventId`) → ACK sin duplicar auditoría;
  `schemaVersion`/`applicationId` inválidos → `reject` sin requeue; caída de BD → `nack` con requeue.
- Skills y fuentes autoritativas: [uco-seguridad](../../../.claude/skills/uco-seguridad/SKILL.md),
  [uco-arquitectura](../../../.claude/skills/uco-arquitectura/SKILL.md),
  [uco-realtime](../../../.claude/skills/uco-realtime/SKILL.md),
  [uco-testing](../../../.claude/skills/uco-testing/SKILL.md),
  [uco-observabilidad](../../../.claude/skills/uco-observabilidad/SKILL.md).
  Fuente externa autoritativa del contrato: manual entregado por el usuario,
  "MANUAL-IMPLEMENTACION-EVENTOS-RABBITMQ.pdf" ("Security Baseline · Eventos RabbitMQ", versión de distribución
  0.1.14) — guía de integración de aplicaciones consumidoras, propiedad del equipo de Seguridad-UCO (owner del PDP).
  Copia de evidencia (texto extraído): `evidence/MANUAL-RABBITMQ-0.1.14.txt` en este work item.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Lab local Security Baseline corre en 0.1.13, sin RabbitMQ. | [infra/security-baseline/compose.yaml](../../../infra/security-baseline/compose.yaml), [lab.env.example](../../../infra/security-baseline/lab.env.example) | `docker ps` confirma `asistencias-security-lab-{pdp,pep,keycloak,opa,redis,surrealdb}-1` en `0.1.13`, sin servicio `rabbitmq`. |
| El `applicationId` de `asistencias-uco` en el PDP ya existe y es estático por instalación. | `%USERPROFILE%\.asistencias-security-lab\lab.env` (fuera de Git) | `ASISTENCIAS_PDP_APPLICATION_ID=a0b42637-39f8-4ddd-bec2-0781ba08e779`. No se versiona ni se imprime el archivo completo; solo este valor no sensible (UUID de aplicación, no secreto). |
| El backend ya resuelve ese mismo `applicationId` en tiempo de ejecución contra la API admin del PDP (uso distinto: aprovisionamiento de perfiles, no mensajería). | [SecurityBaselinePdpProfileClient.java:220-232](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/securitybaseline/SecurityBaselinePdpProfileClient.java) | Caché en memoria por `applicationName=asistencias-uco`; confirma que el valor de lab.env es correcto y es el mismo que usa el backend hoy. |
| El backend es quien empuja los cambios de rol/perfil hacia el PDP (`AccessProfilePort.sincronizar/revocar`), no al revés. | [AccessProfilePort.java](../../../src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/security/AccessProfilePort.java) | Confirma que el evento RabbitMQ es una notificación de confirmación/fan-out, no la fuente de la mutación; no hay caché local de roles que este evento deba invalidar. |
| No existe hoy ninguna caché local de roles/perfiles por usuario que un evento de asignación deba invalidar. | búsqueda `cache.*rol`, `RoleCache` en `docs/` y `src/main/java` | Sin resultados relevantes; decisión confirmada con el usuario (ver abajo) de que el efecto local es solo auditoría. |
| Existe infraestructura de auditoría durable reutilizable, con inserción `REQUIRES_NEW` y columna `id` como PK. | [AuditEventJpaRepository.java](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/AuditEventJpaRepository.java), [AuditEventEntity.java](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/entity/AuditEventEntity.java) | `AuditEventPublisher`/`LoggingAuditEventPublisher` son best-effort (atrapan y solo loguean la falla de persistencia): **no** sirven para esta pieza porque el contrato exige NACK+requeue si la BD falla. Se reutiliza `AuditEventJpaRepository` directamente (ambas clases son Infrastructure), no el publisher. |
| Patrón ya establecido para "adaptador primario de mensajería externa → puerto de entrada de Application", sin idempotencia en BD (usa `Set` en memoria acotado). | [MessageUcoLabEventListener.java](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/messaging/messageucolab/MessageUcoLabEventListener.java), [SincronizarMensajesCatalogoUseCaseImpl.java](../../../src/main/java/co/edu/uco/asistenciasuco/application/features/catalogo/sincronizarmensajes/usecase/impl/SincronizarMensajesCatalogoUseCaseImpl.java) | Se reutiliza la forma (Primary Adapter → Application InputPort), **no** la idempotencia en memoria: el manual del PDP exige persistencia durable del `eventId` ("No use un Set en memoria como solución final"), fuente externa que prevalece para esta pieza. |
| No hay dependencia `spring-boot-starter-amqp` ni Testcontainers en `pom.xml`; las pruebas de integración existentes se conectan a servicios Docker locales reales (convención `**/*IT.java`, perfil `-Pintegration`, `application-integration.yml`), no a Testcontainers. | [pom.xml](../../../pom.xml) | `grep -i amqp\|testcontainers pom.xml` sin resultados. Se sigue la convención existente: IT contra el RabbitMQ real del lab, no Testcontainers nuevo. |
| Las imágenes 0.1.14 de `security-{pdp,pep,keycloak-local,opa}` y `rabbitmq:4.4.1-management` existen en el registry. | Docker Hub API (`davidalzate/security-pdp`, `library/rabbitmq` tags) | Verificado en línea el 2026-11-19; no se asume sin comprobar. |

## TARGET

- `infra/security-baseline/compose.yaml` en `0.1.14`, con el servicio `rabbitmq` (según el bloque canónico del manual,
  endurecido igual que los demás servicios: solo `127.0.0.1`, red `lab`, sin credenciales literales) y las variables
  `SPRING_RABBITMQ_*` + `depends_on.rabbitmq` en el servicio `pdp`.
- Exchange `security.assignment.events` (direct, durable), cola `security.assignment.<applicationId-asistencias-uco>`
  y binding con routing key `assignment.changed.<applicationId-asistencias-uco>`, creados en el lab local.
- Backend: nuevo adaptador consumidor (hexagonal) que decodifica `AssignmentChangedEventV1`, valida `schemaVersion`
  y `applicationId`, y audita el evento de forma idempotente por `eventId` (PK de `dbo.AuditoriaEvento`).
- `pom.xml` con `spring-boot-starter-amqp`.

## Clase de cambio y alcance de rutas

- Change class: **INFRASTRUCTURE** (servicio nuevo + dependencia nueva + nuevo adaptador de entrada; no hay
  `CONTRACT_CHANGE` de HTTP/OpenAPI ni `PERSISTENCE_MIGRATION`; el `BEHAVIOR_CHANGE` queda acotado a "nuevo efecto
  aditivo de auditoría", sin tocar ninguna decisión de autorización existente).
- PRIMARY_VARIABLE: consumo confiable e idempotente del evento `AssignmentChangedEventV1` (una sola variable:
  "¿el backend registra una vez, y solo una vez, cada evento de asignación que le pertenece?").
- ALLOWED_PATHS:
  - `infra/security-baseline/compose.yaml`, `infra/security-baseline/lab.env.example`
  - `pom.xml` (una dependencia: `spring-boot-starter-amqp`)
  - `src/main/resources/application.yml`, `src/main/resources/application-integration.yml` (propiedades nuevas, sin tocar las existentes)
  - NUEVO: `src/main/java/.../application/features/seguridad/registroeventoasignacion/**`
  - NUEVO: `src/main/java/.../application/secondaryports/security/RegistroEventoAsignacionPort.java`
  - NUEVO: `src/main/java/.../infrastructure/adapter/primary/messaging/rabbitmq/**`
  - NUEVO: `src/main/java/.../infrastructure/config/adapters/security/AssignmentEventsRabbitConfiguration.java` (o ruta equivalente de Composition Root)
  - Extensión puntual (un método nuevo, sin tocar los existentes): `AuditEventJpaRepository.java`
  - `src/test/java/.../**` (tests nuevos para lo anterior) y `src/test/resources/application-integration.yml` si aplica
  - `docs/work-items/SEC-BASELINE-020-eventos-rabbitmq/**`
- FORBIDDEN_PATHS: controllers HTTP, OpenAPI, SQL/esquema de BD (se reutiliza `dbo.AuditoriaEvento` existente sin
  alterar su DDL), `AccessProfilePort`/sincronización de perfiles, frontend, cualquier archivo de otra feature en curso
  (p. ej. `AdministracionNotificacionJpaAdapter.java`, `PlantillaPorDefectoCatalogo.java`, `PurgaAuditoriaScheduler.java`
  ya modificados/sin commitear por otra tarea — no se tocan ni se mezclan en este work item).
- CONTRACTS: evento asíncrono externo `AssignmentChangedEventV1` (`schemaVersion=1`), propiedad del PDP; el backend es
  consumidor de solo lectura, no negocia el contrato.
- PROVIDERS: RabbitMQ 4.4.1 (imagen oficial `rabbitmq:4.4.1-management`) dentro del lab `asistencias-security-lab`.
- EXTERNAL_ENVIRONMENT: lab local Docker Compose del usuario (`asistencias-security-lab`), actualizable porque el
  usuario autorizó explícitamente la actualización a 0.1.14 para esta tarea.
- SECURITY_IMPACT: el consumidor valida `applicationId` ajeno (rechazo, nunca procesa eventos de otra aplicación) y no
  imprime ni versiona credenciales RabbitMQ; la cola es propia y durable, sin acceso cruzado entre aplicaciones.
- OBSERVABILITY_IMPACT: nuevo registro en `dbo.AuditoriaEvento` por evento de asignación recibido; logs estructurados
  en el listener (éxito, duplicado, contrato inválido, fallo) sin datos sensibles.
- TEST_LEVEL_REQUIRED: unit (mapper, validación de contrato, servicio de aplicación con puerto secundario fake),
  arquitectura (ArchUnit: el listener no depende de Application más allá del InputPort; Application no importa
  `org.springframework.amqp`/`com.rabbitmq`), integración real contra el RabbitMQ del lab (`**/*IT.java`,
  `-Pintegration`): mensaje válido, duplicado, contrato inválido, aislamiento de otra `applicationId`.
- ROLLBACK: `docker compose -f infra/security-baseline/compose.yaml down` del servicio `rabbitmq` únicamente (no
  `down -v`), revertir `SECURITY_VERSION` a `0.1.13` en `lab.env` si las imágenes 0.1.14 de PDP/PEP/Keycloak dieran
  problemas, y revertir el commit del backend (el nuevo listener es aditivo: su ausencia no rompe nada existente,
  la cola simplemente deja de tener consumidor).
- CONSUMERS: ninguno interno (nadie en AsistenciasUCO depende todavía de este registro de auditoría); consumidor
  externo del exchange es exclusivamente el PDP (productor). No hay impacto en frontend ni en otros repositorios.
- STOP_CONDITIONS: si las imágenes 0.1.14 no levantan healthy, o si el `applicationId` de lab.env no coincide con el
  que realmente tiene `asistencias-uco` en el PDP tras la actualización (reaprovisionamiento involuntario), se detiene
  y se registra `BLOCKED_BY_MISSING_EVIDENCE` antes de crear la cola.

## Alcance

Actualizar el lab local a 0.1.14 con RabbitMQ; crear la topología (exchange/cola/binding) para `asistencias-uco`;
implementar el adaptador consumidor hexagonal (listener AMQP → puerto de entrada de Application → puerto secundario
de auditoría idempotente); pruebas unitarias, de arquitectura e integración real; validar los 4 escenarios del manual
(válido, duplicado, contrato inválido, aislamiento X/Y) contra el lab real.

## No alcance

Ningún efecto funcional más allá de auditar (no se invalida caché porque no existe; no se notifica al usuario; no se
cambia ninguna decisión de autorización). No se toca `AccessProfilePort` ni el flujo de sincronización de perfiles
existente. No se despliega RabbitMQ en ningún ambiente remoto/productivo (fuera del lab local) ni se gestiona ese
despliegue: es responsabilidad del equipo de Seguridad-UCO. No se construye un mecanismo genérico de eventos
reutilizable para otros contratos futuros (YAGNI): solo `AssignmentChangedEventV1`.

## Archivos afectados

**EXISTENTES** (ruta comprobada):
- `infra/security-baseline/compose.yaml`, `infra/security-baseline/lab.env.example`
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/.../infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/AuditEventJpaRepository.java`
  (un método nuevo: `insertIfAbsent`)

**NUEVOS** (realidad final, ver también `common/dto/EventoAsignacionSeguridad.java` compartido entre ambos puertos
para no acoplar el secundario al paquete del primario):
- `src/main/java/.../application/features/seguridad/registroeventoasignacion/common/dto/EventoAsignacionSeguridad.java`
- `src/main/java/.../application/features/seguridad/registroeventoasignacion/primaryports/RegistrarEventoAsignacionInputPort.java`
- `src/main/java/.../application/features/seguridad/registroeventoasignacion/usecase/RegistrarEventoAsignacionUseCase.java`
- `src/main/java/.../application/features/seguridad/registroeventoasignacion/usecase/impl/RegistrarEventoAsignacionUseCaseImpl.java`
- `src/main/java/.../application/secondaryports/security/RegistroEventoAsignacionPort.java`
- `src/main/java/.../infrastructure/adapter/secondary/audit/RegistroEventoAsignacionAuditAdapter.java`
- `src/main/java/.../infrastructure/adapter/primary/messaging/rabbitmq/AssignmentEventsRabbitListener.java`
- `src/main/java/.../infrastructure/adapter/primary/messaging/rabbitmq/AssignmentChangedEventMapper.java` (lectura
  campo a campo del JSON, como `MessageUcoLabEventListener.leer`; se descarta el `AssignmentChangedEventV1.java`
  que sugiere el manual como record de deserializacion porque esa lectura manual ya tolera propiedades
  desconocidas sin depender de configuracion adicional de Jackson — simplificacion deliberada, el contrato JSON
  en si no cambia).
- `src/main/java/.../infrastructure/config/properties/providers/SecurityBaselineRabbitMqProperties.java`
  (prefijo real `app.providers.security-baseline-rabbitmq`, no `security.assignment-events` como en el ejemplo del
  manual — se sigue la convencion ya establecida en este repositorio de `app.adapters.*`=QUE tecnologia y
  `app.providers.*`=COMO se configura, ver `docs/architecture/adapter-composition-standard.md`).
- `src/main/java/.../infrastructure/config/adapters/security/AssignmentEventsRabbitConfiguration.java`
  (gateada por `app.adapters.assignment-events.enabled`, default `false`; no declara el `DirectExchange` como bean
  porque el exchange es compartido entre aplicaciones consumidoras y lo aprovisiona la plataforma, no cada app).
- Tests: `RegistrarEventoAsignacionUseCaseImplTest`, `AssignmentChangedEventMapperTest`,
  `AssignmentEventsRabbitListenerTest`, `RegistroEventoAsignacionAuditAdapterTest` (unit, mocks);
  `MessagingPrimaryAdaptersMustDependOnlyOnInputPortsTest` (ArchUnit, nuevo, generaliza
  `ControllersMustDependOnlyOnInputPortsTest` a `..primary.messaging..`); `AssignmentEventsRabbitListenerIT`
  (integración real contra el RabbitMQ y la BD del lab, perfil `-Pintegration`).
- Un método nuevo en `AuditEventJpaRepository`: `insertIfAbsent` (idempotencia por PK = `eventId`, en vez de un
  Set en memoria).

No se creó `infra/security-baseline/scripts/provision_assignment_events.py`: el exchange/cola/binding de la sección 4
del manual se crearon una sola vez por la API de administración de RabbitMQ (HTTP, idempotente — `PUT` sobre el mismo
recurso no cambia nada) y quedan documentados en `VALIDATION.md`; no hay necesidad funcional de un script reutilizable
para una topología que no vuelve a cambiar (YAGNI, ver "No alcance").

## Contratos y consumidores afectados

REALTIME/EVENTS: nuevo contrato entrante `AssignmentChangedEventV1` (externo, propiedad del PDP, `schemaVersion=1`).
No hay contrato HTTP, de dominio ni de persistencia nuevo (se reutiliza `dbo.AuditoriaEvento` sin cambiar su DDL).
SECURITY: el consumidor refuerza el aislamiento por `applicationId` (rechaza eventos ajenos). Sin consumidores internos
afectados; sin impacto en frontend.

## Riesgos y dependencias

- Riesgo: reprovisionar el PDP a 0.1.14 podría, en teoría, regenerar el `applicationId` de `asistencias-uco` si el
  volumen de SurrealDB se perdiera. Mitigación: `docker compose pull && up -d` sin `down -v` preserva los volúmenes
  (prohibido por el README del lab); se verifica el `applicationId` después de actualizar, antes de crear la cola.
- Riesgo: el exchange compartido `security.assignment.events` no lo aprovisiona el PDP automáticamente en el lab local
  (el manual lo deja como paso manual de integración, sección 4.1); se crea una sola vez y se documenta en VALIDATION.
- Dependencia: el usuario ya autorizó actualizar el lab compartido (`asistencias-security-lab`) — es compartido con
  otras apps locales (Catálogo de Parámetros usa su propio PDP/aplicación, mismo Keycloak central); se verifica que
  subir a 0.1.14 no rompa la aplicación `catalogo-parametros` en el mismo PDP (misma instalación, aplicaciones
  distintas, mismo riesgo de regresión de versión).

## Test plan

Ver `TEST_PLAN.md` (a crear antes de implementar, con RED_SNAPSHOT). Secuencia: RED (tests fallan porque las clases no
existen) → GREEN (implementación mínima) → VALIDATE (manual + los 4 escenarios del manual del PDP sobre el lab real).

## Rollback y stop conditions

Ver `ROLLBACK` en la tabla de clase de cambio. Sin `CONTRACT_CONFLICT` ni `TEST_CONTRACT_CONFLICT` abiertos relevantes
al momento de este PLAN.

## Deuda conocida y validación manual

Ninguna deuda nueva. No se registra `MV-XXX` porque no hay recorrido de UI/negocio involucrado (el único canal es
AMQP + BD); la validación es el IT contra el lab real más la inspección de `dbo.AuditoriaEvento`.

## Definition of Ready

**READY** para implementación, con una condición operativa previa a crear la cola (ver STOP_CONDITIONS): build base,
owner/consumer, Golden Path y scope de BD están identificados; no hay secreto en esta evidencia (el `applicationId`
registrado es un UUID de aplicación, no una credencial); el contrato (`AssignmentChangedEventV1`) es externo y no
admite negociación por este repositorio. Aprobación: el usuario (rol producto/responsable de esta tarea) autorizó en
conversación 2026-11-19 (1) el alcance "solo auditar" y (2) actualizar el lab local a 0.1.14 ahora.

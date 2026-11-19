# VALIDATION — SEC-BASELINE-020: Consumidor de eventos RabbitMQ de asignación

Base commit: `9adf99fb` (develop). Fecha: 2026-11-19.

## 1. Lab local actualizado a 0.1.14

```
docker compose --env-file %USERPROFILE%\.asistencias-security-lab\lab.env -f infra\security-baseline\compose.yaml pull
docker compose --env-file %USERPROFILE%\.asistencias-security-lab\lab.env -f infra\security-baseline\compose.yaml up -d
```

Resultado: `keycloak`, `opa`, `pdp`, `pep` recreados en `0.1.14`; `rabbitmq` (nuevo, `rabbitmq:4.4.1-management`) creado.
Todos los servicios quedaron `healthy` (PDP tardó ~45 s en pasar de `starting` a `healthy`, esperado). No se usó
`down -v` ni se tocaron los volúmenes de Keycloak/SurrealDB/Redis.

`python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py verify` después de la actualización:
el reporte reconoce la aplicación `asistencias-uco` y su catálogo existente (mismo patrón de `orphan_resources`/
`extra_grants` documentado como deuda conocida en `LINEA_BASE.md`, sin relación con este cambio) — confirma que el
`applicationId` (`ASISTENCIAS_PDP_APPLICATION_ID=a0b42637-39f8-4ddd-bec2-0781ba08e779` en `lab.env`) sobrevivió la
actualización de versión.

## 2. Topología RabbitMQ creada (sección 4 del manual)

Creada por la API HTTP de administración (`PUT`/`POST`, idempotente), no por la UI, para que quede reproducible:

- Exchange `security.assignment.events`: `type=direct`, `durable=true`, `auto_delete=false`.
- Cola `security.assignment.a0b42637-39f8-4ddd-bec2-0781ba08e779`: `durable=true`.
- Binding `security.assignment.events` → esa cola, routing key `assignment.changed.a0b42637-39f8-4ddd-bec2-0781ba08e779`.

Verificado listando `/api/queues/%2Fsecurity` y `/api/exchanges/%2Fsecurity/security.assignment.events/bindings/source`
en `http://localhost:25673`: la cola y el binding existen con los nombres esperados.

## 3. Suite completa (unit + arquitectura)

`.\mvnw.cmd -q -o test` — **729 clases de test, 0 fallos, 0 errores** (incluye las 6 clases nuevas de este work item
y toda la suite preexistente, sin regresiones).

## 4. Hallazgo real contra la BD (por qué importaba probar con BD real, no solo mocks)

`dbo.AuditoriaEvento` se diseñó para auditoría HTTP (`AuditInterceptor`): `httpMethod`, `path` y `httpStatus` son
**`NOT NULL`**, y `httpStatus` tiene además `CK_AuditoriaEvento_HttpStatus` que solo acepta **[100, 599]** (comprobado
empíricamente insertando y haciendo `ROLLBACK` contra `gestionasistenciadb`, sin permiso para leer la definición del
constraint con la cuenta `uco_dev_runtime`). Esta es la primera fuente no HTTP que escribe en esa tabla.

Primera versión (`null`/`0`) falló con `DataIntegrityViolationException` (`NOT NULL` y luego el `CHECK`). Decisión
tomada dentro del alcance de este work item (sin tocar el esquema, que es del equipo de BD):
`httpMethod="AMQP"`, `path="rabbitmq:security.assignment.events"`, `httpStatus=200` (el mismo valor que usaría un
`200 OK` real; aquí significa "procesado correctamente", no una respuesta HTTP). Código:
[RegistroEventoAsignacionAuditAdapter.java](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/audit/RegistroEventoAsignacionAuditAdapter.java).

## 5. Integración real contra RabbitMQ y BD reales (los 4 escenarios del manual)

Comando (failsafe directo, sin recompilar toda la suite cada vez):

```
mvnw.cmd -q -o -Pintegration org.apache.maven.plugins:maven-failsafe-plugin:integration-test org.apache.maven.plugins:maven-failsafe-plugin:verify -Dit.test=AssignmentEventsRabbitListenerIT -DfailIfNoTests=true -Dfailsafe.excludedGroups=
```

con `SPRING_DATASOURCE_*` (de `.env`, contra `gestionasistenciadb` real) y `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD`
(de `lab.env`) exportados al proceso de Maven. **Resultado: `Tests run: 4, Failures: 0, Errors: 0` (61 s).**

Evidencia de log por escenario (eventIds reales de la corrida):

| Escenario (manual, sección 7) | Resultado |
|---|---|
| Mensaje válido nuevo | `ACK`; auditado una vez (`eventId=9745561b-...` y los otros 5 publicados junto a él: `resultado=PROCESADO`) |
| Mensaje repetido (mismo `eventId`) | Primera entrega `PROCESADO`; segunda entrega del mismo `eventId=a62add74-...` → `resultado=DUPLICADO`, `ACK`, sin segunda fila |
| `applicationId` ajeno (aislamiento X/Y) | `eventId=018a78c6-...` → "contrato invalido... applicationId ajeno a esta aplicacion: ff3507e0-..." → `reject` sin requeue, no se audita |
| `schemaVersion` no soportada | `eventId=b61fe360-...` → "contrato invalido... schemaVersion no soportada: 2" → `reject` sin requeue, no se audita |

Cada test limpia sus propias filas de `dbo.AuditoriaEvento` en `@AfterEach` (mismo patrón que `AuditEventJpaParityIT`).

## 6. Checklist de entrega del manual (sección 8)

- [x] RabbitMQ está healthy y se accede por `http://localhost:25673`.
- [x] El exchange `security.assignment.events` es `direct` y durable.
- [x] Esta aplicación usa su propio `applicationId`, cola y binding.
- [x] La consumidora valida `schemaVersion` y `applicationId`.
- [x] La consumidora persiste `eventId` procesados (PK de `dbo.AuditoriaEvento`) y confirma después del efecto local.
- [x] Se probaron mensaje válido, duplicado, contrato inválido y aislamiento X/Y (los 4, contra el lab real).
- [x] No hay credenciales RabbitMQ hardcodeadas en el repositorio (`RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` solo en
      `lab.env`, fuera de Git; la app los recibe por `spring.rabbitmq.*`/env vars).

## 7. Alcance no cubierto / limitaciones

- No se validó el flujo "sin aplicación local" de la sección 7.1 del manual (publicar sin consumidor activo y ver
  `Ready=1` en la UI de management): cubierto indirectamente por el aislamiento X/Y, que ya demuestra el enrutamiento
  por `routing key`; no se consideró necesario repetirlo a mano en la UI.
- `management.health.rabbit.enabled=false` evita que una caída de RabbitMQ afecte `/actuator/health`; no se verificó
  explícitamente con un escenario de RabbitMQ caído (el efecto es solo auditoría, no autorización — bajo impacto).
- El lab de RabbitMQ es local y de un solo nodo; no se probó con réplicas del backend (prefetch=1 ya lo hace seguro
  para ese caso, pero no hay evidencia multi-réplica real).

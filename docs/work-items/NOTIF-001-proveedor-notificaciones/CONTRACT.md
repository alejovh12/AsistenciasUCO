---
status: draft-pending-approval
type: contract
scope: database-backend
last-reviewed: 2026-10-27
---

# CONTRACT — NOTIF-001

Estado: **`DRAFT`**; pasa a `FROZEN` con la aprobación de D1–D12 del [PLAN](PLAN.md). Cualquier desvío durante la implementación se documenta en §9 (Enmiendas) y en `VALIDATION.md`; las pruebas no se relajan.

## 1. Reglas comunes

- Capacidad: **notification**. Puerto Application **sin cambios**: `NotificacionPort.notificar(Notificacion)` y el record `Notificacion` (paquete `application.secondaryports.notification`). No se agregan campos ni métodos; HU051 y `LogNotificacionAdapterTest` deben seguir verdes sin modificarse.
- `application` no importa nada de `infrastructure` y no contiene los tokens `CorrelationIdContext`, `MDC`, `idCorrelacion`, `IdCorrelacion`.
- Mejor esfuerzo: **ninguna** implementación de `NotificacionPort` propaga excepciones desde `notificar` (excepto `NullPointerException` por argumento nulo, igual que `LogNotificacionAdapter`).
- Correlación: la completa el adaptador desde `CorrelationIdContext` (infraestructura) y viaja a la DB como `@idCorrelacion`.
- Sin GUIDs ni textos de estado quemados en T-SQL: los estados se resuelven contra `dbo.EstadoNotificacion` por `codigo`.
- Los textos que se escriben en logs **nunca** incluyen `asunto`, `mensaje`, `nombre` ni `correo`.

## 2. Base de datos (repositorio `GestioAsistenciaDB`)

### 2.1 Esquema

**`dbo.EstadoNotificacion`** (catálogo)

| Columna | Tipo | Notas |
| :-- | :-- | :-- |
| `id` | `UNIQUEIDENTIFIER` PK | |
| `codigo` | `VARCHAR(30)` NOT NULL, índice único | ver estados |
| `nombre` | `NVARCHAR(60)` NOT NULL | |
| `esTerminal` | `BIT` NOT NULL | |

Semilla idempotente (`schema/seed/NN_estado_notificacion.sql`, `MERGE` por `codigo`, `id = NEWID()` al insertar):

| codigo | esTerminal | Significado |
| :-- | :-: | :-- |
| `PENDIENTE` | 0 | Registrada, aún no reservada |
| `RESERVADA` | 0 | Tomada por un despachador (con vencimiento) |
| `FALLIDA_REINTENTABLE` | 0 | Falló; tiene `proximoIntentoEn` |
| `ENTREGADA` | 1 | El sender confirmó |
| `FALLIDA_DEFINITIVA` | 1 | Dead-letter: intentos agotados o fallo no reintentable |

**`dbo.NotificacionSaliente`**

| Columna | Tipo | Notas |
| :-- | :-- | :-- |
| `id` | `UNIQUEIDENTIFIER` PK | = `idNotificacion`; lo genera el SP con `NEWID()` |
| `tipo` | `VARCHAR(60)` NOT NULL | `^[A-Z0-9_]+$`, p. ej. `RETIRO_ESTUDIANTE_GRUPO` |
| `asunto` | `NVARCHAR(200)` NULL | |
| `mensaje` | `NVARCHAR(4000)` NULL | |
| `destinatarios` | `NVARCHAR(MAX)` NOT NULL | JSON `[{"rol":"ESTUDIANTE","idUsuario":"<uuid>"}]`, `CHECK (ISJSON(destinatarios)=1)` |
| `datos` | `NVARCHAR(MAX)` NOT NULL DEFAULT `'{}'` | JSON objeto de cadenas, `CHECK (ISJSON(datos)=1)` |
| `idEstado` | `UNIQUEIDENTIFIER` NOT NULL | FK a `EstadoNotificacion` |
| `intentos` | `INT` NOT NULL DEFAULT 0 | Se incrementa al **reservar** |
| `creadoEn` | `DATETIMEOFFSET(7)` NOT NULL | UTC |
| `proximoIntentoEn` | `DATETIMEOFFSET(7)` NULL | |
| `reservadoHasta` | `DATETIMEOFFSET(7)` NULL | Vencimiento de la reserva |
| `reservadoPor` | `NVARCHAR(100)` NULL | Identificador de instancia |
| `entregadoEn` | `DATETIMEOFFSET(7)` NULL | |
| `codigoError` | `VARCHAR(40)` NULL | Código técnico saneado (sin texto del usuario) |
| `detalleError` | `NVARCHAR(300)` NULL | Saneado y truncado |
| `idCorrelacion` | `UNIQUEIDENTIFIER` NULL | |
| `traceId` | `CHAR(32)` NULL | |
| `spanId` | `CHAR(16)` NULL | |

Índices: `IX_NotificacionSaliente_despacho (idEstado, proximoIntentoEn, creadoEn) INCLUDE (reservadoHasta)` y `IX_NotificacionSaliente_retencion (idEstado, creadoEn)`.

Entrega por migración idempotente `migrations/NN_notif001_notificacion_saliente_up.sql` + `..._down.sql`, y los mismos objetos en `schema/tables/` (patrón `IF OBJECT_ID(...) IS NULL`). La migración **no** debe fallar si se ejecuta dos veces.

### 2.2 Procedimientos

Todos: `SET NOCOUNT ON`, `TRY/CATCH`, mensajes por `usp_obtener_mensaje_catalogo`, fachada pública delega la mutación a un `*_interno` con `SET XACT_ABORT ON` y transacción propia (o `SAVE TRANSACTION` si ya hay una; nombre de savepoint **≤ 32 caracteres**), verifican `@@TRANCOUNT` al salir. Salida canónica `(idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado)` salvo donde se indica. Parámetros nulos se evalúan con `IS NULL` (no con GUID vacío). Sin RBAC por perfil (D9).

#### `dbo.usp_registrar_notificacion_saliente`

| Parámetro | Tipo | Notas |
| :-- | :-- | :-- |
| `@tipo` | `NVARCHAR(100)` | `UPPER(TRIM)`; 3–60 y `^[A-Z0-9_]+$` si no `NTF_001` |
| `@asunto` | `NVARCHAR(400)` | opcional; `TRIM`; > 200 → `NTF_001` |
| `@mensaje` | `NVARCHAR(MAX)` | opcional; > 4000 → `NTF_001` |
| `@destinatariosJson` | `NVARCHAR(MAX)` | requerido; JSON arreglo de 1..100 objetos con `rol` (1–30) e `idUsuario` (UUID); inválido/vacío → `NTF_001` |
| `@datosJson` | `NVARCHAR(MAX)` | opcional (default `'{}'`); JSON objeto; > 8000 caracteres → `NTF_001` |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | nulo → `CORR_001` |
| `@traceId`, `@spanId` | `CHAR(32)`, `CHAR(16)` | opcionales |

Inserta en estado `PENDIENTE` (`creadoEn` = UTC actual, `proximoIntentoEn` = `creadoEn`). Éxito `GEN_004`.

#### `dbo.usp_reservar_notificaciones_salientes`

| Parámetro | Tipo | Notas |
| :-- | :-- | :-- |
| `@lote` | `INT` | 1..500, si no `NTF_001` |
| `@leaseSegundos` | `INT` | 5..3600, si no `NTF_001` |
| `@instancia` | `NVARCHAR(100)` | requerido |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | nulo → `CORR_001` |

Selecciona hasta `@lote` filas elegibles con `WITH (UPDLOCK, READPAST, ROWLOCK)` ordenadas por `creadoEn`: estado `PENDIENTE` o `FALLIDA_REINTENTABLE` con `proximoIntentoEn <= ahora`, **o** estado `RESERVADA` con `reservadoHasta < ahora` (recuperación de un trabajador caído). Las marca `RESERVADA`, fija `reservadoHasta = ahora + lease`, `reservadoPor = @instancia`, `intentos = intentos + 1`. **Devuelve filas** (excepción documentada a la tupla canónica, como los `usp_consultar_*`): `id, tipo, asunto, mensaje, destinatarios, datos, intentos, creadoEn, idCorrelacion, traceId, spanId`. Sin filas elegibles → conjunto vacío.

#### `dbo.usp_marcar_notificacion_entregada`

`@id`, `@instancia`, `@idCorrelacion`. Solo si la fila está `RESERVADA` por `@instancia` y la reserva no venció: pasa a `ENTREGADA`, fija `entregadoEn`, limpia reserva y errores. Inexistente → `NTF_002`; no reservada por esa instancia o vencida → `NTF_003`. Éxito `GEN_005`.

#### `dbo.usp_registrar_fallo_notificacion`

| Parámetro | Tipo | Notas |
| :-- | :-- | :-- |
| `@id`, `@instancia`, `@idCorrelacion` | | igual que arriba |
| `@codigoError` | `VARCHAR(40)` | requerido |
| `@detalleError` | `NVARCHAR(300)` | se trunca a 300 |
| `@reintentable` | `BIT` | `NULL` → `NTF_001` |
| `@maxIntentos` | `INT` | 1..20 |
| `@backoffBaseSegundos` | `INT` | 1..3600 |
| `@backoffMaxSegundos` | `INT` | ≥ base, ≤ 86400 |

Solo para reserva vigente de `@instancia` (`NTF_003` si no). Si `@reintentable = 0` **o** `intentos >= @maxIntentos` → `FALLIDA_DEFINITIVA`. Si no → `FALLIDA_REINTENTABLE` con `proximoIntentoEn = ahora + MIN(base * 2^(intentos-1), max)` segundos. Limpia la reserva. Éxito `GEN_005`.

#### `dbo.usp_depurar_notificaciones_salientes`

`@retencionDias` (1..3650), `@lote` (1..5000), `@idCorrelacion`. Borra estados terminales con `creadoEn < ahora - retención`, en lotes. **Devuelve además la columna `eliminadas` (INT)** junto a la tupla canónica. Éxito `GEN_005`.

### 2.3 Códigos

| Código | Cuándo | Mensaje de usuario (SSOT en `04_mensaje.sql`) |
| :-- | :-- | :-- |
| `NTF_001` | Datos de la notificación o parámetros inválidos | «Los datos de la notificación no son válidos.» |
| `NTF_002` | La notificación no existe | «La notificación especificada no existe en el sistema.» |
| `NTF_003` | La reserva no pertenece a la instancia o ya venció | «La notificación ya no está reservada por este proceso.» |
| `CORR_001` / `GEN_004` / `GEN_005` | Existentes | |

Cada código lleva también su mensaje técnico. Las filas se agregan a `schema/seed/04_mensaje.sql` en un commit aparte (archivo caliente) al final de la fase DB.

## 3. Backend — contrato de selección

### 3.1 Propiedades (`application.yml`)

```yaml
app:
  adapters:
    notification:
      provider: ${APP_ADAPTERS_NOTIFICATION_PROVIDER:log}     # log | outbox | composite
      composite:
        targets: ${APP_ADAPTERS_NOTIFICATION_COMPOSITE_TARGETS:log,outbox}   # solo aplica con provider=composite
  providers:
    outbox-notification:
      sender: ${OUTBOX_NOTIFICATION_SENDER:log}               # log (v1)
      dispatcher-enabled: ${OUTBOX_NOTIFICATION_DISPATCHER_ENABLED:true}
      batch-size: ${OUTBOX_NOTIFICATION_BATCH_SIZE:50}        # 1..500
      lease: ${OUTBOX_NOTIFICATION_LEASE:60s}                 # 5s..1h
      sweeper-interval: ${OUTBOX_NOTIFICATION_SWEEPER_INTERVAL:30s}   # 1s..1h
      max-attempts: ${OUTBOX_NOTIFICATION_MAX_ATTEMPTS:5}     # 1..20
      backoff-base: ${OUTBOX_NOTIFICATION_BACKOFF_BASE:30s}   # 1s..1h
      backoff-max: ${OUTBOX_NOTIFICATION_BACKOFF_MAX:15m}     # >= base, <= 24h
      retention-days: ${OUTBOX_NOTIFICATION_RETENTION_DAYS:30}   # 1..3650
      retention-interval: ${OUTBOX_NOTIFICATION_RETENTION_INTERVAL:6h}
      instance-id: ${OUTBOX_NOTIFICATION_INSTANCE_ID:}        # vacío => <hostname>-<8 hex aleatorios>
```

Los valores por defecto son los de la tabla; `application.yml` es archivo caliente (commit aparte, al final).

### 3.2 Properties tipadas

- `infrastructure.config.properties.adapters.NotificationAdapterProperties` (`@ConfigurationProperties("app.adapters.notification")`): `provider` obligatorio (enum `Provider { LOG, OUTBOX, COMPOSITE }`) y `composite.targets` (lista de `Target { LOG, OUTBOX }`). Reglas validadas al arrancar con mensaje explícito: con `COMPOSITE`, `targets` tiene 2..n elementos distintos y no contiene `COMPOSITE`; con otros providers, `targets` se ignora. Se registra en `AdapterPropertiesConfiguration` (archivo caliente).
- `infrastructure.config.properties.providers.OutboxNotificationProperties` (`@ConfigurationProperties("app.providers.outbox-notification")`): record con las claves de §3.1, validación de rangos al construir (violación = el arranque falla con el nombre de la propiedad).
- Valor desconocido de `provider` o `sender` ⇒ falla el arranque (el enum no enlaza). No existe fallback.

### 3.3 Composition Roots (`infrastructure/config/adapters/notification/…`)

```text
notification/
├── log/        LogNotificacionAdapterConfiguration
├── outbox/     OutboxNotificacionAdapterConfiguration
└── composite/  CompositeNotificacionAdapterConfiguration
```

Reglas:
1. Se **elimina** `NotificacionAdapterConfiguration` (`@ConditionalOnMissingBean`) y su comportamiento se reemplaza por estas tres clases. `LogNotificacionAdapter` se conserva intacto.
2. Las clases hoja crean su adaptador **concreto** cuando el provider es ese hoja **o** `composite` lo incluye en `targets`; exponen el bean con el tipo concreto (`LogNotificacionAdapter`, `OutboxNotificacionAdapter`), **nunca** como `NotificacionPort` en modo composite.
3. Exactamente un bean de tipo `NotificacionPort`: en modo hoja lo declara la clase hoja; en modo composite lo declara `CompositeNotificacionAdapterConfiguration`, que recibe los destinos por **inyección tipada** (`ObjectProvider<LogNotificacionAdapter>`, `ObjectProvider<OutboxNotificacionAdapter>`) y los ensambla según `targets`. Se permite un `switch` sobre el enum **solo** dentro del Composition Root.
4. Prohibido: `@Profile` para elegir tecnología, `Map<String, NotificacionPort>`, `ApplicationContext.getBean(nombre)`, leer `Environment`/properties desde Application, y `@Component/@Service` en los adaptadores seleccionables.
5. El despachador, el `NotificacionChannelSender` y el repositorio solo existen cuando algún destino `OUTBOX` está activo.

### 3.4 Proveedor `log`
Comportamiento actual de `LogNotificacionAdapter` (sin cambios de código ni de formato de log).

### 3.5 Proveedor `outbox`

Paquete `infrastructure.adapter.secondary.notification.outbox`:

| Clase | Responsabilidad |
| :-- | :-- |
| `OutboxNotificacionAdapter implements NotificacionPort` | Valida no nulo (`NullPointerException` solo por argumento nulo). Si `destinatarios` está vacío: no registra, cuenta `skipped`. Serializa a JSON **solo** `rol`+`idUsuario` y `datos` (nunca `nombre`/`correo`). Llama al repositorio con la correlación y `traceId/spanId` vigentes. Éxito → `dispatcher.signal()`. Cualquier `RuntimeException` del registro se **captura**: `WARN` con el nombre de la clase de la excepción y `tipo`, contador `failed`, sin propagar. |
| `NotificacionOutboxRepository` (interfaz de infraestructura) | `void registrar(RegistroNotificacion)`, `List<NotificacionReservada> reservar(int lote, Duration lease, String instancia)`, `void marcarEntregada(UUID id, String instancia)`, `void registrarFallo(UUID id, String instancia, String codigoError, String detalle, boolean reintentable, int maxIntentos, Duration base, Duration max)`, `int depurar(int retencionDias, int lote)`. |
| `NotificacionSalienteJpaRepository` | `@Repository` que implementa la interfaz anterior con `JpaProcedureExecutor` (SP con tupla canónica) y `EntityManager` + proyección nativa para `usp_reservar_…` (que devuelve filas; mismo patrón que los adaptadores de KPIs con entidades `Sp*`). Lleva `@ConditionalOnProperty` coherente con §3.3 si la política de unicidad de repositorios lo exige (ver `JpaRepositoryBeanUniquenessTest`). |
| `NotificacionOutboxDispatcher implements SmartLifecycle` | Un solo hilo propio (no el pool compartido). Bucle: esperar señal **o** `sweeper-interval`; `reservar`; por cada fila invocar el sender; éxito → `marcarEntregada`; `NotificacionNoEntregableException` → `registrarFallo(reintentable=false)`; cualquier otra excepción → `registrarFallo(reintentable=true)`. Errores al marcar/fallar (`NTF_002/003` u otros) se registran en `WARN` y se continúa (reserva perdida: otro despachador reintentará). `stop()` deja de reservar, termina el lote en curso y libera el hilo; las reservas restantes vencen solas. Depuración al arrancar y cada `retention-interval`. Con `dispatcher-enabled=false` no arranca el hilo (el adaptador igualmente registra). |
| `NotificacionChannelSender` | SPI: `void enviar(NotificacionPersistida n) throws NotificacionNoEntregableException`. Debe ser **idempotente por `n.id()`**. |
| `NotificacionPersistida` | `record(UUID id, String tipo, String asunto, String mensaje, List<DestinatarioRef> destinatarios, Map<String,String> datos, int intentos, Instant creadoEn)`; `DestinatarioRef(String rol, UUID idUsuario)`. Sin contacto. |
| `LogNotificacionChannelSender` | Único sender de v1: registra `tipo`, `rol:idUsuario` de cada destinatario, claves de `datos` (excluyendo `motivo`, igual que el adaptador `log`) e `id`. Nunca `asunto`, `mensaje`, `nombre`, `correo`. |
| `NotificacionNoEntregableException` | Excepción de infraestructura para fallo definitivo. |

Contrato del sender elegido: `app.providers.outbox-notification.sender` (enum `Sender { LOG }`) se resuelve en el Composition Root de `outbox` (un `@Bean` condicional por valor). Añadir el sender institucional es agregar un valor del enum y su `@Bean`, sin tocar el resto.

### 3.6 Proveedor `composite`
`CompositeNotificacionAdapter implements NotificacionPort` (infraestructura) recibe `List<NamedTarget>` (`record NamedTarget(String nombre, NotificacionPort puerto)`, con nombres `log`/`outbox` fijados por el Composition Root solo para métricas y logs). `notificar` invoca **a todos** los destinos en el orden de `targets`, cada uno dentro de `try/catch (RuntimeException)`; un fallo se registra (`WARN`, nombre del destino y clase de la excepción) y cuenta en `notification.composite.target.failures{target}` sin impedir a los demás. Nunca propaga. `NullPointerException` por argumento nulo se lanza antes de invocar destinos.

### 3.7 Observabilidad (Micrometer; nombres fijados)

| Métrica | Tipo | Etiquetas |
| :-- | :-- | :-- |
| `notification.publish.total` | counter | `provider` (`log`/`outbox`/`composite`), `outcome` (`accepted`/`skipped`/`failed`) |
| `notification.composite.target.failures` | counter | `target` |
| `notification.dispatch.total` | counter | `outcome` (`delivered`/`retry`/`dead`/`lost_lease`) |
| `notification.dispatch.duration` | timer | `outcome` |
| `notification.outbox.pending` | gauge | — (conteo de elegibles; refrescado en cada barrido) |
| `notification.outbox.dead` | gauge | — (conteo de `FALLIDA_DEFINITIVA`) |
| `notification.outbox.purged.total` | counter | — |

Logs estructurados con `idNotificacion`, `tipo`, `intentos`, `estado`, `correlationId`/`traceId`/`spanId` (los últimos restaurados desde la fila, no del hilo del trabajador). Sin cuerpo del mensaje.

## 4. Pruebas de contrato del puerto (obligatorias, reutilizables por el proveedor futuro)

- Clase abstracta de prueba `NotificacionPortContractTest` (en `src/test/.../notification/contract/`) con `protected abstract NotificacionPort puerto()`, ejecutada por **todas** las implementaciones (`log`, `outbox`, `composite`). Casos en TEST_PLAN §3.1. El proveedor institucional futuro debe heredar de esta clase para considerarse integrado.

## 5. Compatibilidad

- `NotificacionPort`, `Notificacion`, `RetirarEstudianteGrupoUseCaseImpl`, `LogNotificacionAdapter` y sus pruebas existentes: **sin modificaciones**.
- Con la configuración por defecto (`provider=log`) el comportamiento observable es idéntico al de hoy.
- Sin endpoints nuevos: `SecurityConfig`, `RbacSecurityFilterChainTest`, `access-catalog.json` y `openapi-golden-path.yaml` no cambian. Los contratos WORM y sus `.sha256` no se tocan.
- Cambia el conteo de `@Repository` productivos en `JpaRepositoryBeanUniquenessTest` (+1 sobre el valor vigente en `develop` al integrar; recontar tras `git merge origin/develop`).

## 6. Seguridad y datos

- El outbox no persiste `nombre` ni `correo`. `mensaje` puede contener texto personal (p. ej. HU051); retención por defecto 30 días con depuración.
- `detalleError` se sanea (sin `asunto`/`mensaje`/direcciones) y se trunca.
- Los secretos de futuros senders se obtienen por `SecretVaultPort`; ninguna credencial en `application.yml` ni en logs.
- Permisos de ejecución de los SP: replicar el mecanismo vigente (verificar en el repositorio de DB; no ampliar privilegios sin autorización del owner).

## 7. Documentación a actualizar (al final, commit aparte por archivo)

1. `docs/architecture/adapter-composition-standard.md`: bloque YAML de §1, nueva sección de Notification, fila en la matriz de capacidades (§14) y marcar el checklist §16.
2. `docs/adr/ADR-005-notification-provider-switch.md` (contexto, decisión, alternativas descartadas: puerto genérico de mensajería, outbox transaccional con negocio, selector por `@ConditionalOnMissingBean`).
3. `docs/baseline/TECHNICAL_DEBT.md`: actualizar la resolución de TD-066 y registrar los seguimientos del PLAN §7 con los **siguientes números libres tras `git merge origin/develop`**.
4. Variables nuevas en los archivos de ejemplo de entorno existentes.

## 8. Fuera de alcance
Ver PLAN §3 y §7.

## 9. Enmiendas
(vacío; se llena durante la implementación)

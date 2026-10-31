---
status: frozen-approved
type: plan
scope: database-backend
last-reviewed: 2026-10-27
---

# PLAN — NOTIF-001: Capacidad de notificaciones con proveedor intercambiable

Estado: **`FROZEN`** (aprobado por el owner con decisiones D1–D12). Este work item se implementa según este plan y su CONTRACT.

## 1. Objetivo

Hoy los avisos institucionales salen por `NotificacionPort`, cuyo único adaptador (`LogNotificacionAdapter`) solo escribe en el log, y el componente institucional de notificaciones del equipo aún no existe (ver [TD-066](../../baseline/TECHNICAL_DEBT.md#td-066)). Se necesita:

1. Una **implementación básica propia** y durable que permita empezar a consumir notificaciones ya (reintentos, dead-letter, métricas), sin esperar al componente externo.
2. Un **selector de proveedor** con el mismo estándar que seguridad, almacenamiento, tiempo real y auditoría (`app.adapters.<capacidad>.provider`), de modo que cambiar al componente real sea configuración, no refactor.
3. La posibilidad de **dejar dos proveedores activos a la vez** (fan-out) durante la transición, con aislamiento de fallos entre ellos.

## 2. Estado actual verificado (2026-10-27, `origin/develop` @ 3f49032)

| Hecho | Evidencia |
| :-- | :-- |
| El puerto neutral ya existe y es de **mejor esfuerzo**. | `application/secondaryports/notification/NotificacionPort.java` (`void notificar(Notificacion)`), `Notificacion.java` (tipo, asunto, mensaje, destinatarios con rol/idUsuario/nombre/correo, datos). |
| Un único consumidor: HU051. Atrapa cualquier excepción y registra `WARN`. | `RetirarEstudianteGrupoUseCaseImpl` (tipo `RETIRO_ESTUDIANTE_GRUPO`); endpoint `POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro`. |
| Adaptador por defecto sin selector: `@ConditionalOnMissingBean(NotificacionPort.class)`. | `NotificacionAdapterConfiguration`, `LogNotificacionAdapter` (+ `LogNotificacionAdapterTest`). |
| El estándar de selección está documentado. | `docs/architecture/adapter-composition-standard.md` (§2 reglas, §16 checklist de nuevo provider). |
| Patrón de referencia a copiar. | Realtime: `RealtimeAdapterProperties` + `LocalSseRealtimeAdapterConfiguration` (`@ConditionalOnProperty`, `matchIfMissing`). Auditoría: `AuditAdapterConfiguration`. |
| **No hay transacción Java que abarque el caso de uso.** La atomicidad de negocio vive dentro de cada procedimiento almacenado; solo `AuditEventJpaRepository` usa `@Transactional`. | `grep @Transactional src/main/java` |
| `application` no puede nombrar `CorrelationIdContext`, `MDC`, `idCorrelacion`. | `ApplicationShouldNotCarryTechnicalCorrelationTest` |
| El backend de `develop` arranca por defecto con el proveedor de seguridad `baseline`; para E2E local se usa `legacy`. | `application.yml`, `infra/security-baseline/README.md` |

Consecuencia de diseño importante: como no existe una transacción Java que una el negocio con el aviso, el outbox **no puede ser "transaccional con el negocio"**. Es durable *después* de que el caso de uso ya confirmó su SP. Queda una ventana documentada de pérdida (caída entre el SP de negocio y el registro del aviso), coherente con el contrato vigente de mejor esfuerzo (D4).

## 3. Alcance

**Dentro:**
- Selector `app.adapters.notification.provider` = `log` | `outbox` | `composite`.
- Proveedor `log` (comportamiento actual, ahora seleccionable y con fail-fast).
- Proveedor `outbox`: tabla de salida + procedimientos + despachador en segundo plano + SPI de canal (`NotificacionChannelSender`) con un único sender `log` en esta versión.
- Proveedor `composite`: fan-out a 2 o más proveedores hoja, con aislamiento de fallos.
- Observabilidad (métricas, logs estructurados sin texto del mensaje), pruebas, ArchUnit, documentación (estándar de composición, ADR, deuda técnica).

**Fuera (seguimientos, ver §7):** frontend; endpoints HTTP de operación (listar, reencolar); canales reales (SMTP, WhatsApp, SMS); resolución de datos de contacto al entregar; bus de eventos de dominio (LB-005); cualquier cambio a `RetirarEstudianteGrupoUseCaseImpl` o a `Notificacion`/`NotificacionPort`.

## 4. Decisiones (D1–D12)

| ID | Decisión | Recomendación |
| :-- | :-- | :-- |
| D1 | La capacidad se llama **notification** y **reutiliza sin cambios** `NotificacionPort`/`Notificacion` (HU051 no se toca). No se crea un puerto de "mensajería" genérico. | Sí |
| D2 | Selector `app.adapters.notification.provider`, variable `APP_ADAPTERS_NOTIFICATION_PROVIDER`, valores `log`, `outbox`, `composite`; **default `log`** (idéntico al comportamiento de hoy). Valor desconocido = el arranque falla (fail-closed, sin fallback silencioso). | Sí |
| D3 | `outbox` = persistencia durable + despachador + SPI `NotificacionChannelSender`. En esta versión el único sender es `log` (`app.providers.outbox-notification.sender=log`). Sin SMTP/WhatsApp/SMS. | Sí |
| D4 | Semántica **mejor esfuerzo, al menos una vez desde el registro**: `notificar` nunca propaga excepciones; si el registro falla se cuenta y se registra `WARN` (solo nombre de clase). Entrega *at-least-once* al sender, que debe ser idempotente por `idNotificacion`. | Sí |
| D5 | Minimización de datos: el outbox **no persiste** `nombre` ni `correo` de los destinatarios (solo `rol` + `idUsuario`); sí persiste `tipo`, `asunto`, `mensaje` y `datos`. Retención configurable (default 30 días) con depuración. | Sí |
| D6 | `composite` entrega a **todos** los destinos listados (`targets`, 2..n, sin repetidos, sin anidar `composite`), cada uno aislado con `try/catch` propio; métrica por destino. No hay destino "primario". | Sí |
| D7 | El despachador es un trabajador del **backend** (no polling de cliente): se despierta al registrar y además barre cada `sweeper-interval` (default 30 s) para recuperar reintentos y reservas vencidas. Seguro con varias instancias mediante reservas con vencimiento (`READPAST`/`UPDLOCK`). | Sí |
| D8 | Reintentos: exponenciales deterministas (`base` 30 s, ×2 por intento, tope 15 min), `max-attempts` 5; agotados → `FALLIDA_DEFINITIVA` (dead-letter, visible en tabla y métrica). Parámetros técnicos en `app.providers.outbox-notification.*` (no en App Configuration). | Sí |
| D9 | Los procedimientos del outbox son **técnicos** (los invoca solo el runtime del backend): sin `@idUsuarioEjecutor` ni RBAC por perfil; se documenta como excepción justificada a la regla de RBAC en SP. El implementador debe **verificar cómo se otorgan permisos de ejecución** a los SP existentes y replicarlo (no asumir). | Sí |
| D10 | Estados y canales como **catálogos** (`EstadoNotificacion`, sin GUID ni texto de estado quemado en T-SQL); los SP resuelven por `codigo`. | Sí |
| D11 | Sin endpoints HTTP en esta versión: no cambian `SecurityConfig`, `access-catalog.json` ni el OpenAPI. La verificación E2E usa el endpoint HU051 ya existente. | Sí |
| D12 | Ramas nuevas desde `develop`: `feature/NOTIF-001-notificaciones-outbox` en DB y en Backend; PR en orden DB → Backend. Nada de push/PR sin orden explícita del owner. | Sí |

## 5. Fases (TDD estricto por capa; cada fase termina en commits atómicos y verde)

| Fase | Contenido | Salida verificable |
| :-- | :-- | :-- |
| **0** | Congelar PLAN/CONTRACT/TEST_PLAN (`frozen-approved`); crear worktrees y ramas propias. | Commit `docs` |
| **1 — DB RED→GREEN** | Tablas, catálogo de estados, SP públicos + internos, mensajes `NTF_*` (archivo caliente al final), pruebas T-SQL y de concurrencia. | `test_usp_*notificacion*` PASS, migración idempotente |
| **2 — Backend núcleo RED→GREEN** | Properties tipadas, Composition Roots (`log`, `outbox`, `composite`), `OutboxNotificacionAdapter`, repositorio JPA, despachador, SPI de sender, `LogNotificacionChannelSender`. Pruebas unitarias, de contrato, de wiring y ArchUnit. | `mvnw test` verde, cobertura ≥ 90 % líneas en código nuevo |
| **3 — Integración** | `NotificacionOutboxIT` (failsafe) contra SQL Server real: registro → despacho → `ENTREGADA`; reintentos → dead-letter; dos despachadores sin doble entrega; depuración. | IT PASS sin cambios en el baseline de IT preexistentes |
| **4 — Archivos calientes y documentación** | `application.yml`, `AdapterPropertiesConfiguration`, `JpaRepositoryBeanUniquenessTest`, `04_mensaje.sql`, `test_suite.sql`, `test_contract_closure.sql`, estándar de composición, ADR-005, TECHNICAL_DEBT; cada archivo en commit aparte. | Suites completas verdes |
| **5 — E2E y cierre** | Arranque real en `:8090` (seguridad `legacy`) con `provider=outbox`; flujo HU051 con JWT reales; `VALIDATION.md` con cifras reales. | VALIDATION.md |

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
| :-- | :-- |
| Dos beans `NotificacionPort` o ninguno según la combinación de propiedades. | Pruebas `ApplicationContextRunner` por cada combinación (CONTRACT §4.4); exactamente un `NotificacionPort`. |
| Service Locator disfrazado (mapas de proveedores por string). | Prohibido por el estándar §2.4; el composite recibe sus destinos por inyección tipada (`ObjectProvider<…>` de clases concretas) desde el Composition Root. ArchUnit lo verifica. |
| PII en reposo (`mensaje` puede contener nombres, p. ej. HU051). | D5 (sin `nombre`/`correo`), retención y depuración, logs sin texto. Riesgo residual documentado en TD. |
| Reserva vencida: un trabajador lento entrega dos veces. | Entrega *at-least-once* declarada; sender idempotente por `idNotificacion`; `marcar` valida dueño de la reserva (`NTF_003`). |
| Conflictos en archivos calientes y numeración de TD al integrar con `develop`. | Editarlos al final, en commit aparte, tras `git merge origin/develop` y recontar (precedente: conflicto TD-062 y conteo de repositorios). |
| El backend de `develop` exige el baseline de seguridad (PDP/PEP) para arrancar. | E2E con `APP_ADAPTERS_SECURITY_PROVIDER=legacy` y `APP_ADAPTERS_IDENTITY_PROVIDER=legacy`; declarar que no se probó contra el PDP. |

## 7. Seguimientos (se registran como deuda técnica al cerrar; números = siguientes libres)

1. Sender real del componente institucional (`app.providers.outbox-notification.sender=institucional` o un `NotificacionPort` propio como provider `institucional`). Actualizar la resolución de TD-066.
2. Resolución de datos de contacto (correo/teléfono) al entregar, sin persistirlos en el outbox.
3. Endpoints de operación (`GET` paginado vía `usp_consultar_*` sobre una vista, reencolar dead-letter) y su pantalla.
4. Ventana de pérdida entre el SP de negocio y el registro del aviso (si algún aviso futuro exige garantía fuerte, requerirá registrar el aviso dentro del SP de negocio).
5. Bus de eventos de dominio / realtime distribuido (LB-005): distinto de esta capacidad.

## 8. Criterios de cierre

- CONTRACT implementado sin desvíos no documentados; TEST_PLAN ejecutado con evidencia real.
- Checklist de nuevo provider (`adapter-composition-standard.md` §16) marcado completo.
- `mvnw test` y `mvnw verify` verdes; JaCoCo global sobre el gate; ArchUnit verde; pruebas DB verdes; IT sin regresiones.
- Documentación y matriz de capacidades actualizadas; deuda registrada.
- Cero archivos temporales en el árbol; sin push ni PR sin orden explícita.

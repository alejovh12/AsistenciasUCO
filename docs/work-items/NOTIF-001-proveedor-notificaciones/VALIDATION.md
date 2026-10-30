# VALIDATION — NOTIF-001: Proveedor de Notificaciones Intercambiable (Log, Outbox, Composite)

## Dictamen

IN PROGRESS (Fases 1 a 5 ejecutadas; E2E 7/7 ejecutados en verde con E2E-06 `NO_EJECUTADO`; pendiente la suite completa unitaria + failsafe posterior al merge con `origin/develop` del 2026-10-29).

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-10-27 Local (Docker SQL Server) | `mvnw.cmd test -Pintegration` (develop) | 1 | BASELINE PREEXISTENTE | Tests run: 271, Failures: 7, Errors: 10, Skipped: 7 |
| 2026-10-27 Local (Docker SQL Server) | `mvnw.cmd test -Pintegration` (feature) | 1 | +9 IT GREEN (NOTIF-001) | Tests run: 280, Failures: 7, Errors: 10, Skipped: 7 |
| 2026-10-27 Local (Java 25) | `mvnw.cmd test` | 0 | PASS | Tests run: 2657, Failures: 0, Errors: 0, Skipped: 0 |
| 2026-10-27 Local (Java 25) | `mvnw.cmd jacoco:report` | 0 | PASS | Reporte JaCoCo generado en target/site/jacoco/ |

## Hallazgos de Producción e Integración (Fase 3)

1. **Mapeo Estricto de Columnas de `usp_reservar_notificaciones_salientes` (CONTRACT §2.2):**
   - *Hallazgo:* En SQL Server real, el SP devuelve 11 columnas (`id, tipo, asunto, mensaje, destinatarios, datos, intentos, creadoEn, idCorrelacion, traceId, spanId`), donde `creadoEn` es la columna 7 (índice 7) y `idCorrelacion` es la 8. En cambio, el mock unitario de `NotificacionSalienteJpaRepositoryTest` retornaba un entero literal `5` (`maxIntentos`) en el índice 7 desplazando `creadoEn` al índice 8.
   - *Resolución:* Siguiendo la aprobación formal de TEST_CONTRACT_CONFLICT, se estableció un mapeo estricto por posición de las 11 columnas en `NotificacionSalienteJpaRepository` (sin `isDateLike` ni defaults inventados, obteniendo `maxIntentos` únicamente de la configuración `OutboxNotificationProperties`), se corrigió el fixture simulado en `NotificacionSalienteJpaRepositoryTest` (`BE-J02`, `BE-J06`, `BE-J08`) y se agregó la prueba `BE-J09` que valida que el repositorio falla si el orden de las columnas viene desplazado.

## Tests existentes modificados (para PR)

1. `NotificationWiringTest` (WI-04, WI-05, WI-11): En modo composite, resolución de bean primario `CompositeNotificacionAdapter` y preservación de hojas (`hasSingleBean(LogNotificacionAdapter)` y `hasSingleBean(OutboxNotificacionAdapter)`).
2. Intervalo del sweeper en fixture de `NotificacionOutboxDispatcherTest` (100 ms a 1 s) y uso del constructor canónico de 11 columnas tras retirar el constructor de compatibilidad en `NotificacionReservada`.
3. `NotificacionSalienteJpaRepositoryTest` (BE-J02, BE-J06, BE-J08): Corrección de fixture de fila simulada para respetar el orden canónico de 11 columnas de CONTRACT §2.2 ([7]=creadoEn, [8]=idCorrelacion), eliminando `maxIntentos` del arreglo devuelto por la consulta nativa.

> [!NOTE]
> `JpaRepositoryBeanUniquenessTest` **NO se modificó** porque `NotificacionSalienteJpaRepository` reside en el paquete `co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox`, el cual está fuera del paquete `...persistence.sqlserver.jpa.repository` que escanea dicha prueba de gobernanza (en estricto cumplimiento de Clean Architecture y las reglas de aislamiento de mensajería AR-01..AR-05).
2. **Binding de Parámetros de Correlación:**
   - *Hallazgo:* Los procedimientos `usp_marcar_notificacion_entregada` y `usp_registrar_fallo_notificacion` esperan `@idCorrelacion` como parámetro de auditoría. Inicialmente se pasaba como parámetro no tipado o nulo en ciertos flujos.
   - *Fix:* Se aseguró el binding de `idCorrelacion` en todas las ejecuciones transaccionales y de fallback de `NotificacionSalienteJpaRepository`.
3. **Propagación de Contexto de Observabilidad en Hilos de Fondo:**
   - *Hallazgo:* Los hilos autónomos `workerThread` y `sweeperThread` de `NotificacionOutboxDispatcher` no heredaban el `CorrelationIdContext` ni el MDC de logging, perdiendo la correlación en trazas y métricas.
   - *Fix:* Se implementó la inicialización explícita y limpieza (`try/finally`) de `CorrelationIdContext` y MDC en cada ciclo de despacho y de depuración.

## Integridad de RED

- En la Fase 3, el commit RED inicial (`e311ab7`) contenía esqueletos de prueba con `fail("PENDING_RED_BODY")` sin aserciones completas; los cuerpos completos de las 9 ITs fueron completados en la fase GREEN. Queda registrado como hallazgo de proceso; desde este punto todo RED contendrá la prueba real observable que falla por la causa funcional correcta.

## Métricas de Cobertura JaCoCo (Código Nuevo NOTIF-001)

- **Instrucciones:** 2.048 / 2.108 (**97,15 %**)
- **Líneas:** 454 / 467 (**97,22 %**)
- **Ramas:** 173 / 197 (**87,82 %**) (Meta ≥ 85 % cumplida)

## Fase 5 — E2E real (puerto 8090)

**Declaración:** ejecutado con `APP_ADAPTERS_SECURITY_PROVIDER=legacy` e identidad `legacy`. **NO se probó contra el PDP remoto de seguridad baseline.** Backend propio en `:8090` (no se tocaron `:8080` ni `:4200`), JWT reales de Keycloak, BD local `gestionasistenciadb`, jar construido tras el fix de `ObjectMapper` (5e7b29c). Script: `scratch/test_notif001.ps1`; salida cruda fuera del repo (`e2e_notif001_raw.log`).

| ID | Resultado | Nota |
|---|---|---|
| E2E-01 | PASS | Arranque con `outbox` y con `composite` sin errores de contexto |
| E2E-02 | PASS | Retiro HU051 → 2xx; 1 fila `RETIRO_ESTUDIANTE_GRUPO` pasa a `ENTREGADA`; sin `nombre`/`correo` en destinatarios; roles DOCENTE/ESTUDIANTE/COORDINADOR |
| E2E-03 | PASS | `composite`: fila entregada + log `NOTIFICACION_PENDIENTE_DE_CANAL` |
| E2E-04 | PASS | `log`: 0 filas en `NotificacionSaliente` |
| E2E-05 | PASS | Despachador deshabilitado: fila `PENDIENTE`, retiro 2xx |
| E2E-06 | NO_EJECUTADO | Simular fallo exigiría alterar un SP en la BD local compartida; TEST_PLAN §6 permite declararlo |
| E2E-07 | PASS | `notification.publish.total` y `notification.dispatch.total` en `/actuator/prometheus` |
| E2E-08 | PASS | `NotificacionSaliente`=0 y matrícula restaurada (estado activo); puerto 8090 liberado |

Totales: ejecutadas 8, PASSED 7, FAILED 0, SKIPPED 1.

**Hallazgo de la Fase 5 (corregido):** el arranque real fallaba porque `OutboxNotificacionAdapterConfiguration` exigía un bean `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) y Spring Boot 4 autoconfigura `tools.jackson` (Jackson 3); el fixture de `NotificationWiringTest` declaraba ese bean y lo enmascaraba. Corrección en 5e7b29c: el adaptador crea su propio `ObjectMapper` (igual que el despachador) y se elimina el bean del fixture. Test modificado para el PR: `NotificationWiringTest` (fixture sin `ObjectMapper`; 12/12 en verde).

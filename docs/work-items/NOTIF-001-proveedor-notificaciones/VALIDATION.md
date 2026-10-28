# VALIDATION — NOTIF-001: Proveedor de Notificaciones Intercambiable (Log, Outbox, Composite)

## Dictamen

IN PROGRESS (Fases 1, 2 y 3 completadas; Fase 3 pendiente de aprobación formal para avanzar a Fase 4).

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-10-27 Local (Docker SQL Server) | `mvnw.cmd test -Pintegration` (develop) | 1 | BASELINE PREEXISTENTE | Tests run: 271, Failures: 7, Errors: 10, Skipped: 7 |
| 2026-10-27 Local (Docker SQL Server) | `mvnw.cmd test -Pintegration` (feature) | 1 | +9 IT GREEN (NOTIF-001) | Tests run: 280, Failures: 7, Errors: 10, Skipped: 7 |
| 2026-10-27 Local (Java 25) | `mvnw.cmd test` | 0 | PASS | Tests run: 2657, Failures: 0, Errors: 0, Skipped: 0 |
| 2026-10-27 Local (Java 25) | `mvnw.cmd jacoco:report` | 0 | PASS | Reporte JaCoCo generado en target/site/jacoco/ |

## Hallazgos de Producción e Integración (Fase 3)

1. **Mapeo de Columnas de `usp_reservar_notificaciones_salientes` (CONTRACT §2.2):**
   - *Hallazgo:* En SQL Server real, el SP devuelve 11 columnas (`id, tipo, asunto, mensaje, destinatarios, datos, intentos, creadoEn, idCorrelacion, traceId, spanId`), donde `creadoEn` es la columna 7 (índice 7) y `idCorrelacion` es la 8. En cambio, el mock unitario de `NotificacionSalienteJpaRepositoryTest` retornaba un entero literal `5` (`maxIntentos`) en el índice 7 y `creadoEn` en el índice 8. Al ejecutar contra base de datos real en Fase 3, `toInstant(fila[8])` fallaba con `DateTimeParseException` al recibir un UUID.
   - *Fix:* Se ajustó `NotificacionSalienteJpaRepository` mediante `isDateLike(fila[7])` para resolver `creadoEn` directamente desde el índice 7 según el contrato canónico DB, manteniendo tolerancia si un mock envía `maxIntentos` en dicho índice.
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

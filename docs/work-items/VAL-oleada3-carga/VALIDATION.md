---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VAL-oleada3-carga — VALIDATION

Fecha: 2026-11-13 (UTC 07:00–07:35). Máquina: portátil de desarrollo, Windows 10, 4 núcleos, 8 GB (Docker Desktop con
3,8 GB). Todo local: SQL Server 2022 (`sql_server_asistencias`), Security Baseline 0.1.11 (`asistencias-security-lab`:
Keycloak, PEP, PDP, OPA, Redis, SurrealDB), MinIO; ClamAV detenido. Backend: jar de `origin/develop` (`da915d75`)
construido con `mvnw -DskipTests package`, modo `baseline`, heap 512 MB y pool Hikari 10 por réplica (por
`JAVA_TOOL_OPTIONS`; el cambio de `application.yml` de este work item fija los mismos valores por defecto).
k6 `grafana/k6:1.3.0@sha256:3ddc8b1a…`; Traefik `v3.2@sha256:e561a37f…`. Evidencias en [evidencias/](evidencias/)
(resumen JSON de k6 + texto; ningún archivo contiene tokens: verificado con `grep eyJ` = 0).

## 1. Resultados

Carga: mezcla 50/25/25 estudiante/docente/coordinador, pausa 0,5–1,5 s, rampa 20 s. "err" = `http_req_failed`.

| Corrida (evidencia) | Topología | VU / meseta | Peticiones | RPS | err | p95 total | p95 lectura propia | p95 agregados | Códigos |
|---|---|---|---|---|---|---|---|---|---|
| `071319Z-una-replica-10vus` | 1 réplica directa | 10 / 2 min | 840 | 5,68 | 0,7 % | 1924 ms | 1945 ms | 1731 ms | 200×834, 503×6 |
| `071556Z-dos-replicas-10vus` | 2 réplicas + Traefik | 10 / 2 min | 797 | 5,32 | **0 %** | 2224 ms | 2297 ms | 1945 ms | 200×797; reparto 398 / 399 |
| `070046Z-una-replica-20vus` | 1 réplica directa | 20 / 2 min | 1441 | 9,64 | 73 % | 3296 ms | 3177 ms | 3487 ms | (sin conteo; corrida de diagnóstico de 1 min: 200×413, 503×543) |
| `073245Z-dos-replicas-20vus` | 2 réplicas + Traefik | 20 / 2 min | 1181 | 7,90 | 38,7 % | 3912 ms | 3918 ms | 3864 ms | 200×724, 503×457; reparto 349+239 / 375+218 |
| `072032Z-dos-replicas-pep-caido-limpio` | 2 réplicas, **PEP detenido** | 10 / 40 s | 433 | 7,88 | 0,2 %* | 219 ms | — | — | 503×432, timeout×1 — **umbrales OK (exit 0)** |
| `072753Z-dos-replicas-caida-replica-limpio` | 2 réplicas, **réplica 1 muerta a los ~50 s** | 10 / 90 s | 677 | 6,18 | 3,1 % | 1548 ms | 1500 ms | 1672 ms | 200×656, 502×21 |
| `20261113-clamav-caido-subida.txt` | 1 réplica, **ClamAV detenido** | 6 subidas secuenciales | 6 | — | — | 163–820 ms | — | — | 500×6 `INTERNAL_APPLICATION_ERROR` |

\* En modo `dependencia-caida` el 503 es la respuesta esperada; el único "fallo" es una petición que no respondió en 30 s.

Corridas auxiliares conservadas por trazabilidad: `071858Z-dos-replicas-pep-caido` (el PEP se detuvo a mitad de la
corrida: 49 × 200 antes y 418 × 503 después, p95 565 ms) y `072222Z-dos-replicas-caida-replica` (coincidió con la
recuperación del PEP: 503 entre 07:22:30 y 07:23:00, luego 100 % 200; caída de la réplica con solo 6 × 502). Los
`*.ventanas.txt` cuentan el access log de Traefik por ventanas de 10 s.

## 2. Lectura de los números

**REA-01 (objetivos):** con 10 VU las lecturas cumplen disponibilidad (0–0,7 % de error) pero **no** los p95 (≈1,9–2,3 s
frente a 800/1500 ms). El cuello de botella no es el backend: durante la carga el PDP del Security Baseline usó 160 % de
CPU, SurrealDB 100 % y Keycloak 75 %, mientras el backend sirve `/actuator/health/liveness` en 16 ms. En secuencia, una
lectura autenticada cuesta ≈200–250 ms, casi todo la decisión PEP→PDP de cada petición (sin caché de decisiones). A 20
VU el PDP se satura, las llamadas al PEP superan el umbral de llamada lenta (2 s) y el breaker
`securityBaselinePep` se abre: el backend falla cerrado y rápido con 503 `ACCESS_DECISION_UNAVAILABLE` (nunca 200 sin
decisión). Capacidad observada en este portátil: ≈5–6 RPS sostenidos sin error.

**Una vs. dos réplicas:** con el PDP compartido como cuello de botella, la segunda réplica no sube el throughput
(5,68 → 5,32 RPS a 10 VU; el p95 empeora algo porque el portátil corre dos JVM más el lab). A 20 VU la tasa de error baja
de 57–73 % a 38,7 % y las respuestas 200 suben de 389 a 724 en 2 min: cada réplica tiene su propio breaker y reparte las
llamadas lentas. El reparto de Traefik es 50/50 exacto (398/399).

**REA-06 (comportamiento medido):**
- PEP caído con dos réplicas: 432/433 respuestas 503 `ACCESS_DECISION_UNAVAILABLE`, p95 219 ms, cero 200. Fallo cerrado
  y rápido en ambas réplicas. Una petición quedó sin respuesta 30 s (Traefik 504): la que estaba en vuelo cuando se
  detuvo el contenedor; el `request-timeout` de 5 s del cliente del PEP no la cortó (hallazgo H4).
- Réplica caída bajo carga: la réplica 1 murió a las 07:28:44; Traefik devolvió 502 durante 4,5 s (21 peticiones, 3,1 %
  de la corrida), su health check falló a las 07:28:49 y desde ahí el 100 % fue a la réplica 0 con 200. Sin intervención.
- Recuperación del PEP: tras `docker start` el PEP tardó ≈2,5 min en atender; mientras tanto 503 rápidos y después
  recuperación sola (breaker half-open), sin reiniciar el backend.
- ClamAV caído: la subida falla cerrada (nada llega a MinIO; el escaneo va antes del almacenamiento) en 160–820 ms,
  pero responde **500** `INTERNAL_APPLICATION_ERROR` en lugar de 503 (hallazgo H3).
- SMTP: no está en la ruta síncrona (outbox, `OUTBOX_NOTIFICATION_SENDER=log` por defecto); no se midió.

## 3. Hallazgos

| # | Hallazgo | Impacto | Responsable sugerido |
|---|---|---|---|
| H1 | Cada petición paga una decisión PEP→PDP (~200 ms) sin caché; el PDP (y SurrealDB) es el límite de capacidad. | REA-01 p95 no se cumple en el lab; se satura a ~20 VU | Security Baseline (terceros) / José Alejandro: caché de decisiones con TTL corto en el PEP o en el adaptador |
| H2 | `/actuator/prometheus` en el puerto de gestión responde **401**: la cadena de Spring Security también cubre el contexto de gestión y solo `/actuator/health/**` es público. El scrape documentado en `docs/observability/README.md` no funciona. | Sin métricas Hikari/HTTP para dimensionar el pool | Equipo (OBS) |
| H3 | ClamAV caído → 500 `INTERNAL_APPLICATION_ERROR` (debería ser 503 como el PEP). Falla cerrado, no almacena. | Semántica de error; alertas confunden caída de dependencia con bug | José Alejandro |
| H4 | Con el PEP detenido de golpe, una petición en vuelo esperó más de 30 s pese a `request-timeout: 5s`. | Hilo retenido; con muchas peticiones en vuelo podría acumularse | José Alejandro |
| H5 | Ventana de detección de réplica caída: 502 durante ≈5 s. | 3 % de error en la prueba | Infra: middleware `retry` de Traefik (solo errores de red) o health check más corto |

## 4. Estado por instancia con dos réplicas (F12-06 / REA-03)

| Componente | Archivo | Con 2 réplicas |
|---|---|---|
| SSE (`Sinks.many().multicast().directBestEffort()`) | `infrastructure/adapter/secondary/realtime/localsse/ReactorRealtimeAdapter.java:39` | **Por instancia.** Un evento publicado en la réplica A no llega a clientes conectados a la B. Caso concreto: `RotarTokensAsistenciaAutomaticoUseCaseImpl.java:79` publica el QR rotado desde la réplica cuyo scheduler rotó; el docente conectado a la otra no recibe el QR nuevo por SSE (≈50 % con 2 réplicas) hasta recargar. Requiere bus distribuido (LB-005) o afinidad. |
| Caché de parámetros (`ConcurrentHashMap`, sin TTL) | `.../jpa/repository/ParameterCatalogJpaRepository.java:31` | **Desfase indefinido.** `ActualizarParametroUseCaseImpl.java:80` invalida solo la réplica que atendió la edición; la otra sigue con el valor viejo hasta reiniciar (con Azure App Configuration lo cubre Event Grid, que llega a una sola instancia). Requiere TTL o invalidación distribuida. |
| Caché de mensajes (`ConcurrentHashMap`, sin TTL) | `.../jpa/repository/MessageCatalogJpaRepository.java:30-31` | Igual que parámetros (los mensajes cambian poco). |
| Caché de plantillas de notificación | `.../notification/template/PlantillaNotificacionJpaRepository.java` | TTL (`SMTP_NOTIFICATION_TEMPLATE_CACHE_TTL`, 60 s): converge solo. |
| Límite de subidas por usuario/IP (`SlidingWindowRateLimiter`) | `controller/filter/OperacionAutenticadaRateLimitFilter.java` | Por instancia: con N réplicas el límite efectivo es hasta N veces el configurado. |
| Límite de verificación de correo por IP | `controller/filter/VerificacionCorreoRateLimitFilter.java` | Por instancia (ya documentado en su Javadoc); detrás de un proxy además todas las IP son la del proxy (`remoteAddr`). |
| Tope SMTP por minuto/día | `notification/channel/smtp/TopeEnvios.java` | Por instancia: con N réplicas el tope real es N × `SMTP_NOTIFICATION_RATE_PER_*`. |
| Outbox de notificaciones | `notification/outbox/NotificacionOutboxDispatcher.java` | Seguro: lotes con lease e `instance-id` (hostname + aleatorio). |
| Schedulers (rotación de QR de matrícula y de asistencia, alertas de inasistencia, festivos) | `adapter/primary/scheduler/*.java` | Corren en todas las réplicas; la BD decide (rotación/cierre idempotentes, cerrojo y marca de agua en alertas). Logs de las dos réplicas sin errores de schedulers, outbox ni pool: solo `Failed to export spans` (no había collector OTLP) y los 6 de ClamAV provocados. |
| Sesión HTTP | `SecurityConfig` (`STATELESS`) | Sin estado: cualquier réplica atiende cualquier petición (demostrado por el round-robin sin errores). |
| Pool de conexiones | `application.yml` `spring.datasource.hikari.*` | 2 × 10 = 20 conexiones; sin errores de pool en los logs. |

## 5. Pruebas y comandos

| Comando | Resultado |
|---|---|
| `.\mvnw.cmd -B -ntp -o test -Dtest=OperationalReadinessConfigurationTest -Djacoco.skip=true` | 8 pruebas, 0 fallos (incluye la nueva `f12_08_pool_de_conexiones_dimensionado_por_variable_y_por_replica`) |
| `docker run … grafana/k6:1.3.0@sha256:… inspect /scripts/carga-lectura.js` | OK (mismo paso que el workflow) |
| `docker compose -f docker-compose.replicas.yml config -q` (Infra) | OK; `docker-compose.yml` sin cambios y `config -q` OK |
| Corridas de la tabla §1 | Evidencias en `evidencias/` |

No ejecutado: `clean verify` completo (memoria del portátil durante las corridas), actionlint sobre `load-test.yml`, el
workflow en GitHub (requiere el secreto `LOAD_TEST_TOKENS_JSON` y un entorno desplegado).

## 6. Cómo repetirlo

```powershell
# 0. Dependencias (sin down -v): SQL Server arriba; lab de seguridad arrancado y sano (Keycloak antes que PDP/PEP)
docker start sql_server_asistencias
docker start asistencias-security-lab-redis-1 asistencias-security-lab-surrealdb-1 asistencias-security-lab-opa-1 asistencias-security-lab-keycloak-1
#    (esperar Keycloak healthy) 
docker start asistencias-security-lab-pdp-1 asistencias-security-lab-pep-1
# 1. Jar del backend
.\mvnw.cmd -B -ntp -DskipTests package
# 2. Una réplica y su línea base
.\scripts\load\start-replicas.ps1 -Replicas 1
.\scripts\load\run-load-test.ps1 -Label una-replica-10vus -Vus 10 -Duration 2m
.\scripts\load\start-replicas.ps1 -Stop
# 3. Dos réplicas detrás de Traefik (repo Infra)
.\scripts\load\start-replicas.ps1 -Replicas 2
docker compose -f ..\Repositorio_GestionAsistenciaInfra\docker-compose.replicas.yml up -d
.\scripts\load\run-load-test.ps1 -Label dos-replicas-10vus -Vus 10 -Duration 2m -BaseUrl http://host.docker.internal:8088 -ProxyContainer proxy_replicas_asistencias
# 4. PEP caído (se detiene tras obtener los tokens y se rearranca al final)
.\scripts\load\run-load-test.ps1 -Label dos-replicas-pep-caido -Mode dependencia-caida -StopContainerDuringRun asistencias-security-lab-pep-1 -Vus 10 -Duration 40s -Ramp 5s -BaseUrl http://host.docker.internal:8088 -ProxyContainer proxy_replicas_asistencias
# 5. Réplica caída: durante una corrida de 90 s, matar el árbol del PID de la réplica 1 (ver %TEMP%\asistencias-uco-replicas.json)
# 6. Apagar
.\scripts\load\start-replicas.ps1 -Stop
docker compose -f ..\Repositorio_GestionAsistenciaInfra\docker-compose.replicas.yml down
```

Exit code de k6: 0 = todos los umbrales cumplen; 99 = alguno falla (esperado hoy en modo `normal`, ver §2).

## 7. Estado propuesto para la lista de chequeo

| Ítem | Antes | Propuesto | Por qué |
|---|---|---|---|
| REA-06 | NO CUMPLE | **CUMPLE** | Medido y reproducible: dos réplicas (reparto, caída de una réplica con recuperación en ≈5 s), PEP caído (503 rápido, fallo cerrado), ClamAV caído (fallo cerrado). Hallazgos H3–H5 quedan como mejoras. SMTP no está en la ruta síncrona. |
| REA-01 | PARCIAL | PARCIAL (avanza) | Tres objetivos definidos y prueba de carga corrida; los p95 **no se cumplen** en el lab por el PDP (H1). Falta re-medir en un entorno dimensionado o con caché de decisiones, y el timeout de consulta JDBC. |
| F12-08 | PARCIAL | PARCIAL (avanza) | Probado con dos réplicas (schedulers y outbox sin conflictos en los logs) y pool fijado por variable con prueba. Falta dimensionar con métricas reales (bloqueado por H2). |
| F12-06 / REA-03 | PARCIAL | PARCIAL | Comportamiento documentado (§4); se agregan dos hallazgos nuevos (cachés sin TTL, QR por SSE con 2 réplicas). El bus distribuido sigue pendiente (LB-005). |

## 8. Pendiente

- Flujo de escritura en la prueba de carga: requiere BD desechable (`e2e_safety.py`); hoy solo lecturas.
- H1–H5 (§3) y los dos riesgos nuevos de §4 (QR por SSE, cachés sin TTL).
- Correr `load-test.yml` contra un entorno desplegado (staging) para fijar los objetivos con hardware real.

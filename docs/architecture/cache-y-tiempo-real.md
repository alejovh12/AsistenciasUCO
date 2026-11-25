---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-25
---

# Caché de consultas y tiempo real entre réplicas (REDIS-001)

Decisión: [ADR-007](../adr/ADR-007-redis-cache-y-relevo-realtime.md). Work item: [REDIS-001](../work-items/REDIS-001-cache-y-tiempo-real/PLAN.md).
Complementa [reactive-realtime.md](reactive-realtime.md) (SSE) y [adapter-composition-standard.md](adapter-composition-standard.md).

## 1. Resumen

| Capacidad | Antes | Ahora | Selección |
| :--- | :--- | :--- | :--- |
| Tiempo real | SSE por grupo en memoria de una JVM (TD-003) | Igual, más relevo entre réplicas por Redis Pub/Sub | `APP_ADAPTERS_REALTIME_PROVIDER=local-sse` (defecto) \| `redis-sse` |
| Caché de consultas | Solo catálogos de mensajes y parámetros (Caffeine) | Portal, indicadores y catálogos con cache-aside | `APP_ADAPTERS_CACHE_PROVIDER=local` (defecto) \| `redis` \| `none` |
| Límite del QR por estudiante/token | En memoria por réplica | Compartido en Redis cuando la caché es `redis` | el mismo `APP_ADAPTERS_CACHE_PROVIDER` |

Redis es **opcional**: sin él, el Backend funciona como antes (caché en memoria y SSE local). Si Redis se cae con `redis`
activo, las consultas van a la BD, el límite lo decide la réplica y los eventos se entregan en la réplica de origen; ninguna
petición falla por Redis y la readiness no depende de él (`REDIS_HEALTH_ENABLED=false` por defecto).

## 2. Tiempo real: qué casos lo necesitan y con qué tecnología

### 2.1 SSE, WebSocket o polling

| Tecnología | Dirección | Cuándo conviene | En este proyecto |
| :--- | :--- | :--- | :--- |
| **SSE** | Servidor → cliente sobre HTTP | Avisos y contadores que el servidor empuja; el cliente actúa con peticiones REST normales | **Elegida** (ya existía). Pasa por el mismo Bearer, CORS y PEP que la API; reconexión del navegador |
| WebSocket | Bidireccional, protocolo propio | Chat, edición colaborativa, juegos: el cliente envía muchos mensajes por el mismo canal | **Descartada**: ningún caso necesita que el navegador envíe datos por el canal (el estudiante marca con un POST). Añade handshake propio, otra ruta para el PEP y estado de sesión |
| Polling | Cliente pregunta cada N s | Datos que cambian poco y sin infraestructura push | Solo como **respaldo** al reconectar (el cliente rehidrata por REST). La regla del proyecto prohíbe simular tiempo real con polling |

### 2.2 Casos de uso por módulo

Los eventos ya existen (`RealtimeAudienciaPolicy`); la tabla dice cuáles **necesitan** tiempo real y por qué.

| Módulo | Caso de uso | Evento | ¿Necesita tiempo real? | Justificación |
| :--- | :--- | :--- | :--- | :--- |
| Toma de asistencia (docente) | Proyección del QR que rota | `ASISTENCIA_QR_ROTADO`, `MATRICULA_QR_ROTADO` | **Sí, crítico** | El código cambia cada pocos segundos; un QR viejo rechaza al estudiante |
| Toma de asistencia (docente) | Contador de presentes mientras los estudiantes escanean | `ASISTENCIA_QR_REGISTRADA`, `ASISTENCIAS_TOMA_ACTUALIZADAS` | **Sí, crítico** | 30 a 200 registros en 1-2 minutos; el docente decide cuándo cerrar viendo el conteo |
| Toma de asistencia | Apertura y cierre de toma | `TOMA_ABIERTA`, `TOMA_CERRADA` | **Sí** | El estudiante solo puede marcar con la toma abierta |
| Sesiones | Cancelación de una sesión | `SESION_CANCELADA` | **Sí** | Evita que estudiantes vayan al aula o intenten marcar |
| Sesiones | Sesión extraordinaria creada, solicitada o rechazada | `SESION_MANUAL_CREADA`, `SESION_SOLICITUD_*` | Sí, deseable | Contador de solicitudes del coordinador y del docente |
| Matrícula por QR | Estudiante matriculado | `ESTUDIANTE_MATRICULADO` | Sí, deseable | El docente ve crecer el padrón durante la matrícula en clase |
| Alertas e inasistencia | Riesgo/pérdida por inasistencia, faltas avisadas | `ALERTA_INASISTENCIA`, `FALTA_SESION_AVISADA` | Sí, deseable | Toca **notificaciones (Sergio)**: aquí solo se entrega el evento existente |
| Revisiones de cancelación | Creada, decidida, resuelta, reasignada | `REVISION_CANCELACION_*` | Sí, deseable | Bandeja del coordinador/decano |
| Configuración | Parámetro o mensaje cambiado | `PARAMETER_UPDATED`, `MESSAGE_UPDATED` | No para el usuario final | Sirve para invalidar cachés; DR-AZ-001 sigue abierta para mostrarlos |
| Indicadores (coordinador, decano, admin) | KPIs y comparativas | — | **No** | Agregados; basta la caché de 5 min y el botón de recargar |
| Catálogos, planes, periodos, festivos | Consultas | — | **No** | Cambian por semestre o por migración |

### 2.3 Hallazgo: el canal es por grupo

El navegador abre **un** stream por grupo (`startForGroup`). Los contadores globales del menú (solicitudes del coordinador y del
docente, faltas reclamables del estudiante) solo se actualizan mientras el usuario está en la página de un grupo. La solución es
un **canal SSE por usuario**, ya construido (TD-084 resuelta): `GET /api/v1/realtime/stream/usuario`, que el layout del
frontend abre una vez por sesión (`RealtimeService.startForUser`). Entrega (i) los eventos cuyo tipo admite la audiencia `DESTINATARIO` y nombran al usuario (`FALTA_SESION_AVISADA`,
`ALERTA_INASISTENCIA`, `REVISION_CANCELACION_*`) y (ii) a docentes titulares y roles con ámbito, `SESION_SOLICITUD_CREADA`,
`SESION_SOLICITUD_RECHAZADA` y `SESION_MANUAL_CREADA` de los grupos sobre los que tiene titularidad o ámbito. El ámbito se
resuelve con el mismo SP de audiencia del canal de grupo, una vez por grupo y conexión, fuera del hilo que publica; si el
usuario solo tiene rol ESTUDIANTE no se consulta. Nunca entrega eventos del aula (QR, toma, presentes).
No hay bandeja in-app de notificaciones (las de Sergio salen por correo), así que el canal no duplica ese componente.

### 2.4 Varias réplicas (TD-003)

Con `redis-sse`, `RedisRealtimeRelayAdapter` publica cada evento en el canal `asistencias-uco:realtime:v1`; cada réplica lo
recibe con `RedisRealtimeSuscriptor` y lo entrega a sus clientes SSE (incluida la de origen, así nunca hay duplicados). Contrato
HTTP, filtro por grupo y audiencias, heartbeat y métricas no cambian. Garantías: las mismas del SSE local (best-effort, sin
replay). Métricas nuevas: `realtime.relay.published` y `realtime.relay.fallback`.

## 3. Caché de consultas

### 3.1 Estrategias comparadas

| Estrategia | Cómo funciona | Ventaja | Riesgo | Decisión |
| :--- | :--- | :--- | :--- | :--- |
| **Cache-aside** (lazy loading) | La app busca en caché; si no está, lee la BD y guarda con TTL. Escribir = BD + invalidar | Simple, la BD sigue siendo la verdad, Redis caído = ir a la BD | Datos viejos hasta el TTL o la invalidación; estampida al vencer (mitigada) | **Elegida** |
| Read-through | Igual, pero la caché carga por sí misma (loader dentro de la caché) | Código de lectura más limpio | Acopla la caché a la fuente; en Redis no existe nativo | Equivale a cache-aside encapsulado (así funciona Caffeine en `local`) |
| Write-through | Cada escritura va a la caché y a la BD a la vez | Caché siempre fresca | Las escrituras pasan por procedimientos almacenados que devuelven otra forma de dato; duplicaría reglas de la BD | Descartada |
| Write-behind (write-back) | Se escribe en caché y la BD se actualiza después, en lote | Escrituras muy rápidas | **Pérdida de asistencias** si Redis cae antes de volcar; rompe auditoría y reglas de la BD | Descartada |

TTL e invalidación: cada región tiene un TTL y además:

| Disparador | Qué se borra |
| :--- | :--- |
| Registro por QR del estudiante | Sus materias y su resumen |
| Matrícula por QR del estudiante | Todo su portal |
| Evento del grupo: `ASISTENCIAS_TOMA_ACTUALIZADAS`, `ASISTENCIAS_SESION_ACTUALIZADAS`, `TOMA_CERRADA`, `SESION_CANCELADA`, `SESION_MANUAL_CREADA`, `ESTUDIANTE_MATRICULADO`, `FALTA_SESION_AVISADA` | Portal de cada estudiante y del docente con ese grupo en caché (etiqueta `grupo:<id>`) |
| `ALERTA_INASISTENCIA` | Portal del estudiante destinatario |

Las etiquetas se asocian al cargar materias u horarios (estudiante) y asignaturas u horarios (docente): cada `idGrupo` del
resultado etiqueta todas las claves del portal de ese usuario. En Redis la etiqueta es un SET
`<prefijo>:cache:v1:portal:etiqueta:grupo:<id>` con el TTL de la región; en `local`, un índice Caffeine. La invalidación la hace
`InvalidacionCachePorEventos`, que observa cada evento que la réplica entrega (`ReactorRealtimeAdapter.alEntregar`), sea propio
o relevado por Redis, así también funciona con caché `local` y varias réplicas. `ASISTENCIA_QR_REGISTRADA` no invalida el grupo
(sería borrar la caché 200 veces por toma). Residual (TD-083): cambios sin evento de grupo esperan el TTL de 60 s. Anti-estampida: Caffeine carga una vez por clave; en Redis, una sola carga por clave y
réplica (las demás peticiones esperan ese resultado).

### 3.2 Qué se cachea (implementado)

| Región (TTL) | Consulta | Clave | Por qué |
| :--- | :--- | :--- | :--- |
| `portal` (60 s) | Materias, horarios, resumen y plan del estudiante | `<usuario>:<consulta>` | Es lo que abren 200 estudiantes al entrar a clase o tras escanear; recargas repetidas |
| `portal` (60 s) | Asignaturas y horarios del docente | `<usuario>:<consulta>` | Se piden en cada navegación del docente |
| `indicadores` (5 min) | KPIs, alertas tempranas, comparativas, aforo y tendencia (coordinador, decano, admin) | hash del filtro completo (actor, periodo, programa, página) | Agregados costosos (antes >240 s, hoy ~3 s); se consultan al abrir cada tablero |
| `catalogos` (30 min) | Tipos de programa, modalidades, jornadas, tipos de identificación | nombre fijo | Solo consulta (semilla); iguales para todos |

Nunca se cachea: la toma en curso, el registro por QR, los faltantes, la sábana del docente, reclamos y cualquier escritura.
La clave del portal siempre incluye al usuario y la de indicadores al actor: un usuario nunca ve el resultado de otro ni se
salta el alcance institucional (la autorización sigue en el caso de uso, que se ejecuta en cada fallo de caché con el actor).

### 3.3 Candidatos para una fase siguiente

| Consulta | Estrategia sugerida | Nota |
| :--- | :--- | :--- |
| Periodos académicos, festivos, recesos | Cache-aside 30 min + invalidar en sus escrituras (solo admin) | Requiere decorar los casos de escritura |
| Plan de estudios por programa (compartido) | Cache-aside por programa | Hoy se cachea por estudiante |
| Padrón del grupo (docente) | Cache-aside 30 s + invalidar con `ESTUDIANTE_MATRICULADO` | El evento ya existe |
| Autorización del PEP por petición | Caché corta en el PEP/PDP | Pertenece al componente de Seguridad |

## 4. Configuración

```yaml
app:
  adapters:
    realtime:
      provider: ${APP_ADAPTERS_REALTIME_PROVIDER:local-sse}   # local-sse | redis-sse
      redis:
        canal: ${APP_ADAPTERS_REALTIME_REDIS_CANAL:asistencias-uco:realtime:v1}
    cache:
      provider: ${APP_ADAPTERS_CACHE_PROVIDER:local}          # local | redis | none
      prefijo: ${APP_ADAPTERS_CACHE_PREFIJO:asistencias-uco}
      ttl: { catalogos: 30m, indicadores: 5m, portal: 60s }  # APP_ADAPTERS_CACHE_TTL_*
spring.data.redis: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_TIMEOUT (500ms), REDIS_CONNECT_TIMEOUT (1s)
management.health.redis.enabled: ${REDIS_HEALTH_ENABLED:false}
```

Laboratorio: sección `asis-cache` o escenario `cache` de `infra.ps1 stack` (AsistenciasUCO-Infra), o el perfil `cache` del
`docker-compose.yml` de Infra. Redis sin persistencia, `maxmemory 128mb`, `allkeys-lru`.

## 5. Código

| Pieza | Ruta |
| :--- | :--- |
| Contrato interno y regiones | `infrastructure/cache/CacheConsultas`, `RegionCache`, `MetricasCache` |
| Proveedores | `infrastructure/cache/local/CaffeineCacheConsultas`, `cache/redis/RedisCacheConsultas`, `cache/SinCacheConsultas` |
| Decoradores de InputPorts | `infrastructure/cache/decorador/ConsultasCacheadasPostProcessor` (no toca Application ni los `*WiringConfiguration`) |
| Invalidación por eventos | `infrastructure/cache/decorador/InvalidacionCachePorEventos` (registrada en `CacheAdapterConfiguration`) |
| Composition Root | `infrastructure/config/adapters/cache/CacheAdapterConfiguration`, `config/properties/adapters/CacheAdapterProperties` |
| Límite del QR | `adapter/secondary/security/RedisLimitadorIntentosAdapter` |
| Relevo realtime | `adapter/secondary/realtime/redis/*`, `config/adapters/realtime/redis/RedisSseRealtimeAdapterConfiguration` |

Métrica de la caché: `asistencias_cache_consultas_total{proveedor,region,resultado=acierto|fallo|error}`.

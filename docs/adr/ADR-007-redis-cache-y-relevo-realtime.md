---
status: accepted
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-11-25
---

# ADR-007 — Redis opcional para caché de consultas y relevo de tiempo real entre réplicas

- **Estado:** accepted (pedido y aprobado por John, owner del proyecto, el 2026-11-25)
- **Fecha:** 2026-11-25
- **Work item:** [REDIS-001](../work-items/REDIS-001-cache-y-tiempo-real/PLAN.md)
- **Guía:** [cache-y-tiempo-real.md](../architecture/cache-y-tiempo-real.md)
- **Relación:** levanta para este alcance la restricción "No Redis" de LB-004 ([DECISIONS](../work-items/LB-004-stateless-serverless-readiness/DECISIONS.md)); atiende TD-003.

## Contexto

1. El laboratorio ya corre el Backend con dos réplicas detrás de Traefik (VAL-oleada3). El SSE vive en memoria de cada JVM
   (TD-003): un evento publicado en una réplica no llega a los clientes conectados a la otra, y el límite por minuto del QR se
   cuenta por réplica.
2. En una toma por QR, decenas o cientos de estudiantes abren a la vez las mismas pantallas (materias, horarios, resumen) y los
   tableros de coordinador, decano y administrador ejecutan agregados costosos en cada visita.
3. El owner pidió revisar qué casos necesitan tiempo real (WebSocket, SSE) y dónde aplicar caché Redis con qué estrategia.

## Decisión

1. **SSE se mantiene; no se introduce WebSocket.** Ningún caso necesita un canal del navegador al servidor.
2. **Redis Pub/Sub como relevo del SSE** (`app.adapters.realtime.provider=redis-sse`). Cada réplica conserva su sink Reactor y su
   endpoint; el contrato HTTP y las garantías (best-effort, sin replay) no cambian. Sin Redis, entrega local.
3. **Caché de consultas cache-aside con TTL por región e invalidación puntual** (`app.adapters.cache.provider=local|redis|none`).
   Se aplica con decoradores de InputPorts registrados desde el Composition Root; Application no conoce la caché.
   Write-through y write-behind se descartan (ver la guía).
4. **Redis es opcional y no es autoridad**: proveedor por defecto `local` (Caffeine) y `local-sse`; una caída de Redis degrada a
   la BD y al SSE local, nunca falla una petición ni la readiness.
5. Sin persistencia en Redis (`--save ""`, sin AOF), tope de 128 MB con `allkeys-lru` en el laboratorio (PC de 8 GB).
6. RabbitMQ (mencionado como evolución) no se adopta para el realtime: Redis ya cubre la caché y Pub/Sub basta para eventos
   efímeros. Un broker durable solo se justificaría si se exige replay (TD-011).

## Consecuencias

- Nuevas dependencias: `spring-boot-starter-data-redis` (Lettuce). Se conecta solo con `redis`/`redis-sse`.
- Frescura del portal: el registro por QR y la matrícula del propio estudiante invalidan su caché; los eventos de grupo que
  ya existen (toma actualizada o cerrada, sesión cancelada o creada, matrícula, falta avisada, alerta) invalidan el portal de
  los estudiantes y del docente del grupo mediante etiquetas `grupo:<id>`. Queda un residual de hasta 60 s en cambios sin
  evento de grupo (TD-083).
- El canal SSE por usuario queda pendiente (TD-084) por cruzarse con notificaciones, el PEP y el frontend.
- Pruebas: dobles para los adaptadores y una prueba con Redis real (`REDIS_TEST_HOST`) que corre fuera de CI.

## Alternativas descartadas

| Alternativa | Motivo |
| :--- | :--- |
| WebSocket/STOMP | Bidireccional innecesario; otra superficie de seguridad para el PEP |
| Sticky sessions en Traefik | No resuelve que el evento nazca en otra réplica |
| Spring Cache (`@Cacheable`) | Las anotaciones irían en Application, que no depende de Spring, y varios casos de uso son clases `final` (sin proxy) |
| Redis Streams | Daría replay, pero exige gestionar consumidores y offsets por cliente; no hay requisito de replay |

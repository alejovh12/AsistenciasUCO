---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# PLAN — VAL-oleada3-carga (REA-01, REA-03, REA-06, F12-06, F12-08)

Origen: `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`, ítems REA-01 (objetivos p95 y prueba de carga), REA-06 (prueba
con dos réplicas y con dependencias caídas), F12-08 (dos réplicas y pool dimensionado), F12-06 y REA-03 (estado por
instancia). Rama `fix/VAL-oleada3-carga-replicas` en Backend e Infra, desde `origin/develop`.

## 1. Objetivos REA-01 (medidos en el cliente, extremo a extremo, con Bearer real)

| # | Objetivo | Umbral k6 | Endpoints |
|---|---|---|---|
| 1 | Lecturas propias de estudiante y docente | `http_req_duration{clase:lecturaPropia}` p95 < 800 ms | `auth/me`, horarios, materias, alertas, reclamos (paginado), asignaturas |
| 2 | Agregados de coordinación | `http_req_duration{clase:agregado}` p95 < 1500 ms | indicadores, grupos (paginado), consolidado de asistencia, alertas tempranas |
| 3 | Disponibilidad bajo carga | `http_req_failed` < 1 % y `checks` > 99 % | todos |

Carga de referencia: 10 usuarios concurrentes (50 % estudiantes, 25 % docentes, 25 % coordinadores) con pausa de
lectura de 0,5–1,5 s entre pantallas, 2 min de meseta. Es la carga que el portátil de desarrollo (4 núcleos, 8 GB, todo
el lab local) sostiene; el objetivo de producción se re-mide en un entorno dimensionado (workflow `load-test.yml`).

Modo `dependencia-caida` (REA-06): con el PEP detenido toda petición autenticada debe responder 503
`ACCESS_DECISION_UNAVAILABLE` (fallo cerrado, nunca 200) con p95 < 2,5 s (connect-timeout 2 s o breaker abierto).

## 2. Suite de carga (Backend)

| Archivo | Qué hace |
|---|---|
| `scripts/load/k6/carga-lectura.js` | Escenarios por rol (`ramping-vus`), umbrales REA-01, submétricas por endpoint, conteo de códigos de estado, modo `dependencia-caida`. |
| `scripts/load/get_tokens.py` | Tokens PKCE de `uco-estudiante`, `uco-docente`, `uco-coordinador` con `spa_token.py` del lab; contraseñas de `lab.env`, nunca impresas. |
| `scripts/load/run-load-test.ps1` | Tokens a un temporal propio → k6 en Docker (`grafana/k6:1.3.0@sha256:3ddc8b1a…`) → evidencia en `evidencias/` → borra el temporal. Con `-ProxyContainer` cuenta el reparto por réplica en el access log de Traefik. |
| `scripts/load/start-replicas.ps1` | 1..4 réplicas en el host con el launcher oficial (`start-backend-baseline.ps1`): API 8080/8082, gestión 8090/8092, heap y pool por réplica, logs por réplica; `-Stop` mata el árbol registrado. |
| `.github/workflows/load-test.yml` | Solo `workflow_dispatch`; valida el script (`k6 inspect`) y, con el secreto `LOAD_TEST_TOKENS_JSON`, corre contra `base_url`. Acciones fijadas por SHA. |

Escritura: no se incluye un flujo de escritura. Todos los POST/PATCH de estudiante y docente mutan la BD de desarrollo
compartida (`gestionasistenciadb`); sin una BD desechable (ver `infra/security-baseline/README.md`, `e2e_safety.py`)
no es seguro repetirlos cientos de veces. Queda como pendiente.

## 3. Dos réplicas (Infra + Backend)

- Infra: `docker-compose.replicas.yml` (proyecto Compose independiente `asistencias-uco-replicas`, no toca
  `docker-compose.yml`) con Traefik v3.2 fijado por digest en `127.0.0.1:8088`; `components/proxy/replicas/` reparte
  round-robin entre `host.docker.internal:8080` y `:8082` con health check por réplica a `/actuator/health/readiness` de su
  puerto de gestión (cada 5 s, timeout 2 s) y servicio ponderado que propaga la salud. Sin rate-limit (k6 sale de una IP).
- Las dos réplicas comparten SQL Server, PEP/PDP y MinIO. El arranque no migra esquema (Hibernate `ddl-auto: none`).

## 4. Pool (F12-08)

`spring.datasource.hikari.maximum-pool-size: ${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:10}`, `minimum-idle` y
`connection-timeout` por variable (valores por defecto de Hikari, sin cambio de comportamiento). Regla: conexiones
totales = réplicas × pool (2 × 10 = 20) por debajo del tope del login de la app; con hilos virtuales el pool es el
limitador real de concurrencia contra la BD, así que se sube solo si la métrica `hikaricp_connections_pending` lo pide.
Prueba: `OperationalReadinessConfigurationTest.f12_08_pool_de_conexiones_dimensionado_por_variable_y_por_replica`.

## 5. Estado por instancia con dos réplicas (F12-06 / REA-03)

Ver la tabla de VALIDATION.md §4. Resumen: lo que vive en la BD (outbox con lease, schedulers con cerrojo o idempotentes,
tokens QR) es seguro con N réplicas; SSE y los límites de tasa son por instancia (comportamiento documentado, no
corregido aquí: el bus distribuido es LB-005 de José Alejandro y Sergio).

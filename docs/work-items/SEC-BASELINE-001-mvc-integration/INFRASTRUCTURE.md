# Infraestructura aislada

## Release

- Repositorio local de lectura: `.workspace/security-baseline-local`, tag `v0.1.6` (`272c8e3168aeee6934194e2278b3537dc3c5dd78`).
- Assets del release: `compose.yaml`, `default.env.example` (organización Docker como placeholder), `MANUAL.md`, `README.md`. Sin manuales PDF.
- Imágenes y procedencia: ver `LAB_RUNBOOK.md`.

## Docker (2026-10-14)

- Engine accesible: Docker Desktop 4.47.0, Engine 28.4.0, contexto `desktop-linux` (usado explícitamente con `--context`).
- El bloqueo anterior (named pipe denegado) no se reprodujo en esta sesión; no se cambió el contexto global.

## Laboratorio levantado

Proyecto `asistencias-security-lab`, archivo `infra/security-baseline-lab/compose.lab.yaml`, env-file local fuera de Git.

| Servicio | Imagen | Publicación | Salud |
|---|---|---|---|
| keycloak | `davidalzate/security-keycloak-local:0.1.6` | `127.0.0.1:19090` | healthy; realm `security-baseline` |
| surrealdb | `surrealdb/surrealdb:v3.3.0` | interna | healthy |
| redis | `redis:7.4-alpine` | interna | healthy |
| opa | `davidalzate/security-opa:0.1.6` | interna | `/health` y `/health?bundles=true` 200 |
| pdp | `davidalzate/security-pdp:0.1.6` | `127.0.0.1:18080` | healthy, `/actuator/health` UP |
| pep | `davidalzate/security-pep:0.1.6` | `127.0.0.1:18081` | `/actuator/health` y readiness UP |

- Red: `asistencias-security-lab-net` (`172.31.240.0/24`, Keycloak `172.31.240.10`). Se eliminó la red por defecto vacía `asistencias-security-lab_default` creada por el primer arranque.
- Volúmenes: `asistencias-security-lab-keycloak-data`, `-surrealdb-data`, `-redis-data`.
- Logs revisados (saneados): solo avisos benignos (`sun.misc.Unsafe`, exportación OTLP sin colector, features deprecadas de Keycloak).

## Servicios ajenos — sin alteración

`sql_server_asistencias`, `asistencias-uco-security-keycloak-1` (8081) y su DB, `minio_asistencias`, `clamav_asistencias` y los 5 servicios de observabilidad mantienen `startedAt` 2026-10-14T04:48Z (anterior a la sesión) y `restarts=0`. Backend 8080 y Angular 4200 no estaban en ejecución y no se arrancaron. No se ejecutaron escrituras SQL ni seeds académicos.

## Backend experimental

Puerto 8083, jar del worktree `.w/sec-mvc`, lanzado con `start-backend-pep-lab.ps1` contra la DB local (`ddl-auto=none`) y el Keycloak institucional. Solo peticiones GET; ninguna llegó a un handler de negocio. Detenido al terminar.

## Findings de seguridad

Ver `RISKS.md` (`SECURITY_FINDING` por archivo/tipo; valores no reproducidos).

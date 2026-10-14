# Runbook del laboratorio Security Baseline v0.1.6

Laboratorio **local y no productivo**. Proyecto Compose `asistencias-security-lab`, definido en
[`infra/security-baseline-lab/compose.lab.yaml`](../../../infra/security-baseline-lab/compose.lab.yaml).
Sustituye a la copia previa sin versionar de `.workspace/security-baseline-local/release-assets/`.

Prohibido en todo momento: `docker compose down -v`, `docker system prune`, borrar volúmenes,
tocar contenedores de otros proyectos (SQL Server, Keycloak institucional 8081, MinIO, ClamAV,
observabilidad).

## Procedencia de imágenes

La organización Docker Hub no está en el repositorio upstream (es el secreto de CI
`DOCKERHUB_NAMESPACE`). Se verificó `davidalzate` porque sus etiquetas `0.1.6` se publicaron entre
02:05 y 02:16 UTC del 2026-10-07, dentro de la ejecución del workflow oficial
`Publish security Docker distribution` del tag `v0.1.6` (`272c8e3`, 02:04:44–02:16:24 UTC), lanzado por
el único autor del repositorio, y las etiquetas 0.1.2–0.1.5 siguen el mismo patrón. Digests descargados:

| Imagen | Digest |
|---|---|
| `davidalzate/security-keycloak-local:0.1.6` | `sha256:3e94a8c51ef6cfb422eb9e8aa430a3083efb94276a5e8d598dff65a6673c178c` |
| `davidalzate/security-pdp:0.1.6` | `sha256:2b434d42952b73d5f714aae74b0f304fa84d68746b64e5dd00e775c9c0a686c9` |
| `davidalzate/security-pep:0.1.6` | `sha256:1efeca702841d9fcae2a348fca1a046bc801559cf5cb9b5ec8f5eac7ed4e0b51` |
| `davidalzate/security-opa:0.1.6` | `sha256:5f1d0dfa3424d7cca7332dace3ca72614d491417c56cd0e6c150249e92993d27` |
| `surrealdb/surrealdb:v3.3.0` | `sha256:681c6c22c287421b5c7d99e0fde79b6e0d32c36c1ddeaab2762a1661cb04cd20` |
| `redis:7.4-alpine` | `sha256:858f009f9709ce576febc734aa78b8f6d624b82571f9ddb6bda4377c833b3499` |

Tras un `pull`, comprobar: `docker image inspect <imagen> --format '{{index .RepoDigests 0}}'`.

## Primera instalación (PowerShell, desde la raíz del worktree)

```powershell
$lab = 'infra\security-baseline-lab'
$envFile = Join-Path $env:USERPROFILE '.asistencias-security-lab\lab.env'
$dc = @('--context','desktop-linux','compose','--project-name','asistencias-security-lab','--env-file',$envFile,'-f',"$lab\compose.lab.yaml")

# 1. Secretos aleatorios fuera de Git (ACL solo del usuario actual). Idempotente.
& "$lab\scripts\new-lab-env.ps1"

# 2. Validar y descargar.
docker @dc config --quiet
docker @dc pull

# 3. Solo Keycloak, y sustituir los secretos literales del realm upstream.
docker @dc up -d --wait keycloak
& "$lab\scripts\rotate-lab-client-secrets.ps1"

# 4. Resto del stack.
docker @dc up -d --wait
docker @dc ps

# 5. Usuarios sintéticos, aplicación, recursos, rol y asignación en el PDP (idempotente).
python -I "$lab\scripts\provision_lab_pdp.py"
```

Comprobación de salud: `http://127.0.0.1:18080/actuator/health` y `http://127.0.0.1:18081/actuator/health`
deben devolver `{"status":"UP"}`; la readiness del PEP (`/actuator/health/readiness`) incluye el PDP.

## Arrancar / detener (sin perder datos)

```powershell
docker @dc stop          # detiene solo el laboratorio; conserva contenedores y volúmenes
docker @dc start
docker @dc up -d --wait  # recrea lo que haya cambiado en compose.lab.yaml; los volúmenes persisten
```

Para retirar los contenedores conservando volúmenes: `docker @dc down` (**sin** `-v`).

## Backend experimental contra el laboratorio

```powershell
.\mvnw.cmd -B -ntp -DskipTests package
& infra\security-baseline-lab\scripts\start-backend-pep-lab.ps1   # 8083, PEP ON solo en ese proceso
```

Matriz HTTP (solo GET y decisiones, sin escrituras):

```powershell
python -I infra\security-baseline-lab\scripts\lab_matrix.py pep
python -I infra\security-baseline-lab\scripts\lab_matrix.py backend
```

Caso PEP caído: `docker stop asistencias-security-lab-pep-1`, luego
`python -I ...\lab_matrix.py backend-pep-down`, y `docker start asistencias-security-lab-pep-1`.

## Rollback

1. Backend: detener el proceso de 8083 (o no arrancarlo). El flag `SECURITY_BASELINE_PEP_ENABLED` es
   `false` por defecto en `application.yml`; el backend estable no cambia.
2. Laboratorio: `docker @dc stop` o `docker @dc down` (sin `-v`).
3. Borrar datos del laboratorio solo con decisión explícita del usuario; los volúmenes son
   `asistencias-security-lab-keycloak-data`, `asistencias-security-lab-surrealdb-data` y
   `asistencias-security-lab-redis-data`, y la red `asistencias-security-lab-net` (172.31.240.0/24).
4. Secretos locales: `%USERPROFILE%\.asistencias-security-lab\lab.env`.

## Administración PDP (manual, si se prefiere no usar el script)

El PDP v0.1.6 en perfil `keycloak` administra solo con sesión BFF (cookie `SECURITY_BASELINE_SESSION`
+ cabecera `X-XSRF-TOKEN`), no con Bearer. El laboratorio no incluye SPA de administración; el
script automatiza exactamente estas llamadas:

| Paso | Llamada | Notas |
|---|---|---|
| Login | `GET http://localhost:18080/oauth2/authorization/keycloak` → login Keycloak → callback | Aprovisiona al usuario en el PDP (requiere email verificado) |
| CSRF | `GET /api/v1/session` | Emite la cookie `XSRF-TOKEN` |
| Aplicación | `POST /api/v1/applications {name, description, baseUrl}` | Devuelve la credencial **una sola vez**; el creador queda como administrador |
| Recurso | `POST /api/v1/applications/{id}/resources {path, method}` | Rutas exactas; en v0.1.6 las plantillas `{x}` devuelven 500 |
| Rol | `POST /api/v1/roles {name, scope: APPLICATION, applicationId}` | |
| Concesión | `POST /api/v1/roles/{roleId}/resources {roleId, resourceId}` | Idempotente |
| Asignación | `POST /api/v1/roles/{roleId}/assignments {roleId, userId, applicationId}` | `userId` es el id interno del PDP (`GET /api/v1/users`), no el `sub` |
| Rotar credencial | `POST /api/v1/applications/{id}/credential-rotations` | Guardar el nuevo valor en `lab.env` |

## Identidad compartida y E2E académico (SEC-BASELINE-002)

```powershell
& infra\security-baseline-lab\scripts\verify-academic-identity.ps1 -Mode Verify   # solo SELECT
python -I infra\security-baseline-lab\scripts\provision_shared_identity_e2e.py
& infra\security-baseline-lab\scripts\start-backend-pep-lab.ps1 -IdentityMode SharedLab
python -I infra\security-baseline-lab\scripts\lab_matrix.py e2e
```

Casos de PEP caído y de respuesta anómala (stub `pep_fault_stub.py`): ver
[MANUAL_TEST_GUIDE](../SEC-BASELINE-002-shared-identity-e2e/MANUAL_TEST_GUIDE.md). `lab_matrix.py` devuelve 0 (todo
PASS), 1 (fallo propio) o 2 (solo `KNOWN_UPSTREAM_FAIL`, p. ej. PEP-03).

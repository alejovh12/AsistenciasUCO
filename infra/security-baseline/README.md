# Security Baseline 0.1.7 — instalación local única

Estado: 2026-10-26. Esta es **la única** instalación local de Security Baseline (PDP, PEP, OPA, Keycloak central,
Redis, SurrealDB). El Keycloak legacy de AsistenciasUCO (`infra/keycloak`, puerto 8081) es otro proyecto y se conserva.

| Servicio | Imagen | Puerto host |
|---|---|---|
| Keycloak central (realm `security-baseline`) | `davidalzate/security-keycloak-local:0.1.7` | `127.0.0.1:19090` (issuer `http://keycloak.localhost:19090/realms/security-baseline`) |
| PDP | `davidalzate/security-pdp:0.1.7` | `127.0.0.1:18080` |
| PEP | `davidalzate/security-pep:0.1.7` | `127.0.0.1:18081` |
| OPA / Redis / SurrealDB | `security-opa:0.1.7` / `redis:7.4-alpine` / `surrealdb:v3.3.0` | solo red interna |

Proyecto Compose: **`asistencias-security-lab`** (se conserva a propósito: sus volúmenes `asistencias-security-lab_*-data`
contienen el realm, el catálogo del PDP y las asignaciones; renombrarlo los dejaría huérfanos).

## Levantar / actualizar

```powershell
docker compose --env-file "$env:USERPROFILE\.asistencias-security-lab\lab.env" -f infra\security-baseline\compose.yaml up -d
```

`lab.env` (fuera de Git, ACL solo del usuario) define `SECURITY_VERSION=0.1.7` y todos los secretos; plantilla en
`lab.env.example`. **Prohibido:** `docker compose down -v`, `docker volume prune`, `docker system prune -a`.
Antes de actualizar versión respalde los volúmenes (`tar` desde un contenedor efímero con el volumen en solo lectura).

## Catálogo de acceso: una definición, tres verificaciones

`access-catalog.json` es la **única** definición de qué roles llaman a cada endpoint (método, ruta con `{variables}`,
roles, `access`, ownership contextual), de los perfiles del PDP y de las exenciones. Nada más la replica a mano.

| Qué comprueba | Dónde |
|---|---|
| Endpoints que existen (Spring MVC real, `RequestMappingHandlerMapping`) = catálogo; sin fila ⇒ falla | `AccessCatalogConsistencyTest` (Maven) |
| Roles de cada endpoint = `SecurityFilterChain` real, por rol, sin Bearer y sin rol institucional | `AccessCatalogConsistencyTest` (Maven) |
| Descubrimiento sin JVM (`extract_endpoints.py`) y validación estricta del catálogo | `python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"` |
| El código = catálogo, y esqueleto de lo que falte | `provision_asistencias.py catalog` |
| PDP = catálogo (recursos, concesiones, perfiles) | `provision_asistencias.py verify` |
| Decisión real PEP→PDP ALLOW/DENY por endpoint y rol, rutas no registradas y sin Bearer | `provision_asistencias.py verify-decisions` |

**Una HU nueva**: (1) controlador + regla en `SecurityConfig`; (2) `catalog` o la prueba Maven fallan hasta que exista su
fila; (3) completar `roles`/`access`/`ownership`; (4) `pdp` registra solo lo que falta; (5) `verify` + `verify-decisions`;
(6) documentar en el work item. Un endpoint sin regla bloquea la validación: nunca recibe acceso por defecto, no se
registra `/**` y no se asigna a todos los roles por comodidad (`access=AUTHENTICATED` exige los cinco roles a propósito).

## Aprovisionar AsistenciasUCO (idempotente, incremental)

```powershell
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py catalog      # offline: código vs catálogo
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py keycloak     # clientes del realm
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py pdp          # app, recursos, roles, concesiones, perfiles
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py users        # usuarios de prueba por rol + perfil
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py verify       # solo lectura
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py verify-decisions
```

- Realm: cliente de recurso `asistencias-api` (cinco roles), cliente público `asistencias-uco-spa` (PKCE S256), cliente
  de servicio `asistencias-provisioner`.
- PDP: aplicación `asistencias-uco` (ambientes `dev|lab|prod`), un recurso por endpoint del catálogo, los cinco roles, sus
  concesiones y cinco perfiles. `pdp` **solo agrega** lo que falta; nunca borra recursos ni roles. Las concesiones que el
  catálogo no autoriza (deriva de matrices antiguas) se listan en `verify` y solo se revocan con `pdp --prune`, decisión
  explícita del responsable. Los recursos huérfanos (sin endpoint) quedan sin concesiones, o sea denegados.
- Roles vs. perfiles: el **rol** (`ADMINISTRADOR`, `DECANO`, `COORDINADOR`, `DOCENTE`, `ESTUDIANTE`) concede recursos; el
  **perfil** agrupa roles y es lo que se asigna a una persona (`Administración institucional`, `Decanatura`,
  `Coordinación de programa`, `Docencia`, `Estudiantes`; cada uno contiene su rol, y un perfil compuesto futuro, p. ej.
  coordinador que también dicta clase, se agrega al catálogo con ambos roles). El PDP rechaza (409) asignar un perfil a quien
  ya tiene directamente uno de sus roles: `users --migrate-direct` retira primero la directa y reasigna el perfil, con
  reversión si falla; sin esa bandera no toca asignaciones existentes. `GET /api/v1/me/applications` devuelve
  `applicationRoles` (directos) y `profiles`; la SPA no lo usa para autorizar (su identidad y roles salen de
  `GET /api/v1/auth/me`) y el servidor sigue siendo la autoridad.
- La credencial de la aplicación y el secreto del provisioner se guardan en `lab.env`; nunca se imprimen.
- Starter oficial `security-pep-integration-spring-boot-starter` 0.1.3: **no se incorpora** (WebFlux/Boot 4.1; este backend es
  MVC/Boot 4.0.8). Justificación y paridad de contrato en
  [la arquitectura de seguridad](../../docs/security/runtime-security-provider-architecture.md#56-starter-oficial-security-pep-integration-spring-boot-starter-013-no-se-incorpora).

## Selector en IntelliJ (un solo valor gobierna seguridad e identidad)

| Modo | Variables |
|---|---|
| `legacy` (por defecto) | `APP_ADAPTERS_SECURITY_PROVIDER=legacy` + las `KEYCLOAK_*` actuales (8081) |
| `baseline` | `APP_ADAPTERS_SECURITY_PROVIDER=baseline`, `SECURITY_BASELINE_PEP_URL=http://localhost:18081`, `SECURITY_BASELINE_PEP_APPLICATION_NAME=asistencias-uco`, `SECURITY_BASELINE_PEP_ENVIRONMENT=dev`, `SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL=<lab.env: SECURITY_BASELINE_PEP_APPLICATION_CREDENTIAL_ASISTENCIAS>`, `SECURITY_BASELINE_IDENTITY_ADMIN_CLIENT_SECRET=<lab.env>` |

Un valor desconocido, o `APP_ADAPTERS_IDENTITY_PROVIDER` distinto del de seguridad, impide arrancar. En modo baseline la DB
debe tener la migración SEC-BASELINE-017 (el arranque lo comprueba). `scripts/start-backend-baseline.ps1 -Provider baseline|legacy`
arranca el jar con esas variables sin imprimirlas.

Frontend (`SECURITY_PROVIDER=legacy|baseline`, `SECURITY_BASELINE_URL`, `SECURITY_BASELINE_ISSUER_URL`,
`SECURITY_BASELINE_CLIENT_ID`): un valor desconocido deja la SPA sin autenticación y el contenedor no arranca.

## Verificación E2E (sin UI)

```powershell
python -X utf8 -I infra\security-baseline\scripts\e2e_baseline.py --pep-down
python -X utf8 -I infra\security-baseline\scripts\e2e_identity_binding.py --sql-container <db> --sql-password-file <archivo> [--database <nombre>]
python -X utf8 -I infra\security-baseline\scripts\e2e_provisioning.py     --sql-container <db> --sql-password-file <archivo> [--database <nombre>]
python -X utf8 -I infra\security-baseline\scripts\e2e_profile_assignment.py --sql-container <db> --sql-password-file <archivo> [--database <nombre>]
```

Los tres últimos crean/alteran usuarios y vínculos, así que **solo** corren contra una base desechable identificada
explícitamente (`scripts/e2e_safety.py`); cualquier otro destino se rechaza con código 2 antes de tocar nada. Las tres
señales deben coincidir: (1) el contenedor SQL lleva la etiqueta `asistencias.e2e.disposable=true`
(`docker run --label ...`); (2) la base lleva la propiedad extendida `uco.e2e.disposable='true'`, que se fija una sola vez con
`python -X utf8 -I infra\security-baseline\scripts\e2e_safety.py mark-disposable --sql-container <db> --sql-password-file <archivo> --database <nombre>`
(también exige la etiqueta); (3) el backend en `--backend` (por defecto `http://127.0.0.1:8080`) tiene sesiones JDBC abiertas contra esa base.
`sql_server_asistencias`, el puerto 1433 y la base `gestionasistenciadb` están bloqueados aunque se etiqueten. La base
desechable por defecto se llama `gestionasistenciadb_e2e` y se obtiene restaurando un `BACKUP ... COPY_ONLY` de la de desarrollo
en otro contenedor (loopback, login de runtime sin `sa`). Los E2E capturan el estado inicial (usuarios, vínculos, conteo de
filas por tabla, cuenta de Keycloak), lo restauran sin valores fijos, eliminan la identidad sintética con sus vínculos y
verifican que no queden residuos, incluso si abortan (`e2e_profile_assignment.py` usa una identidad sintética nueva y compara las asignaciones VIGENTES del PDP: el PDP no borra usuarios ni historial, solo cierra `validUntil`); el crecimiento de `dbo.AuditoriaEvento` (append-only) se informa como `INFO`.
La contraseña de `sa` se lee de un archivo fuera de Git y viaja por variable de entorno, no por la línea de comandos.
Pruebas offline de estas guardas: `python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"`.

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

## Aprovisionar AsistenciasUCO (idempotente)

```powershell
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py keycloak   # clientes del realm
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py pdp        # app, recursos, roles
python -X utf8 -I infra\security-baseline\scripts\provision_asistencias.py users       # usuarios de prueba por rol
```

- Realm: cliente de recurso `asistencias-api` (cinco roles), cliente público `asistencias-uco-spa` (PKCE S256), cliente
  de servicio `asistencias-provisioner`.
- PDP: aplicación `asistencias-uco` (ambientes `dev|lab|prod`), un recurso por endpoint de los controladores
  (`extract_endpoints.py`), los cinco roles y sus concesiones con la misma matriz que `SecurityConfig`.
- La credencial de la aplicación y el secreto del provisioner se guardan en `lab.env`; nunca se imprimen.

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
python -X utf8 -I infra\security-baseline\scripts\e2e_identity_binding.py --sql-container <db> --sql-password-file <archivo>
python -X utf8 -I infra\security-baseline\scripts\e2e_provisioning.py     --sql-container <db> --sql-password-file <archivo>
```

Los dos últimos modifican la base indicada: úselos solo con una base desechable.

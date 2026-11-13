---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# VAL-oleada2-cumplimiento: cadena de suministro y CI del backend (agente T)

Rama `fix/VAL-oleada2-cumplimiento`. Ítems de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`: TRI-01, TRI-02, TRI-06, TRI-07, SEC-05, SEC-08, EST-09, PRU-04 y la espera del quality gate de Sonar (EST-01).

## Plan y cambios

| Ítem | Cambio | Archivo |
| :--- | :--- | :--- |
| TRI-01 | Trivy de repositorio en dos pasos: inventario SARIF (`exit-code 0`, a Code Scanning) y compuerta `exit-code 1` con `severity HIGH,CRITICAL` e `ignore-unfixed: true`. Escáneres `vuln,misconfig,secret` | `.github/workflows/security-deep-scan.yml` |
| TRI-02 | Lo mismo para la imagen construida (`vuln,secret,misconfig`) | `.github/workflows/security-deep-scan.yml` |
| TRI-06 | `.trivyignore` con formato, motivo y vencimiento (`exp:`); triage abajo | `.trivyignore`, `docs/testing/TRIVY_ARCHUNIT_RUNBOOK.md` |
| SEC-08 | Escáner `secret` agregado en los dos Trivy; gitleaks bloquea en PR | `security-deep-scan.yml`, `security.yml` |
| SEC-05 | Job `Gitleaks Secret Scan`: binario oficial 8.30.1 con SHA-256 fijado, `.github/gitleaks.toml` (reglas por defecto + falsos positivos conocidos), solo commits del PR/push | `.github/workflows/security.yml`, `.github/gitleaks.toml` |
| EST-09 | Dependabot semanal: maven (con registro de GitHub Packages), docker, docker-compose y github-actions, hacia `develop` | `.github/dependabot.yml` |
| PRU-04 | `integration.yml` corre en cada PR a `develop` con SQL Server efímero y esquema del repo de BD (diseño en `.github/CI.md`) | `.github/workflows/integration.yml`, `.github/CI.md` |
| Fijado | Todas las acciones fijadas por SHA de commit con comentario del tag (checkout v7.0.1, setup-java v6.2.0, upload-artifact v7.0.2, codeql-action v4.39.3, dependency-review v5.0.0, trivy-action v0.36.0) | los 4 workflows |
| Sonar | `backend-ci.yml` ya esperaba el gate (`sonar.qualitygate.wait=true`, timeout 300 s): sin cambio funcional | `.github/workflows/backend-ci.yml` |

### Diseño de PRU-04

1. SQL Server efímero `mcr.microsoft.com/mssql/server:2022-CU20-GDR1-ubuntu-22.04` con `docker run` en el job (no `services:`: el contenedor de servicio arranca antes de los pasos y no puede recibir una contraseña generada en tiempo de ejecución; así el job no necesita ningún secreto de BD).
2. Contraseñas de `sa` y MinIO generadas con `openssl rand`, enmascaradas y descartadas al final.
3. Checkout del repo público `johnjduque/gestion-asistencia-db` (rama `develop`, o `db_ref` en ejecución manual) y su `deploy_schema.ps1`, el mismo script del CI de BD (tablas, triggers, funciones, vistas, SP, seguridad, semillas).
4. MinIO y ClamAV de `infra/files/compose.yaml` para los IT de soportes, que fallan a propósito sin ese ambiente.
5. `./mvnw verify -Pintegration` con `SPRING_DATASOURCE_*` y `APP_DATABASE_EXPECTED_NAME=gestionasistenciadb`.

### Historial y gitleaks

El historial de git ya contiene valores viejos (contraseñas de laboratorio de SEC-04) que quedaron invalidados con la rotación de credenciales. No se reescribe el historial: gitleaks escanea solo `base..head` del PR o `before..after` del push, de modo que bloquea cualquier secreto nuevo sin fallar por los valores ya rotados.

## Validación

| Prueba | Resultado |
| :--- | :--- |
| `rhysd/actionlint:1.7.12` sobre `.github/workflows/*.yml` (incluye shellcheck) | 0 hallazgos |
| Parseo YAML (js-yaml) de los 4 workflows y `dependabot.yml` | OK |
| Trivy local `aquasec/trivy:0.74.0 fs --scanners vuln,secret,misconfig --severity HIGH,CRITICAL --ignore-unfixed` sobre el árbol rastreado (sin `target/`, `.env`), con `~/.m2/repository` local en solo lectura y `--offline-scan` (Maven Central devolvió 429) | 3 HIGH, 0 secretos, 0 misconfig (ver triage) |
| gitleaks 8.30.1 `dir` sobre el árbol rastreado, sin configuración | 15 hallazgos, todos falsos positivos (digests SHA-256 en docs, literales de prueba, una fila de la DoD) |
| gitleaks 8.30.1 `dir` con `.github/gitleaks.toml` | 0 hallazgos |
| gitleaks con configuración en modo `git`, rango `HEAD~5..HEAD` (como en CI) | rc 0 |
| gitleaks con configuración sobre el historial completo (926 commits), solo como evidencia de SEC-05 | 13 hallazgos `generic-api-key` en versiones viejas de `role-matrix-provisioning.json` (8), `provisioning.json` (3), `backend-ci.yml` (1) y la DoD (1): valores de laboratorio ya rotados; no se reescribe el historial |
| Workflows ejecutados en GitHub | No ejecutados (fuera de alcance); la primera corrida del PR es la evidencia |

### Triage Trivy (TRI-06 / TRI-07), 2026-11-12

| Hallazgo | Paquete | Instalada | Corregida | Origen | Decisión |
| :--- | :--- | :--- | :--- | :--- | :--- |
| CVE-2026-102554 (HIGH) | com.google.guava:guava | 33.6.0-jre | 33.7.2 | transitiva de io.minio:minio 9.0.3 | Real. Subir en `pom.xml` (fuera del alcance de CI). La compuerta bloquea hasta corregir |
| CVE-2026-108106 (HIGH) | org.xerial.snappy:snappy-java | 1.1.10.8 | 1.1.10.9 | transitiva de io.minio:minio 9.0.3 | Igual |
| CVE-2026-93451 (HIGH) | org.xerial.snappy:snappy-java | 1.1.10.8 | 1.1.10.9 | transitiva de io.minio:minio 9.0.3 | Igual |

Conteo real HIGH/CRITICAL con corrección disponible: 3 HIGH, 0 CRITICAL (repositorio). La imagen se mide en el job `trivy-image` (no se escaneó una imagen local).

## Pendiente externo

- Subir guava y snappy-java (dependencyManagement o una versión de minio que las traiga); hasta entonces `Trivy repository gate` falla.
- Dependabot: crear el secreto de Dependabot `MESSAGEUCOLAB_PACKAGES_TOKEN` (solo lectura de paquetes) para resolver el starter de GitHub Packages.
- Branch protection: marcar `Gitleaks Secret Scan`, los dos gates de Trivy y `Backend Integration Gate` como required checks (requiere admin del repo).
- Push protection de GitHub (SEC-08) se activa en Settings > Code security (requiere admin).

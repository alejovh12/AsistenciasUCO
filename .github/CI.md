---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# AsistenciasUCO Backend CI Baseline

Este documento define el gate mínimo que debe conservar el backend antes de incorporar capacidades tecnológicas adicionales.

## Objetivo

La integración continua protege la línea base de Clean Architecture y evita que cambios posteriores en reactividad, cache, mensajería, storage, observabilidad o seguridad degraden contratos ya aprobados.

## Workflows

### `backend-ci.yml` — obligatorio

Se ejecuta en `pull_request` y `push` hacia `develop` y `master`, además de ejecución manual.

Gate `Backend Quality Gate`:

1. checkout del repositorio;
2. Java/Javac 25 con Temurin;
3. Maven Wrapper;
4. `mvn clean verify`;
5. unit tests y architecture tests (incluidos por Maven/JUnit);
6. empaquetado del JAR;
7. comprobación explícita del JAR;
8. build de la imagen Docker;
9. publicación de reportes Surefire;
10. publicación del JAR como artifact.

Este job debe ser **required status check** para merge.

### `security.yml` — seguridad

#### `Dependency Review`

Solo en pull requests. Requiere que GitHub Dependency Graph esté habilitado en el repositorio. Bloquea nuevas dependencias con severidad `high` o superior.

#### `CodeQL Java Analysis`

Analiza Java/Kotlin con CodeQL. Debe conservarse activo para PR y push hacia las ramas protegidas.

### `integration.yml` — integración con SQL Server (PRU-04)

Corre en cada `pull_request` a `develop` (y manual con la entrada `db_ref`). Diseño:

1. SQL Server efímero (`mcr.microsoft.com/mssql/server:2022-CU20-GDR1-ubuntu-22.04`) levantado con `docker run` dentro del job, en `127.0.0.1:1433`. No se usa `services:` porque el contenedor de servicio se crea antes de los pasos y no puede recibir una contraseña generada en la ejecución; así no hace falta ningún secreto de BD.
2. Contraseñas de `sa` y de MinIO generadas con `openssl rand` en cada ejecución y enmascaradas (`::add-mask::`); mueren con el job.
3. El esquema se despliega haciendo checkout del repo público de BD (`johnjduque/gestion-asistencia-db`, rama `develop` por defecto) y corriendo su `deploy_schema.ps1`, el mismo que usa el CI de BD (tablas, triggers, funciones, vistas, SP, seguridad y semillas).
4. MinIO y ClamAV para los IT de soportes salen de `infra/files/compose.yaml` (`up --wait` + `minio-init`).
5. `./mvnw verify -Pintegration` con `SPRING_DATASOURCE_*` apuntando al contenedor y `APP_DATABASE_EXPECTED_NAME=gestionasistenciadb`.
6. Reportes Surefire y Failsafe como artifacts; limpieza de contenedores al final.

Cuando un PR del backend dependa de un cambio de esquema aún no unido en `develop` de BD, correr el workflow manual con `db_ref` apuntando a la rama de BD. Volverlo required check cuando tenga una racha de ejecuciones verdes.

### `security.yml` — gitleaks (SEC-05/SEC-08)

Job `Gitleaks Secret Scan`: binario oficial `gitleaks 8.30.1` con SHA-256 fijado, configuración `.github/gitleaks.toml` (reglas por defecto + falsos positivos conocidos). Escanea solo los commits del PR (`base..head`) o del push (`before..after`); el historial anterior tiene valores invalidados por la rotación de credenciales y no se reescribe.

## Sonar y evidencia de ejecución

`backend-ci.yml` ejecuta análisis Sonar y espera Quality Gate solo en PR/push develop. Los umbrales remotos y Rulesets no están versionados en este checkout; no declarar PASS o protección efectiva sin evidencia de GitHub/Sonar. Gates locales exactos en [TESTING_STANDARD](../docs/testing/TESTING_STANDARD.md).

## Branch governance

Configurar GitHub Rulesets para `develop` y `master`:

- Require a pull request before merging.
- Require status checks to pass.
- Required: `Backend Quality Gate`.
- Required: `CodeQL Java Analysis` cuando CodeQL esté operativo.
- Required: `Dependency Review` cuando Dependency Graph esté habilitado.
- Block force pushes.
- Block branch deletion.
- No bypass salvo administración explícita para recuperación.

- Required (recomendado tras una racha verde): `Backend Integration Gate` y `Gitleaks Secret Scan`.

## Principios que CI debe preservar

- Domain no depende de Spring ni Infrastructure.
- Application no depende de Infrastructure ni de APIs tecnológicas concretas.
- Las tecnologías externas se integran mediante puertos/capacidades neutrales y adapters.
- Los architecture tests no se deshabilitan para hacer pasar CI.
- No se añaden `@Disabled` para ocultar regresiones.
- El build debe usar Java 25.
- El artefacto Docker debe construirse a partir del JAR generado por el mismo gate.
- Los secretos nunca se versionan.

## Evolución prevista (opciones sujetas al roadmap activo)

La secuencia y selección futura se rigen por [LINEA_BASE](../docs/baseline/LINEA_BASE.md); la lista siguiente no autoriza incorporar tecnologías ni implica que ya existan.


Cuando se agreguen nuevas capacidades, CI debe crecer con ellas:

- Reactor/realtime: architecture + unit/contract tests reactivos.
- Redis: tests del adapter/cache y degradación controlada.
- RabbitMQ: contrato de eventos, serialization y tracing propagation.
- MinIO: contract tests de storage.
- OpenAPI: validación contract-first.
- Observability: smoke tests de health/metrics y convenciones.
- CD: imágenes inmutables, SBOM, container scanning y promoción por ambientes.

## Incidente histórico PR #15 (2026-10-08)
Backend Quality Gate: Maven clean verify y XML JaCoCo PASS; SonarCloud FAIL por 48.4% new coverage (<80%), Security C y Reliability C (requiere A/A); Docker/JAR downstream omitidos. CodeQL PASS separado. No es una medición del commit siguiente. Para recuperar: [QUALITY-PR15](../docs/work-items/QUALITY-PR15-recovery/PLAN.md). No confundir global JaCoCo y métricas Sonar; no bajar umbrales ni convertir skips en PASS.

## SEC-001 — Trivy + evidencia ArchUnit dirigida (postmerge PR #15)

Workflow adicional [security-deep-scan.yml](workflows/security-deep-scan.yml) ejecuta ArchUnit dirigido y Trivy filesystem/imagen con versión/commit verificados. Desde VAL oleada 2 bloquea HIGH/CRITICAL con corrección disponible (inventario SARIF aparte); excepciones justificadas en `.trivyignore`. Todas las acciones están fijadas por SHA y Dependabot (`.github/dependabot.yml`) propone sus actualizaciones semanalmente. Ver [runbook](../docs/testing/TRIVY_ARCHUNIT_RUNBOOK.md) y [work item](../docs/work-items/SEC-001-trivy-archunit/PLAN.md). No reemplaza CodeQL, Sonar o SQL integration.

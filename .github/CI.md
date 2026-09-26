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

### `integration.yml` — integración con SQL Server

Workflow manual mientras la DB se mantenga en un repositorio independiente y no exista una estrategia versionada de provisioning cross-repo.

Requiere:

#### Secrets

- `CI_SPRING_DATASOURCE_URL`
- `CI_SPRING_DATASOURCE_USERNAME`
- `CI_SPRING_DATASOURCE_PASSWORD`

#### Repository variable

- `CI_APP_DATABASE_EXPECTED_NAME`

Ejecuta:

```bash
./mvnw clean verify -Pintegration
```

y conserva Surefire y Failsafe como artifacts.

No debe convertirse en required check hasta contar con una DB CI reproducible y versionada. No debe apuntar a una DB personal ni de producción.

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

`Backend Integration Gate` se mantendrá manual hasta disponer de infraestructura CI reproducible.

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

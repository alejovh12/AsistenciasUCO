---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---
# Runbook post-Sonar — Trivy + ArchUnit

## Objetivo
Reproducir indicación profesor Farid después de Sonar: detectar CVE en dependencias/imágenes/configuración con Trivy y dependencias de capas con ArchUnit; ambos complementan SonarCloud y CodeQL.

## Fuente oficial
- Trivy Action: https://github.com/aquasecurity/trivy-action
- Seguridad 2026: https://github.com/aquasecurity/trivy/security/advisories/GHSA-69fq-xp46-6x23
- Maven Surefire test selection: https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html
- CI: [deep scan workflow](../../.github/workflows/security-deep-scan.yml)
- Plan y estado: [SEC-001](../work-items/SEC-001-trivy-archunit/PLAN.md)

## Ejecución por GitHub Actions
Abrir PR branch `jose-valencia/sec-001-trivy-archunit` contra `develop` y consultar job `Backend Deep Security Scan`. Produce:
1. `ArchUnit Architectural Guardrails`: reporte XML de las clases de `architecture`, exit failure si regla rota.
2. `Trivy Repository Vulnerability and Misconfiguration Scan`: SARIF con findings y artifact.
3. `Trivy Production Docker Image Scan`: construye imagen fresca desde Dockerfile y analiza OS/libs.

Trivy se ejecuta por primera vez en `report-only` (`exit-code:0` para findings): es obligatorio leer SARIF y clasificar HIGH/CRITICAL, no llamar a estos jobs `CVE-free`. Mantener `security-events: write` solo para subir SARIF.

## Ejecución local (solo JDK25, Docker y Trivy previamente verificados)
```powershell
git checkout develop
git pull --ff-only
java -version
.\mvnw.cmd -B -ntp "-Dtest=co/edu/uco/asistenciasuco/architecture/*Test" test
docker build -t asistencias-uco-backend:local .
trivy --version
trivy fs --scanners vuln,misconfig --severity HIGH,CRITICAL --format table .
trivy image --scanners vuln --severity HIGH,CRITICAL --format table asistencias-uco-backend:local
```
No introducir tags de acción mutables: Trivy sufrió alteración de tags en marzo 2026. Utilizar versiones aprobadas y commit SHA fijado en workflow; no descargar binarios sin verificar fuentes. No guardar tokens, DB dumps ni reportes de secret scanner sin sanitizar.

## Cómo leer resultados
- CVE HIGH/CRITICAL real: `VulnerabilityID`, paquete, versión instalada, fixedVersion, severidad fuente, ruta, reachability/uso y dueño.
- Si no existe versión corregida: documentar workaround/mitigación y reevaluación; no ignorar automáticamente ni false positive.
- Base image: registrar digest y distinguir hallazgos OS de libs Java.
- No considerar 0 findings cuando falló actualización de bases o se escaneó una imagen vieja.
- Arquitectura: cada falla ArchUnit debe asociarse a clase origen, dependencia destino y regla infringida; no desactivar reglas para obtener PASS.
- Guardar `run ID` + SHA, métricas de ArchUnit, SARIF y fecha de Trivy DB. Solicitar aprobación de política fail-on HIGH/CRITICAL en work item posterior.

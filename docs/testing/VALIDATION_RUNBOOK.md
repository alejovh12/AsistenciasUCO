---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Runbook de validación

## Preparación

Desde la raíz: revisar `git status --short`, PLAN/DoR y [gates](TESTING_STANDARD.md). Verificar `java -version`, `javac -version` y versión de Maven/wrapper. Java debe ser 25. No cargar `.env` para unit/ArchUnit ni imprimir secretos.

Windows con JDK 25 instalado (adaptar su ruta comprobada):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd verify
```

Unix: `./mvnw verify`. Maven instalado: `mvn verify`. En CI se usa `./mvnw -B -ntp --no-transfer-progress clean verify`. Test puntual puede ayudar al diagnóstico, pero no sustituye verify.

## Evidencia

- Guardar comando exacto, fecha/base, entorno, exit code, tests/failures/errors/skipped y causa del fallo en VALIDATION.
- Surefire: `target/surefire-reports`; cobertura: `target/site/jacoco/jacoco.xml` e `index.html`. Reportar contadores LINE/BRANCH y proporción; archivo ausente o antiguo no es evidencia de la corrida.
- Si verify falla antes de JaCoCo, cobertura/gate quedan NO EJECUTADOS; no usar un reporte anterior para declararlos PASS.
- Scanners/CI remoto: enlace a corrida/artefacto y contexto. Local verify no ejecuta Sonar, CodeQL, Dependency Review ni Docker automáticamente.

## Integración DB cuando aplica

Solo en ambiente de prueba controlado, nunca DB personal ajena/producción. [Conexión SQL Server](../integration/sqlserver-connection.md) describe scripts y variables; comprobar fixtures y cleanup antes de ejecutar porque algunos IT escriben y eliminan sus datos de prueba.

Windows: `.\scripts\test-local.ps1` (carga variables sin imprimirlas y ejecuta perfil). Equivalente con entorno listo: `.\mvnw.cmd -Pintegration verify`; Unix `./mvnw -Pintegration verify`.

Failsafe: `target/failsafe-reports`. Verificar tests realmente ejecutados y assumptions. Asistencia requiere grupo con ≥3 matriculados, docente titular y otro docente activo; un skip por fixture ausente es evidencia insuficiente. No alterar DB ni fixtures institucionales para esta reorganización documental.

## Reorganización documental

Comprobar rutas/enlaces, AGENTS único fuera de estado local, CLAUDE breve, skills/rutas existentes, pack retirado tras fusión, archivo histórico marcado, un ledger/roadmap/DoD, ignores locales y ausencia de worktrees versionados. Revisar `git diff --check`, `git diff --stat`, `git status --short` y hashes/diff de archivos funcionales. Git diff normal no incluye nuevos archivos: inspeccionarlos también.

## Fallos

Problemas de JDK/red/dependencias/permisos/DB se registran como `VALIDATION_BLOCKED_BY_ENVIRONMENT`, con causa exacta y comando. Respetar permisos del entorno. Un test que falla por una regla de arquitectura/contrato es FAIL. No cambiar producción, tests ni gates para hacer pasar un fallo ajeno a la tarea. Comparar con baseline previo cuando esté disponible.

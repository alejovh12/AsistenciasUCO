# AsistenciasUCO Backend

Este repositorio contiene el backend Spring Boot de AsistenciasUCO.

## Configuracion Local

`.env` contiene secretos locales y nunca debe versionarse ni incluirse en artefactos de entrega.

`.env.example` contiene unicamente la forma esperada de configuracion, con placeholders seguros para levantar o revisar el proyecto localmente.

### Arranque local en Windows

Tres repositorios oficiales (no crear clones, worktrees ni carpetas de laboratorio):

| Proyecto | Ruta oficial |
|---|---|
| Backend | `%USERPROFILE%\AsistenciasUCO\AsistenciasUCO` |
| Frontend | `%USERPROFILE%\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend` |
| DB (SQL Server) | `%USERPROFILE%\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` |

Orden: SQL Server (1433) → seguridad (Security Baseline 19090/18080/18081 por defecto) → MinIO/ClamAV (9000/3310, ver [infra/files](infra/files/README.md)) → backend (8080) → frontend (4200). Detalle operativo en [infra/README.md](infra/README.md).

- Stack completo: `.\scripts\start-local-stack.ps1` (`-Check` diagnostica rutas y puertos sin iniciar nada; `-Stop` detiene los procesos que inicio; `-Provider baseline|legacy`, `baseline` por defecto).
  Las carpetas del frontend y de la DB son las rutas oficiales; solo se pueden sustituir con `-FrontendRoot`/`-DbRoot` o `ASISTENCIAS_FRONTEND_ROOT`/`ASISTENCIAS_DB_ROOT`, y el script rechaza rutas que parezcan clones o laboratorios o que no sean el repositorio esperado.
  SQL Server se inicia con `docker start sql_server_asistencias` (nunca se recrea desde este repositorio).
- Solo backend con Security Baseline: `.\mvnw.cmd -B -DskipTests package` y `.\infra\security-baseline\scripts\start-backend-baseline.ps1` (`-Provider legacy` es la alternativa explicita).
- Solo backend sin jar: `.\scripts\run-local.ps1` (logs en `logs\backend.out.log`; el proveedor lo fija `APP_ADAPTERS_SECURITY_PROVIDER`, `baseline` por defecto).

## Gobernanza del backend

Para trabajar en una tarea, comienza por [AGENTS.md](AGENTS.md). El [mapa de documentación](docs/README.md) identifica arquitectura, contratos, fases y evidencia. Los [work items](docs/work-items/README.md) conservan planes, pruebas, validación y cierre; el [informe LB-000](docs/work-items/LB-000-gobernanza-documentacion/REPORT.md) registra esta consolidación.

# AsistenciasUCO Backend

Este repositorio contiene el backend Spring Boot de AsistenciasUCO.

## Configuracion Local

`.env` contiene secretos locales y nunca debe versionarse ni incluirse en artefactos de entrega.

`.env.example` contiene unicamente la forma esperada de configuracion, con placeholders seguros para levantar o revisar el proyecto localmente.

### Arranque local en Windows

Orden: SQL Server (1433) → Keycloak (8081) → MinIO/ClamAV (9000/3310, ver [infra/files](infra/files/README.md)) → backend (8080) → frontend (4200).

- Solo backend: `.\scripts\run-local.ps1` (logs en `logs\backend.out.log`).
- Stack completo: `.\scripts\start-local-stack.ps1` (`-Check` diagnostica rutas y puertos sin iniciar nada; `-Stop` detiene los procesos que inicio).
  Las carpetas del frontend y de la DB se resuelven en este orden: parametros `-FrontendRoot`/`-DbRoot`, variables de entorno `ASISTENCIAS_FRONTEND_ROOT`/`ASISTENCIAS_DB_ROOT`, repositorios hermanos `..\Repositorio_GestionAsistenciaFrontend` y `..\Repositorio_GestionAsistenciadb`, y por ultimo la ruta historica del equipo.

## Gobernanza del backend

Para trabajar en una tarea, comienza por [AGENTS.md](AGENTS.md). El [mapa de documentación](docs/README.md) identifica arquitectura, contratos, fases y evidencia. Los [work items](docs/work-items/README.md) conservan planes, pruebas, validación y cierre; el [informe LB-000](docs/work-items/LB-000-gobernanza-documentacion/REPORT.md) registra esta consolidación.

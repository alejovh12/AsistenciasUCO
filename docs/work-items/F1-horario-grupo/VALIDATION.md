---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-01
---

# VALIDATION — F1: horario semanal del grupo (backend)

| Paso | Resultado |
|---|---|
| `mvnw -B verify` (2026-11-01, sobre `develop` `e6e54e7c` con NOTIF-002A e IDENT-001) | **2987 pruebas, 0 fallos y 0 errores**. JaCoCo: "All coverage checks have been met". `BUILD SUCCESS` |
| ArchUnit (`CleanArchitectureRulesTest`, `DomainShouldNotDependOnSpringTest`, `JpaRepositoryArchitectureRulesTest`, ...) | Pasan: los DTO comunes viven en `features.grupo.common.dto`, igual que `coordinador.common` |
| BD | El SP y las vistas que usa (`usp_guardar_horarios_grupo`, `uv_horario.codigoDia`) se validaron en el CI local del repo de BD (rama `feature/F1-horario-grupo`) |

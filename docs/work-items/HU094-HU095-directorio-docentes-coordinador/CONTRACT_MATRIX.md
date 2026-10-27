---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT MATRIX — HU094-HU095-directorio-docentes-coordinador

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

## Evidencia

Las tres ramas se llaman `feature/HU094-HU095-directorio-docentes-coordinador` y están en commits locales, sin subir.

| Sistema | Repo | Archivo/objeto | Autoridad |
|---|---|---|---|
| DB | gestion-asistencia-db | `usp_consultar_historial_grupos_docente_programa`, `usp_consultar_resumen_docentes_programa` | OWNER |
| Backend | AsistenciasUCO | `DocenteProgramaReporteJpaRepository`, `CoordinadorDocentesReporteController`, `access-catalog.json` | CONSUMER / PROVIDER HTTP |
| Frontend | AsistenciasUCO-Frontend | `CoordinatorManagementService.getFichaDocente` / `getResumenDocentes`, `CoordinatorDocenteFichaModalComponent`, `CoordinatorDocentesComponent` | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-094-DB | Historial | READ_QUERY_RESULTSET de 21 columnas (resumen y `totalRegistros`); `VAL_001`, `KPI_001`, `GEN_001` | Repositorio con mapeo posicional; `GEN_001` ⇒ 404 | MATCH | H-01..H-06, HR-01..HR-06, CLH-01/02 | — | — |
| C-094-CAB | Cabecera de la ficha | `usp_consultar_docentes_programa` con `@idDocente` (contrato de HU062) | El caso de uso toma la primera fila; vacío ⇒ 404 | MATCH | FD-01, FD-03 | — | — |
| C-094-HTTP | `GET .../{idDocente}/ficha` | `ApiDataResponse<FichaDocenteDTO>`; 400, 403 o 404 | El modal pagina de 5 y muestra el error en la ficha | MATCH | FC-01..05, FM-01..06 | — | — |
| C-095-DB | Consolidado | READ_QUERY_RESULTSET de 5 columnas por tipo de vinculación | Proyección `ResumenTipoVinculacionProjection` | MATCH | R-01..R-03, RR-01..03 | — | — |
| C-095-HTTP | `GET .../docentes/resumen` | `ApiDataResponse<ResumenDocentesProgramaDTO>` con totales y `total` por tipo | Contadores y chips; filtro con `idTipoVinculacion` en el directorio | MATCH | RD-01..05, RC-01/02, DIR-095-01..04 | — | — |
| C-094-CAT | Autorización | 2 filas nuevas en `access-catalog.json` (COORDINADOR/ADMINISTRADOR) | PEP del baseline | MATCH en código, PENDIENTE en el PDP | `AccessCatalogConsistencyTest` | Rutas denegadas en modo baseline hasta reaprovisionar | Alejo: `provision_asistencias.py` |

## Códigos/enums

- **Códigos:** `VAL_001` 400/403 (ejecutor o docente nulo), `KPI_001` (el caso de uso responde 403 antes), `GEN_001` 404 (solo en `consultarHistorialGruposDocente`).
- **`estadoPeriodo`:** `VIGENTE`, `FINALIZADO` o `PROXIMO`.

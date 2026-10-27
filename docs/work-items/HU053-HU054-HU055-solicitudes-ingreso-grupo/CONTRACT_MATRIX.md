---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT MATRIX — HU053-HU054-HU055-solicitudes-ingreso-grupo

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db `feature/HU053-HU054-HU055-solicitudes-ingreso-grupo` | tabla `SolicitudIngresoGrupo` + 6 SP | commit local (sin push) | OWNER |
| Backend | AsistenciasUCO, misma rama | `SolicitudIngresoGrupoJpaRepository`, `EstudianteSolicitudIngresoController`, `DocenteSolicitudIngresoController`, `access-catalog.json` | commit local (sin push) | CONSUMER / PROVIDER HTTP |
| Frontend | AsistenciasUCO-Frontend, misma rama | `SolicitudIngresoService`, `StudentEnrollmentRequestsComponent`, `TeacherGrupoSolicitudesComponent` | commit local (sin push) | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence |
|---|---|---|---|---|---|
| C-053-REG | Solicitar | canónico; `SIG_001..SIG_006` | `{grupoId}` → `{id}`; 403/404/409 | MATCH | S-01..S-03, SI-01..03, CTE-01/02, SV-01, SE-02/03 |
| C-053-RES | Resolver | canónico + `codigoEstadoResultante`; `SIG_007..SIG_014`; bloqueo de cupo | `{accion, motivo?}` → `estadoResultante` ACEPTADA/EN_ESPERA/RECHAZADA | MATCH | S-04..S-08, RS-01..07, RG-02, CTD-02/03, SV-05, TS-02..05 |
| C-055-RET | Retirar | canónico; `SIG_007`, `SIG_008`, `SIG_015` | POST `.../{id}/retiro` | MATCH | S-10, RW-01..03, CTE-04, SV-03, SE-04 |
| C-053-BAN | Bandeja del docente | READ + `totalRegistros`, `posicionEspera`, cupo | `ApiListResponse<SolicitudIngresoDocenteResponse>` | MATCH | S-09, CD-01..04, RG-04, CTD-01, SV-04, TS-01 |
| C-053-MIS | Mis solicitudes | READ + `totalRegistros` | `ApiListResponse<SolicitudIngresoEstudianteResponse>` | MATCH | S-09, CE-01..03, RG-05, CTE-03, SV-02, SE-01/05 |
| C-053-CAT | Autorización | 4 filas nuevas en el catálogo | PEP del baseline | MATCH en código / PENDIENTE en el PDP | `AccessCatalogConsistencyTest` |
| C-053-NOT | Avisos | `NotificacionPort` (REGISTRADA, RESUELTA, RETIRADA) | provider de log | MATCH | SI-01, RS-01, RW-01 (TD-066) |

## Códigos/enums

Estados `P`, `A`, `R`, `E`, `X` ↔ `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `EN_ESPERA`, `RETIRADA`. `SIG_001` 403; `SIG_002`, `SIG_007` 404; `SIG_003`, `SIG_004`, `SIG_005`, `SIG_008`, `SIG_013` 409; `SIG_009`, `SIG_010` 400; éxitos `SIG_006`, `SIG_011`, `SIG_012`, `SIG_014`, `SIG_015`.

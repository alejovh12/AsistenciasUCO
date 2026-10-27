---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT MATRIX — HU008-oferta-grupos-estudiante

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin secretos.

## Evidencia

Ramas `feature/HU008-oferta-grupos-estudiante` en commits locales, sin subir.

| Sistema | Repo | Archivo/objeto | Autoridad |
|---|---|---|---|
| DB | gestion-asistencia-db | `usp_consultar_oferta_grupos_estudiante`, `usp_registrar_estudiante_en_grupo_interno`/`_autonomo`, `OFERTA.INCLUIR_TRANSVERSALES` | OWNER |
| Backend | AsistenciasUCO | `OfertaGruposEstudianteJpaRepository`, `EstudianteOfertaAcademicaController`, `access-catalog.json` | CONSUMER / PROVIDER HTTP |
| Frontend | AsistenciasUCO-Frontend | `OfertaAcademicaService`, `StudentGroupOfferComponent` | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Consumer expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-008-DB | Oferta | READ_QUERY_RESULTSET de 21 columnas (`codigoComponente` y `totalRegistros`); ámbito del estudiante; parámetro de transversales | Repositorio con mapeo posicional | MATCH | O-01..O-05, OR-01..06 | — | — |
| C-008-REGLA | Inscripción en el programa | Solo ESP crea `EstudiantePrograma` | HU053-055 y matrícula del docente siguen funcionando | MATCH | RI-01..RI-03, S-01..S-12 | Las transversales ya no amplían el directorio del programa ajeno | — |
| C-008-HTTP | `GET /estudiante/oferta-academica` | `ApiListResponse<GrupoOfertaDTO>` con `tipoAsignatura` y `transversal`; 400 y 403 | Pantalla con etiqueta "Transversal" y botón "Solicitar ingreso" | MATCH | OG-01..08, OC-01..05, OF-01..07 | — | — |
| C-008-CAT | Autorización | Fila nueva en `access-catalog.json` (ESTUDIANTE) | PEP del baseline | MATCH en código / PENDIENTE en el PDP | `AccessCatalogConsistencyTest` | Ruta denegada en modo baseline hasta reaprovisionar | Alejo: `provision_asistencias.py` |

## Códigos/enums

- `VAL_001` (ejecutor nulo).
- `estadoPeriodo`: VIGENTE o PROXIMO.
- `codigoComponente`: ESP, BAS o INS, que se traduce a ESPECIFICA, BASICA o INSTITUCIONAL.
- `estadoSolicitud`: PENDIENTE o EN_ESPERA.

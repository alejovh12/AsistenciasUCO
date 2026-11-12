---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-11
---

# VALIDATION — PC (backend, BE-1 y BE-2)

Corrida local del 2026-11-11, JDK 25, rama `feature/PC-tomas-puntos-control`.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd -B -o clean verify -Dsurefire.rerunFailingTestsCount=2` (Java 25, 19 min) | `BUILD SUCCESS`: 4375 tests, 0 fallos, 0 errores, 0 omitidos, 1 inestable; "All coverage checks have been met". La inestable es `RealtimeStreamErrorHttpTest.grupoId_en_mayusculas...` (espera 2 s un evento SSE): falló en las dos corridas completas anteriores sin reintento y pasa sola y en el reintento; no toca el código de este trabajo. Una corrida previa también falló `UseCasesShouldNotKnowSqlResultProtocolTest` (el token `codigoResultado` en un `UseCaseImpl`); se renombró el componente a `codigoAprobacion` |
| Backend, pruebas puntuales | `.\mvnw.cmd -B -o test -Dtest=<clases del work item>` | 178 de 178 en verde, 0 fallos |
| Integración SQL Server | `.\mvnw.cmd -Pintegration verify` | NOT_RUN: requiere la BD con la migración 37; no es PASS |
| Catálogo de rutas y PDP | `provision_asistencias.py catalog` / `pdp` | NOT_RUN en esta corrida |

## Pruebas nuevas o cambiadas

- Nuevas: `ResultadoAprobacionReclamoTest`, `PuntosControlMappingTest` (reclamos del estudiante, detalle del docente y sesiones hasta la respuesta HTTP), `AsistenciaSesionEstudiantePuntosControlJpaRepositoryTest` (aislamiento por estudiante y asignatura, troceo del `IN`, mapeo).
- Ampliadas: `TomaAsistenciaUseCasesTest` (resumen y titularidad), `TomaAsistenciaControllersTest` (ruta nueva, campos nuevos, 409 `TOM_012`), `TomaAsistenciaJpaRepositoryTest` (columnas nuevas y resumen), `AsistenciaQrJpaRepositoryTest` y `AsistenciaQrUseCasesTest` (constructor del resumen), `ConsultarSesionesMateriaEstudianteUseCaseImplTest` (minutos, estado de la BD, tomas, reclamable, cancelada, respaldo por marcas), `AsistenciaJpaCommandNativeQueryPatternTest` (v4 y `@codigoResultadoAprobacion`), `ResolverSolicitudRevisionAsistenciaDomainTest`, `AsistenciaControllerContractTest` (`tomaId`), `DocentePortalControllerTest` (`resultado`), `SolicitudRevisionEstudianteJpaRepositoryTest`, `SolicitudRevisionDocenteDetalleJpaRepositoryTest`, `DbFailureClassifier*Test` (TOM_012 y REV_026..029) y `AccessCatalogConsistencyTest` (sin cambios; falló hasta agregar la ruta a `SecurityConfig`).

## Revisión en pantalla (2026-11-12)

Un agente QA probó la rama `feature/PC-tomas-puntos-control` en pantalla con la BD migrada (migración 37), el backend en 8080 y el frontend en 4200, con los usuarios `uco-docente` y `uco-estudiante`. Reporte completo: `informes/REVISION_PANTALLA_PUNTOS_CONTROL_2026-11-12.md`.

- Pasan: punto de control y minutos de tramo por toma, toma cerrada no editable tras abrir la siguiente, plazo de la última toma (fin más 30 minutos), aviso al cerrar, resumen de la sesión coincidente con `uv_asistencia_sesion_horas`, estado "Parcial: 34 de 60 minutos", botón de reclamo solo en la toma ausente, radicación del reclamo por toma (202), "Reclamo pendiente", aprobación "Estuvo presente" con recálculo a 60 de 60 minutos, y el resultado visible para el estudiante.
- Hallazgo menor: la bandeja de reclamos del docente no muestra la toma en la tarjeta (solo en el detalle), porque el listado no envía los campos de toma.
- Sin probar: tope de 20 tomas, `TOM_012` y `ERR_RECLAMO_TOMA_DUPLICADO` por API, "Ausencia justificada" y "Rechazar" sobre un reclamo de toma, "Marcar faltantes", QR con cámara y sesión cancelada con tomas.

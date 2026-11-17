---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W3I - PLAN

Paquete vertical BD -> backend -> frontend del cierre: ultimo paquete de la regla "el frontend no calcula". Contrato en `CONTRACT.md`.

| Capa | Cambio |
| :--- | :--- |
| BD (`gestion-asistencia-db`) | Columnas nuevas AL FINAL de `usp_consultar_kpis_coordinador` / `_decano` / `_institucion`; SP nuevo `usp_consultar_resumen_grupos_docente`; `codigoPermanencia` en el consolidado y `codigoPermanenciaActual` en revisiones de cancelacion y alertas del estudiante (`ufn_clasificar_permanencia`); permiso `rol_asistencias_runtime_w3i_indicadores.sql`; pruebas `test_w3i_indicadores.sql`. |
| Backend | Campos nuevos en `KpisCoordinador*`, `KpisDecano*`, `KpisInstitucion*` (Projection, Domain, DTO, mapper, entidad de SP y adaptador JPA); caso de uso `ConsultarResumenGruposDocente`, `ResumenGruposDocenteQueryPort`, `ResumenGruposDocenteJpaRepository` y `GET /api/v1/docente/resumen-grupos`; `codigoPermanencia` en `ConsolidadoGrupoDTO` y `codigoPermanenciaActual` en `RevisionFila` / `AlertaFila` y sus respuestas HTTP. Catalogo de accesos y matriz HTTP con la ruta nueva. |
| Frontend | Tableros de administrador, decano, coordinador y docente leen los totales del servidor y distinguen "cargando" de "sin datos"; ocupacion de cupos del coordinador y chips de Mi facultad desde los KPI; total de estudiantes del docente desde `resumen-grupos`; semaforo unico por `codigoPermanencia` sin umbrales en el cliente; simulador con las rutas nuevas. |

## Punto 4 (contadores sobre la lista o la pagina cargada): decision por pantalla

| Pantalla | Que cuenta | Decision |
| :--- | :--- | :--- |
| `admin-facultades` (`totalActivas`, `totalInactivas`, `totalConDecano`) | filas de `GET /admin/facultades` | Sin cambio: el endpoint devuelve el catalogo completo (lista simple, sin paginar) y la pantalla lo filtra y pagina por diseno; contar sus filas es presentacion. |
| `dean-estructura` (`totalProgramas`, `totalPlanes`) | nodos de `GET /decano/estructura` | Sin cambio: es el arbol completo de UNA facultad (una respuesta, no paginada). |
| `dean-programas` (`planesVigentes`, `totalAsignaturas`) | planes de la estructura y suma de `totalAsignaturas` por plan de `GET /decano/programas-creditos` | Sin cambio con salvedad: la lista de planes es la de la estructura (completa) y los creditos se piden pagina por pagina hasta completar y no se muestra la suma ("-") mientras falte algun plan. Es una suma entre filas de un catalogo pequeno cargado completo por diseno; si el catalogo creciera habria que mover el total al SP. |
| `dean-faculty` (grupos, inscritos, cupos) | descargaba todas las paginas de `/decano/grupos` | Corregido (punto 2): vienen de `GET /decano/indicadores`. Periodos, areas y planes cuentan listas pequenas completas: sin cambio. |
| `coordinator-subjects` ("Ocupacion de Cupos") | solo la pagina visible | Corregido (punto 2): viene de `GET /coordinador/indicadores`. |

## Fuera de alcance (declarado)
Listados ya migrados, matriz de asistencia y resumen de sesiones (W3M), la parte del estudiante del tablero, variacion de tendencia del administrador, "proxima clase", el interceptor `role-scope`.
Quedan escritos a mano en el tablero del docente el rotulo "Semestre 2026-II" y en otras pantallas rotulos de periodo analogos (no son calculos; se reportan).

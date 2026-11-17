---
status: active
type: contract
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W3I - CONTRACT

Clasificacion: extension NO Golden Path (no modifica `openapi-golden-path.yaml`). Todo es aditivo: campos nuevos en tres respuestas existentes,
una ruta nueva `GET /api/v1/docente/resumen-grupos` y un campo nuevo de nivel en cinco listados. Regla que se hace cumplir: el frontend es
presentacion pura (no suma, cuenta, promedia ni clasifica datos de negocio, ni aplica umbrales). Es el ultimo paquete de esa regla.

## 0. Inventario de lo que el cliente hacia (antes)

| Indicador (pantalla) | Donde se calculaba | Con que datos | Defecto |
| :--- | :--- | :--- | :--- |
| Admin: "Facultades Activas" (`overview`) | `totalFacultadesAdmin`: facultades distintas del conjunto de DECANOS | `GET /admin/decanos` | una facultad que no aparece en la lista de decanos no cuenta; cuenta tambien las inactivas |
| Admin: "Decanos en ejercicio" | `decanos().filter(estaActivoDecano !== false).length` y `decanos().length` | `GET /admin/decanos` | cuenta en el cliente |
| Decano: "Coordinadores Asignados" | `coordinadores().length` | `GET /decano/coordinadores` | primera pagina |
| Decano: "Docentes de Facultad" | suma de `totalDocentes` de cada coordinador | `GET /decano/coordinadores` | un docente de dos programas se cuenta dos veces; solo la primera pagina |
| Decano: "Grupos en Curso" / periodo | `kpis.gruposActivos` y `nombrePeriodoAcademico`, `null` mientras cargaba o si fallaba | `GET /decano/indicadores` | ver F-DEC-01 |
| Coordinador: "Planta Docente Activa" | `docentes().length` | `GET /coordinador/docentes?pagina=1&tamano=100` | primera pagina de 100 |
| Coordinador: "Grupos en Curso" | suma de `totalGruposAsignados` de los docentes de la primera pagina; rotulo "Periodo 2026-II" quemado | idem | truncado y con el periodo escrito a mano |
| Coordinador: "Estudiantes Registrados" | `res.datos.length` tras descargar TODAS las paginas de `GET /coordinador/estudiantes` (51 peticiones seguidas con 5007) | idem | ver F-COORD-01 |
| Coordinador: "Ocupacion de Cupos" (`coordinator-subjects`) | suma de inscritos y cupos de la pagina visible | `GET /coordinador/grupos` | rotulado "de la pagina" |
| Decano: totales de "Mi Facultad" (`dean-faculty`) | `courses().reduce` sobre las paginas de `GET /decano/grupos` | idem | calcula en el cliente |
| Docente: "Estudiantes Registrados" (`overview`, `teacher-grupos-list`) | `courses().reduce(enrolledStudentsCount)` | `GET /docente/asignaturas` | calcula en el cliente |
| Semaforo (`nivel-asistencia.ts`) | compara % de inasistencia con umbrales 15/20 de respaldo | % de inasistencia + umbrales (a veces) | umbrales duplicados en el cliente |

## 1. Indicadores por rol (campos nuevos, sin cambiar los existentes)

### `GET /api/v1/admin/indicadores` (`usp_consultar_kpis_institucion`)
`totalFacultades` (ya existia: todas las facultades de la institucion, con o sin decano activo). Nuevos: `facultadesActivas` (`Facultad.estado = 1`), `totalDecanos`
(decanos cuya facultad es de la institucion; un decano sin facultad no tiene institucion resoluble y no se cuenta, igual que el directorio de estudiantes) y
`decanosActivos` (`uv_decano.estaActivoDecano = 1`: usuario, rol y facultad activos). `totalProgramas` y `porcentajeAsistencia` ya existian.

### `GET /api/v1/decano/indicadores` (`usp_consultar_kpis_decano`)
Nuevos `totalCoordinadores` (coordinadores con `estaActivoCoordinador = 1` de la facultad, una vez por persona) y `totalDocentes` (docentes con vinculo
`estaActivoDocentePrograma = 1` en algun programa de la facultad, una vez por docente aunque este en varios). `gruposActivos`, `cupoTotal`,
`matriculadosActivos` y `tasaAforo` ya existian (grupos habilitados del periodo resuelto: `matriculadosActivos / cupoTotal`, NULL con cupo 0).

### `GET /api/v1/coordinador/indicadores` (`usp_consultar_kpis_coordinador`)
Nuevos: `totalDocentes` (vinculos activos del programa, una vez por docente; sin tope de pagina), `gruposActivos`, `cupoTotal`, `matriculadosActivos`, `tasaAforo`
(grupos habilitados del programa en el periodo resuelto, misma regla que el decano) y `estudiantesRegistrados` (padron del programa: estudiantes con
`EstudiantePrograma` en el programa, sin filtrar por matricula ni por estado: es el "Total en el Programa" de la pantalla de estudiantes).
El periodo lo resuelve el SP (el solicitado o el mas reciente con grupos habilitados del programa) y `nombrePeriodoAcademico` se muestra tal cual; sin periodo vigente
los conteos de grupos y cupo son 0 y `tasaAforo` es NULL (nunca un valor inventado).

### `GET /api/v1/docente/resumen-grupos` (RUTA NUEVA, `usp_consultar_resumen_grupos_docente`)
Rol DOCENTE (otro rol: 403 `SEC_001`). `ApiDataResponse<ResumenGruposDocenteDTO>`:
`{ gruposHabilitados: int, estudiantesActivos: int, cupoTotal: int, tasaOcupacion: decimal|null }` sobre los grupos donde el ejecutor es titular y cuyo periodo
esta vigente hoy (`uv_grupo.grupoEstaHablitado = 1`, el mismo criterio de "habilitado" del decano y el coordinador). `tasaOcupacion = 100 x estudiantesActivos / cupoTotal`,
NULL con cupo total 0. Un docente sin grupos recibe una fila en cero.

### Nivel de permanencia ("semaforo") calculado en la base
La unica regla es `ufn_clasificar_permanencia(porcentaje, estado)`: `REGULAR < UMBRAL_ALERTA_ADVERTENCIA <= RIESGO < UMBRAL_ALERTA_PERDIDA <= PERDIDA` con los
umbrales de `CatalogoParametro` (grupo `ASISTENCIA`); la matricula cancelada por inasistencia (`CI`) es `PERDIDA`. Ya viajaba como `codigoPermanencia` en el resumen del estudiante,
el resumen del docente y las alertas del coordinador (`semaforo`). Se completa donde faltaba:

| Respuesta | Campo nuevo | Valor |
| :--- | :--- | :--- |
| consolidado de asistencia por grupo (cada fila) | `codigoPermanencia` | nivel del grupo con su % de inasistencia por horas (ya redondeado a 2 decimales, como el resto) |
| revisiones de cancelacion de coordinador, decano y docente (cada fila) | `codigoPermanenciaActual` | nivel vigente de la matricula |
| alertas de inasistencia del estudiante (cada fila) | `codigoPermanenciaActual` | nivel vigente de la matricula |

El cliente solo mapea `REGULAR/RIESGO/PERDIDA` (o `VERDE/AMBAR/ROJO` en las alertas del coordinador) a color, icono y texto; no hay umbrales en el cliente.
Los campos existentes `umbralAdvertencia` / `umbralPerdida` se conservan por compatibilidad.

## 2. Causa raiz de F-COORD-01, F-DEC-01 y F-DEC-02 (recorrido del 15-nov)

Los tres vienen del mismo origen: **el cliente calcula o decide sobre datos que aun no llegaron y no distingue "cargando" de "sin datos"**.
El recorrido manual (VALIDATION.md) ya anota las pantallas como lentas (5-7 s). No se pudo medir contra el laboratorio (prohibido tocarlo); la causa sale de leer el codigo y se cubre con pruebas.

- **F-COORD-01** ("Estudiantes Registrados" = 0 mientras la pantalla dedicada muestra 5007): `overview.component.ts` inicializaba `totalEstudiantesPrograma = signal(0)` y lo llenaba
  con `getEstudiantesDirectorio()`, que descarga **todas** las paginas de `/coordinador/estudiantes` una tras otra (`obtenerTodasLasPaginas` usa `expand`, secuencial: 51 peticiones de 100 con 5007 estudiantes).
  Mientras llegaban se veia el 0 inicial y, ademas, la llamada no tenia manejador de error: un fallo dejaba el 0 para siempre. Tambien se pedia sin esperar a
  `CoordinatorProgramService.cargar()`; con un coordinador de varios programas y sin programa elegido el backend responde `COO_002` (el SP exige `programaId`) y el 0 quedaba fijo.
  Correccion: `estudiantesRegistrados` viene del resumen del servidor (una peticion, con el `programaId` ya resuelto) y la vista muestra "Sin datos" si falla y no pinta cifra hasta tenerla.
- **F-DEC-01** ("Grupos en Curso" muestra "—" y "Sin período vigente" aunque el periodo esta en curso): el estado inicial de `gruposActivosFacultad` y `periodoFacultad` es `null` y la plantilla
  imprimia `'Sin período vigente'` para ese `null`, es decir, **durante la carga y ante cualquier error se afirmaba que no habia periodo**. El backend si entrega el periodo (`usp_consultar_kpis_decano`
  resuelve el periodo mas reciente con grupos habilitados de la facultad). No hay dos criterios de "vigente": la pantalla Periodos Academicos marca "en curso" por fechas y el KPI toma el
  periodo con grupos habilitados (`uv_grupo.grupoEstaHablitado`, tambien por fechas): coinciden mientras la facultad tenga grupos en el periodo en curso. Correccion: estado de carga explicito
  ("Cargando…", luego cifra; "Sin período vigente" solo si el servidor devolvio periodo `null`; "Sin datos" si fallo).
- **F-DEC-02** ("Ausentismo por programa: No hay programas en esta facultad." con "Programas: 1" en la misma pantalla): `dean-indicators.component.ts` pedia en paralelo `indicadores` (rapido),
  `comparativa-programas` (lenta: recorre `uv_resumen_asistencia_estudiante_programa_grupo`) y `aforo-grupos`, y mostraba "No hay programas" cuando `comparativa().length === 0`, que tambien es el estado **antes de que
  responda** o si respondio con error. El SP devuelve siempre una fila por programa de la facultad (parte de `uv_programa`), asi que vacio solo ocurre sin respuesta. Correccion: estado "cargando" y "error con reintento"
  distintos del vacio real; el vacio solo se afirma con respuesta recibida.

## 3. 958 frente a 675 (perdida por grupo frente al KPI del decano)

No es un error: son **dos definiciones distintas**, ambas intencionales y documentadas en los SP.

| | Consolidado por grupo (`usp_consultar_consolidado_asistencia_grupos_paginado`) | KPI del decano (`usp_consultar_kpis_decano`) |
| :--- | :--- | :--- |
| Unidad | matricula (estudiante x grupo x programa del ambito) | persona: la peor clasificacion de sus matriculas (decision D3) |
| Estado | cuenta `PERDIDA` incluyendo cancelados por inasistencia (`CI`) | solo matriculas activas (`A`) |
| Periodo | el filtrado, o todos si no hay filtro | solo el periodo resuelto (vigente) |

Un estudiante que pierde dos grupos suma 2 en la tabla y 1 en el KPI. La prueba `W3I_DIF_01_PERDIDA` lo deja demostrado con un fixture (un estudiante pierde dos grupos: consolidado 2, KPI 1 de 3).
No se corrige nada; si se quisiera una sola cifra seria una decision de producto.

## 4. Contadores sobre la lista o la pagina cargada (punto 4)

Ver PLAN.md (decision por pantalla).

## 5. Rutas y permisos

Ruta nueva: `GET /api/v1/docente/resumen-grupos` (DOCENTE). `SecurityConfig` ya cubre `/api/v1/docente/**`; alta en `infra/security-baseline/access-catalog.json` y `docs/contracts/HTTP_AS_IS_MATRIX.md`.
Permiso SQL: `schema/security/rol_asistencias_runtime_w3i_indicadores.sql` (repositorio de BD).

## 6. Compatibilidad

BD -> backend -> frontend, todo aditivo: columnas nuevas AL FINAL de los resultsets (un backend viejo las ignora al mapear por nombre), campos nuevos en JSON (un frontend viejo los ignora)
y una ruta nueva. El backend nuevo contra una BD vieja falla en las columnas nuevas: se mezcla despues de la BD.

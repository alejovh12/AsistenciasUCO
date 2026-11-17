---
status: active
type: contract
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W3M - CONTRACT

Clasificacion: extension NO Golden Path (no modifica `openapi-golden-path.yaml`). Rutas nuevas bajo `/api/v1/docente/**`; el unico cambio
sobre una ruta existente es un parametro opcional (`busqueda`) en `GET /docente/grupos/{grupoId}/resumen-asistencia`. Regla que se hace
cumplir: el frontend es presentacion pura (no filtra, ordena, pagina, cruza ni deduce estados con el reloj del navegador).

## 0. Inventario de lo que el cliente hacia (antes)

| Pantalla | Que descargaba / calculaba |
| :--- | :--- |
| `attendance-control` (`onCourseSelect`, tras crear sesion) | `GET /api/v2/sesiones/grupo/{id}` + `GET /grupos/{id}/estudiantes` (padron completo) + `GET /grupos/{id}/asistencias` (todas las asistencias del grupo); filtraba matriculas con `codigoEstado === 'A'`, agrupaba por sesion y armaba `records` por sesion (AN, EX, SJC o sin registro). |
| `group-sessions-overview` | Por sesion: presentes = `records` con AN, justificadas = `records` con EX, total = `records.length` (activos). Sesiones "programadas" = `sessions.length` menos las canceladas; promedio = media de `porcentajeAsistencia` y en riesgo = conteo de `RIESGO/PERDIDA` sobre TODAS las matriculas del resumen (incluidas las retiradas). Sesion "de hoy" y "proxima" con `findActiveOrUpcomingSession` y la fecha de hoy del navegador (fuera de alcance). |
| `teacher-grupos` / `teacher-grupo-hub` | Mismo cruce en `sessionsConRegistros` (padron + asistencias + sesiones) y `resumirSesiones` / `sesionDeHoy` con `new Date()`: ejecutadas = no canceladas con `fin <= ahora`; en curso = `inicio <= ahora < fin`; proxima = la de menor inicio mayor que ahora. |
| `teacher-grupo-sabana` | `slice` del padron en memoria para paginar y `sessions[].records` para pintar AN / EX / SJC; el resumen por horas ya venia paginado del servidor. |
| `teacher-sesion-detalle-modal` | `records` de la sesion elegida (lista de una sola sesion: no cambia). |
| `coordinator-sesiones-grupo` | No usa `resumirSesiones` ni calcula conteos: solo formatea el horario de la lista que entrega el servidor (sin cambio). |

## 1. `GET /api/v1/docente/grupos/{grupoId}/matriz-asistencia` (RUTA NUEVA)

Rol DOCENTE titular del grupo (grupo ajeno o inexistente: 403 `SEC_002`).

| Parametro | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `pagina` | int | si | 1 | >= 1, si no 400 `PAG_001` |
| `tamano` | int | si | 50 | 1..100 (`PAGINACION/TAMANO_MAXIMO`), si no 400 `PAG_001` |
| `busqueda` | string | si | sin filtro | subcadena del nombre completo o del documento; recortada; vacia = sin filtro; > 100 caracteres = 400 |

Respuesta 200:

```
{ "exitoso": true,
  "sesiones": [ { "idSesion": UUID, "numero": int, "nombre": string, "estadoSesion": "PROGRAMADA|EN_CURSO|EJECUTADA|CANCELADA",
                  "fechaHoraInicioUtc": instante UTC, "fechaHoraFinUtc": instante UTC, "horarioConfirmado": bool } ],   // TODAS las sesiones, por numero
  "elementos": [ { "idEstudianteGrupo": UUID, "idEstudiante": UUID, "nombreCompleto": string, "documento": string,
                   "codigoEstadoEstudiante": string,                                    // A = activo; I, CI, CVP, F = no activo
                   "celdas": [ { "idSesion": UUID, "codigoEstado": string, "minutosAsistidos": int, "minutosSesion": int } ] } ],  // una por sesion, mismo orden
  "numeroPagina": int, "tamanoPagina": int, "totalElementos": int /* estudiantes que cumplen la busqueda */, "totalPaginas": int }
```

`codigoEstado` de la celda: `PRESENTE` (asistio todos los minutos), `PARCIAL` (asistio una parte), `JUSTIFICADA` (solo excusa), `AUSENTE`,
`SIN_REGISTRO` (el estudiante no tiene cabecera de asistencia en una sesion no cancelada) y `NO_DISPONIBLE` (sesion cancelada). Los cuatro
primeros son los de `uv_asistencia_sesion_horas.codigoEstadoSesionEstudiante` (DEC-066: tramos entre tomas), la misma vista que usan los
reportes y el resumen por horas. Orden de filas: nombre completo y luego `idEstudiante` (el mismo del resumen por horas, de modo que
`pagina`/`tamano`/`busqueda` identicos devuelven los mismos estudiantes en ambos endpoints). Incluye TODAS las matriculas del grupo (no solo
activas); el cliente distingue por `codigoEstadoEstudiante`. Sin sesiones: cada estudiante con `celdas = []`.

### SP `dbo.usp_consultar_matriz_asistencia_grupo_docente_paginado`

```
@idUsuarioEjecutor, @idCorrelacion, @idGrupo, @numeroPagina = 1, @tamanoPagina = 50, @busqueda NVARCHAR(100) = NULL
```

Resultset plano (una fila por matricula y sesion): `idEstudianteGrupo, idEstudiante, nombreCompletoEstudiante, documento,
codigoEstadoEstudiante, totalRegistros, idSesion, codigoEstado, minutosAsistidos, minutosSesion`. La pagina y la busqueda se aplican a
estudiantes (`OFFSET/FETCH`, `COUNT(*) OVER()`), no a celdas. Perfil DOCENTE y titularidad validados antes de leer filas (`SEC_001`,
`SEC_002`, `ERR_GRUPO_NO_EXISTE`, `PAG_001`). El encabezado `sesiones` sale del SP del punto 2 (una consulta mas por pagina, acotada por las
sesiones del grupo).

## 2. `GET /api/v1/docente/grupos/{grupoId}/resumen-sesiones` (RUTA NUEVA)

Rol DOCENTE titular. Respuesta 200 `ApiDataResponse<ResumenSesionesGrupoDTO>` (`exitoso`, `datos`):

```
datos: { "sesionesProgramadas": int /* todas las del grupo: ejecutadas y canceladas incluidas */, "sesionesEjecutadas": int,
         "sesionesCanceladas": int, "sesionesEnCurso": int, "idSesionEnCurso": UUID|null, "idSesionProxima": UUID|null,
         "estudiantesActivos": int, "estudiantesEnRiesgo": int, "promedioAsistencia": decimal|null,
         "umbralAdvertencia": decimal, "umbralPerdida": decimal,
         "sesiones": [ { "idSesion", "numero", "nombre", "fechaHoraInicioUtc", "fechaHoraFinUtc", "horarioConfirmado": bool,
                         "estadoSesion": "PROGRAMADA|EN_CURSO|EJECUTADA|CANCELADA", "estadoSolicitud": string|null,
                         "pendienteAprobacion": bool, "totalEstudiantes": int, "presentes": int, "parciales": int,
                         "justificadas": int, "ausentes": int, "sinRegistro": int } ] }
```

Definiciones (todas en `dbo.usp_consultar_resumen_sesiones_grupo_docente`, con `SYSUTCDATETIME()`: las sesiones se guardan en UTC, igual
que `uv_cumplimiento_sesion_grupo.estaVencida`; la zona `TIEMPO/ZONA_HORARIA_SQLSERVER` solo interviene en comparaciones de fecha local,
que aqui no hay):

- `estadoSesion`: `CANCELADA` si tiene `SesionCancelacion` (docente, coordinador, festivo o solicitud rechazada); si no, `PROGRAMADA` cuando
  el horario no esta confirmado (`procedenciaTemporal` NULL, `horarioConfirmado = false`) o aun no inicia; `EN_CURSO` si
  `inicio <= ahora < fin`; `EJECUTADA` si `fin <= ahora`. Una sesion pendiente de aval conserva su estado temporal y trae
  `pendienteAprobacion = true`.
- `sesionesProgramadas` = todas las sesiones del grupo; `sesionesEjecutadas` = las `EJECUTADA`; `sesionesCanceladas` = las `CANCELADA`;
  `sesionesEnCurso` = las `EN_CURSO`. `idSesionEnCurso` = la de menor inicio entre las `EN_CURSO`; `idSesionProxima` = la `PROGRAMADA` con
  horario confirmado y `inicio > ahora` mas cercana.
- Conteos por sesion sobre las matriculas ACTIVAS (`MATRICULA/ESTADO_INSCRITA`): `presentes` = estado `PRESENTE`; `parciales`;
  `justificadas` = `JUSTIFICADA`; `ausentes` = `AUSENTE`; `sinRegistro` = `totalEstudiantes` menos esos cuatro (0 si la sesion esta cancelada);
  `totalEstudiantes` = `estudiantesActivos`.
- `estudiantesEnRiesgo` = matriculas activas con `codigoPermanencia` `RIESGO` o `PERDIDA` (umbrales `ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA` y
  `UMBRAL_ALERTA_PERDIDA`, expuestos tal cual como `umbralAdvertencia` y `umbralPerdida`). `promedioAsistencia` = promedio de
  `porcentajeAsistenciaHoras` de las matriculas activas; `null` si el grupo no tiene horas dictadas o activos.
- Un grupo sin sesiones devuelve `sesiones = []`, conteos en 0 y `promedioAsistencia = null`.

### SP `dbo.usp_consultar_resumen_sesiones_grupo_docente`

```
@idUsuarioEjecutor, @idCorrelacion, @idGrupo
```

Una fila por sesion (orden por numero) con las columnas del grupo repetidas; un grupo sin sesiones devuelve una fila con `idSesion` NULL.
Perfil DOCENTE y titularidad validados antes de leer filas. Sin paginar: las sesiones de un grupo son acotadas, como en
`GET /api/v2/sesiones/grupo/{id}`.

## 3. `GET /api/v1/docente/grupos/{grupoId}/resumen-asistencia` (extendido)

Parametro nuevo opcional `busqueda` (misma regla del punto 1) enviado a `@busqueda NVARCHAR(100) = NULL` al final de
`usp_consultar_resumen_asistencia_grupo_docente_paginado`. El resultset (39 columnas) no cambia; `totalRegistros` cuenta con el filtro. La sabana
pide matriz y resumen con los mismos `pagina`, `tamano` y `busqueda` (mismo orden y filtro).

Permisos: `schema/security/rol_asistencias_runtime_w3m_matriz_asistencia.sql` (los dos SP nuevos). `SecurityConfig` ya cubre
`/api/v1/docente/**`; alta en `access-catalog.json` y `HTTP_AS_IS_MATRIX.md`.

## 4. Diferencias entre la regla del cliente y la de la base (visibles para el usuario)

Se usa siempre la de la base; el cliente no conserva su regla.

| Tema | Cliente (antes) | Base (ahora) | Que cambia a la vista |
| :--- | :--- | :--- | :--- |
| Sesion parcial | `SJC` (estado consolidado de la sesion) | `PARCIAL` con minutos asistidos de total (DEC-066, regla 9) | La planilla muestra "Parcial X de Y min" en vez de una falta. |
| Excusa y asistencia en tramos distintos | `EX` | `PARCIAL` | Igual que arriba. |
| Estudiante retirado | La planilla le pintaba "—" en todas las sesiones (solo se cruzaban activos) | Se muestran sus marcas reales de las sesiones en que asistio | Aparece su historial y su estado de matricula. |
| Promedio de asistencia del grupo | Media sobre todas las matriculas del resumen, incluidas las retiradas, y 0 % si no habia horas dictadas | Media sobre las matriculas activas; `null` ("Sin registros") si no hay horas dictadas | El promedio deja de arrastrar retirados; un grupo sin clases dictadas ya no dice 0 %. |
| Estudiantes en riesgo | Incluia retirados con `PERDIDA` (p. ej. cancelados por inasistencia) | Solo matriculas activas | El conteo baja si habia retirados con perdida. |
| Sesiones "ejecutadas" | `fin <= ahora` del navegador (incluia sesiones sin horario confirmado con la hora local sin zona) | `fin <= ahora` en UTC y solo con horario confirmado | Una sesion con horario no confirmado queda "programada". |
| "Ejecutadas" del resumen por horas | Sin cambio (`sesionesEjecutadas` del resumen por horas exige registro de asistencia): es otro indicador | Igual | No se mezclan: "sesiones ejecutadas" del cronograma = ya terminaron; "horas dictadas" = terminaron con registro. |

## 5. Compatibilidad

BD nueva con backend viejo: SP nuevos y `@busqueda` opcional al final. Backend nuevo con front viejo: rutas nuevas y parametro opcional;
`GET /grupos/{id}/estudiantes`, `GET /grupos/{id}/asistencias` y `GET /api/v2/sesiones/grupo/{id}` no cambian (el front viejo sigue
funcionando). Backend nuevo con BD vieja: no soportado (la BD se mezcla primero).

## 6. Fuera de alcance

Ventana de toma (`session-attendance-window.util.ts`), sesion recomendada (`session-selection.util.ts`), reclamos y padron de matricula
(W1A), indicadores de portada, semaforo y nivel de asistencia de los consolidados, interceptor `role-scope`. El coordinador no consume
`resumirSesiones` (solo formatea el horario de su lista), por lo que no queda calculo duplicado de este paquete en otro rol.

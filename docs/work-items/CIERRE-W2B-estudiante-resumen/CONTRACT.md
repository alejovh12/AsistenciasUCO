---
status: active
type: contract
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W2B - CONTRACT

Clasificacion: extension NO Golden Path (no modifica `openapi-golden-path.yaml`). Cambios aditivos y opcionales: backend nuevo con frontend
viejo y base nueva con backend viejo siguen funcionando. Regla que se hace cumplir (decision del responsable, 2026-11-16): el frontend es
presentacion pura; no filtra, ordena, pagina ni calcula datos de negocio. Todo lo entrega el backend y se resuelve en la base con el
aislamiento validado dentro del SP (un estudiante solo ve lo suyo).

## 1. `GET /api/v1/estudiante/materias/{materiaId}/sesiones` (extendido)

Rol: ESTUDIANTE. Parametros existentes: `pagina` (>= 1), `tamano` (1..`PAGINACION/TAMANO_MAXIMO`).

| Parametro nuevo | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `grupoId` | UUID | si | todos los grupos inscritos del estudiante en la asignatura | UUID invalido -> 400. Un grupo en el que el estudiante no esta inscrito (o inexistente) devuelve 0 filas, no 403: el mismo comportamiento que una materia ajena |

- **Orden (decision de producto)**: `fechaHoraInicio ASC, numero ASC, id ASC`, resuelto en el SP antes de `OFFSET/FETCH`. Es el criterio que antes
  aplicaba la pantalla (inicio y, a igualdad, numero) con el identificador como desempate estable: dos paginas nunca repiten ni saltan filas.
  Antes el SP ordenaba `numero, fechaHoraInicio, id`.
- **`total`** de `ApiListResponse` pasa a ser el total que cumple el filtro (`COUNT(1) OVER()`), no el numero de filas de la pagina.
  El cliente lo usa para paginar; ningun consumidor anterior usaba `total` de este endpoint.
- Respuesta: `ApiListResponse<SesionMateriaEstudianteResponse>`, sin campos nuevos ni cambios de forma por elemento.

### SP `dbo.usp_consultar_sesiones_materia_estudiante`

```
@idUsuarioEjecutor UNIQUEIDENTIFIER, @idCorrelacion UNIQUEIDENTIFIER, @idMateria UNIQUEIDENTIFIER,
@numeroPagina INT=1, @tamanoPagina INT=100,
@idGrupo UNIQUEIDENTIFIER=NULL       -- NUEVO (al final)
```

Resultset: SIN cambios (13 columnas, `totalRegistros` la 13). Ambito: matricula inscrita (`MATRICULA/ESTADO_INSCRITA`) del ejecutor; con `@idGrupo`
nulo el comportamiento es el actual salvo el orden. Un backend anterior (sin `@idGrupo`) sigue funcionando.

## 2. `GET /api/v1/estudiante/materias/resumen` (RUTA NUEVA)

Rol: ESTUDIANTE (cubierto por `/api/v1/estudiante/**`; alta en `access-catalog.json` y en `HTTP_AS_IS_MATRIX.md`). Precedente de "resumen aparte":
`GET /coordinador/docentes/resumen`. Respuesta 200 `ApiDataResponse<ResumenMateriasEstudianteDTO>`:

| Campo | Tipo | Definicion exacta |
| :--- | :--- | :--- |
| `totalMaterias` | int | Materias del listado `GET /estudiante/materias`: una por asignatura y grupo con matricula inscrita (`MATRICULA/ESTADO_INSCRITA`) del estudiante autenticado |
| `materiasEnRiesgo` | int o `null` | Materias con `codigoPermanencia` RIESGO o PERDIDA en `uv_resumen_asistencia_estudiante_grupo` (regla `ufn_clasificar_permanencia` con los umbrales `ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA` y `UMBRAL_ALERTA_PERDIDA` del catalogo de parametros: los mismos de las alertas y de la perdida por inasistencia). `0` si no hay materias. `null` si hay materias pero ningun umbral esta configurado (sin dato; el cliente muestra "No disponible", nunca un cero) |
| `totalHorasFalta` | decimal (2) | Suma de `horasFalta` (horas de falta sin justa causa) de esas materias, cada una redondeada a 2 decimales como la entrega el listado; `0` sin materias |
| `totalCreditos` | int | Suma de los creditos de esas materias (una vez por materia del listado); `0` sin materias |

Mismo criterio que el listado por construccion: `usp_consultar_materias_estudiante` y el SP del resumen leen la misma vista `uv_materia_estudiante` y aplican el
mismo estado de matricula, por lo que tablero y listado no pueden discrepar. No hay filtro por periodo en ninguno de los dos (el listado no lo tenia):
el resumen no lo introduce.

### SP `dbo.usp_consultar_resumen_materias_estudiante` (nuevo)

```
@idUsuarioEjecutor UNIQUEIDENTIFIER, @idCorrelacion UNIQUEIDENTIFIER
```

Una fila de 4 columnas (`totalMaterias`, `materiasEnRiesgo`, `totalHorasFalta`, `totalCreditos`). `THROW 51000` si falla la validacion: `GEN_002` (ejecutor nulo), `SEC_001`
(no es estudiante -> 403). Solo lectura; permiso `EXECUTE` para `rol_asistencias_runtime` (`rol_asistencias_runtime_w2b_estudiante_resumen.sql`).
Application tambien rechaza con 403 (`ACC_APP_004`) a quien no resuelve como estudiante.

## 3. Causa raiz de F-EST-01 ("Materias Matriculadas: 0" con 1 materia real)

Comprobado en el codigo: tablero y "Mis Materias" consumen la MISMA consulta (`getMaterias()` -> `GET /estudiante/materias`, mismo periodo y mismo estado de
matricula). No hay discrepancia de periodo ni de estado entre las dos pantallas. La discrepancia era del tablero: contaba `materias().length` de un arreglo
inicial vacio y mostraba "No tienes materias inscritas" sin distinguir "aun no respondio" ni "fallo" de "cero materias" (un error se tragaba en silencio:
`error: () => isLoading.set(false)`); ademas "riesgo" y "horas de falta" se recontaban en el cliente de las mismas filas. Con la latencia del listado
(documentada en la propia validacion) el tablero afirmaba un cero que nadie habia confirmado. No se reprodujo contra el laboratorio (prohibido en este paquete); la
correccion es estructural: las cifras vienen del resumen de la base, y mientras no llegan, o si fallan, la vista dice "Cargando" / "No disponible" / error, nunca cero.

## 4. Modal de decision del docente (punto 3, sin cambio de backend)

`GET /api/v1/docente/revisiones-cancelacion` ya entrega `accionesPermitidas` como `List<String>` (`RevisionCancelacionResponse`): `RevisionCancelacionJpaRepository` convierte
la lista separada por comas de `usp_consultar_revisiones_cancelacion_docente_paginado` (columna 52) y devuelve `[]` si esta vacia (pruebas existentes en
`RevisionCancelacionJpaRepositoryTest` y `RevisionCancelacionControllersTest`). El frontend conserva el campo como arreglo y muestra solo esas acciones.

## 5. Seguridad y catalogo

`/api/v1/estudiante/materias/resumen`: ESTUDIANTE; `SecurityConfig` ya exige ESTUDIANTE en `/api/v1/estudiante/**`; nueva fila en
`infra/security-baseline/access-catalog.json` (`ROLES`). Permisos de BD: nuevo `GRANT EXECUTE` del SP del resumen; el SP de sesiones solo agrega un parametro con valor por defecto.

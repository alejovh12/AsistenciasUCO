---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT — HU094/HU095: ficha del docente y consolidado del directorio

## 1. HTTP

### `GET /api/v1/coordinador/docentes/{idDocente}/ficha` (HU094)

- **Rol:** `COORDINADOR` o `ADMINISTRADOR` (`SecurityConfig` `/api/v1/coordinador/**`). Tiene una fila nueva en el catálogo de acceso.
- **Path:** `idDocente` (UUID; inválido ⇒ 400).
- **Query:**
  - `idPeriodoAcademico` (UUID, opcional);
  - `pagina` (por defecto 1, mínimo 1);
  - `tamano` (por defecto 10, de 1 a 100).
- **200:** `ApiDataResponse<FichaDocenteDTO>`

```json
{
  "exitoso": true,
  "datos": {
    "docente": { "...": "DocenteProgramaDTO (mismo contrato del directorio HU062)" },
    "resumen": { "totalGrupos": 4, "totalPeriodos": 3, "gruposVigentes": 1, "estudiantesActivosVigentes": 25 },
    "historial": [{
      "idGrupo": "uuid", "codigoGrupo": 7, "nombreGrupo": "Grupo 7",
      "idAsignatura": "uuid", "nombreAsignatura": "Algebra",
      "idPeriodoAcademico": "uuid", "codigoPeriodoAcademico": 20261, "nombrePeriodoAcademico": "2026-1",
      "fechaInicioPeriodo": "2026-01-20", "fechaFinPeriodo": "2026-06-10",
      "estadoPeriodo": "VIGENTE|FINALIZADO|PROXIMO",
      "capacidadMaxima": 30, "estudiantesActivos": 25, "estudiantesFinalizados": 0,
      "estudiantesCanceladosVoluntad": 2, "estudiantesCanceladosInasistencia": 1
    }],
    "totalHistorial": 4
  }
}
```

| Situación | HTTP | Origen |
|---|---|---|
| `pagina < 1`, `tamano` fuera de 1..100 | 400 | Controller |
| Usuario sin programa a cargo | 403 | Application (`ForbiddenException`) |
| Docente inexistente o sin vínculo con el programa | 404 | Application (directorio vacío); la DB lo revalida con `GEN_001`, que se clasifica como 404 en esta operación |

### `GET /api/v1/coordinador/docentes/resumen` (HU095)

- **Rol:** el mismo de la ficha. Tiene una fila nueva en el catálogo de acceso.
- **200:** `ApiDataResponse<ResumenDocentesProgramaDTO>`

```json
{
  "exitoso": true,
  "datos": {
    "totalDocentes": 5, "totalActivos": 3, "totalInactivos": 2,
    "porTipoVinculacion": [{
      "idTipoVinculacion": "uuid", "codigoTipoVinculacion": "CAT", "nombreTipoVinculacion": "Cátedra",
      "totalActivos": 2, "totalInactivos": 1, "total": 3
    }]
  }
}
```

| Situación | HTTP | Origen |
|---|---|---|
| Usuario sin programa a cargo | 403 | Application (`ForbiddenException`) |

El directorio existente `GET /api/v1/coordinador/docentes` ya aceptaba `idTipoVinculacion`. El frontend ahora lo envía; no hay cambio de contrato.

## 2. Base de datos

### `dbo.usp_consultar_historial_grupos_docente_programa` (READ_QUERY_RESULTSET)

- **Parámetros:**
  - `@idUsuarioEjecutor`, `@idCorrelacion`, `@idDocente`;
  - `@idPeriodoAcademico = NULL`;
  - `@numeroPagina = 1`, `@tamanoPagina = 10`, acotados como R2 (página ≥ 1, tamaño 1..100).
- **Validaciones:**
  - programa del ejecutor (`usp_resolver_programa_coordinador_interno`; `VAL_001` si el ejecutor es nulo, `KPI_001` si no coordina);
  - docente nulo ⇒ `VAL_001`;
  - sin fila en `DocentePrograma` para el programa ⇒ `GEN_001`.
- **Columnas**, en este orden:
  - `idGrupo`, `codigoGrupo`, `nombreGrupo`, `idAsignatura`, `nombreAsignatura`;
  - `idPeriodoAcademico`, `codigoPeriodoAcademico`, `nombrePeriodoAcademico`, `fechaInicioPeriodo`, `fechaFinPeriodo`;
  - `estadoPeriodo` (NVARCHAR(10), según la fecha local `TIEMPO.ZONA_HORARIA_SQLSERVER`);
  - `capacidadMaxima`, `estudiantesActivos`, `estudiantesFinalizados`, `estudiantesCanceladosVoluntad`, `estudiantesCanceladosInasistencia`;
  - `resumenTotalGrupos`, `resumenTotalPeriodos`, `resumenGruposVigentes`, `resumenEstudiantesActivosVigentes`;
  - `totalRegistros`.
- **Orden:** `fechaInicioPeriodo DESC`, nombre de la asignatura, código e id.

### `dbo.usp_consultar_resumen_docentes_programa` (READ_QUERY_RESULTSET)

- **Parámetros:** `@idUsuarioEjecutor`, `@idCorrelacion`.
- **Validaciones:** igual que el historial (`VAL_001` y `KPI_001`).
- **Columnas:** `idTipoVinculacion`, `codigoTipoVinculacion`, `nombreTipoVinculacion`, `totalActivos`, `totalInactivos`.
- **Filas:** una por tipo de vinculación activo, más las de los tipos inactivos que aún tengan vínculos.
- **Criterio de activo:** `uv_docente_programa.estaActivoDocentePrograma` (vínculo, usuario y programa activos), el mismo del directorio.

## 3. Puertos de Application

- `HistorialGruposDocenteQueryPort.consultarHistorial(usuarioEjecutor, idDocente, idPeriodoAcademico, pagina, tamano)` → `HistorialGruposDocentePagina`, con las filas, el total y el resumen.
- `ResumenDocentesProgramaQueryPort.consultarResumen(usuarioEjecutor)` → `List<ResumenTipoVinculacionProjection>`.
- Ambos los implementa `DocenteProgramaReporteJpaRepository`.

## 4. Compatibilidad

Es un cambio aditivo: no modifica ningún contrato existente. El frontend deja de llamar a `CourseService.getCoordinatorCourses` desde la ficha del directorio.

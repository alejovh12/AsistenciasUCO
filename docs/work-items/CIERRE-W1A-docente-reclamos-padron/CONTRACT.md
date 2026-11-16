---
status: active
type: contract
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W1A - CONTRACT

Clasificacion: extension NO Golden Path (no modifica `openapi-golden-path.yaml`). Cambios aditivos y opcionales.

## 1. `GET /api/v1/docente/reclamos` (extendido)

Rol: DOCENTE. Parametros existentes: `estado` (P/A/R/X o PENDIENTE/APROBADA/RECHAZADA/RETIRADA), `pagina` (>=1), `tamano` (1..100).

| Parametro nuevo | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `busqueda` | string | si | sin filtro | Se recorta; vacio = sin filtro. Longitud maxima 100 (mayor -> 400 `VALIDATION_ERROR` con `details[busqueda]`). Coincidencia por subcadena (sin distinguir mayusculas por la collation de la BD) en nombre del estudiante, nombre y codigo de la asignatura y justificacion de la solicitud |
| `grupoId` | UUID | si | todos los grupos del docente | UUID invalido -> 400. Un grupo ajeno al docente devuelve 0 filas (el SP filtra por titularidad), no 403 |

Respuesta: sin cambios (`ApiListResponse<ReclamoDocenteResponse>`: `exitoso`, `datos`, `total`). `total` es el total que cumple TODOS los
filtros, no el de la pagina. Sin parametros nuevos el resultado es identico al actual. Campo `nombre` del elemento = folio del
reclamo (`REC-XXXXXXXX`, generado y persistido por `usp_internal_radicar_solicitud_revision`); ya existe, no se agregan campos.
El folio tambien llega como `nombre` en `GET /estudiante/reclamos` y en `GET /docente/reclamos/{id}`.

### SP `dbo.usp_consultar_solicitudes_revision_docente`

```
@idUsuarioEjecutor UNIQUEIDENTIFIER, @idCorrelacion UNIQUEIDENTIFIER, @codigoEstado NVARCHAR(5)=NULL,
@numeroPagina INT=1, @tamanoPagina INT=10,
@busqueda NVARCHAR(100)=NULL,      -- NUEVO (al final)
@idGrupo  UNIQUEIDENTIFIER=NULL    -- NUEVO (al final)
```

Resultset: SIN cambios (31 columnas; `totalRegistros` es la 25; `COUNT(*) OVER()` ya cuenta con los filtros nuevos).
Ambito: `Docente.usuario = @idUsuarioEjecutor` se mantiene en el WHERE junto al filtro de grupo, de modo que un `@idGrupo` ajeno da 0 filas.
Con ambos parametros NULL el comportamiento es el actual (compatible con el backend viejo).

## 2. `GET /api/v1/docente/grupos/{grupoId}/estudiantes` (RUTA NUEVA)

Rol: DOCENTE (cubierto por `/api/v1/docente/**`; alta en `access-catalog.json`). Padron del grupo del docente titular, paginado en servidor.

| Parametro | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `grupoId` (ruta) | UUID | no | | UUID invalido -> 400 |
| `pagina` | int | si | 1 | >= 1, si no 400 `PAG_001` |
| `tamano` | int | si | 10 | 1..100, si no 400 `PAG_001` |
| `busqueda` | string | si | sin filtro | Recortada, max 100 (400 si mayor). Subcadena en nombre completo o documento |

Respuesta 200 `ApiPageResponse<EstudianteGrupoDTO>`: `exitoso`, `elementos[]` (`id`, `idEstudiante`, `documento`, `nombreCompleto`, `correo`,
`codigoEstado`, `nombreEstado`: igual que el DTO actual), `numeroPagina`, `tamanoPagina`, `totalElementos` (con el filtro de busqueda),
`totalPaginas`. Orden: `nombreCompleto`, `id`. Todos los estados de matricula (DR-007). Grupo ajeno o inexistente: 403 (`SEC_002`,
ya existente), por Application (`canDocenteAccessGrupo`) y por el SP.
`GET /api/v1/grupos/{grupoId}/estudiantes` NO cambia (Golden Path, lista completa).

### SP `dbo.usp_consultar_estudiantes_grupo_ambito_paginado`

```
@idGrupo, @idUsuarioEjecutor, @idCorrelacion, @numeroPagina INT=1, @tamanoPagina INT=NULL,
@busqueda NVARCHAR(100)=NULL       -- NUEVO (al final)
```

Resultset: SIN cambios (8 columnas, `totalRegistros` la 8, ya con el filtro). `@tamanoPagina = NULL` sigue devolviendo todas las filas.
Ambito: sin cambios (DOCENTE titular / ADMINISTRADOR / COORDINADOR; otro => `THROW 51000` con `DBCODE=SEC_002` antes de leer filas).

## 3. Seguridad y catalogo

`GET /api/v1/docente/grupos/{grupoId}/estudiantes`: DOCENTE; `SecurityConfig` ya exige DOCENTE en `/api/v1/docente/**`; nueva fila en
`infra/security-baseline/access-catalog.json` (`ROLES`). Permisos de BD: sin cambios (`EXECUTE` ya concedido a ambos SP; las firmas
solo agregan parametros con valor por defecto).

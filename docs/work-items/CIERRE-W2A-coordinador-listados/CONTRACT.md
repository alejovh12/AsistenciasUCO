---
status: active
type: contract
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W2A - CONTRACT

Clasificacion: extension NO Golden Path (no modifica `openapi-golden-path.yaml`). Cambios aditivos salvo la envoltura de
`GET /coordinador/grupos` (sin consumidores previos).

## 1. `GET /api/v1/coordinador/grupos` (extendido)

Rol: COORDINADOR. Parametros existentes sin cambio: `idPeriodo` (UUID, opcional), `busqueda` (opcional; el SP busca en nombre y codigo
del grupo y nombre de la asignatura), `pagina` (>= 1, defecto 1), `tamano` (1..100, defecto 10; si no 400 `PAG_001`).

Respuesta: ANTES `ApiListResponse` (`exitoso`, `datos`, `total`); AHORA `ApiPageResponse<GrupoProgramaDTO>`: `exitoso`, `elementos`,
`numeroPagina`, `tamanoPagina`, `totalElementos` (total del filtro, no de la pagina), `totalPaginas`.

Campo nuevo del elemento: `franjas: [{codigoDia, nombreDia, horaInicio, horaFin}]` (horario semanal, lunes a domingo y por hora; `[]`
si el grupo no tiene horario). El resto del elemento no cambia (`id`, `codigo`, `nombre`, `idAsignatura`, `nombreAsignatura`,
`idDocente`, `capacidadMaxima`, `estudiantesActivos`, `cuposDisponibles`, `habilitado`, `inicioPeriodo`, `finPeriodo`).
Ambito: `usp_consultar_grupos_paginado` sobre `uv_auth_grupo` (programas del coordinador). Sin cambio de SP.

## 2. `GET /api/v1/coordinador/asignaturas` (extendido)

| Parametro nuevo | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `caracter` | string | si | sin filtro | CODIGO del caracter (`OBL`, `OPT`; catalogo `GET /coordinador/caracteres-asignatura`). Se recorta; vacio = sin filtro; un codigo inexistente (o demasiado largo) devuelve 0 filas (no hay mensaje nuevo) |

### SP `dbo.usp_consultar_asignaturas_coordinador_paginado`

```
@idUsuarioEjecutor, @idCorrelacion, @numeroPagina, @tamanoPagina, @idPrograma=NULL, @busqueda=NULL, @estaActiva=NULL,
@codigoCaracter NVARCHAR(50)=NULL   -- NUEVO (al final)
```

Resultset sin cambios (21 columnas; `totalRegistros` cuenta con el filtro). Con `@codigoCaracter` NULL el comportamiento es el actual.

## 3. `GET /api/v1/coordinador/consolidado-asistencia/opciones` y `GET /api/v1/decano/consolidado-asistencia/opciones` (RUTAS NUEVAS)

Roles: los de `/coordinador/**` (COORDINADOR; ADMINISTRADOR) y `/decano/**` (DECANO; ADMINISTRADOR). Alta en `access-catalog.json`,
`SecurityConfig` (cubiertas por los patrones de prefijo) y `HTTP_AS_IS_MATRIX.md`.

| Parametro | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `idPeriodoAcademico` | UUID | si | todos los periodos | UUID invalido -> 400. Un periodo sin grupos en el ambito devuelve listas vacias |

Respuesta 200 `ApiDataResponse<OpcionesConsolidadoDTO>`:

```
{ "exitoso": true, "datos": { "asignaturas": [{"id": UUID, "nombre": string}], "docentes": [{"id": UUID, "nombre": string}] } }
```

Asignaturas y docentes (titulares) distintos de los grupos del ambito del usuario (coordinador: sus programas; decano: su facultad;
administrador: su institucion), ordenados por nombre. Sin paginar (opciones de un selector).

### SP `dbo.usp_consultar_asignaturas_ambito_consolidado` y `dbo.usp_consultar_docentes_ambito_consolidado`

```
@idUsuarioEjecutor UNIQUEIDENTIFIER, @idCorrelacion UNIQUEIDENTIFIER, @idPeriodoAcademico UNIQUEIDENTIFIER = NULL
```

Resultset: `(id, nombre)`. Perfil COORDINADOR, DECANO o ADMINISTRADOR (si no `SEC_001`, 403); ambito por `uv_auth_grupo`.
Permiso de ejecucion para `rol_asistencias_runtime` en `schema/security/rol_asistencias_runtime_w2a_coordinador_listados.sql`.

## 4. `GET /api/v1/coordinador/periodos-academicos` (extendido)

Respuesta: `ApiListResponse<PeriodoAcademicoDTO>` sin cambio de envoltura. Campo nuevo SIEMPRE presente: `estado` = `PLANEACION`
(hoy < fechaInicio), `ACTIVO` (fechaInicio <= hoy <= fechaFin) o `CERRADO` (hoy > fechaFin), con "hoy" = fecha local de
`TIEMPO/ZONA_HORARIA_SQLSERVER`. Lo calcula la vista `uv_periodo_academico` (columna nueva `estado`).

| Parametro nuevo | Tipo | Opcional | Defecto | Regla |
| :--- | :--- | :--- | :--- | :--- |
| `estado` | string | si | todos | `PLANEACION`, `ACTIVO` o `CERRADO` (sin distinguir mayusculas); otro valor devuelve 0 filas. `total` es el de la lista filtrada |

## 5. Totales (sin endpoints nuevos)

El frontend usa `totalElementos` de listados existentes pedidos con `tamano=1` y su filtro: `/estudiantes?activo=true|false`
(activos/inactivos del programa), `/coordinador/planes-estudio?estaActivo=true` (vigentes), `/coordinador/asignaturas` (vinculos),
`/coordinador/sesiones/{id}/asistencias?asistio=true|false` (presentes/ausentes de la sesion) y
`/coordinador/periodos-academicos?estado=ACTIVO`.

## Compatibilidad

BD nueva con backend viejo: el SP de asignaturas tiene el parametro nuevo opcional y la vista suma una columna. Backend nuevo con
front viejo: `GET /coordinador/grupos` no tenia consumidores; el resto es aditivo. Backend nuevo con BD vieja: no soportado para
`caracter`, opciones y `estado` (la BD se mezcla primero).

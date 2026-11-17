---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-16
---

# CONTRACT — CIERRE-W1B

## 1. Directorio `/api/v1/docentes/**`: inventario de consumidores y decisión

| Ruta | Consumidores reales | Decisión |
|---|---|---|
| `GET /docentes` | Solo `TeacherService.getAllTeachers()` → `user-picker-modal` (pantallas de administrador, «Nombrar decano», y de decano, «Nombrar coordinador»). El decano recibía 403 en esta ruta y el selector caía a una lista simulada. | **Retirada.** El selector usa `GET /admin/docentes` (administrador) o `GET /decano/docentes` (decano), que ya validan ámbito en el SP y paginan. |
| `GET /docentes/{id}` | Ninguno (`TeacherService.getTeacherById` sin llamadores). | **Retirada.** |
| `GET /docentes/{id}/asignaciones` | Ninguno (`getTeacherAssignments` y `getCurrentTeacherAssignments` sin llamadores). | **Retirada.** |
| `POST /docentes/consultas/id`, `POST /docentes/consultas/asignaciones` | Ninguno; marcadas `@Deprecated`. | **Retiradas.** |

Otros usos de la ruta, fuera de los repositorios del paquete: `scripts/general/tests/test_all_hus.ps1` (HU125-HU129, `GET /docentes` con
token de decano; ya recibía 403) y `scratch/test_hu09x.ps1`. La documentación as-is y el catálogo de accesos se actualizan.
Los endpoints con ámbito equivalentes siguen siendo `/admin/docentes`, `/decano/docentes` y `/coordinador/docentes`
(+ `/candidatos`, `/resumen`, `/{id}/ficha`).

Se eliminan: `DocenteController`, los tres casos de uso (`consultardocentes`, `consultardocenteporid`,
`consultarasignacionesacademicas`), `DocenteRepositoryPort` y `DocenteJpaRepository`, sus beans, requests, validadores,
mapeadores y pruebas; la regla de `SecurityConfig` y cinco entradas de `infra/security-baseline/access-catalog.json`.

## 2. Selector de personas (frontend)
`app-user-picker-modal` recibe `fuente = input<'FACULTAD' | 'INSTITUCION'>()` y consulta con `busqueda`, `pagina`, `tamano`
(sin polling). Muestra el total que entrega el servidor y pagina con `PaginationComponent`. No hay lista simulada de respaldo
ni correos o roles deducidos.

## 3. `GET /api/v1/decano/coordinadores` (DECANO)

| Parámetro | Tipo | Opcional | Defecto | Límites |
|---|---|---|---|---|
| `pagina` | int | sí | 1 | ≥ 1 (`PAG_001` si no) |
| `tamano` | int | sí | 10 | 1..100 (`PAG_001`) |
| `busqueda` | string | sí | sin filtro | recortada; vacía = sin filtro; máx. 100 caracteres (400 si más); compara nombre completo, documento, correo y programa |
| `estaActivo` | boolean | sí | sin filtro | filtra por el estado efectivo del coordinador |
| `sinPrograma` | boolean | sí | sin filtro | `true` solo sin programa, `false` solo con programa |

Respuesta: `ApiPageResponse<CoordinadorDTO>`; el DTO suma `nombres` y `apellidos` (primer y segundo nombre / apellido) a los
campos existentes. Un coordinador aparece una sola vez (con su primer programa por nombre), como hoy.
SP: `dbo.usp_consultar_coordinadores_facultad_paginado(@idUsuarioEjecutor, @idCorrelacion, @numeroPagina, @tamanoPagina,
@busqueda = NULL, @estaActivo = NULL, @sinPrograma = NULL)`; resultset de lectura (filas + `totalRegistros`). Ámbito: perfil
DECANO; solo coordinadores de la facultad del decano; otro perfil → `SEC_001`; decano sin facultad a cargo → sin filas.
Antes la paginación se hacía en memoria sobre la lista completa (JPA); ahora el SP aplica `OFFSET/FETCH`.

## 4. Nombres y apellidos
Campos agregados (aditivos; el nombre completo se conserva):
- `GET /admin/docentes` y `GET /decano/docentes` (`DocenteFacultadDTO`): `primerNombre`, `segundoNombre`, `primerApellido`,
  `segundoApellido`, `nombres`, `apellidos`, `idTipoIdentificacion`, `codigoTipoIdentificacion`.
- `GET /admin/estudiantes` (`EstudianteInstitucionDTO`): `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `nombres`, `apellidos`.
- `GET /admin/decanos` (`DecanoDTO`): `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `nombres`, `apellidos`.
- `GET /decano/coordinadores`: `nombres` y `apellidos` (los cuatro campos separados ya existían).
Un dato ausente es `null` (un segundo nombre ausente no se rellena).

## 5. RBAC por perfil (BD)
`usp_validar_permiso_rbac_usuario_interno` aceptaba un perfil si `codigoPerfil = valor` o si `UPPER(nombrePerfil) LIKE '%valor%'`.
Inventario de llamadores: todos pasan el **nombre** del rol (`'DOCENTE'`, `'COORDINADOR'`, `'DECANO'`, `'ADMINISTRADOR'`,
`'ESTUDIANTE'` o listas separadas por coma; unos 120 sitios) salvo `usp_resolver_revision_cancelacion`, que pasa el **código**
(`DE`, `CD`). En `Perfil` el código es de dos letras (`AD`, `DE`, `CD`, `DO`, `ES`) y el nombre va en minúscula
(`administrador`, …). Forzar «solo código» exigiría reescribir todos los llamadores, por lo que se aplica la corrección
compatible: coincidencia **exacta** con el código o con el nombre completo (sin distinguir mayúsculas), sin comodines, y se
descartan los elementos vacíos de la lista (un valor como `'DOCENTE,'` producía un patrón vacío que coincidía con cualquier
perfil). Migrar los llamadores a códigos queda como deuda opcional.

## 6. Seguridad y permisos
- `access-catalog.json`: bajas de las cinco rutas `/api/v1/docentes/**`; sin altas (`/decano/coordinadores` ya existe).
- BD: `GRANT EXECUTE` del SP nuevo al rol `rol_asistencias_runtime` (`schema/security/rol_asistencias_runtime_w1b_directorios.sql`).

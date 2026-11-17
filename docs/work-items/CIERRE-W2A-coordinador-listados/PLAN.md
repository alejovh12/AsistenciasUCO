---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W2A - PLAN (listados del coordinador en servidor)

Fecha: 2026-11-16. Rama `feat/cierre-w2a-coordinador-listados` en los tres repositorios (BD, backend, frontend), desde `develop`.
Orden de mezcla: BD -> backend -> frontend. Cada PR es compatible hacia atras con el anterior y con el siguiente aun sin mezclar.

Regla que se hace cumplir (decision del responsable, 2026-11-16): el frontend es presentacion pura; no filtra, ordena, pagina ni
calcula datos de negocio ni deduce estados con el reloj del navegador. Todo llega del backend (`ApiPageResponse<T>`), resuelto en
stored procedures de consulta con el ambito del rol validado DENTRO del SP.

| Punto | Cambio | Capas |
| :--- | :--- | :--- |
| 1 | `GET /coordinador/grupos` pasa a `ApiPageResponse` y suma `franjas` (horario editable); el front deja de pedir todos los grupos y de paginar/filtrar en memoria | backend (el SP ya filtra por texto y periodo), frontend |
| 2 | `GET /coordinador/asignaturas` acepta `caracter` (codigo) y lo resuelve el SP; las opciones del filtro salen del catalogo `GET /coordinador/caracteres-asignatura` | BD, backend, frontend |
| 3 | `GET /{coordinador,decano}/consolidado-asistencia/opciones` (ruta NUEVA): asignaturas y docentes con grupos en el ambito del usuario y el periodo consultado | BD, backend, frontend |
| 4 | El DTO de periodo trae SIEMPRE `estado` (PLANEACION / ACTIVO / CERRADO) resuelto en la vista `uv_periodo_academico` con la fecha local institucional; `GET /coordinador/periodos-academicos` acepta `estado` | BD, backend, frontend |
| 5 | Totales reales: se usan `total`/`totalElementos` de los endpoints existentes con filtros que ya existen (`estaActivo`, `activo`, `asistio`) y el nuevo `estado` de periodos; ningun contador cuenta la pagina | frontend (backend solo el filtro `estado`) |

## Decisiones

- Punto 1, via `/coordinador/grupos` y no `GET /grupos`: `GET /api/v1/grupos` es un listado sin parametros que consumen otros
  roles y es hermano de rutas del Golden Path (`GrupoController`), cuyas firmas fija `OpenApiGoldenPathConformanceTest`.
  `/coordinador/grupos` ya pagina y filtra (`idPeriodo`, `busqueda`) con ambito en `usp_consultar_grupos_paginado`, y el front
  actual no lo consumia (`getGruposPrograma` sin uso), asi que cambiar su envoltura a `ApiPageResponse` no rompe a nadie. Las
  franjas se leen con la misma consulta de `uv_horario` que ya usa `GET /grupos` (`GrupoJpaRepository.conHorarios`), compartida.
- Punto 3: el consolidado vive en `ConsultasTransversales*`; se agrega un metodo y dos SP de lectura
  (`usp_consultar_asignaturas_ambito_consolidado`, `usp_consultar_docentes_ambito_consolidado`). No existia un endpoint con ese
  ambito: `/coordinador/docentes` lista el directorio del programa (no los docentes con grupos del periodo) y no sirve al decano.
  El decano usa la misma ruta con prefijo `/decano`.
- Punto 4: no habia columna ni regla de estado en BD (el front lo calculaba con `new Date()` cuando faltaba). La vista suma `estado`
  con la zona `TIEMPO/ZONA_HORARIA_SQLSERVER`, igual que `uv_grupo.grupoEstaHablitado`. Los codigos de estado son el enum del cable,
  como los `SI/NO` de `uv_grupo`. El SP del administrador ya entrega `esVigente` y no cambia; todos los consumidores del front del
  DTO pasan por una sola funcion (`normalizarPeriodoAcademico`).
- Punto 5, precedente de "listado + resumen": `GET /coordinador/docentes/resumen` (HU095), un endpoint de resumen aparte. No se
  inventa otra estructura: cada total sale del `totalElementos` de la misma lista pedida con el filtro que ya existe y `tamano=1`.
- Sin migracion de BD: objetos `CREATE OR ALTER` (vista y SP) sin datos nuevos.

## Fuera de alcance

Semaforo/nivel de asistencia y umbrales, KPI de la portada, matriz de asistencia, resumen de sesiones del grupo, listados del
decano/administrador (salvo el consolidado y el DTO de periodo) y el interceptor `role-scope`.

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W1A - PLAN (reclamos y padron del docente en servidor)

Fecha: 2026-11-16. Rama `feat/cierre-w1a-docente-reclamos` en los tres repositorios (BD, backend, frontend), desde `develop`.
Orden de mezcla: BD -> backend -> frontend. Cada PR es compatible hacia atras con el anterior y con el siguiente aun sin mezclar.

Regla que se hace cumplir (decision del responsable, 2026-11-16): el frontend es presentacion pura; no filtra, ordena, pagina ni
calcula datos de negocio ni genera identificadores de negocio. Filtro, busqueda, paginacion y folio los entrega el backend, que
los resuelve en stored procedures de consulta con el ambito del rol validado DENTRO del SP.

| Punto | Cambio | Capas |
| :--- | :--- | :--- |
| 1 | `GET /docente/reclamos` acepta `busqueda` (opcional) y la resuelve el SP | BD, backend, frontend |
| 2 | `GET /docente/reclamos` acepta `grupoId` (opcional); el front deja de recorrer todas las paginas y filtrar en cliente | BD, backend, frontend |
| 3 | Folio del reclamo: la BD ya lo persiste (`SolicitudRevisionAsistencia.nombre`, `REC-<8 hex>`, NOT NULL) y el DTO ya lo entrega como `nombre`; el front lo muestra y deja de componerlo | frontend (sin cambio de BD ni backend) |
| 4 | Padron del grupo paginado y con busqueda para el docente titular | BD, backend, frontend |

## Decisiones

- Punto 4 en una ruta nueva `GET /api/v1/docente/grupos/{grupoId}/estudiantes`: `GET /api/v1/grupos/{grupoId}/estudiantes` esta
  congelado en el Golden Path (WORM, `openapi-golden-path.yaml` + `OpenApiGoldenPathConformanceTest` fija la firma
  `listarEstudiantesGrupo(UUID)`), y lo consumen tambien `attendance-control` (matriz, otro paquete) y el coordinador. La ruta
  nueva reutiliza el mismo SP (`usp_consultar_estudiantes_grupo_ambito_paginado`, con `@busqueda` nuevo). `/estudiantes/busqueda`
  (documento exacto para matricular) tiene otro proposito y no se toca.
- `busqueda` de reclamos busca lo mismo que filtraba el front: nombre del estudiante, nombre y codigo de la asignatura y texto de
  la justificacion (precedente: `teacher-claims.component.ts`). La del padron compara nombre y documento (precedente:
  `usp_buscar_estudiantes_institucion_paginado`, `CHARINDEX`).
- `GET /docente/reclamos` conserva `ApiListResponse` (`exitoso`, `datos`, `total`): cambiarlo a `ApiPageResponse` rompe al front
  actual. La ruta nueva del padron usa `ApiPageResponse`.
- Sin migracion de BD: solo cambian firmas de SP con parametros opcionales al final (`CREATE OR ALTER`), como los cambios de SP recientes.

## Fuera de alcance

Matriz de asistencia, interceptor `role-scope`, indicadores/KPI y cualquier otro listado.

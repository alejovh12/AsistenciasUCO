---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W3M - PLAN (matriz de asistencia del docente en servidor)

Fecha: 2026-11-16. Rama `feat/cierre-w3m-matriz-asistencia` en los tres repositorios (BD, backend, frontend), desde `develop`.
Orden de mezcla: BD -> backend -> frontend. Cada PR es compatible hacia atras con el anterior y con el siguiente aun sin mezclar.

Regla que se hace cumplir (decision del responsable, 2026-11-16): el frontend es presentacion pura; no filtra, ordena, pagina, cruza ni
calcula datos de negocio, y no deduce estados con el reloj del navegador. Lo entrega el backend, resuelto en stored procedures con el
ambito validado dentro del SP.

| Punto | Cambio | Capas |
| :--- | :--- | :--- |
| 1 | Matriz estudiantes x sesiones del grupo cruzada en la base, paginada por estudiante y con busqueda (`GET /docente/grupos/{id}/matriz-asistencia`) | BD, backend, frontend |
| 2 | La sabana del docente pagina y busca en el servidor (matriz + resumen por horas con `busqueda`) en vez de `slice` del padron | BD, backend, frontend |
| 3 | Resumen de sesiones del grupo (`GET /docente/grupos/{id}/resumen-sesiones`): estado temporal, conteos, en riesgo y promedio desde la base; el front elimina `resumirSesiones` / `sesionDeHoy` y los conteos por sesion | BD, backend, frontend |

## Decisiones

- Rutas nuevas bajo `/docente/**`: `GET /grupos/{id}/estudiantes`, `GET /grupos/{id}/asistencias` y `GET /api/v2/sesiones/grupo/{id}` siguen
  igual (Golden Path / consumidores de otros roles).
- La matriz sale de `uv_asistencia_sesion_horas` (DEC-066), la misma vista del resumen por horas y los reportes: no se reimplementan reglas.
- El estado temporal se calcula en UTC con `SYSUTCDATETIME()`, igual que `uv_cumplimiento_sesion_grupo`; ver CONTRACT seccion 2.
- Dos verdades distintas (cliente vs base): se usa la de la base y queda documentada la diferencia visible (CONTRACT seccion 4).
- La toma de asistencia en vivo (lista de una sola sesion, marcas locales sin guardar) no cambia: sigue pidiendo padron y asistencias de la
  sesion elegida. Solo desaparece el cruce historico estudiantes x sesiones.
- Sin migracion de BD: objetos `CREATE OR ALTER` con parametros opcionales al final.

## Fuera de alcance

Ver CONTRACT seccion 6.

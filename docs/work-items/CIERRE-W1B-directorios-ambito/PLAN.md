---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-16
---

# PLAN — CIERRE-W1B (directorios de personas con ámbito y paginación)

Decisiones del responsable (2026-11-16): el frontend es presentación pura (no filtra, ordena, pagina ni deduce datos); el
coordinador ve solo los docentes vinculados a sus programas, el administrador todos y el decano solo los de su facultad.

| # | Punto | Capa | Decisión |
|---|---|---|---|
| 1 | Directorio `/api/v1/docentes/**` sin ámbito | BE (+ catálogo de accesos, docs, front) | Retirar las cinco rutas (ver CONTRACT §1). No hay SP nuevo: los consumidores pasan a los endpoints con ámbito que ya existen. |
| 2 | Selector de personas en servidor | FE | `user-picker-modal` busca y pagina contra `/decano/docentes` o `/admin/docentes` según un `input()` de fuente. |
| 3 | Coordinadores del decano con filtros reales | BD + BE + FE | SP nuevo `usp_consultar_coordinadores_facultad_paginado`; `GET /decano/coordinadores` suma `busqueda`, `estaActivo`, `sinPrograma`. |
| 4 | Nombres y apellidos desde el backend | BD + BE + FE | Las vistas/SP ya tienen `primerNombre..segundoApellido`; se exponen en los DTO y el front deja de partir cadenas. |
| 5 | Perfil por nombre aproximado en RBAC | BD | Se elimina el `LIKE` con comodines: comparación exacta (código o nombre completo del perfil, sin distinguir mayúsculas) y se ignoran elementos vacíos. Ver CONTRACT §5. |

Orden de mezcla: BD → backend → frontend.

## Método
TDD por capa. BD: `test/test_w1b_directorios.sql` registrado en `test_suite.sql` y `test_summary.ps1`. Backend: pruebas de
caso de uso, adaptador, controlador y reglas de seguridad por ruta y rol. Frontend: specs de servicio y componente.

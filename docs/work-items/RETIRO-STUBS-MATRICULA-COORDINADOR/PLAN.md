---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-07
---

# PLAN — Retiro de los stubs de matrícula del coordinador (HU156 cubierta)

## Decisión de John (2026-11-07)

Directriz: no se aceptan perfiles ni se crean usuarios "por crear". La primera alta de un usuario o perfil va atada a
su ámbito: administrador a una universidad, decano a una facultad, coordinador a un programa, y docente y estudiante a
un grupo.

HU156 ("Inscribir a un nuevo estudiante en la universidad") pedía admitir al estudiante en un programa, sin grupo, y
eso crearía un estudiante suelto. Lo que sí cumple la directriz ya existe y está CERTIFICADO:

- HU050: el docente matricula al estudiante nuevo en su grupo. `usp_registrar_estudiante_en_grupo` acepta
  `DOCENTE,COORDINADOR`, así que el coordinador también puede inscribir estudiantes en los grupos de su programa, como
  pide `HOJA_DE_RUTA_JERARQUIA_ROLES.md` (Infra). Usa `POST /api/v1/grupos/{grupoId}/estudiantes`.
- HU176B: el estudiante se registra solo con el QR del grupo.

HU156 queda cubierta por HU050/HU176B y no lleva código nuevo. Al revisar el catálogo de acceso apareció
`POST /api/v1/coordinador/grupos/{grupoId}/estudiantes` con el rol ADMINISTRADOR, aunque la jerarquía no le da ese
alcance. La ruta era un stub 501 sin uso, igual que otras 3 del mismo controlador, ya reemplazadas por flujos
certificados. John aprobó retirarlas.

## Alcance

| Capa | Cambio |
|---|---|
| Backend | Se retiran de `CoordinadorPortalController` los 4 endpoints 501: `POST /api/v1/coordinador/grupos/{grupoId}/estudiantes`, `DELETE /api/v1/coordinador/grupos/{grupoId}/estudiantes/{estudianteId}`, `GET /api/v1/coordinador/solicitudes-matricula` y `PATCH /api/v1/coordinador/solicitudes-matricula/{id}`. También salen sus 4 rutas de `access-catalog.json` |
| Frontend | Se retiran de `CoordinatorManagementService` las llamadas sin pantalla a `GET`/`PATCH /coordinador/solicitudes-matricula` (cargar y resolver solicitudes del coordinador) |
| BD | Sin cambios |
| Infra | HU-CATALOG: HU156 queda cubierta por HU050/HU176B |

Fuera de alcance: `SecurityConfig` deja pasar al ADMINISTRADOR por todo `/api/v1/coordinador/**`. Las rutas reales
igual lo frenan en el SP (exige perfil COORDINADOR), pero el permiso es más amplio que la jerarquía. Es configuración de
seguridad (José Alejandro) y se le avisa en el mensaje consolidado del equipo.

## PDP

Rutas retiradas, para el `--prune` (solo con aprobación de John):

- `POST /api/v1/coordinador/grupos/{grupoId}/estudiantes`
- `DELETE /api/v1/coordinador/grupos/{grupoId}/estudiantes/{estudianteId}`
- `GET /api/v1/coordinador/solicitudes-matricula`
- `PATCH /api/v1/coordinador/solicitudes-matricula/{id}`

## Pruebas

- `CoordinadorPortalControllerTest.stubs_de_matricula_y_solicitudes_del_coordinador_retirados_no_tienen_ruta`: las 4
  rutas responden 404.
- `AccessCatalogConsistencyTest`: el catálogo y los controladores coinciden.
- Frontend: `coordinator-management.service.spec` sin las llamadas retiradas.

## Rollback

Revertir el commit en backend y frontend.

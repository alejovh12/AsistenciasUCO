---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# PLAN — HU006: consulta del perfil propio (solo GET)

## 1. Identidad y objetivo

- **Historia:** HU006 "Consultar perfil y ficha de información personal".
- **Work Item ID:** `HU006-perfil-usuario`
- **Fecha:** 2026-10-27
- **Base Git:** `develop`, rama `feature/HU006-perfil-usuario` en los 3 repos:
  - DB `fec4503`
  - backend `84ac59d`
  - frontend `f010db3`
- **Clase de cambio:** funcional aditivo de solo lectura (1 SP y 1 GET). Ninguna tabla cambia.
- **Alcance aprobado por John (2026-10-27):** solo el GET. El PUT sigue en 501 porque toca la identidad y se coordina con Sergio.

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| `GET/PUT /api/v1/usuarios/perfil` respondían 501 | `UsuarioController`, [TD-049](../../baseline/TECHNICAL_DEBT.md#td-049) |
| `GET /api/v1/auth/me` solo devuelve `idUsuario` y los roles efectivos | `AuthController`, `AuthMeResponse` |
| El frontend ya consumía `/usuarios/perfil` (nombres, documento, correo) detrás del flag `USER_PROFILE_ENDPOINT_ENABLED` (configuración de Infra) | `AuthService#fetchProfileFromBackend` |
| La fila del catálogo de acceso ya existía (`AUTHENTICATED`, los 5 roles) | `infra/security-baseline/access-catalog.json` |

### Criterios de aceptación

1. **Datos personales:** el usuario autenticado consulta su perfil y ve los datos de la DB: tipo y número de documento, nombres, correo (confirmado o no) y estado de la cuenta. Nunca se expone la contraseña.
2. **Vínculos institucionales:** el perfil incluye un vínculo por rol:
   - estudiante y docente: los programas (el docente con su tipo de vinculación y estado);
   - coordinador: el programa que coordina;
   - decano: su facultad;
   - administrador: su institución.
3. **Ámbito:** solo se ve el perfil propio. El endpoint no recibe un id. Un usuario institucional inexistente responde 404.
4. **Frontend:** "Mi perfil" agrega la sección "Expediente y vínculos institucionales" con datos del servidor. La edición sigue deshabilitada.

## 2. Alcance

| Capa | Cambio | Archivos |
|---|---|---|
| DB | SP `usp_consultar_perfil_usuario` (una fila por vínculo con los datos personales); permiso aditivo; prueba | `schema/stored-procedures/usp_consultar_perfil_usuario.sql`, `schema/security/rol_asistencias_runtime_hu006.sql`, `test/test_usp_consultar_perfil_usuario.sql`, `test_suite.sql`, `test/test_contract_closure.sql` |
| Backend | Feature `usuario/consultarperfil`; `PerfilUsuarioQueryPort` + `PerfilUsuarioJpaRepository` (agrupa los vínculos); `UsuarioPerfilController` (`GET /api/v1/usuarios/perfil`); `PerfilUsuarioWiringConfiguration` (archivo propio); se **quita el stub 501 del GET** en `UsuarioController` y sus 2 pruebas de 501; texto de `ownership` en la fila del catálogo; repositorios 47 → 48 | NUEVOS + `UsuarioController`, `UsuarioControllerTest`, `access-catalog.json`, `JpaRepositoryBeanUniquenessTest` |
| Frontend | `PerfilService`; `ProfileLinksComponent` dentro de `ProfileComponent`; mock | `core/services/perfil.service.ts`, `features/profile/**`, `mock-backend.interceptor.ts` |

**No alcance:**
- el PUT (sigue en 501; TD-049 queda parcial);
- `SecurityConfig`;
- el flag `USER_PROFILE_ENDPOINT_ENABLED`, que es Infra (sin tocar: la sección nueva consulta el endpoint directamente);
- tablas;
- `UsuarioWiringConfiguration` (identidad).

## 3. Seguridad y datos personales

- **Autorización:** la ruta exige autenticación (`AUTHENTICATED`, los 5 roles). El SP lee solo el `idUsuarioEjecutor` resuelto del token; no hay parámetro para consultar a otro usuario.
- **Datos expuestos:** son los del propio usuario. La contraseña nunca se lee: el SP usa `uv_usuario`, que la enmascara, y no la devuelve.
- **Observabilidad:** la correlación se propaga al SP y la operación queda registrada como `consultarPerfilUsuario`.

## 4. Decisiones

- **D1:** Un SP con una fila por vínculo (los datos personales se repiten). El repositorio agrupa en un perfil con su lista de vínculos. Sin vínculos, devuelve una sola fila con el vínculo en NULL.
- **D2:** Un controlador y un wiring propios (`UsuarioPerfilController`, `PerfilUsuarioWiringConfiguration`), para no ampliar el constructor ni el wiring de `UsuarioController` (provisión de identidad).
- **D3:** La sección de vínculos es un componente aparte (`ProfileLinksComponent`), para no reescribir `ProfileComponent`. `AuthService` y su flag quedan como estaban.

## 5. Riesgos, rollback y parada

- **PDP:** la ruta ya figuraba en el catálogo. Solo cambia el texto de `ownership`, así que no requiere reaprovisionar rutas nuevas.
- **Rollback:**
  - revertir los commits;
  - `DROP PROCEDURE`;
  - `REVOKE`;
  - restaurar el stub 501.
- **Parada:** no implementar el PUT en esta rama.

## 6. DoR

READY: alcance "solo GET" aprobado por John. Sin tablas nuevas. Objetos congelados intactos.

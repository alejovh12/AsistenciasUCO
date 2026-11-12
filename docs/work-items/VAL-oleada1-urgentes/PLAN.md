---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# PLAN — VAL-oleada1-urgentes (backend)

Origen: `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`. Rama `fix/VAL-oleada1-urgentes` (desde develop, base `4877c2dfed6d`).

| Ítem | Brecha | Cambio |
|---|---|---|
| SEC-04 | Contraseñas literales en `scratch/*.ps1` | Los 7 scripts leen `MSSQL_SA_PASSWORD`, `E2E_USER_PASSWORD` y (hu091/092/093) `E2E_DOCENTE_PASSWORD`; sin valores por defecto y con error explícito si falta alguna. `scratch/README.md` lista las variables. Los scripts de `infra/security-baseline/scripts` no tenían literales. |
| EST-02 | java:S5998 en `ValidationPatterns`, javabugs:S2259 en `SecurityErrorResponseWriter`, githubactions:S8233 en `security.yml` | `PERSON_NAME` y `EMAIL` sin cuantificadores anidados (una clase de caracteres repetida más lookarounds), mismo conjunto aceptado/rechazado. `safePath` nunca devuelve null. Permisos del workflow reducidos a `contents: read`; `security-events: write` queda solo en el job `codeql` (los otros jobs ya declaraban los suyos). |
| OBS-08 | Prometheus raspaba `:8080` | `management.server.address: ${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}`; `prometheus.yml` del BE apunta a `host.docker.internal:8090`; `docs/observability/README.md` documenta `MANAGEMENT_SERVER_ADDRESS=0.0.0.0` solo en desarrollo. El repo Infra no se toca (su `prometheus.yml` sigue pendiente de su dueño). |

Fuera de alcance: rotación de credenciales que coincidan con entornos reales; contraseñas citadas en documentos de work items anteriores; repo Infra y BD.

## ROL-05: retiro de POST /api/v1/usuarios

Decisión de John (2026-11-12, ítems ROL-05 y ROL-11 de la lista de chequeo). `POST /api/v1/usuarios` estaba en `.authenticated()`, abierto a los cinco roles en `access-catalog.json` sin titularidad, no pasaba el ejecutor y `ProvisionarUsuarioUseCaseImpl` creaba el usuario (`usp_sincronizar_usuario`) y la cuenta en el proveedor de identidad. Ninguna pantalla del frontend lo usaba (la ruta `/register` del frontend ya redirigía a `/login`). El alta legítima va por los flujos con ámbito: crear decano, crear coordinador, vincular docente, admitir estudiante y registrar estudiante en grupo.

| Qué | Cambio |
|---|---|
| Ruta | Se borran `UsuarioController` y, en `controller/usuario`, `mapper/CrearUsuarioHttpMapper`, `request/CrearUsuarioRequest`, `response/CrearUsuarioResponse` y `validation/CrearUsuarioRequestValidator`. Se conserva `UsuarioPerfilController` (`GET /api/v1/usuarios/perfil`, HU006). |
| Casos de uso | Se borran los paquetes `application/features/usuario/provisionarusuario` y `application/features/usuario/crearusuario` (este último solo lo usaba el primero) y `UsuarioWiringConfiguration` (solo cableaba esos dos). Se conservan `UsuarioRegistroDomain`, `PasswordRegistroRule`, `NombrePersonaRule`, `UsuarioErrorCode` y `PasswordEncoderPort`, que usan los altas con ámbito. |
| Puerto y adaptador | `UsuarioRepositoryPort.crearUsuario` y su implementación en `UsuarioJpaRepository` (`EXEC dbo.usp_sincronizar_usuario`) se retiran con `CrearUsuarioRepositoryDTO` y `CrearUsuarioRepositoryProjection`. `UsuarioJpaRepository` queda de solo consulta y su constructor ya no recibe `JpaProcedureExecutor`. Las consultas del puerto siguen igual. |
| Seguridad | Se quita la regla de `SecurityConfig` y la fila de `infra/security-baseline/access-catalog.json`. El OpenAPI (`openapi-golden-path.yaml`) no tenía la ruta: no cambia. |
| Pruebas | Se borran las de las clases retiradas (`UsuarioControllerTest`, `CrearUsuarioRequestValidatorTest`, pruebas de `crearusuario` y `provisionarusuario`, `UsuarioJpaCommandPatternTest`, `UsuarioPasswordHashSqlServerIT`). Se ajustan `RegistrarEstudianteDomainTest` (compara con `UsuarioRegistroDomain`), los dobles de `UsuarioRepositoryPort`, `SqlStoredProcedureContractIT`, `UsuarioPlanEstudioProvidersSqlServerIT` y `AcademicUserCommandsSpParityIT` (sin el escenario de usuario). Nueva: `AltaUsuarioSinAmbitoRetiradaTest`. |
| Documentos | `HTTP_AS_IS_MATRIX.md`, `keycloak-identity-provider.md`, `runtime-security-provider-architecture.md` y `TECHNICAL_DEBT.md` (TD-013) marcan la ruta como retirada. |
| BD | Migración 39 (`Repositorio_GestionAsistenciadb`, rama `fix/VAL-oleada1-urgentes`): retira `usp_sincronizar_usuario` y su `GRANT EXECUTE`. No se toca `usp_sincronizar_usuario_interno`. |

Pendiente fuera de este repo: retirar la ruta del PDP con `provision_asistencias.py` y `--prune` (aprobado por John; lo ejecuta José Alejandro).

Pendiente en este repo: `infra/security-baseline/scripts/e2e_provisioning.py` y `e2e_profile_assignment.py` crean la cuenta de prueba con `POST /api/v1/usuarios`; hoy fallarán con 404. Hay que pasarlos a un alta con ámbito (por ejemplo crear decano) o retirarlos; no se cambiaron aquí.

### Validación ROL-05

| Prueba | Resultado |
|---|---|
| `AltaUsuarioSinAmbitoRetiradaTest` (4): `POST /api/v1/usuarios` con cada uno de los cinco roles y con un token sin rol responde 404 o 405 sin invocar casos de uso; sin Bearer, 401; la ruta no está en el inventario de Spring MVC ni en el catálogo; las clases retiradas no existen y el puerto ya no tiene `crearUsuario` | PASS |
| `AccessCatalogConsistencyTest` (3), `OpenApi*Test` (22), `SwaggerUi*Test` | PASS |
| `RegistrarEstudianteDomainTest`, `RegistrarEstudianteUseCaseImplTest`, `ReenviarVerificacionCorreoUseCaseImplTest`, `UsuarioPerfilControllerTest`, `CoreViewJpaQueryErrorSemanticsTest`, `Culm001AJpaRepositoriesTest`, `JpaQueryAdapterContractTest`, `DbExceptionTranslatorTest`, `AuditInterceptorTest` | PASS |
| Pruebas de arquitectura (`co.edu.uco.asistenciasuco.architecture`, 109) | PASS |
| Compilación de todas las pruebas (incluye los `*IT` ajustados) | PASS. Los `*IT` no se ejecutaron (requieren SQL Server con la migración 39). |

## SEC-04: contraseñas citadas en documentos de work items

Los documentos `HU076-HU077-HU078-programa-academico-decano/TEST_PLAN.md`, `HU121-HU122-gestion-coordinador/TEST_PLAN.md`, `IAM-001-ciclo-vida-cuenta-identidad/CONTRACT.md`, `IAM-001-ciclo-vida-cuenta-identidad/PLAN.md` e `IDENT-001-correo-institucional-verificacion/MANUAL_TEST_GUIDE.md` citaban contraseñas de prueba. Cada valor se reemplazó por la variable de entorno que documenta `scratch/README.md` (`MSSQL_SA_PASSWORD` o `E2E_USER_PASSWORD`). Los valores siguen en el historial de git: si alguno coincide con un entorno real, hay que rotarlo.

### Seguimiento: scripts e2e y contraseñas citadas (2026-11-12)

- `infra/security-baseline/scripts/e2e_provisioning.py` y `e2e_profile_assignment.py` ya no usan `POST /api/v1/usuarios`: crean la cuenta sintética con el alta con ámbito `POST /api/v1/admin/decanos` (ADMINISTRADOR) y `facultadNueva` (facultad propia del E2E en la primera institución activa). `e2e_safety.py` gana `delete_created_faculty`, que borra esa facultad antes del usuario (`Facultad.decano` referencia a `Decano`) y deja los conteos como al inicio; su comentario ya cita la ruta nueva. Validados solo con `py_compile`; no se ejecutaron (requieren el laboratorio). En `e2e_profile_assignment.py` la cuenta ahora nace con el rol de decano, así que `initial_roles` (que ya se leía de Keycloak) lo incluirá: conviene confirmarlo en la primera corrida.
- Se quitó del historial documental la plantilla de la contraseña por defecto del coordinador en `HU121-HU122`, `HU062-HU089-HU063-HU090-HU091`, `HU062-HU093` y `REG-000` ("plantilla retirada; ver historial"). Búsqueda final de contraseñas literales en `.md`, `.ps1`, `.py`, `.yml` y `.sh` versionados: sin casos.

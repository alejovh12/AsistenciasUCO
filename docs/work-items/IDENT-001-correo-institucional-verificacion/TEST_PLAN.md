---
status: frozen-approved
type: test-plan
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-31
---

# TEST_PLAN — IDENT-001: Correo institucional y verificación del correo

**Estado:** `RED_PLANNED` (congelado con el CONTRACT). Se deriva del [CONTRACT](CONTRACT.md) y de los criterios CA-01…CA-11 del [PLAN](PLAN.md). Orden: DB → Backend → Frontend (bloqueado). RED real antes de GREEN; el implementador no modifica las pruebas RED.

## Patrones a reutilizar
- BD: `test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql`, pruebas de NOTIF (`test_notif002a_plantillas_contacto.sql`).
- Backend: `RegistrarEstudianteUseCaseImplTest`, `GrupoControllerWriteEndpointsTest`, `FakeKeycloakAdminServer`, `NotificacionSmtpGreenMailIT`, `SmtpNotificacionChannelSenderTest`, ArchUnit AR-07..AR-10.

## 1. Base de datos (nuevo archivo `test_ident001_correo_verificacion.sql`, registrado en la suite)

| ID | Requisito | Acción | Resultado esperado |
|---|---|---|---|
| DB-D01 | CA-03 función | `ufn_correo_cumple_dominio_institucional` con `a@uco.edu.co`, `A@UCO.EDU.CO `, `a@a.uco.edu.co`, `a@uco.edu.co.evil.com`, `@uco.edu.co`, `a@@uco.edu.co`, `NULL`, dominio vacío | 1,1,0,0,0,0,0,0 |
| DB-D02 | CA-01 | Registrar estudiante nuevo con `x@gmail.com` | `exitoso=0`, `VCE_001`; **0** filas nuevas en Usuario/Estudiante/EstudianteGrupo |
| DB-D03 | CA-02 | Cambiar el parámetro a `@otra.edu` y registrar `a@otra.edu` | Pasa; `a@uco.edu.co` falla; se restaura el parámetro |
| DB-D04 | D5 | Re-registrar a un estudiante existente (mismo correo y documento) con correo de otro dominio | No se revalida; matrícula normal |
| DB-D05 | CA-04 | Registro válido | `Usuario.correoConfirmado = 0` |
| DB-T01 | Emisión | `usp_emitir_token_verificacion_correo` | Una fila con `tokenHash`, expiración = ahora + `TTL_HORAS`; sin el token en claro |
| DB-T02 | Un activo por usuario | Emitir dos veces | El primero queda con `fechaRevocacionUtc`; solo el segundo es válido |
| DB-T03 | Ya verificado | Emitir para un usuario con `correoConfirmado=1` | `VCE_005`, sin filas |
| DB-V01 | Validar válido | Token vigente | Éxito con `idUsuario`; **no** consume |
| DB-V02 | CA-07 uniformidad | Token inexistente / vencido (fixture) / usado / revocado / usuario inactivo | Mismo código `VCE_002`, mismo mensaje |
| DB-C01 | CA-06 | Confirmar token vigente | `fechaUsoUtc` marcada, `correoConfirmado=1`, mensaje `VCE_006` |
| DB-C02 | Un solo uso | Confirmar dos veces | La segunda: `VCE_002` |
| DB-C03 | Atomicidad | Falla forzada tras marcar el uso | Rollback total, `@@TRANCOUNT=0` |
| DB-C04 | Concurrencia | Dos confirmaciones simultáneas del mismo token | Exactamente una exitosa |
| DB-R01 | CA-09 ámbito | Docente titular / otro docente / coordinador del programa / coordinador de otro programa | Éxito / `VCE_004` / éxito / `VCE_004` |
| DB-R02 | Ya verificado | Reenvío a un correo confirmado | `VCE_005` |
| DB-R03 | Espera mínima | Reenviar dos veces seguidas | La segunda: `VCE_003` |
| DB-P01 | Permisos | Rol runtime | Puede ejecutar los 4 SPs; sin DML directo sobre `TokenVerificacionCorreo` |
| DB-G01 | Gate | Suite completa | `DB GATE PASS`, `@@TRANCOUNT=0` |

## 2. Backend — unitarias

| ID | Requisito | Resultado esperado |
|---|---|---|
| BE-P01 | `DominioCorreoInstitucionalPolicy`: mismos casos que DB-D01 (parametrizado, incluye mayúsculas, espacios, doble `@`) | Misma salida que la función SQL |
| BE-P02 | Dominio leído de `ParameterCatalogPort` | Cambiar el valor del puerto cambia el resultado; sin dominio quemado |
| BE-U01 | `RegistrarEstudianteUseCaseImpl` alta nueva con correo inválido | Excepción de validación (`VCE_001`); `GrupoRepositoryPort` e Identity **no** se invocan |
| BE-U02 | Alta nueva válida | SP → `crearCuenta(requiereVerificacionCorreo=true)` → `NotificacionPort.notificar(VERIFICACION_CORREO)`; el DTO y el aviso **no** contienen token ni correo |
| BE-U03 | Usuario existente | No se valida el dominio ni se notifica |
| BE-U04 | Falla del `NotificacionPort` | El registro termina bien (mejor esfuerzo) |
| BE-U05 | Falla de Identity | Error controlado como hoy; no se notifica |
| BE-I01 | `KeycloakIdentityProviderAdapter` con `requiereVerificacionCorreo=true` (con `FakeKeycloakAdminServer`) | Payload con `enabled=false` y `emailVerified=false`; contraseña establecida como hoy; con `false`, payload idéntico al actual |
| BE-I02 | Cuenta ya existente en Keycloak | No se modifica |
| BE-I03 | `confirmarCorreoCuenta` | `PUT` con `enabled=true` y `emailVerified=true`; idempotente |
| BE-V01 | `VerificarCorreoUseCase` éxito | Orden validar → Keycloak → confirmar |
| BE-V02 | CA-11 Keycloak falla | Error controlado y **no** se invoca `confirmar` |
| BE-V03 | Token inválido | Excepción uniforme; no toca Keycloak |
| BE-V04 | El hash lo calcula el adaptador | Application no manipula el SHA-256 |
| BE-M01 | `VerificacionCorreoMarcadorProvider` | Token de 32 bytes (43 caracteres base64url), enlace con `#token=`, emite el hash correcto; no escribe el token en logs |
| BE-M02 | Emisión con `VCE_005` | `NotificacionNoEntregableException` |
| BE-M03 | Fallo transitorio de BD en la emisión | Excepción reintentable |
| BE-M04 | Reintento | Cada intento emite un token nuevo |
| BE-S01 | `SmtpNotificacionChannelSender`: tipo `VERIFICACION_CORREO` con `require-confirmed-email=true` | Se envía igual |
| BE-S02 | Otros tipos | Sin cambios de comportamiento |
| BE-S03 | CA-10 | Logs y métricas no contienen el token ni el correo (captura de appender) |
| BE-R01 | Rate limit | La solicitud 21 en 60 s → 429; `X-Forwarded-For` se ignora; memoria acotada |
| BE-C01 | Controladores | `GET configuracion`: roles; `POST verificar-correo`: formato de token (longitud/alfabeto) antes del caso de uso; `reenviar-verificacion`: 200/403/404/409/429 |
| BE-C02 | `SecurityConfig` | Solo `POST /api/v1/public/verificar-correo` es público; `POST /api/v1/public/otra` → 401; `GET configuracion` con `ESTUDIANTE` → 403 |
| BE-A01 | ArchUnit | `application` sin `infrastructure`, `JdbcTemplate`, `EntityManager`; módulo `notification` aislado (AR-07..AR-10); el proveedor de marcadores no es referenciado desde fuera del paquete |
| BE-T01 | Plantilla `VERIFICACION_CORREO` | Render con `nombre` y `enlace`; HTML escapado; texto plano presente; sin `enlace` → cae a respaldo |

## 3. Backend — integración
| ID | Escenario | Resultado esperado |
|---|---|---|
| IT-V01 | Registro real → outbox | Fila `VERIFICACION_CORREO` sin token en `datos`, `asunto` ni `mensaje` |
| IT-V02 | GreenMail | Correo recibido con enlace `#token=`; en la BD solo el SHA-256 |
| IT-V03 | Verificación de extremo a extremo | Abrir el enlace → cuenta habilitada, `correoConfirmado=1`; reabrir → `VCE_002` |
| IT-V04 | Token vencido (fixture de fecha) | `VCE_002` uniforme |
| IT-V05 | Reenvío | Revoca el token anterior; segundo reenvío inmediato → 429/`VCE_003` |
| IT-V06 | Línea base | Unitarias 2.864 + nuevas; IT 258 + nuevas, sin regresiones |

## 4. Pruebas existentes a ajustar (solo dato de prueba; ver PLAN §4)
Se actualizan únicamente donde el correo de prueba no es institucional: `test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql`, `test_transaction_ownership.sql`, `verify_runtime_role_login.ps1`, `RegistrarEstudiante*Test`, `GrupoControllerWriteEndpointsTest`, `GrupoRepositorySqlServerIT`, `SesionGrupoCommandsSpParityIT` (GRP_04). Cada una se enumera en la sección «Tests existentes modificados» del PR, con el commit y la justificación. No se relajan aserciones.

## 5. Frontend (bloqueado: falta el repositorio)
Pruebas `.spec.ts` por componente: validación contra el dominio del endpoint (incluye mayúsculas y variantes inválidas), muestra de `mensajeUsuario` ante 400, página `verificar-correo` (lee y limpia el fragmento, no persiste el token, maneja 400/429/200), `OnPush`, cobertura de ramas.

## 6. E2E (fase 07)
Registro manual con GreenMail → correo → verificación → inicio de sesión con la contraseña del docente; vencimiento; reenvío; Keycloak caído durante la verificación; correo no institucional rechazado. Se ejecuta con el modo de pruebas de NOTIF y se documenta en `VALIDATION.md`, con cifras reales.

## 7. Entorno de ejecución (documentado)
En esta PC: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp -Djdk.net.hosts.file=C:\jtmp\hosts`, `.env` cargado solo en el proceso y claves de MinIO de Infra. La BD debe estar recreada desde el esquema del repositorio.

## 8. Estado de ejecución y desviaciones (2026-10-31)

Resultados y cifras reales en [VALIDATION](VALIDATION.md). Desviaciones respecto a este plan:

| Prueba planeada | Estado | Nota |
|---|---|---|
| DB-C03 (atomicidad con falla forzada) y DB-C04 (concurrencia de dos confirmaciones) | No automatizadas en T-SQL | Requieren inyectar un fallo o dos conexiones; la unicidad del uso la garantizan `UPDLOCK, HOLDLOCK` y el índice único filtrado. Seguimiento: script de concurrencia de dos conexiones (como `test_enroll.ps1`) |
| DB-V01/V02 y DB-R01 con `usp_validar_*` / `usp_preparar_*` | Ejecutadas con los SP renombrados | `usp_consultar_token_verificacion_correo` y `usp_consultar_reenvio_verificacion_correo` (CONTRACT §9, E1/E2) |
| BE-C01 `POST verificar-correo` valida el formato en el validator | Cubierta en el caso de uso | CONTRACT §9, E6 |
| BE-R01 429 del reenvío por espera mínima | 409 | CONTRACT §9, E5 |
| BE-C02 (`SecurityConfig` con `POST /public/otra` → 401) | Cubierta por `AccessCatalogConsistencyTest` | Solo la ruta de verificación es pública; el catálogo y la cadena de filtros deben coincidir |
| Sección 5 (frontend) | Pendiente | TD-081 |
| IT-V01…V08 | Ejecutadas (7 pruebas: V01/V02 y V03…V08) | `VerificacionCorreoGreenMailIT` |
| E2E | 23/23 | `scratch/test_ident001.ps1` (no versionado: contiene credenciales de desarrollo local) |

---
status: validated-local
type: validation
scope: database-backend
owner: fullstack-team
last-reviewed: 2026-10-31
---

# VALIDATION — IDENT-001: Correo institucional y verificación del correo (registro manual del estudiante)

## Dictamen

**VALIDADO EN LOCAL para base de datos y backend** (sin push, PR ni merge con `develop`). El frontend queda **pendiente** (TD-081): el repositorio no estaba disponible en este workspace. Todas las cifras se midieron en la corrida indicada; ninguna se tomó de otra.

Alcance validado (HU050, registro manual por el docente): el correo del estudiante nuevo debe terminar en el dominio configurado (`REGISTRO/DOMINIO_CORREO_ESTUDIANTE`, hoy `@uco.edu.co`), validado en el procedimiento almacenado, en el backend y expuesto al frontend por un endpoint; el estudiante recibe un enlace de un solo uso, su cuenta nace deshabilitada y se habilita al verificar. La contraseña se asigna como hasta ahora (TD-079).

## Comandos ejecutados

| Fecha / entorno | Comando | Resultado | Cifras |
|---|---|---|---|
| 2026-10-31, BD Docker local recreada desde el esquema del repo | `deploy_schema.ps1` + `test_summary.ps1` | **DB GATE PASS** | `TOTAL_EXECUTED=377`, `PASSED=376`, `FAILED=0`, `SKIPPED=1` (permitida), `SQL_ERROR_COUNT=0`, `@@TRANCOUNT=0` |
| 2026-10-31, Java 25 | `mvnw.cmd test jacoco:report` | **PASS** | `Tests run: 2944, Failures: 0, Errors: 0, Skipped: 0`; global LINE 93,68 % / BRANCH 84,14 % |
| 2026-10-31, BD local, entorno cargado sin BOM | `mvnw.cmd -Pintegration failsafe:integration-test failsafe:verify` | **PASS** | `Tests run: 265, Failures: 0, Errors: 0, Skipped: 5`; `VerificacionCorreoGreenMailIT` 7/7, `NotificacionSmtpGreenMailIT` 11/11, `NotificacionOutboxIT` 9/9 |
| 2026-10-31, backend en `:8090` + Keycloak real + sumidero SMTP | `scratch/test_ident001.ps1` | **PASS** | 23/23 (ver E2E) |

### Línea base anterior y diferencias
| Capa | Antes (NOTIF-002A) | Ahora | Diferencia |
|---|---|---|---|
| Gate de BD | 376 ejecutadas, 375 pasadas, 1 omitida | 377 / 376 / 1 | +1 archivo de prueba nuevo (`test_ident001_correo_verificacion.sql`) |
| Unitarias | 2.864 | 2.944 | +80 (política, verificación, reenvío, configuración, Identity, canal SMTP, renderer, controladores, filtro, helpers) |
| Integración | 258 ejecutadas, 7 omitidas | 265 ejecutadas, 5 omitidas | +7 (`VerificacionCorreoGreenMailIT`); 2 omitidas menos porque la BD local ahora tiene los datos que esas pruebas necesitan |

Cobertura de las clases nuevas del backend: 100 % de líneas y ramas (`CorreoInstitucionalPolicy`, `VerificarCorreoUseCaseImpl`, `ReenviarVerificacionCorreoUseCaseImpl`, `ConsultarConfiguracionRegistroEstudiante*`, `VerificacionCorreoMarcadorProvider`, `TokenVerificacionCorreoJpaRepository`, interactors).

## E2E (puerto 8090)

**Declaración:** ejecutado con `APP_ADAPTERS_SECURITY_PROVIDER=legacy` y `APP_ADAPTERS_IDENTITY_PROVIDER=legacy`, catálogos de parámetros y mensajes en `sqlserver`, JWT reales del Keycloak local (realm `asistencias-uco`, docente `maria.rostagno@uco.edu.co`) y provisionamiento real de cuentas en Keycloak con el service account `asistencias-backend-admin`. **No se probó contra el PDP de seguridad baseline ni contra Azure App Configuration.** El SMTP fue un sumidero local basado en GreenMail (`scratch/GreenMailSink.java`) en `127.0.0.1:3025`; no se envió ningún correo real (el usuario y la contraseña de Gmail se anularon para ese proceso). El usuario de prueba se eliminó de la BD y de Keycloak al terminar.

| ID | Resultado | Evidencia |
|---|---|---|
| E2E-01 | PASS | `GET /configuracion/registro-estudiante` → 200 con `@uco.edu.co`; sin token → 401 |
| E2E-02 | PASS | Correo `@gmail.com` → 400 `VCE_001` con «…terminar en @uco.edu.co.»; 0 filas creadas |
| E2E-03 | PASS | Registro institucional → 201; `correoConfirmado=0`; la cuenta de Keycloak responde `Account disabled` |
| E2E-04 | PASS | Llega 1 correo con asunto «Verifique su correo institucional», el nombre y el enlace `…/verificar-correo#token=<43 caracteres>`; el outbox no contiene el token ni el correo; 1 token activo en BD |
| E2E-05 | PASS | Reenvío inmediato → 409 `VCE_003` |
| E2E-06 | PASS | Reenvío tras la espera → 200; llega un segundo correo con un token distinto |
| E2E-07 | PASS | Token anterior (revocado) → 400 `VCE_002`; token inexistente y token de formato inválido responden **cuerpos idénticos** |
| E2E-08 | PASS | Verificar con el token vigente → 200 «Su correo fue verificado. Ya puede iniciar sesión.»; `correoConfirmado=1`; Keycloak pasa a `Account is not fully set up` (cuenta habilitada, contraseña temporal) |
| E2E-09 | PASS | Reutilizar el token → 400; reenviar a un correo verificado → 409 `VCE_005` |
| E2E-10 | PASS | 25 intentos seguidos al endpoint público → aparecen 429 (límite por IP) |
| E2E-11 | PASS | Limpieza de Keycloak y BD |

## Tests existentes modificados (para el PR)

Cambios por contrato formal (PLAN §4 / CONTRACT §6 y §9). Ninguno relaja una aserción ni elimina un caso.

| Archivo | Cambio | Justificación |
|---|---|---|
| `test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql` (DB) | El correo de prueba usa el dominio del parámetro en lugar de `@test.local` | El alta nueva exige correo institucional |
| `test_transaction_ownership.sql` (DB) | Ídem para `ownership.estudiante` | Ídem |
| `test_notif002a_plantillas_contacto.sql` (DB) | Los conteos «exactamente 4 plantillas» y «9 marcadores» se acotan a los 4 tipos originales | Entra el tipo `VERIFICACION_CORREO`; la aserción sigue exigiendo exactamente 4 y 9 de los tipos que verifica |
| `test_contract_closure.sql` (DB) | Dos SP de consulta (`usp_consultar_token_verificacion_correo`, `usp_consultar_reenvio_verificacion_correo`) entran a la lista de resultsets de lectura no canónicos | Devuelven `idUsuario`; el resultset canónico de 4 columnas es solo para comandos |
| `test_suite.sql` (DB) | Registro de `test_ident001_correo_verificacion.sql` | Prueba nueva |
| `RegistrarEstudianteUseCaseImplTest` | Las 11 construcciones pasan por un helper que arma el caso de uso con la política y el puerto de aviso | El constructor gana dos colaboradores; las aserciones no cambian |
| `GrupoWiringConfigurationTest` | La llamada al bean pasa los colaboradores nuevos | Cambio de firma del bean |
| `JpaRepositoryBeanUniquenessTest` | `EXPECTED_REPOSITORIES` 50 → 51 | Repositorio nuevo (`VerificacionCorreoJpaRepository`), como en cada work item anterior |
| `SesionGrupoCommandsSpParityIT` (GRP_04) | El correo usa el dominio configurado (leído de BD) en lugar de `@example.test` | El alta nueva exige correo institucional |

## Hallazgos durante la implementación

| Hallazgo | Resolución |
|---|---|
| Los nombres de savepoint de SQL Server admiten 32 caracteres | Nombres cortos `sp_ident_*` |
| `INSERT … EXEC` no admite `ROLLBACK` dentro del SP | `usp_confirmar_verificacion_correo` no escribe al rechazar, así que no necesita deshacer nada |
| AR-07 define el módulo de correo como `template`, `contact` y `channel.smtp` | Las clases del canal se ubicaron en `template`; la regla no se relajó |
| `StorageProviderIsolationRulesTest` rechaza nombres con «minio» y «Dominio» lo contiene | Renombrado a `CorreoInstitucionalPolicy` |
| El catálogo Java usa `MessageFormat` (`{0}`) y el de la BD `{}`: `VCE_001` salía con el marcador sin reemplazar | La política sustituye `{}`; el manejador global nunca muestra un marcador sin resolver y prefiere el mensaje de la excepción |
| `DbExceptionTranslator` usaba el mensaje genérico de validación para `VCE_002` | Códigos de error propios (`VerificacionCorreoErrorCode`) |
| GreenMail guarda una copia por buzón (To y BCC) y reordena su arreglo | La IT afirma «≥ 1»; el sumidero del E2E deduplica por `Message-ID` |
| El realm de Keycloak exige contraseñas de **12 o más caracteres** y el backend acepta 8: un alta con una clave corta termina en 500 tras confirmar la BD | Preexistente (TD-013/TD-015); el E2E usa una clave de 16. Conviene alinear la política del backend con la del realm |
| El proveedor de catálogos por defecto es Azure App Configuration | Registrado como TD-077 |

## Entorno de ejecución (documentado)
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp -Djdk.net.hosts.file=C:\jtmp\hosts` (la ruta temporal con espacio rompe los sockets del JDK 25 y el DNS inverso del equipo tarda ~5 s); `.env` cargado solo en el proceso; claves de MinIO de Infra; la BD debe estar recreada desde el esquema del repositorio (sin SP obsoletos) y con los datos de prueba que piden las pruebas de paridad.

## Pendientes y límites
- **Frontend (TD-081):** implementado y probado con pruebas unitarias (1359/1359) en el repositorio del frontend (commits `bb3651c`, `f954eb9`, sin push). Pendiente: prueba en navegador real contra backend y Keycloak, y su E2E.
- **Contraseña** definida por el docente (TD-079); **purga** de cuentas nunca verificadas (TD-078); **doble fuente** del dominio con Azure (TD-077); límite de intentos por instancia (TD-080); **HU176B** debe reutilizar el mecanismo (TD-082).
- **SMTP institucional y SPF/DKIM/DMARC** (TD-073/TD-074): ahora condicionan el registro, porque sin entrega del correo el estudiante no puede activar su cuenta.
- El correo de verificación sale por el módulo de NOTIF (no por Keycloak), así que el modo de pruebas con redirección sí lo cubre.

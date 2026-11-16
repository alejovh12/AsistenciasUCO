# FASE 2A — VALIDATION

Preflight: 2026-11-14; última actualización: 2026-11-15 (America/Bogota)
Estado: `IN_PROGRESS`; preflight seguro de inicio `BLOCKED`; browser/E2E `NOT_RUN`.

## Base observada

| Recurso | Estado/evidencia | Límite |
|---|---|---|
| Backend | `develop` `9a72fce693efffada6a55b2c6176e45e3031d43c`; detenido, puerto 8080 libre | No arrancado en esta validación. |
| Frontend | `develop` `91f89f5681c560703f94e7feda0205e1f35f5513`; puerto 4200 libre | Navegador integrado no abierto ni autenticado. |
| DB | `develop` `39da95388eb4606991e99af94f060a898bf38593`; contenedor `sql_server_asistencias` Up; listener 1433 | Se preservan cambios locales `.env.example` y `.env.template`; no se consultaron ni cambiaron fixtures. |
| Security Baseline | Keycloak/PDP healthy; PEP `GET /actuator/health` 200/UP; Redis/OPA/SurrealDB Up | Contenedores ejecutan `0.1.11`; README del snapshot documenta `0.1.13`. No se ejecutó aprovisionamiento, `--prune`, E2E ni cambios de perfiles; diferencia pendiente de owner externo. |
| Archivos | Backend conserva `?? .claude/launch.json`, `?? .codex/`; DB conserva `.env.example`, `.env.template` modificados | No se borraron, movieron ni alteraron. |
| Runtime | `java -version` predeterminado devuelve OpenJDK 17.0.18; está disponible `C:\Program Files\Java\jdk-25` | JDK 25 se usó solo para Maven; no se inició el servicio. `clean verify` recreó el JAR en `target/AsistenciasUCO-0.0.1-SNAPSHOT.jar`. |

## Comprobaciones operacionales realizadas

- `docker ps --no-trunc --format "{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}"`: lectura; SQL Server, Security Baseline, MinIO y ClamAV están arriba; no se arrancó ni detuvo ningún contenedor.
- `Get-NetTCPConnection -State Listen ...`: 1433/18080/18081/19090/3310/9000 activos; 8080 y 4200 sin listener.
- `java -version`: exit code 0, OpenJDK 17.0.18; runbook exige Java 25.
- Revisión del launcher oficial `infra/security-baseline/scripts/start-backend-baseline.ps1`: `%LOCALAPPDATA%\AsistenciasUCO\backend-run` no existe, así que su limpieza de JAR antiguos no tendría archivos que borrar en este primer arranque. No se invocó el script.
- Backup: `C:\Users\josev\AsistenciasUCO\db-backups` no existe. La búsqueda acotada encontró un backup de migración de 2026-10-28 en `.codex/dba-evidence-20261028/` y otro bajo evidencia de FIX-003; ninguno está identificado como backup de Fase 1. Un informe histórico P0 reportó siete backups en la ubicación anterior; no se recorrió OneDrive ni se movieron archivos. Integridad/restaurabilidad de un respaldo de Fase 1 no está comprobada.
- Jobs de inicio: rotación de tokens de asistencia y matrícula puede escribir desde el primer tick a los 5 s; alertas y festivos arrancan con primer tick a 1 min y pueden escribir DB/SSE; el outbox purga notificaciones terminales al iniciar y procesa lote inmediato. Configuración local indica dispatcher SMTP habilitado, host loopback:3025, modo de prueba y redirección activa hacia GreenMail local saludable; el sink retiene mensajes en memoria y no los reenvía. No se exponen host/cuenta de destino ni credenciales.

## Decisión de seguridad de arranque

**Backend no iniciado.** El respaldo requerido de Fase 1 no está localizado/verificado. Al iniciar el servicio, jobs potencialmente activos pueden rotar tokens, cerrar tomas, alterar alertas/sesiones o festivos y modificar/purgar outbox de la BD compartida; no se leyó el estado de filas elegibles y no se puede acotar ese efecto. SMTP sí está verificado contra GreenMail local en modo de prueba/redirección, por lo que la configuración actual no entrega correo externo. Security Baseline muestra diferencia entre versión documentada `0.1.13` y contenedores `0.1.11`; no se actualizó el componente externo.

No se desactivaron jobs ni se cambiaron configuración/seguridad para obtener un smoke test. Pendientes para reconsiderar el inicio: localizar y verificar el backup existente autorizado; acotar mediante consulta de solo lectura los datos que los jobs podrían consumir/cambiar y documentar la aceptación del efecto; resolver con el owner externo la diferencia de versión si afecta compatibilidad; documentar parada; volver a comprobar puerto 8080 y seleccionar JDK 25 justo antes del launcher. No crear backup/base/contenedor auxiliar.

## Pruebas de aplicación

| ID | Estado | Evidencia |
|---|---|---|
| OPS-01/02 | `BLOCKED` | Jobs y backup descritos arriba; no hay arranque. |
| AUTH-01/02/03 | `NOT_RUN` | Requieren Backend y navegador integrado. |
| ADM-01/02, DEC-01, COOR-01, DOC-01 | `NOT_RUN` | No se enviaron requests válidos ni se crearon datos/cuentas. Falta fixture con limpieza segura. |
| COOR-02 / F2A-DEF-001 | `PASS` (regresión unitaria) / `NOT_RUN` (HTTP y E2E) | RED focalizado: exit 1, 1 fallo causal/0 errores/0 skips; GREEN focalizado: exit 0, 1/1; suite `VincularDocenteProgramaUseCaseImplTest`: exit 0, 12/12. Hash, comandos y causalidad RED constan en `TEST_PLAN.md`. El endpoint real no se ejercitó. |
| F2A-DEF-002 | `PASS` (paridad unitaria) / `NOT_RUN` (HTTP live y E2E) | RED revisada: exit 1, 6 tests/3 fallos causales/0 errores/0 skips; GREEN focalizado: exit 0, 6/6. En cada alta cubre cuatro nombres y correo, máximos acolchados y max+1. Snapshot/hash RED en `TEST_PLAN.md`; suite completa también pasa. |
| VALID-01/02 | `PASS` (subset unit/contract) / `NOT_RUN` (navegador/API real) | Suite focalizada de validación, correo institucional, Bean Validation y controllers: exit 0, 211 tests/0 fallos/0 errores/0 skips. No incluye payloads contra el Backend en ejecución. |
| IDEM-01, DRIFT-01, STUD-01 | `NOT_RUN` | No se creó cuenta ni vínculo. Faltan Backend seguro, fixture con limpieza y login manual en navegador; el estado `ACTIVA` ante sincronización PDP pendiente necesita definición contractual. |

## Validación automatizada Backend (Java 25)

| Fecha/hora (Bogotá, 2026-11-14) | Comando / alcance | Resultado |
|---|---|---|
| 18:22:41 | `.\mvnw.cmd '-Dtest=VincularDocenteProgramaUseCaseImplTest#F2A_DEF_001_mismo_correo_y_vinculo_con_documento_distinto_es_conflicto_sin_mutaciones_ni_iam' test` (GREEN focalizado) | Exit 0; 1 test/1 pass/0 fallos/0 errores/0 skips. |
| 18:23:18 | `.\mvnw.cmd '-Dtest=VincularDocenteProgramaUseCaseImplTest' test` | Exit 0; 12 tests/12 pass/0 fallos/0 errores/0 skips; incluye retry de la misma identidad. |
| 18:24:28 | `.\mvnw.cmd '-Dtest=AdminPortalCrearDecanoContractTest,AdminAdmisionEstudianteControllerTest,DecanoPortalControllerTest,CoordinadorDocentesControllerTest,GrupoControllerTest,GrupoControllerWriteEndpointsTest,VerificacionCorreoControllersTest,VincularDocenteProgramaRequestValidatorTest,ValidationHelperDec073Test,AnotacionesDec073Test,BeanValidationFieldErrorMapperDec073Test,BeanValidationControllersTest,CorreoInstitucionalPolicyTest,AccessCatalogConsistencyTest,RbacSecurityFilterChainTest' test` | Exit 0; 211 tests/211 pass/0 fallos/0 errores/0 skips. |
| 18:37:08 | `.\mvnw.cmd clean verify` | Exit 0; 4533 tests/4533 pass/0 fallos/0 errores/0 skips. JaCoCo: líneas 94.90 % (22408/23612); ramas 86.46 % (7010/8108). Los gates de línea ≥80 % y rama ≥70 % pasaron. `clean` recreó solo artefactos ignorados de `target`; no alteró datos/volúmenes. |

El primer `.\mvnw.cmd verify` también pasó con 4533 tests, pero avisó que el `jacoco.exec` contenía clases de una compilación anterior. Se ejecutó `clean verify` como comprobación limpia y no repitió la advertencia. Estas ejecuciones cubren tests locales, arquitectura y gates JaCoCo; no certifican SQL Server, Keycloak/PDP, navegador, CI remoto ni proveedores reales.

## Validación GREEN — 2026-11-15 (America/Bogota)

- Java 25 (`C:\Program Files\Java\jdk-25`); la constraint compiló y pasó RED congelada con `& ([string]::Concat('.', [char]92, 'mvnw.cmd')) '-Dtest=AltaRequestLengthParityTest' test`: exit 0, 6 tests/6 pass/0 fallos/0 errores/0 skips.
- Regresión global: `& ([string]::Concat('.', [char]92, 'mvnw.cmd')) clean verify`, finalizó 17:27:51, exit 0; 4539 tests/4539 pass/0 fallos/0 errores/0 skips. Los 12 tests de `VincularDocenteProgramaUseCaseImplTest` también pasaron dentro de la suite.
- JaCoCo XML: LINE 22.416 covered / 1.200 missed = 94.92 %; BRANCH 7.019 / 1.093 = 86.53 %. Pasaron gates LINE ≥80 % y BRANCH ≥70 %. `clean` recreó solo `target` ignorado; sin cambios en datos/volúmenes.
- Alcance: unitarios, controllers con doubles/contextos de prueba, ArchUnit y gates de cobertura. No prueba proveedor real, SQL compartida, API live, navegador integrado, CI remoto ni `-Pintegration verify`.

## Hallazgos fuera de la corrección implementada

- `BLOCKED_BY_MISSING_EVIDENCE-2A-004`: `EstadoCuentaAprovisionada.ACTIVA` describe cuenta existente/habilitada según Identity Provider; `AccesoCuentaDTO.estado` compone DB+IdP+PDP. `docs/security/PDP_ADMIN_API_AUTHORIZATION_FINDING.md:64` espera `PENDIENTE_SINCRONIZACION` hasta confirmar perfil, pero es un security finding, no una definición aprobada del contrato público de `estadoCuenta`. El consumidor de admisión usa `estadoCuenta=ACTIVA` para mostrar éxito. Falta recuperar/versionar CULM-001 D.4 §5.3 o una decisión que aclare si el campo significa cuenta IdP o acceso efectivo. No cambiar el estado público por inferencia. Responsable: owner contractual/seguridad.
- F2A-DEF-002 confirmó la diferencia entre longitud cruda de `@Size` y `trim()` del validator propio en admisión de estudiante, alta de docente y alta de decano. Se corrigió con `LongitudTrasTrim` en Infrastructure Validation y `BeanValidationCodes.InvalidLength` para la clasificación prevista. No se envió una request a API live; status/correlation de esos casos siguen `NOT_RUN`.

## Alcance y limitaciones

- Las comprobaciones de contenedores, puertos y Git son diagnósticos locales de solo lectura, no health del Backend ni PASS funcional.
- Se ejecutaron pruebas unitarias/contractuales locales y `clean verify`; no se ejecutó `-Pintegration verify`, SQL Server real, Identity Provider real, API live ni prueba de interfaz.
- No se solicitaron credenciales; no se capturaron cookies, tokens ni valores secretos.
- La validación de PR/CI remota y la confirmación de sesiones/roles en navegador no se presumen.

## Revalidación de entorno — 2026-11-15 (continuidad a Fase 2B)

- `.\mvnw.cmd verify` con Java 25 terminó a las 17:52:06 (-05:00), exit 0. Surefire: 4.539 tests, 0 fallos, 0 errores, 0 skips. JaCoCo: LINE 22.416/23.616 = 94,92 %; BRANCH 7.019/8.112 = 86,53 %. `-Pintegration verify` no se ejecutó.
- El respaldo que faltaba en la inspección inicial de Fase 2A se encontró en `%USERPROFILE%\.asistencias-security-lab\db-backups\gestionasistenciadb_antes_migrar_20261114-173254.bak` (66.203.648 bytes). En SQL Server, `RESTORE VERIFYONLY ... WITH CHECKSUM` devolvió `The backup set on file 1 is valid.` La cabecera `msdb` identifica `gestionasistenciadb`, backup completo (`D`), `COPY_ONLY=1`, `CHECKSUM=1`, `is_damaged=0`, inicio 2026-11-14 22:32:55 UTC y fin 22:32:56 UTC. SHA-256 externo e interno coinciden: `85465716794F5140DB806B60E91CB1F0772499B84CB485D5C3937C60E970A4AE`. No se restauró ni modificó la base. La recuperación documentada por el owner DB está en `ACTUALIZAR_BD_LOCAL_EXISTENTE.md` §10: con backend detenido, restaurar desde el archivo del contenedor con `WITH REPLACE, CHECKSUM` y volver a `MULTI_USER`; esta operación no se ejecutó.
- Consulta SQL de solo lectura: DB de aplicación `gestionasistenciadb`, usuario runtime `uco_dev_runtime`; 0 tokens QR de asistencia vigentes, 0 ventanas de matrícula vigentes, 0 tomas abiertas y 0 candidatos actuales a rotación/cierre. `FESTIVOS/HABILITADO=true`, `ANIOS_ADELANTE=1`; hay 19 días festivos activos por año para 2026 y 2027. No se ejecutaron escrituras.
- SMTP local apunta a GreenMail (`127.0.0.1:3025`, contenedor healthy). La configuración ordinaria mantiene activo el dispatcher outbox; el worker de alertas también se activa por defecto. Para la prueba de solo lectura se usarán explícitamente, solo en el proceso Backend, `OUTBOX_NOTIFICATION_DISPATCHER_ENABLED=false` (documentado en `docs/work-items/NOTIF-001-proveedor-notificaciones/CONTRACT.md`) y `APP_ALERTAS_INASISTENCIA_EVALUACION_ENABLED=false` (interruptor `@ConditionalOnProperty` en `AlertaInasistenciaWiringConfiguration`). No se cambia `.env`, DB, código ni configuración persistente; se registrará el override y no se consumirán filas existentes del outbox.
- Keycloak, PDP y PEP locales usan imagen `0.1.11`; el README local describe `0.1.13`. Se conserva esa versión, sin recrear/actualizar contenedores. PEP responde `GET /actuator/health` HTTP 200; PDP y Keycloak reportan healthcheck healthy. La prueba de autenticación aún debe confirmar la compatibilidad real.
- Estado al corte de esta actualización: Backend aún no iniciado; puertos 8080/4200 libres; SQL Server y Security Baseline existentes arriba; GreenMail healthy. La primera inspección de ruta del respaldo y de impacto de jobs queda supersedida por esta evidencia. El arranque dependerá de aplicar los dos overrides explícitos y volver a comprobar los puertos; las escrituras de altas individuales y el E2E de Fase 2A siguen `NOT_RUN` por falta de fixture con limpieza segura.

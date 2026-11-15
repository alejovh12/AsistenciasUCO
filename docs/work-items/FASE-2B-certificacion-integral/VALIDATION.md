# VALIDATION — FASE 2B: certificación integral

## Dictamen

IN_PROGRESS. La suite Backend y el respaldo están PASS; el preflight y el arranque local controlado terminaron sin cambios observados en la BD. El navegador integrado no está disponible en esta sesión; autenticación manual y escenarios funcionales siguen NOT_RUN. No se declara la certificación.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-11-15, Windows/JDK 25, 01d60b0a | .\mvnw.cmd verify | 0 | 4.539 tests; 0 fallos/errores/skips. LINE 94,92 % (22.416/23.616); BRANCH 86,53 % (7.019/8.112). | Suite/ArchUnit/JaCoCo locales; no incluye -Pintegration ni E2E. |
| 2026-11-15, SQL Server existente | RESTORE VERIFYONLY FROM DISK = N'/var/opt/mssql/backup/gestionasistenciadb_antes_migrar_20261114-173254.bak' WITH CHECKSUM | 0 | “The backup set on file 1 is valid.” | DB metadata: gestionasistenciadb, D, COPY_ONLY=1, checksum=1, is_damaged=0; host y volumen SHA-256 iguales. No restore. |
| 2026-11-15, SQL Server existente | SELECT de vistas de token, tomas abiertas, festivos y parámetros | 0 | 0 tokens/tomas elegibles; 19 festivos activos en 2026 y 2027. | Solo conteos/flags; sin IDs personales ni cambios. |
| 2026-11-15 23:29:20 UTC, SQL Server existente | SELECT agregado de `uv_token_asistencia_toma`, `uv_token_matricula_grupo`, `TomaAsistencia`/`SesionCancelacion`, `uv_dia_festivo` y parámetros de jobs | 0 | QR y matrícula habilitados; 0 tokens activos/por rotar, 0 ventanas vigentes/por rotar, 0 tomas abiertas/candidatas a cierre; festivos habilitado con horizonte 1 y 19 activos por año en 2026/2027. | La consulta inicial con `uco_dev_runtime` devolvió permiso denegado para `ufn_obtener_parametro_int` y `SesionCancelacion`; sin cambios. Se repitió como SELECT agregado mediante la cuenta administrativa ya presente en el contenedor existente. No se muestran IDs ni PII. |
| 2026-11-15 23:32:25 UTC, antes de que Backend escuchara en 8080 | SELECT de elegibles según los predicados de rotación, cierre, alertas y reserva de outbox; festivos activos por año | 0 | Candidatos a rotación/cierre/evaluación de alertas=0; festivos activos 2026/2027=19/19; 1.158 mensajes reservables; 0 terminales elegibles para depuración por antigüedad. | Snapshot justo antes de que el servicio estuviera disponible; solo SELECT, sin IDs/PII. Fuentes de predicados: SP de DB para rotación/cierre/alertas y repositorio del dispatcher para outbox. |
| 2026-11-15, antes del arranque | SELECT agregado `NotificacionSaliente` por `EstadoNotificacion.codigo` | 0 | ENTREGADA=506, FALLIDA_DEFINITIVA=6, FALLIDA_REINTENTABLE=38, PENDIENTE=1.120. | No se leyeron contenidos ni destinatarios; se repitió el conteo después del arranque para confirmar que el override evita consumo/depuración. |
| 2026-11-15, Security Baseline local | Docker inspect + GET PEP /actuator/health | 0 | Keycloak/PDP healthy; PEP HTTP 200, sin healthcheck Docker. GreenMail healthy. | Imágenes 0.1.11; README local menciona 0.1.13, sin actualización. |
| 2026-11-15 18:32 (-05:00), Backend | `infra/security-baseline/scripts/start-backend-baseline.ps1 -Provider baseline -Port 8080 -BackendEnv %LOCALAPPDATA%\AsistenciasUCO\backend-run\phase2b-safe-workers.env` | proceso activo hasta 18:37 | JDK 25; HTTP 8080, management 8090; `/actuator/health` status `UP`; perfil local; verificador de contrato DB UTC-D06-POST-FREEZE PASS. Log explícito: `dispatcherEnabled=false`. | El archivo de override solo contiene dos booleanos false; `.env` no se modificó. `ddl-auto: none`, `spring.sql.init.mode: never`; no se aplicaron migraciones. SMTP test mode redirige a un buzón de prueba sin registrar destinatario. El aviso de FilterRegistrationBean PEP deshabilitado es intencional: el filtro solo se añade a `SecurityFilterChain` (`SecurityBaselinePepConfiguration`/`SecurityConfig`). |
| 2026-11-15 18:33 (-05:00), Frontend original | `node_modules/.bin/ng.cmd serve --host 127.0.0.1 --port 4200` | proceso activo | Angular real disponible solo en loopback, puerto 4200. La configuración fuente usa API real (`useMocks` false por defecto). | Build dev mostró avisos NG8113 por imports no usados en componentes; sin errores de compilación ni cambios al Frontend. Pendiente inspección visual desde navegador integrado. |
| 2026-11-15 18:34 (-05:00), SQL Server existente | SELECT agregado de estado de `NotificacionSaliente` antes y después de iniciar Backend | 0 | Conteos idénticos: ENTREGADA=506, FALLIDA_DEFINITIVA=6, FALLIDA_REINTENTABLE=38, PENDIENTE=1.120. | Confirma que no se consumió ni depuró el outbox durante el arranque; dispatcher apagado explícitamente. Solo conteos, sin datos de mensajes/destinatarios. |
| 2026-11-15 18:37 (-05:00), Backend/Frontend | Ctrl+C en las sesiones del launcher oficial y Angular CLI | 0 | Ambos servidores cerraron ordenadamente; puertos 8080/8090/4200 comprobados libres tras el cierre. | No se tocaron contenedores ni se hizo cleanup de datos. |
| 2026-11-15, Backend | git diff --check antes de commit F2A | 0 | PASS | commit F2A 01d60b0a; archivos locales protegidos fuera de commit. |
| 2026-11-15, copia original Frontend/DB | `git rev-parse --short HEAD`; inspección de estado | 0 | Frontend `91f89f5`, DB `39da953`; solo lectura. | DB conserva sus modificaciones locales de `.env.example` y `.env.template`; no se tocaron. Frontend sin cambios locales. |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | PLAN/criterios | PASS para investigación documental y preflight; NOT_READY para mutaciones | No hay fixture seguro ni contrato completo para escenarios críticos. |
| Arquitectura | ArchUnit/dependencias | PASS | Incluido en verify local del commit 01d60b0a. |
| Contratos | HTTP/DB/security/eventos | BLOQUEADO parcial | Discrepancias de cruce de medianoche, cierre manual, DEC-066, DEC-073/soporte/plazo; no se cambian por suposición. |
| Tests | RED → GREEN, negativos | PASS para correcciones F2A; NO APLICA a F2B documental | F2A RED/GREEN y hashes preservados; esta fase aún sin implementación. |
| Coverage | Gates | PASS | LINE 94,92 %; BRANCH 86,53 %. |
| Integración | DB real, fixtures, skips | BLOQUEADO para escrituras | Solo consultas SQL de preflight; fixture manual seguro no identificado; -Pintegration NOT_RUN. |
| Build | mvn verify | PASS | Exit 0, 4.539 tests, 0 skips. |
| Seguridad | Secretos, RBAC, scanners | PARCIAL | No se imprimieron secretos ni se consultaron tokens; autenticación/roles en navegador pendientes. |
| Documentos | Enlaces, deuda, manuales, ADR | EN CURSO | PLAN/TEST_PLAN/VALIDATION creados; ledger manual y cierre se actualizan tras observar navegador. |
| Navegador | E2E integrado | BLOQUEADO / NOT_RUN | El agente F llamó `createBrowserTab("iab", "http://127.0.0.1:4200", {visible:true})`: respuesta `Browser is not available: iab`; `cua.getState()` devolvió `browsers: []`. No se abrió otro navegador, no se cargó la SPA, no hubo login ni request funcional. |

## Integridad de RED

F2B no tiene RED Java mientras siga DOCUMENTATION_ONLY. Para F2A, snapshots RED originales están documentados en su TEST_PLAN.md; commit 01d60b0a conserva los tests y GREEN. No se cambiaron los tests RED al implementar.

## Bloqueantes y limitaciones

- No se reutilizarán grupos persistidos 9105/9106 ni seed 001 para escrituras: son datos históricos y no tienen cleanup seguro para E2E manual.
- Los límites DEC-066 20/21 y [-15,+30), TOM_012 y asistencia QR/manual requieren fixture controlado y, donde falta, contrato DB vigente.
- Reclamos: falta DEC-073, hay discrepancia entre soporte opcional y requerido por categoría y dudas sobre plazo/archivos; se informará al owner en evidencia, sin ampliar contrato.
- Sesiones: regla de medianoche cliente vs zona institucional y cierre manual SES_003 siguen CONTRACT_CONFLICT.
- No se ejecutó -Pintegration verify, no se abrió navegador, no hubo login ni requests funcionales.

## Evidencias remotas/manuales pendientes

- PR #154 (Fase 2A) está abierto como draft, sin merge; su develop remoto está delante del snapshot local.
- La corrida local controlada se cerró sin cambios de datos. Para continuar se requiere que el entorno vuelva a exponer el navegador integrado (o autorización explícita para un navegador alterno); entonces abrir la SPA y solicitar login manual DOCENTE antes de cualquier lectura autenticada.
- Login manual DOCENTE y luego las demás cuentas solo con nueva solicitud manual por rol; todos siguen NOT_RUN.
- Actualizar estado escenario por escenario y MANUAL_VALIDATION_LEDGER.md; no inventar capturas.

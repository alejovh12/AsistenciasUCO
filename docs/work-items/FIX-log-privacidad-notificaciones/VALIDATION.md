---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-10-31
---

# VALIDATION — privacidad de logs

- Base local y remoto personal: `acc1f2c9db24a339adabf6b112f3a634c67b44fb` en `develop`; consulta remota de solo lectura. `.codex/` y evidencia FIX-003 preexistentes sin modificar.
- Baseline remota: Backend Quality Gate y ArchUnit exitosos en el SHA base. Java 25 y `CorreoInstitucionalPolicyTest`: 8 tests, 0 fallos/errores/omitidos, exit 0.
- RED: comando y hashes congelados en TEST_PLAN; 4 tests, 3 fallos esperados de privacidad, 0 errores/omitidos, exit 1.
- GREEN focalizado: `mvnw.cmd -o -B -ntp -Dtest=LogNotificacionPrivacidadTest,LogNotificacionChannelPrivacidadTest,LogNotificacionAdapterTest,LogNotificacionChannelSenderTest,OutboxNotificacionAdapterTest,NotificacionOutboxDispatcherTest,PlantillaRendererTest,SmtpNotificacionChannelSenderTest,RegistrarEstudianteUseCaseImplTest,ReenviarVerificacionCorreoUseCaseImplTest,VerificacionCorreoMarcadorProviderTest,GenerarReporteAsistenciaUseCaseImplTest,ReporteAsistenciaTitularidadHttpTest,ReporteAsistenciaAdministradorAccessHttpTest,ReporteAsistenciaControllerTest test`: 146 tests, 0 fallos/errores/omitidos, exit 0. Java 25; SMTP y persistencia simulados.
- `mvnw.cmd -o -B -ntp verify` en sandbox: exit 1, 2977 pruebas, 0 fallos, 130 errores, 0 omitidas. Los 11 grupos con error reportan `Permission denied` o imposibilidad de conectar loopback en dobles locales. ArchUnit: 98 pruebas, 0 fallos/errores/omitidos. JaCoCo no llegó al gate.
- Segundo `mvnw.cmd -o -B -ntp -Djacoco.destFile=target/fix-log-verify.exec -Djacoco.dataFile=target/fix-log-verify.exec verify`, con loopback autorizado: exit 0; 2976 tests, 0 fallos/errores/omitidos. ArchUnit: 98/98. El informe JaCoCo cargó el archivo nuevo: LINE 14670/15901 = 92,26 %; BRANCH 4170/4974 = 83,84 %.
- Limitación del segundo `verify`: el goal `check` del POM cargó `target/jacoco.exec` anterior, mientras `report` usó el archivo nuevo. Se ejecutó por separado `jacoco:check` sobre `target/fix-log-verify.exec` y las mismas reglas copiadas del POM, con igualdad estructural comprobada: exit 0, umbrales satisfechos. El primer intento de este check separado falló por el namespace del POM generado; se corrigió el artefacto temporal sin modificar reglas y el segundo pasó.
- `git diff --check`: exit 0; Git avisó de normalización futura CRLF/LF en LINEA_BASE. Hashes de las tres pruebas RED coinciden tras GREEN.
- Integración SQL/Keycloak/SMTP real: NOT_RUN por alcance; ningún servicio externo modificado.

Riesgo residual: el despachador registra mensajes de excepciones de dependencias; si un provider introduce datos personales en esos mensajes, requieren revisión y sanitización en un trabajo separado. Los campos técnicos `tipo` y `rol` son cadenas del productor y deben mantenerse como metadatos institucionales, sin incorporar contenido de usuarios.

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# VAL-oleada8-backend - VALIDATION

Fecha: 2026-11-15. Worktree `fix/VAL-oleada8-backend` sobre `repo-antiguo/develop` 5b1a9cd9. Windows 11, Java 25, `mvnw.cmd`,
con `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\jtmp`.

## Compuerta del backend

| Comando | Resultado |
| :--- | :--- |
| `./mvnw.cmd -B -ntp clean verify` (corrida final, con las dos tandas) | `BUILD SUCCESS`, exit 0, 12:20 min |
| Pruebas (Surefire, ArchUnit incluido) | 4658 ejecutadas, 0 fallos, 0 errores, 0 omitidas |
| JaCoCo | `All coverage checks have been met` (agregado del reporte: líneas 95,02 %, ramas 86,82 %) |
| Corrida intermedia | Falló `NotificationEmailModuleIsolationRulesTest.AR07`: `SmtpHealthIndicator` estaba en `config.health` y la regla solo deja depender del módulo SMTP a `config.adapters.notification`. Se movió la clase a `config.adapters.notification.smtp`; la regla no se tocó. |
| `SecurityErrorResponseWriter` | 4 de 4 ramas cubiertas |
| `SensitiveDataSanitizer` | 45 de 46 ramas cubiertas |

## RED → GREEN

| Ítem | Prueba | RED (antes del cambio) | GREEN |
| :--- | :--- | :--- | :--- |
| EST-02 | `SecurityErrorResponseWriterTest.nullRequestIsWrittenAsEmptyPathWithoutNullPointer` | `NullPointerException: Cannot invoke "HttpServletRequest.getRequestURI()" because "request" is null` (el mismo camino que reporta Sonar `javabugs:S2259`) | 12 de 12 |
| F12-07 | `OperationalReadinessConfigurationTest.f12_07_puerto_http_se_asigna_por_variable_con_8080_por_defecto` | `expected: <${SERVER_PORT:8080}> but was: <8080>` | 9 de 9 |
| OBS-06 | 9 casos nuevos en `SensitiveDataSanitizerTest` | No compila: faltan `PERSONAL_DATA` e `isPersonalDataKey` | 22 de 22, incluida la entrada de 90 000 caracteres por debajo de 2 s |
| PRU-17 | `BaselineJwtDecoderCompositionTest` (8 casos) | Prueba de caracterización: el decodificador ya era correcto, faltaba la evidencia | 8 de 8 |
| F12-02 | `OperationalReadinessConfigurationTest.f12_02_cada_imagen_base_del_dockerfile_esta_fijada_por_digest` | `la imagen base debe llevar etiqueta y digest ...: eclipse-temurin:25-jdk-alpine` | 10 de 10 |
| HEX-09 | `ProduccionSoloInvocaProcedimientosPublicosTest` (2 casos) | Prueba de caracterización: hoy no hay infracciones; la aserción positiva exige más de 100 referencias públicas | 2 de 2 |
| OBS-10 | `SmtpHealthIndicatorTest` (6, con un servidor SMTP local real y un puerto cerrado) y `EmisorIdentidadHealthIndicatorTest` (4, con un servidor HTTP local) | Clases nuevas | 10 de 10 |
| OBS-07 | `MetricasNegocioRealtimeTest` (3) | No compila: falta `METRICA_EVENTOS_NEGOCIO` | 3 de 3; `ReactorRealtimeAdapterTest` (11) sigue en verde |

## E2E de cinco roles contra el laboratorio (ROL-12)

| Comando | Resultado |
| :--- | :--- |
| `python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"` | 66 pruebas, OK |
| `e2e_baseline.py` con `E2E_USUARIO_DECANO`, `..._COORDINADOR`, `..._DOCENTE`, `..._ESTUDIANTE` (usuarios activos de `GestioAsistenciaDB/scripts/datos-prueba/USUARIOS.md`) y `E2E_USER_PASSWORD` (clave común de prueba de `AGENTS.md`, solo en el proceso) contra `asis-app-backend` (`127.0.0.1:8080`) | **19/19 PASS**: 401 sin credencial, con Bearer inválido, con cookie BFF forjada y con `X-User-Id`; ALLOW del PEP para los 5 roles; 403 cruzados; métodos con efectos que atraviesan Spring y el PEP; ruta retirada del coordinador → 404; petición repetida → 200 |

Sin las variables, el script usa `uco-decano`, `uco-coordinador`, etc., que están desactivadas desde la recarga de datos del
2026-11-15: el login PKCE vuelve a la página de inicio (`login sin código de autorización (HTTP 200)`).

## Integración con SQL Server real (PRU-11 y PRU-12)

| Comando | Resultado |
| :--- | :--- |
| `./mvnw.cmd verify -Pintegration -Dit.test=TomaAsistenciaBordesSqlServerIT -Dtest=NoExiste -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true` contra `sql_server_asistencias` del laboratorio (base `gestionasistenciadb`; credenciales del `.env` del backend cargadas solo en el proceso) | 3 de 3, 44,8 s |

- **PRU-11:** la toma 20 abre y la 21 da `ConflictException` con código `TOM_004`; quedan 20 filas. La prueba lee primero
  `TOMA_ASISTENCIA/MAXIMO_TOMAS_POR_SESION` (20) para que el borde sea el del parámetro vigente.
- **PRU-12, dos cierres:** con barrera de dos hilos, uno cierra (`yaEstabaCerrada = false`) y el otro recibe el éxito idempotente
  `TOM_011`. Quedan 3 detalles en la toma (uno por estudiante) y 0 duplicados.
- **PRU-12, lote y cierre:** ninguno termina con una excepción técnica, quedan 0 duplicados y un detalle por estudiante.
- **Corrección de la prueba durante la corrida:** la primera versión sumaba `totalAusentesMarcados` de los dos cierres y esperaba 3;
  dio 6. `usp_cerrar_toma_asistencia` (rama `TOM_011`) devuelve sin escribir el total guardado por el cierre efectivo, así que la
  aserción era errónea. Ahora la prueba exige que los dos resultados reporten 3 y que la BD tenga 3 detalles. No se cambió código
  de producción.
- **Limpieza:** después de la corrida, 0 usuarios `%@it.invalid` y 0 grupos huérfanos de la fixture en la BD del laboratorio.

## Verificación en el laboratorio (H2)

Dentro de `asis-app-backend`: `GET http://127.0.0.1:8090/actuator/prometheus` responde `HTTP/1.1 200` sin credenciales. Expone
las métricas que usan el dashboard nuevo y las alertas de Infra. Desde el host no responde porque el puerto de gestión escucha en
loopback del contenedor, como está documentado.

## Dashboard (OBS-11)

`infra/observability/grafana/provisioning/dashboards/asistencias-uco-metricas.json` se cargó en un Grafana 11.5.2 desechable:
Carpeta "Asistencias UCO" (20 paneles tras OBS-07; detalle en `AsistenciasUCO-Infra/docs/work-items/VAL-oleada8-observabilidad/VALIDATION.md`).

## Limitaciones

- El Quality Gate de SonarCloud (EST-02 cerrado en Sonar) solo se puede ver en el análisis del PR.
- La IT se corrió contra la BD del laboratorio, no contra una efímera. En el CI la corre el `Backend Integration Gate`.
- PRU-15 queda bloqueado (ver PLAN.md): falta la regla de negocio de "periodo cerrado".

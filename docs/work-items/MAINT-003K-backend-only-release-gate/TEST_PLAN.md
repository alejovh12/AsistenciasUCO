# MAINT-003K — Tests y oráculos de salida

**Estado:** EJECUTADO el 2026-10-10 contra `sql_server_asistencias` / `gestionasistenciadb` (DB reconstruida desde Git), Java 25, backend en el código de `b60a6a1`.
Evidencia y cifras: [VALIDATION.md](VALIDATION.md). JVM probadas: `UTC`, `America/Bogota`, `Europe/Berlin`.

| Caso | Configuración | Oráculo | Resultado |
|---|---|---|---|
| TIM-01 | SQL `datetime2(7)` = 2026-09-14 13:00:00.1234567; JVM UTC | GET v1 devuelve el literal, sin desplazamiento | PASS (E2E `TIM-v1` + ITs de paridad) |
| TIM-02 | MISMA fila; JVM America/Bogota | mismo literal | PASS |
| TIM-03 | MISMA fila; JVM Europe/Berlin | mismo literal | PASS |
| TIM-04 | `UvSesionEntity` mapeado Hibernate | `fechaHoraInicio/Fin` son `LocalDateTimeJdbcType`, no `Date/Timestamp` | PASS (`SesionLocalClockProjectionTest`) |
| TIM-04b | Otros lectores de Sesión (materia del estudiante, reporte de asistencia) | `LocalDateTime` literal; ejecutan contra SQL real | PASS tras corrección (antes: `Missing constructor` en runtime) |
| TIM-05 | UTC_V2 confirmado `2026-09-14T13:00:00.1234567Z` | GET v2 siempre devuelve Z exacta en las 3 JVM | PASS (28/28 por JVM) |
| TIM-06 | sesión histórica `procedenciaTemporal=NULL` (creada por SP v1) | GET v2 conserva fila, horas NULL, INDETERMINADA | PASS |
| TIM-07 | v2 POST `2042-07-15T20:00:00.1234567-05:00` | SQL UTC `2042-07-16 01:00:00.1234567`, marca `UTC_V2` | PASS |
| TIM-08 | v2 PATCH mismo instante con offset Berlín +02:00 | SQL UTC exacto, sin doble conversión; GET v2 Z idéntica | PASS |
| TIM-09 | naive, 8 decimales, offset sin minutos | 400 | PASS (3 casos) |
| TIM-10 | v1 consultas con UUID y grupo inexistente | mismo not-found/empty que baseline | PASS (ITs de paridad) |
| AUTH-01 | sin bearer | 401 | PASS (POST/GET v2 y GET v1) |
| AUTH-02 | rol ESTUDIANTE | 403 | PASS |
| AUTH-03 | DOCENTE ajeno consulta/cambia sesión | 403, sin mutación | PASS (POST, GET grupo, GET id, PATCH, GET v1) |
| AUTH-04 | DOCENTE titular | POST→GET→PATCH→GET, id estable, UTC preciso | PASS |
| AUTH-05 | actor en el body | 400 `FIELD_UNKNOWN` | PASS |
| GATE-01 | v2 deshabilitado | con JWT 404 (POST y GET); sin JWT 401 (el filtro de seguridad precede a MVC); v1 sigue 200 | PASS |
| GATE-02 | v2 habilitado contra DB sin contrato D06 (backup pre-D06 restaurado) | el arranque aborta (`IllegalStateException … UTC-D06-POST-FREEZE`) | PASS |
| COMP-01 | OpenAPI 9 operaciones v1 + 4 v2 | rutas/métodos/esquemas | PASS (`OpenApiGoldenPathConformanceTest` 9, `…ValidationTest` 2, `…SesionesV2ContractRedTest` 4) |
| COMP-02 | PUT v2 | 405 | PASS |
| REG-01 | Failsafe SQL+Keycloak | 0 fallos, 0 errores, 0 skips | PASS: 196/196 |
| REG-02 | JaCoCo | líneas ≥80 %, ramas ≥70 % | PASS: 91,31 % / 78,83 % |
| REG-03 | ArchUnit + guards | sin violaciones | PASS (`CleanArchitectureRulesTest` 20, `JpaRepositoryArchitectureRulesTest` 3) |
| REG-04 | CI HEAD | checks exigidos | **NOT_RUN** (sin PR) |
| VOL-01 | ITs de listado de estudiantes con 5003 estudiantes sintéticos | recorren todas las páginas; oráculo SQL == concatenación de páginas | PASS tras volver los ITs independientes del volumen |
| ORD-01 | reporte de asistencia con estudiantes homónimos | orden total `(numero, inicio, nombre, ei.id, eg.id)` idéntico a JDBC | PASS (3 JVM) |

**Advertencia:** `SesionLocalClockProjectionTest` prueba el mapper aislado; TIM-01..03 se demuestran con SQL real (ITs de paridad ×3 JVM y E2E HTTP ×3 JVM). No se sustituyó la DB real por H2 ni mocks.

Comandos de referencia (Java 25; `.env` cargado en el proceso):
`./mvnw -B -ntp -Pintegration clean verify`; matriz de zona: `JAVA_TOOL_OPTIONS=-Duser.timezone=<zona>` + `-Dit.test=<ITs> -Djacoco.skip=true verify`;
E2E: backend `java -Duser.timezone=<zona> -jar target/AsistenciasUCO-0.0.1-SNAPSHOT.jar` con `SERVER_PORT=18181 APP_SESIONES_V2_ENABLED=true` y `scripts/e2e/utc-real-jwt-e2e.ps1 -BaseUrl http://127.0.0.1:18181 -TimeZoneLabel <zona>`.

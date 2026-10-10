# MAINT-003K — Tests y oráculos de salida (BACKEND ONLY)

**Estado:** pruebas documentadas; no se han ejecutado en esta revisión GitHub.

| Caso | Configuración | Oráculo |
|---|---|---|
| TIM-01 | SQL `datetime2(7)` = 2026-07-16 14:00:00.1234567; JVM UTC | GET v1 devuelve literal 14:00:00.1234567, sin +/− desplazamiento |
| TIM-02 | MISMA fila; JVM America/Bogota | GET v1 devuelve mismo 14:00:00.1234567 |
| TIM-03 | MISMA fila; JVM Europe/Berlin | GET v1 devuelve mismo 14:00:00.1234567 |
| TIM-04 | `UvSesionEntity` mapeado Hibernate | `fechaHoraInicio/Fin` son `LocalDateTimeJdbcType`, no `Date/Timestamp` |
| TIM-05 | registro UTC_V2 confirmado UTC=2026-07-16T01:00:00Z | GET v2 siempre devuelve Z exacta sin desplazamiento en las 3 JVM |
| TIM-06 | sesión histórica `procedenciaTemporal=NULL` | GET v2 conserva fila, horas NULL, estado INDETERMINADA |
| TIM-07 | v2 POST 2026-07-15T20:00:00-05:00 | SQL UTC 2026-07-16 01:00:00.0000000, marca UTC_V2 |
| TIM-08 | v2 PATCH same instant offset Berlín +02 | SQL UTC exacto, sin doble conversión; GET v2 Z idéntica |
| TIM-09 | 0 a 7 decimales, +HH:mm/Z estricto | aceptación correcta; 8+ decimales, offset sin minutos, naive => 400 con campo |
| TIM-10 | v1 consultas con UUID y grupo inexistente | mismo HTTP not-found/empty que baseline |
| AUTH-01 | sin bearer JWT | 401 (o contrato OFF explicitado por capa de filtros) |
| AUTH-02 | bearer de rol distinto de DOCENTE | 403 sin ejecutar input port |
| AUTH-03 | DOCENTE ajeno consulta/cambia sesión | 403 por ownership; no datos/mutación |
| AUTH-04 | DOCENTE dueño | POST/GET/PATCH/GET, id estable, UTC preciso |
| GATE-01 | v2 disabled, JWT válido y sin JWT | resolver precedencia SecurityFilter/MVC y preservar no-exposición |
| GATE-02 | v2 enabled contra DB sin vista/SP | Spring aborta arranque |
| COMP-01 | v1 OpenAPI 9 operaciones | mismas rutas/métodos/esquemas; v2 4 adicionales |
| COMP-02 | PUT v2 | HTTP 405; v1 no cambia de forma accidental |
| REG-01 | Failsafe SQL+Keycloak mínimo | 0 fallos, 0 errores, 0 skips focales; fixtures reales |
| REG-02 | JaCoCo | líneas >= 80%, ramas >= 70% |
| REG-03 | ArchUnit + guards | sin violaciones; no JDBC nuevo |
| REG-04 | CI HEAD | Checks exigidos realmente PASS, Sonar/CodeQL/Trivy sin hallazgos bloqueantes |

**Advertencia**: `SesionLocalClockProjectionTest` prueba mapeo y salida del mapper aislado; por sí solo no demuestra comportamiento JDBC/JPA, por lo que TIM-01 a TIM-03 son obligatorios con SQL real. No inventar números ni sustituir DB real por H2 o mocks.

Comandos locales de referencia: Java25, `./mvnw -B -ntp clean verify` y `./mvnw -B -ntp -Pintegration verify`. Seleccionar perfiles/flags con lectura del POM vigente; capturar Surefire/Failsafe XML, jacoco.xml, SQL fingerprint, puertos y PID del jar de prueba para no invocar un backend viejo.

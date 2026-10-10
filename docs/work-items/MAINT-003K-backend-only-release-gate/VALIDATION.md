# MAINT-003K — Evidencia de validación (ejecutada)

Fecha: 2026-10-10. Máquina de desarrollo Windows, Java 25 (`JAVA_HOME=C:\Program Files\Java\jdk-25`), Maven wrapper 3.9.15, SQL Server 2022 Developer en
`sql_server_asistencias:1433` (`gestionasistenciadb`, la conexión real de `.env`), Keycloak 26.7.1 local.
**Código backend certificado: `b60a6a1`** (rama `jose-valencia/maint-003k-backend-only-release-gate`); los commits posteriores solo modifican documentación.
**DB desplegada desde** `johnjduque/gestion-asistencia-db` rama `feat/maint-003k-db-dev-consolidation` (código SQL: `9f3889f`; `336472c`/docs solo fixtures de test y documentación).

## 1. Estados independientes

| Estado | Valor |
|---|---|
| `BACKEND_LOCAL_GREEN` | **PASS** |
| `DEV_DB_CONSOLIDATED` | **PASS (local)** — ver VALIDATION.md del repo DB |
| `DB_PR_READY` | rama publicada, comparable y «Able to merge»; **PR Draft por abrir** (sin sesión de GitHub en la máquina) |
| `BACKEND_CI_GREEN` | **NOT_RUN** — sin PR no corren los workflows; no se presume |

## 2. Maven `clean verify` (Java 25, JaCoCo ON) y perfil de integración

Comando final: `./mvnw -B -ntp -Pintegration clean verify` → `BUILD SUCCESS` (9 min 12 s, exit 0).

| Fase | Resultado |
|---|---|
| Surefire (unit/componente/ArchUnit/OpenAPI) | **1502** tests, 0 failures, 0 errors, 0 skipped (275 clases) |
| Failsafe (ITs contra la DB principal) | **196** tests, 0 failures, 0 errors, **0 skipped** (31 clases; `AzureCloudIntegrationIT` queda fuera por perfil, `NOT_RUN`) |
| JaCoCo (`jacoco:check`) | líneas **91,31 %** (8221 cubiertas / 782 no), ramas **78,83 %** (1903 / 511) — gates 80/70 cumplidos |
| ArchUnit | `CleanArchitectureRulesTest` 20/20, `JpaRepositoryArchitectureRulesTest` 3/3 |
| OpenAPI | `OpenApiGoldenPathConformanceTest` 9/9, `OpenApiGoldenPathValidationTest` 2/2, `OpenApiSesionesV2ContractRedTest` 4/4; contrato y checksum sin cambios (9 operaciones v1 + 4 v2) |
| ITs de SQL/SP | `SqlStoredProcedureContractIT`, `GoldenPathSqlStoredProcedureContractIT`, `UsuarioPasswordHashSqlServerIT`, `UsuarioPlanEstudioProvidersSqlServerIT`, paridad JPA, asistencia, auditoría — todos verdes |

Historial de ejecuciones hasta el verde (no se oculta ningún rojo): (1) `clean verify` inicial: error de compilación de tests; (2) segunda: 1502 con 2 errores; (3) IT completo: 5 rojos (3 failures + 2 errors); (4) IT completo: 2 rojos; (5) final: todo verde.

## 3. Regresión de zona horaria (JVM UTC / America/Bogota / Europe/Berlin) con SQL real

Nueve clases IT (`AcademicQueryJpaParityIT`, `AuthorizationReportJpaParityIT`, `CoreViewQueriesJpaParityIT`, `EstudianteProgramaJpaParityIT`, `EstudianteAcademicFilterJpaParityIT`,
`SesionGrupoCommandsSpParityIT`, `AsistenciaQueryJpaParityIT`, `EstudianteRepositorySqlServerIT`, `GrupoRepositorySqlServerIT`) con `JAVA_TOOL_OPTIONS=-Duser.timezone=<zona>`:
45 tests por zona; en las tres zonas el único rojo fue el orden del reporte (corregido, §5.4); tras la corrección `AuthorizationReportJpaParityIT` 4/4 en UTC, Bogotá y Berlín
y el resto 41/41 en las tres. La lectura v1 por sesión/grupo coincide con el `SELECT` de la misma fila en 100 ns (oráculo JDBC `LocalDateTime`), y en HTTP real (§4).

## 4. HTTP real con JWT de Keycloak (28 comprobaciones × 3 JVM = 84/84)

Backend `java -jar` con `app.sesiones.v2.enabled=true`, puerto 18181, `-Duser.timezone` ∈ {UTC, America/Bogota, Europe/Berlin}; script `scripts/e2e/utc-real-jwt-e2e.ps1`
(usuario temporal DOCENTE en Keycloak mapeado al docente titular del grupo fixture; eliminado al terminar — verificado; sesiones `E2E-%` borradas de SQL).

- Seguridad: sin bearer 401 (POST/GET v2, GET v1); ESTUDIANTE 403; DOCENTE ajeno 403 (POST, GET grupo, GET id, PATCH, v1); actor en el body → 400 `FIELD_UNKNOWN`; actor solo del JWT.
- v1: lecturas = reloj SQL literal (grupo seed y grupo fixture) y v1 POST `08:00–10:00` se lee idéntico en las 3 JVM.
- v2: GET fixture `2026-09-14T13:00:00.1234567Z`/`15:00:00.7654321Z` CONFIRMADA/`UTC_V2`; POST `-05:00` → SQL `2042-07-16 01:00:00.1234567` marca `UTC_V2`; GET por id y por grupo;
  PATCH con offset Berlín `+02:00` mismo instante → GET idéntico sin doble conversión; fila v1 → INDETERMINADA con horas y procedencia `null`; PUT → 405; parser estricto (naive, 8 decimales, offset sin minutos → 400).
- Regresión de lecturas v1: `docente/horarios`, `grupos/{id}/estudiantes`, `grupos/{id}/asistencias` → 200.
- Login SQL de mínimo privilegio: lo certifica el gate SQL (`UTC_D06_RUNTIME_PERMISSIONS`, `CC003G01_RUNTIME_*`, login real no-`sa`) ejecutado contra la DB principal (220/219/0/1).

Flag: **GATE-01** OFF (defecto) → con JWT: POST/GET v2 404, v1 200; sin JWT: 401 (el filtro de seguridad responde antes que MVC; no se abre ningún endpoint v2). El OpenAPI dice «404 con v2 deshabilitado»;
el 401 sin credenciales es el comportamiento de capa de seguridad — **no se modificó el contrato** (propuesta documental pendiente de decisión contractual). **GATE-02** ON contra DB sin D06 → el arranque aborta con
`app.sesiones.v2.enabled=true pero la base de datos no expone el contrato UTC-D06-POST-FREEZE …`. Con la DB principal: «Contrato DB UTC-D06-POST-FREEZE verificado; /api/v2/sesiones habilitado».

## 5. Defectos encontrados y corregidos (sin borrar pruebas ni bajar cobertura)

1. **Tests obsoletos de MAINT-003I que no compilaban/ejecutaban** (`CoreViewJpaProjectionBehaviorTest`, `ViewEntityProjectionMappingTest`, `JpaQueryAdapterContractTest`): hidrataban `java.util.Date` en `UvSesionEntity` (ya `LocalDateTime`). Ahora afirman el reloj literal.
2. **Regresión de runtime de MAINT-003I (grave):** `GET sesiones de materia del estudiante` y `reporte de asistencia por grupo` fallaban con `SemanticException: Missing constructor for type SesionMateriaQueryRow/ReporteAsistenciaQueryRow` porque los records seguían tipados `java.util.Date`. Records y mappers pasan a `LocalDateTime` literal (sin `toUtcLocalDateTime`).
3. **Oráculos de test con el desplazamiento de +5 h** (`ReporteAsistenciaJdbcBaseline`, `SesionMateriaEstudianteJdbcBaseline`): leían `Timestamp→Instant→UTC`; ahora `rs.getObject(col, LocalDateTime.class)` (como ya hace `CoreViewQueriesJpaParityIT`).
4. **Orden no determinista del reporte** con estudiantes homónimos: `order by … , ei.id, eg.id` en HQL y en el baseline.
5. **ITs con supuesto de volumen** (`EstudianteAcademicFilterJpaParityIT` ≤500, `CoreViewQueriesJpaParityIT` ≤100 estudiantes): ahora recorren todas las páginas; con 5003 estudiantes siguen comparando oráculo == páginas.
6. **DB** (repo DB): `uv_estudiante_programa` duplicaba filas para estudiantes multigrupo (hallado por `EstudianteProgramaJpaParityIT`); seed 14 con fechas ambiguas.
7. **Higiene del entorno de prueba:** un backend huérfano de una sesión previa ocupaba el puerto 18080 (y 10 conexiones al contenedor temporal): una primera corrida del E2E contra ese puerto habría validado el artefacto equivocado. Se detectó (404 inesperado), se usó el puerto 18181 con PID propio, y el huérfano se detuvo al retirar el contenedor temporal.

Deuda menor: `CoreViewJpaProjectionMapper.toUtcLocalDateTime(Date)` ya no se usa en producción (queda cubierto por `CoreViewJpaQueryErrorSemanticsTest`); candidato a retirar junto a su test en un cambio aparte.

## 6. Residuos `Date/Timestamp` en lectores de Sesión
`grep` en `src/main`: solo permanece el helper citado; no hay otros lectores de `uv_sesion` con `Date/Timestamp`. Los demás lectores de horas (`Horario`) son hora académica local `LocalTime`, no UTC.

## 7. Seguridad / alcance
No se cambió Keycloak ni su configuración; sin secretos en código, logs ni documentos (las credenciales E2E se leen de `infra/keycloak/.env`, ignorado por Git). Frontend, Redis y MinIO sin cambios (los ITs de MinIO/ClamAV del Failsafe usan la configuración existente).
El IT `ArchivoUploadDownloadFlowIT`/`MinioFileStorageAdapterIT` puede escribir objetos de prueba en el bucket de desarrollo existente (comportamiento previo).

## 8. NOT_RUN / limitaciones
- CI remoto (backend-ci, security, CodeQL, Trivy), SonarCloud: **NOT_RUN** (no hay PR de esta rama; no se presumen verdes). Sonar/CodeQL/Trivy locales no instalados.
- `AzureCloudIntegrationIT`: NOT_RUN (requiere Azure real).
- Rollback de la DB: restauración probada desde `.bak` verificado (repo DB, VALIDATION §2). Rollback backend: `git revert` de `aaf703a`, `55ddd4e`, `b60a6a1` y docs.
- No se hizo merge, no se borraron ramas, no se desplegó a producción.

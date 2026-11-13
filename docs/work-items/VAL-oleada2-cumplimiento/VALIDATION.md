---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# VAL-oleada2-cumplimiento - VALIDATION

## Cadena de suministro y CI (agente T)

actionlint 0 hallazgos; YAML OK; Trivy local 3 HIGH reales (guava, snappy-java, transitivas de minio); gitleaks con configuración 0 hallazgos. Detalle en [CI-CADENA-SUMINISTRO.md](CI-CADENA-SUMINISTRO.md).

## Agente B: ROL-06, F12-09, OBS-10, REA-02, PAR-05, PAR-06, CF-05, CF-06, F12-11 y CVE de minio

Fecha: 2026-11-12. JDK 25.0.5, `.\mvnw.cmd -B -o test -Dtest=... -Djacoco.skip=true` (sin `clean verify`, por memoria).

| Corrida | Alcance (`-Dtest`) | Resultado |
|---|---|---|
| 1 | `architecture.**`, `infrastructure.adapter.primary.**`, `adapter.secondary.{securitybaseline,storage,malwarescan}.**`, `infrastructure.config.**`, `application.features.{archivo,consultastransversales}.**`, sin `*IT` | 1490 pruebas, 9 fallos: 1 de UUID nulo en `AdminPortalControllerTest` (se agregó `@NonEmptyUuid`), 1 de carrera en `RealtimeStreamErrorHttpTest`, 1 de `FileProvidersConfigurationTest` (ahora publica el decorador) y 6 de `OperationalReadinessConfigurationTest` (leía el `application.yml` de test; ahora lee el de `src/main/resources`). Todos corregidos. |
| 2 | Las clases que fallaron + `BeanValidationControllersTest`, `MinioClientTimeoutBoundaryTest`, controladores `archivo`, `asistencia`, `coordinador`, `sesion`, `docente`, `adapter.secondary.{storage,malwarescan}.**`, `application.features.archivo.**` (ya con guava 33.7.2 y snappy 1.1.10.12) | 413 pruebas, 1 fallo en `RealtimeStreamErrorHttpTest.grupoId_en_mayusculas...` (leía el evento antes de que llegara su `data:`; se corrigió la espera). |
| 3 | `RealtimeStreamErrorHttpTest` tres veces | 7/7 en cada corrida. |
| 4 | `architecture.**`, `adapter.primary.security.**`, `infrastructure.config.**`, controladores `admin`, `identidad`, `decano`, `validation`, `error`, `adapter.secondary.securitybaseline.**`, `application.features.consultastransversales.**` | **850 pruebas, 0 fallos, BUILD SUCCESS.** |

Pruebas nuevas:
- `controller/validation/BeanValidationControllersTest` (ROL-06): toma (lote vacío, cascada por registro, modo vacío sigue en Application), asignaturas del plan (campos obligatorios en orden, rango, mapeo al DTO), portal del coordinador (`inp`, longitud de código), retiro (motivo > 500), verificación de correo (token desmedido), payload de código y ausencia de mensajes del proveedor.
- `adapter/secondary/securitybaseline/SecurityBaselinePepCircuitBreakerTest` (REA-02): el breaker abre con el PEP caído o inalcanzable; abierto responde `UNAVAILABLE` (503) sin llamar al PEP y en menos de 500 ms; 401/403 no lo abren; sin Bearer no lo toca.
- `adapter/secondary/storage/resilience/CircuitBreakerFileStorageAdapterTest` y `adapter/secondary/malwarescan/resilience/CircuitBreakerMalwareScanAdapterTest` (REA-02): abre y falla cerrado; `ObjectNotFound` e `INFECTED` no cuentan; errores de programación no cuentan.
- `config/OperationalReadinessConfigurationTest` (F12-09, OBS-10, F12-11, CF-05, REA-02): claves del `application.yml`, `HEALTHCHECK` del Dockerfile y breakers construidos por la autoconfiguración de Resilience4j con los valores del YAML.
- `SecurityConfigTest` (CF-05, CF-06): CSP, `X-Frame-Options: DENY`, nosniff, Referrer-Policy y Permissions-Policy en 200 y 401; HSTS solo en HTTPS; CSP propia de Swagger UI; preflight de origen rechazado sin encabezados CORS.
- `SubirArchivoTamanioParametrizadoTest` (PAR-05) y `ConsultasAuditoriaRangoParametrizadoTest` (PAR-06).

Dependencias: `mvnw dependency:tree -Dincludes=com.google.guava:guava,org.xerial.snappy:snappy-java` → `guava:33.7.2-jre`, `snappy-java:1.1.10.12`.

No corrido aquí: `clean verify` completo (JaCoCo, CodeQL, Sonar), `-Pintegration` y los `*IT` (`ArchivoUploadDownloadFlowIT` necesita MinIO y ClamAV reales), arranque real del contenedor con el `HEALTHCHECK`.

### Propuesta de migración para la BD (no se tocó la BD)

PAR-06 necesita este parámetro, que no existe en el seed (mientras tanto el código usa 90 días):

| grupo | clave | valor | tipo | rango sugerido para la edición |
|---|---|---|---|---|
| `AUDITORIA` | `RANGO_MAXIMO_DIAS` | `90` | `INT` | 1..366 (el código ignora valores fuera de ese rango) |

`AUDITORIA` está en `PARAMETROS/GRUPOS_NO_EDITABLES`: si se quiere editable desde la pantalla de parámetros hay que sacarlo de esa lista o crear la fila con `esEditable = 0`.

## Recuperación del CI del PR #137 (2026-11-12)

### Causas

| Check | Causa |
|---|---|
| Backend Quality Gate / SonarCloud | `new_security_rating` = E (único criterio en rojo; cobertura 87,2 %, duplicación 0,9 % y hotspots 100 % estaban bien). Lo provocaban 3 vulnerabilidades: `java:S6418` (BLOCKER) por el nombre `TOKEN_*` de dos constantes regex de `BeanValidationPatterns` (falso "secreto"), y `githubactions:S6506` en `security.yml:71` (`curl --location` sin forzar HTTPS en las redirecciones). Además, 14 code smells nuevos. |
| Trivy Production Docker Image Scan | El `clean verify` dentro del `docker build` falla en `OperationalReadinessConfigurationTest.f12_09_dockerfile_declara_healthcheck...`: la prueba lee `Dockerfile` y el stage de build no lo copiaba (`NoSuchFileException`). El `HEALTHCHECK` es válido: `eclipse-temurin:25-jre-alpine` trae el `wget` de BusyBox. |
| Backend Integration Gate | PRU-04 hizo que las `*IT` corrieran por primera vez en cada PR. Fallan por deuda previa, no por el código de la oleada 2: (1) el workflow no sembraba las fixtures Java que publica el repo de BD (`test/prepare_java_integration_fixture.ps1`), lo que da `FIXTURE_MISSING`; (2) `SqlStoredProcedureContractIT` tenía desfasado el contrato de `usp_resolver_solicitud_revision_asistencia` (la migración 37 agregó `@codigoResultadoAprobacion`); (3) quedan ITs con datos esperados o reglas desfasadas (ver tabla pendiente). La hipótesis de orden con la BD ya no aplica: `develop` de BD ya tiene unido el PR #126 (migraciones 39 y 40, seed `57_val_parametro_rango_auditoria.sql`). |
| Avisos | `server-username`/`server-password` de `actions/setup-java` v6 son alias deprecados. `HandlerMappingIntrospector` está deprecado en Spring Framework 7. |

### Arreglos

- `BeanValidationPatterns`: `TOKEN_43_OR_BLANK` → `BASE64URL_43_OR_BLANK` y `TOKEN_43_STRICT_OR_BLANK` → `BASE64URL_43_STRICT_OR_BLANK` (usos en `RegistrarAsistenciaQrRequest` y `MatriculaQrRequests`). Mismo regex.
- `security.yml`: `curl --proto '=https' --proto-redir '=https'` al descargar gitleaks.
- `MinioStorageAdapterConfiguration`: imports duplicados eliminados.
- `CircuitBreakerFileStorageAdapter` y `CircuitBreakerMalwareScanAdapter`: ya no capturan `Error`. Una bandera `registrado` y un `finally` devuelven el permiso ante cualquier otro `Throwable`. El comportamiento no cambia.
- `SubirArchivoUseCaseImpl` y `ConsultasTransversalesUseCaseImpl`: `catch (RuntimeException _)`. El Javadoc colgante pasó a `normalizar(...)`.
- `BeanValidationFieldErrorMapper.defaultMessage`: un solo literal `"El campo '"`.
- `SubirArchivoTamanioParametrizadoTest` y `ConsultasAuditoriaRangoParametrizadoTest`: los argumentos se construyen fuera de la lambda de `assertThrows` (S5778).
- `Dockerfile`: `COPY Dockerfile Dockerfile` en el stage de build.
- `integration.yml`: (a) el paso `Resolve database schema ref` usa la rama homónima del PR en el repo de BD si existe (`git ls-remote`, solo con nombres seguros). Si no, usa `develop`. Un `db_ref` de `workflow_dispatch` siempre manda. (b) El paso `Prepare Java integration fixtures from the DB repository` corre después del despliegue del esquema.
- Los 4 workflows con `setup-java` usan `server-username-env-var`/`server-password-env-var`.
- `UnmappedRouteFilter` y `SecurityConfig`: consultan `ObjectProvider<RequestMappingHandlerMapping>` en vez de `HandlerMappingIntrospector`. Si no hay mappings MVC, el filtro no decide y deja la autorización al PEP.
- `SqlStoredProcedureContractIT`: el contrato incluye `@codigoResultadoAprobacion`.

### Reproducción local de las IT (como CI)

Se desplegó el esquema de `origin/develop` de BD (exportado con `git archive` al scratchpad, sin tocar el repo de BD) en una base aparte `gestionasistenciadb_ci` del contenedor local. Después se aplicó `prepare_java_integration_fixture.ps1` y se corrieron solo las IT: `mvnw -o -Pintegration verify -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true`. ClamAV no estaba levantado en local, así que las 11 IT de antivirus fallaron solo por el entorno; en CI se levanta. Resultado: de 347 IT, sin fixtures fallaron 39 (11 de ellas por ClamAV ausente); con las fixtures y el contrato corregido fallan 32 (las mismas 11 de ClamAV y 21 de deuda previa). `SqlStoredProcedureContractIT` y las de `FIXTURE_MISSING` (`AsistenciaQueryJpaParityIT`, `VerificacionCorreoGreenMailIT`) quedan en verde. También se aplicó la migración 39 a la BD local `gestionasistenciadb`.

### Pendiente: IT con deuda previa (no tocadas, dominio de otros o con cambio de BD)

| IT | Síntoma | Probable causa |
|---|---|---|
| `AsistenciaRepositorySqlServerIT` (3), `JpaAttendanceStoredProcedureFeasibilityIT` (5) | "El horario de la sesion no esta confirmado" / SP no completa | La fixture crea sesiones sin horario confirmado (regla de CULM-001, sesiones) |
| `TomaAsistenciaQrSqlServerIT` | esperado 4, real 20 | Regla de tomas por puntos de control (QR) |
| `AsistenciaCommandsSpParityIT.RES_01` | `ConflictException` | Resolución de reclamos con tomas/horario |
| `SoporteArchivoSqlIT`, `SolicitudRevisionDocenteDetalleSqlIT` | `UTC_V2` en vez de `null`; campos del detalle | Proyecciones desfasadas tras puntos de control |
| `AcademicQueryJpaParityIT` (2), `EstudianteAcademicFilterJpaParityIT`, `CoreViewQueriesJpaParityIT`, `AcademicUserCommandsSuccessParityIT` (2), `AvisoDocenteUmbralCancelacionIT` | datos esperados distintos o fixture insuficiente | Seed/fixtures cambiaron (DB-DEV consolidation) |
| `AuditEventJpaParityIT` (2) | `Unexpected row count (-1)` en el insert JPA de `AuditoriaEvento` | El row count de SQL Server no llega al insert JPA (se debe revisar en la BD) |

### Cuarentena explícita (decisión de John, 2026-11-12)

Las IT de la tabla anterior quedan en cuarentena con `@Tag("cuarentena-oleada3")` y un comentario `VAL-PRU-04, se corrige en oleada 3`. El perfil `integration` del `pom.xml` define `failsafe.excludedGroups=cuarentena-oleada3` y lo pasa a Failsafe como `excludedGroups`, así que el `Backend Integration Gate` las excluye y todas las demás IT siguen corriendo y bloqueando. Surefire (pruebas unitarias) no se ve afectado: la propiedad solo vive en el perfil `integration` y solo la lee Failsafe. La cuarentena no oculta la deuda: se corrige en la oleada 3 y entonces se quita la etiqueta.

Correr las IT en cuarentena localmente (con la BD y fixtures listas): `.\mvnw.cmd -Pintegration verify -Dfailsafe.excludedGroups=` (vacío = no excluye nada). Solo las de cuarentena: `-Dfailsafe.excludedGroups= -Dgroups=cuarentena-oleada3`.

Nota: no se tenían los reportes de Failsafe del último run completo, así que las clases en las que no se identificó el método exacto se pusieron en cuarentena a nivel de clase (incluye métodos que hoy pasan). Al corregirlas en la oleada 3 conviene bajar la etiqueta al método real.

| IT | Nivel de la etiqueta | Causa | Área / responsable | Plan oleada 3 |
|---|---|---|---|---|
| `AsistenciaRepositorySqlServerIT` (3 métodos) | clase | "El horario de la sesión no está confirmado" | Sesiones, José Alejandro | Fixture con sesiones de horario confirmado (CULM-001) |
| `JpaAttendanceStoredProcedureFeasibilityIT` (5) | clase | El SP no completa; mismo horario sin confirmar | Sesiones, José Alejandro | Igual que la anterior |
| `TomaAsistenciaQrSqlServerIT` (1) | clase (un solo método) | Esperado 4, real 20 | QR, Sergio | Ajustar a la regla de tomas por puntos de control |
| `AsistenciaCommandsSpParityIT.RES_01` | método | `ConflictException` al aprobar | Equipo (reclamos con tomas/horario) | Fixture con tomas y horario |
| `SoporteArchivoSqlIT` (1) | clase | `UTC_V2` en vez de `null` | Equipo | Alinear la proyección o el valor esperado |
| `SolicitudRevisionDocenteDetalleSqlIT` (1) | clase | Campos del detalle distintos | Equipo | Alinear el detalle con las proyecciones actuales |
| `AcademicQueryJpaParityIT` (2) | clase | Datos esperados distintos de la seed | Equipo | Reajustar a la seed consolidada |
| `EstudianteAcademicFilterJpaParityIT` (1) | clase | Ídem | Equipo | Ídem |
| `CoreViewQueriesJpaParityIT` (1) | clase | La fixture necesita al menos 11 estudiantes | Equipo | Ampliar la fixture |
| `AcademicUserCommandsSuccessParityIT` (2) | clase | Datos esperados distintos | Equipo | Reajustar a la seed |
| `AvisoDocenteUmbralCancelacionIT` (1) | clase (un solo método) | La fixture no tiene la matrícula correspondiente | Equipo | Completar la fixture |
| `AuditEventJpaParityIT` (2) | clase | `Unexpected row count (-1)` en el insert JPA | Equipo, con BD | Revisar el row count de SQL Server en el insert JPA |

## Compuerta Trivy de la imagen de producción (2026-11-12)

- **Síntoma:** el paso "Trivy image gate (blocks HIGH/CRITICAL with fix available)" del job "Trivy Production Docker Image Scan" fallaba con exit 1; el escaneo del repositorio pasaba.
- **Causa:** el único hallazgo era CVE-2025-59250 (HIGH) en `com.microsoft.sqlserver:mssql-jdbc` dentro de `app.jar`. El JAR empaquetado es `mssql-jdbc-13.2.1.jre11.jar` (la versión corregida, verificada por `scripts/security/verify_jdbc_artifact.py`), pero su `pom.properties` interno declara `13.2.1` sin el sufijo `.jre11`. Trivy compara `13.2.1` contra la corrección `13.2.1.jre11` y lo marca como vulnerable: falso positivo. El escaneo de `pom.xml` no lo ve porque ahí la versión sí lleva `.jre11`. La imagen base Alpine (con `apk upgrade libpng zlib`), guava 33.7.2-jre y snappy-java 1.1.10.12 salen limpios.
- **Corrección:** entrada justificada y con vencimiento en `.trivyignore` (`CVE-2025-59250 exp:2027-02-12`). No hay actualización real posible: la versión instalada ya es la corregida.
- **Evidencia:** reproducido con `aquasec/trivy:0.74.0 image --scanners vuln,misconfig --pkg-types os,library --severity HIGH,CRITICAL --ignore-unfixed` sobre una imagen con la misma etapa final del Dockerfile y el JAR de `mvnw package -DskipTests`. Antes: 1 hallazgo (exit 1). Después con `.trivyignore`: alpine 0, jar 0 (exit 0). Limitación: el escaneo de secretos del JAR de 155 MB agota el timeout de 5 min por defecto de Trivy en el portátil, por lo que se omitió localmente; el repositorio ya pasa ese escáner en CI.

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VALIDATION — VAL-oleada3-pru04

Fecha: 2026-11-12/13. JDK 25.0.5. Detalle de causas y arreglos en [PLAN.md](PLAN.md).

## Cómo se replicó el CI

- Contenedor local `sql_server_asistencias` (SQL Server Developer). Base APARTE `gestionasistenciadb_ci`; la base de
  desarrollo `gestionasistenciadb` no se tocó. Copia del repo de BD (rama `fix/VAL-oleada3-pru04-cuarentena`, que parte de
  `develop`) con el nombre de la base reescrito, `deploy_schema.ps1` completo y `test/prepare_java_integration_fixture.ps1`.
  Se tomó un respaldo de la base recién desplegada para volver al mismo estado entre corridas. Al final se borró la base y
  su respaldo.
- Credenciales: las del propio contenedor, leídas solo en memoria del proceso; no se imprimieron ni se escribieron.
- Backend: `mvnw -o verify -Pintegration -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true
  -Dfailsafe.excludedGroups=` con `SPRING_DATASOURCE_*` y `APP_DATABASE_EXPECTED_NAME` apuntando a la base aparte.
  MinIO y ClamAV no se levantaron en local (en CI sí).

## Corridas

| # | Alcance | Resultado |
|---|---|---|
| 1 | Solo cuarentena (`-Dgroups=cuarentena-oleada3`), con el arreglo de sesiones v2 ya aplicado a `AsistenciaRepositorySqlServerIT` y `AttendanceCommandFixture`, y los esperados `UTC_V2` y tope 20 | 66 pruebas, 6 fallas y 4 errores: `AcademicQueryJpaParityIT` (2, oráculos), `AcademicUserCommandsSuccessParityIT` (2: área/componente vacíos y cierre sin `CI`), `AsistenciaCommandsSpParityIT.RES_01` (`ConflictException` = `REV_004`), `AvisoDocenteUmbralCancelacionIT` (sin matrícula sobre el umbral), `CoreViewQueriesJpaParityIT` ("encontrados: 2"), `TomaAsistenciaQrSqlServerIT` (limpieza: FK `FK_EvaluacionAlertaGrupo_Grupo`) y `AuditEventJpaParityIT` (2, "Unexpected row count (-1)", efecto de la limpieza anterior). En verde ya: `AsistenciaRepositorySqlServerIT` 6/6, `JpaAttendanceStoredProcedureFeasibilityIT` 14/14, `SoporteArchivoSqlIT` 11/11, `SolicitudRevisionDocenteDetalleSqlIT` 4/4, `EstudianteAcademicFilterJpaParityIT` 3/3. |
| 2 | Las 12 clases (`-Dit.test=...`), ya sin etiqueta, con todos los arreglos menos el oráculo de `AsignaturaDocente` | 69 pruebas, 1 falla: `AcademicQueryJpaParityIT.materias_y_asignaturas_de_docente...` (el oráculo `AsignaturaDocenteJdbcBaseline` no traía `capacidadMaximaPermitida`, APP-04). Las otras 11 clases en verde. |
| 3 | **Perfil `integration` completo** (`-Dfailsafe.excludedGroups=`, sin filtro de clases), base restaurada y fixtures de la rama | **347 IT: 321 en verde, 17 omitidas** (las `*LiveIT` que piden servicios externos), **26 fallas solo de ambiente**: `ClamAvMalwareScanAdapterIT` 7 (ClamAV apagado), `MinioFileStorageAdapterIT` 10 ("defina MINIO_ENDPOINT") y `ArchivoUploadDownloadFlowIT` 9 (no carga el contexto sin las variables de MinIO). En CI el workflow levanta los dos servicios. Las 12 clases que estaban en cuarentena pasan, incluida `AcademicQueryJpaParityIT` 8/8. |
| 4 | `.\mvnw.cmd -B -o test -Djacoco.skip=true` sobre `infrastructure.adapter.secondary.persistence.sqlserver.**` y `architecture.**` | **156 clases de prueba unitaria, 1102 pruebas, 0 fallas.** (El patrón también arrastró las `*IT` de esos paquetes a Surefire, sin la base configurada; esas fallas de conexión no cuentan: las IT se validan en las corridas 1 a 3.) |

Por clase (corridas 2 y 3): `AsistenciaRepositorySqlServerIT` 6/6, `JpaAttendanceStoredProcedureFeasibilityIT` 14/14,
`TomaAsistenciaQrSqlServerIT` 1/1, `AsistenciaCommandsSpParityIT` 6/6 (con `RES_01`), `SoporteArchivoSqlIT` 11/11,
`SolicitudRevisionDocenteDetalleSqlIT` 4/4, `AcademicQueryJpaParityIT` 8/8, `EstudianteAcademicFilterJpaParityIT` 3/3,
`CoreViewQueriesJpaParityIT` 7/7, `AcademicUserCommandsSuccessParityIT` 6/6, `AvisoDocenteUmbralCancelacionIT` 1/1,
`AuditEventJpaParityIT` 4/4.

## Lo que queda en cuarentena

Nada. Ninguna clase ni método lleva `@Tag("cuarentena-oleada3")`; el mecanismo del `pom.xml` queda disponible.

## No corrido aquí

- MinIO y ClamAV reales (las 26 IT de archivos): se validan en el CI, que los levanta con `infra/files/compose.yaml`.
- `clean verify` completo con JaCoCo/Sonar: el cambio solo toca `src/test`, `pom.xml` (comentario) y el workflow
  (comentario y aviso).

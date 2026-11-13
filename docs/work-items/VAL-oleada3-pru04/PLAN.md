---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# PLAN — VAL-oleada3-pru04 (salida de cuarentena de las IT)

Origen: ítem PRU-04 de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`. En la oleada 2, 12 clases `*IT` quedaron con
`@Tag("cuarentena-oleada3")` y el perfil `integration` las excluía del `Backend Integration Gate`
(ver `docs/work-items/VAL-oleada2-cumplimiento/VALIDATION.md`, secciones "Recuperación del CI del PR #137" y "Cuarentena
explícita"). Esta oleada corrige la causa real de cada una, quita la etiqueta y el gate vuelve a correrlas todas.

Rama local `fix/VAL-oleada3-pru04-cuarentena` en backend y en BD (el workflow de integración despliega la rama homónima
de BD si existe). Decisión **A** de John: se puede tocar código de sesiones (José Alejandro) y QR (Sergio).

## Criterio

1. Reproducir el CI en local: esquema de BD desplegado con `deploy_schema.ps1`, fixtures con
   `test/prepare_java_integration_fixture.ps1` y `mvnw verify -Pintegration`, en una base aparte (nunca la de desarrollo).
2. Por cada IT: causa raíz exacta. Si la regla de producción es la vigente, se actualiza la prueba o su fixture (con la
   regla citada). Si faltan datos, se agregan al fixture de BD o al fixture de la prueba. No se borran pruebas ni se aflojan
   aserciones.
3. Quitar `@Tag("cuarentena-oleada3")` de cada clase o método corregido.

## Causas y arreglos

| IT | Causa | Arreglo |
|---|---|---|
| `AsistenciaRepositorySqlServerIT` (6) | SES-01: registrar asistencia exige la sesión activa en UTC y con procedencia confirmada. La prueba creaba la sesión por el contrato v1 (`usp_crear_sesion`, procedencia `NULL`) y con hora local: `SES_009`. | La sesión se crea por el contrato v2 (`ContratoHorarioSesion.UTC_CONFIRMADO_V2`) en el primer hueco libre del docente (SES_017) y se lleva a la ventana viva en UTC reafirmando la procedencia. Mismo patrón que `AttendanceCommandParityFixture`. |
| `JpaAttendanceStoredProcedureFeasibilityIT` (14) | Igual: `AttendanceCommandFixture.newSession()` creaba sesiones v1 con hora local. | `AttendanceCommandFixture` crea la sesión v2 en UTC. |
| `TomaAsistenciaQrSqlServerIT` (1) | (a) DEC-066 subió el tope de tomas por sesión de 4 a 20 (parámetro `TOMA_ASISTENCIA/MAXIMO_TOMAS_POR_SESION`, migración 37); la prueba esperaba 4. (b) La limpieza de `TomaQrCargaFixture` no borraba lo que escribe el motor de alertas (DEC-065): `EvaluacionAlertaGrupo` (FK al grupo), alertas, revisiones y reclamos. | (a) Se compara contra el parámetro vigente y contra 20. (b) La limpieza borra reclamos, revisiones de cancelación (+historial), alertas y evaluaciones del grupo. |
| `AuditEventJpaParityIT` (4) | No era la tabla: era una conexión del pool envenenada. `TomaQrCargaFixture` corría lotes con `SET NOCOUNT ON` y los cerraba con `SET NOCOUNT OFF` al final; cuando su limpieza fallaba (punto anterior) el lote abortaba antes del `OFF` y la conexión volvía al pool con `NOCOUNT ON`. El siguiente INSERT de Hibernate en esa conexión recibía `-1` filas: "Unexpected row count (-1)". | `TomaQrCargaFixture.ejecutarLote(...)` restaura `NOCOUNT`/`XACT_ABORT` en un `finally`, falle o no el lote. Con la limpieza corregida el lote ya no falla. |
| `AsistenciaCommandsSpParityIT.RES_01` | SES-04 (`REV_004`): solo se aprueba (justifica) una inasistencia registrada. La prueba radicaba el reclamo sin `DetalleAsistencia`. | Antes de radicar se registra la ausencia `SJC` del estudiante en las dos sesiones (oráculo JDBC, solo preparación). Aserción nueva: la aprobación justifica el detalle existente (1 detalle). |
| `SoporteArchivoSqlIT` (11) y `SolicitudRevisionDocenteDetalleSqlIT` (4) | Esperaban procedencia `null` ("sesiones históricas v1"), pero `AttendanceCommandParityFixture` ya crea sesiones v2 (SES-01). La proyección devolvía `UTC_V2` correctamente. | Se espera `UTC_V2` y el instante UTC exacto de inicio de la sesión (literal del `DATETIME2`). |
| `AcademicQueryJpaParityIT` (2 de 8) | Los oráculos JDBC de prueba estaban desfasados: `PlanEstudioJdbcBaseline` no calculaba `totalAsignaturas`/`totalCreditos` (certificación F3), `MateriaEstudianteJdbcBaseline` no traía `creditos` (CULM-001 D.9-a) y `AsignaturaDocenteJdbcBaseline` no traía `capacidadMaximaPermitida` (APP-04). La producción era correcta. | Los oráculos calculan esas columnas con SQL propio sobre las vistas (`uv_asignatura_plan_estudio` con vínculo activo; `uv_asignatura.credito`; `uv_grupo.capacidadMaximaPermitida`). |
| `AcademicUserCommandsSuccessParityIT` (2 de 6) | (a) `usp_crear_asignatura` regla P5: el área y el componente deben existir (`GEN_001`); la prueba mandaba `""`. (b) CULM-001 C.1 (DEC-041 C-10): el cierre masivo ya no escribe `CI` por conteo de faltas; `CI` solo nace de una revisión de cancelación aprobada. | (a) Se envían el área y el componente de la semilla. (b) El fixture del cierre deja al estudiante 1 ya en `CI` (resultado de esa revisión); el cierre lo conserva y lo cuenta, y el estudiante 2 pasa `A -> F`. La aserción "CI y F" se mantiene. |
| `CoreViewQueriesJpaParityIT` (1 de 7) | La paginación de 3 páginas de 5 exige al menos 11 estudiantes en el ámbito del administrador; la BD de CI trae 2. | Fixture de BD nuevo `test/fixtures/val_pru04_directorio_estudiantes_fixture.sql`: 12 estudiantes vinculados por `EstudiantePrograma` (sin grupos, no altera otros conteos). |
| `EstudianteAcademicFilterJpaParityIT` (3) | No falló en la réplica del CI. | Solo se quita la etiqueta. |
| `AvisoDocenteUmbralCancelacionIT` (1) | Dependía de que el "laboratorio" tuviera una matrícula activa sobre el umbral de pérdida; el esquema de CI no trae asistencias históricas. | La prueba crea su propia matrícula sobre el umbral (grupo propio, tres sesiones vencidas `UTC_V2` con su toma cerrada y falta `SJC`) dentro de su transacción, que se revierte. No se agrega al fixture de BD a propósito: una matrícula con pérdida sin revisión bloquearía cierres y cambiaría alertas y KPIs que comparan otras IT. |

Ninguna corrección cambió código de producción: todas las fallas eran pruebas o fixtures desfasados frente a reglas ya
vigentes, o datos ausentes.

## Gate

La propiedad `failsafe.excludedGroups=cuarentena-oleada3` del perfil `integration` se conserva como mecanismo para una
cuarentena explícita futura, pero hoy ninguna IT lleva la etiqueta, así que el gate corre todas. Se actualizaron el
comentario del `pom.xml` y el del paso "Run integration profile" de `integration.yml` (se quitó el aviso `::notice::` de
IT excluidas, que ya no aplica).

## Avisos al equipo

- Sesiones (José Alejandro): `AsistenciaRepositorySqlServerIT` y `AttendanceCommandFixture` ahora crean sesiones por el
  contrato v2 (UTC confirmado). Cualquier IT nueva que registre asistencia debe hacerlo igual (SES-01).
- QR / tomas (Sergio): `TomaAsistenciaQrSqlServerIT` compara el tope con el parámetro (20, DEC-066) y `TomaQrCargaFixture`
  limpia lo que deja el motor de alertas y no deja conexiones con `NOCOUNT ON` en el pool.

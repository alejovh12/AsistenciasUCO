# VALIDATION — P0-I (2026-10-20)

## Dictamen
`LOCAL_PASS` para unit/integración de backend y frontend. `NOT_RUN` para los E2E HTTP de estudiante (identidades `files.e2e.estudiante1/2` con `invalid_grant`). `BLOCKED`: merge a `develop` por D1 sin aprobación del owner DB; CI/Sonar remotos sin ejecutar sobre los SHA finales.

| Comando / evidencia | Resultado |
|---|---|
| `.\mvnw.cmd -B -ntp verify` (backend) | exit 0, 1651 tests, 0 fallos/omitidos, gates JaCoCo cumplidos |
| `npm run verify` (frontend) | exit 0, 819 tests SUCCESS, cobertura realtime y build de producción OK (líneas 89,05 %, ramas 70,8 %) |
| `SecurityErrorResponseWriterTest` + suites de seguridad | PASS (incluidos en el verify) |
| Fixture `E2E-FIX-A/B` (`fixture_apply`/`fixture_cleanup`) en SQL local | PASS: idempotente, `SES-SQL` PASS; conteos Sesion/Asistencia/Detalle/Solicitud/Archivo 16/6354/6354/7/7 antes y después |
| `support_binding_e2e.py idempotency`, `student_sessions_e2e.py` por HTTP | NOT_RUN (credenciales estudiante); se ejecutan tras realinear identidades |
| Estado remoto PR #31 (SHA `02338e0`) | CodeQL: failure; Backend Quality Gate: success pero **sin Sonar** (solo corre con base `develop`); sin análisis Sonar del PR #31 |

## Bloqueos
- `CONTRACT_CONFLICT P0H-DB-001` / D1 sin aprobación del owner DB (ADR-LB004D-001 `proposed`). Bloquea el merge de DB, backend y frontend dependientes. D1.1 propuesta en `DB_D1_1_IDEMPOTENCY_PROPOSAL.md`.
- Sin `gh`/sesión GitHub: no se retargeteó el PR #31 ni se abrieron los PR de frontend y DB.
- `UTC_GAP`: horarios recurrentes sin zona institucional contractual; `SolicitudRevisionAsistencia.fecha` usa la zona del servidor SQL.

## Addendum de revalidación posterior — reclamos y soportes — 2026-10-25

La marca histórica `NOT_RUN` por `invalid_grant` queda superada únicamente para las corridas HTTP posteriores documentadas en `P0-H-activar-version-funcional/VALIDATION.md`. `student_sessions_e2e.py` terminó `15/15 PASS` y `support_binding_e2e.py main` terminó `34/34 PASS`, ambos contra los servicios oficiales y con limpieza de fixtures.

El recorrido vigente usa `POST /api/v1/asistencias/revisiones`, no `POST /api/v1/estudiante/reclamos`. La bandeja, detalle, descarga y resolución docente responden mediante `/api/v1/docente/reclamos` y el archivo se descarga por `/api/v1/archivos/{fileId}`. La ruta estudiantil legacy continúa en `501` y se mantiene separada.

La repetición con dos estudiantes confirmó `201` de subida, `202` de radicación, `200` de listado/descarga y `200` de `PATCH` de aprobación. El primer fallo funcional aparece en el GET de sesiones del estudiante después de aprobar: devuelve `estadoSolicitudRevision=aceptada` pero `estadoAsistencia=NO_DISPONIBLE`. La traza SQL mostró `asistio=1` con razón `SJC`; el proyector actual no traduce esa combinación a `JUSTIFICADA`. No se declara PASS completo ni se implementa el arreglo en este work item documental.

La evidencia de navegador manual de aprobación/rechazo del 20-10 no está materializada en los documentos versionados; se conserva como evidencia aportada por el solicitante y no se convierte en resultado automatizado. El retiro/eliminación por `DELETE /api/v1/estudiante/reclamos/{id}` sigue siendo una funcionalidad separada y legacy `501`.

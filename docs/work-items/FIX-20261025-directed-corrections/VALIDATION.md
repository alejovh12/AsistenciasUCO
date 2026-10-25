# Validation — FIX-20261025-directed-corrections

Estado: IN_PROGRESS — focos en verde; validación completa pendiente por
dependencias de entorno y decisiones contractuales no resueltas.

Fecha de ejecución: 2026-10-25 (America/Bogota).

## Backend — validación focal

Comando:

```text
.\mvnw.cmd -B -ntp "-Dtest=DecanoCoordinadorGestionControllerTest,SesionUtcActivationGuardTest" test
```

Resultado: PASS, exit code 0. Se ejecutaron 17 tests; 0 fallos, 0 errores y 0
skips. Hora reportada por Maven: 2026-10-25T02:18:21-05:00.

Repetición tras centralizar la validación estricta de `app.sesiones.v2.enabled`
(`SesionesV2Activation.isEnabled` + condiciones `Enabled`/`Disabled`, único punto
que interpreta la propiedad; valores inválidos o vacíos fallan al arrancar):
`DecanoCoordinadorGestionControllerTest` 12 tests y `SesionUtcActivationGuardTest`
7 tests, 0 fallos, 0 errores, 0 skips (cobertura focal, no el gate global).

Cobertura: el comando fue focal y no constituye el gate global JaCoCo.

## Frontend — validación focal

Checkout oficial: `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`.

Servicios y contratos:

```text
npm run test:ci -- --include=src/app/core/services/session-v2-default.spec.ts --include=src/app/core/services/session.service.spec.ts --include=src/app/core/services/session.service.contract.spec.ts --include=src/app/core/services/session.service.nombre.spec.ts
```

Resultado: PASS, exit code 0. 26/26 tests.

Vistas y flujo Golden Path:

```text
npm run test:ci -- --include=src/app/core/golden-path-real-mode.spec.ts --include=src/app/features/dean/dean-grupo-detalle/dean-grupo-detalle.component.spec.ts --include=src/app/features/dean/dean-faculty/dean-faculty.component.spec.ts --include=src/app/features/teacher/teacher-grupos/teacher-grupos.component.spec.ts
```

Resultado: PASS, exit code 0. 33/33 tests. La salida conserva warnings ya
existentes de `NG0205: Injector has already been destroyed` y specs sin
expectations; no cambiaron el exit code.

Los dos comandos focales reportaron cobertura inferior al gate global porque no
ejecutan la suite completa: respectivamente statements 68.56% / 40.75% y
branches 45.67% / 17.56%. No se interpreta como incumplimiento del gate del
proyecto sin ejecutar la validación completa del frontend.

## Backend — verify completo

Comando:

```text
.\mvnw.cmd -B -ntp verify
```

Resultado: NO PASS. La sesión del proceso ya había terminado cuando se intentó
recuperar el exit code, por lo que el código numérico no quedó disponible. Los
reportes generados registran 2,233 tests, 2 failures, 49 errors y 1 skipped en
391 reportes de Surefire/Failsafe.

Los errores reproducidos se concentran en infraestructura de prueba y servicios
externos del entorno:

- dobles de Keycloak con `java.net.SocketException: Permission denied: connect`;
- dobles de ClamAV con `Permission denied: getsockopt`;
- pruebas Failsafe de upload/descarga y MinIO afectadas por el mismo entorno de
  servicios/socket.

Esto no se clasifica como fallo del cambio focal. Requiere repetir `verify` en
un entorno que permita los sockets y servicios requeridos antes de declarar el
gate completo.

## Alcance no ejecutado o bloqueado

- No se ejecutó la integración contra una base de datos controlada desde este
  work item ni se modificó el repositorio DB.
- No se ejecutó la suite completa del frontend ni su gate global de cobertura;
  solo los focos descritos arriba.
- No se ejecutaron checks CI remotos.
- No se hizo push, merge ni creación/modificación de PR.
- La semántica posterior a aprobar un reclamo continúa bloqueada por contrato:
  en `gestion-asistencia-db/schema/stored-procedures/usp_resolver_solicitud_revision_asistencia.sql`
  el SQL observado conserva `asistio=1` y razón `SJC`, mientras
  `ConsultarSesionesMateriaEstudianteUseCaseImpl` clasifica razones distintas
  de `AN` como `NO_DISPONIBLE`. No se inventó una traducción ni se modificó SQL
  o proyección. El responsable del contrato HU035/HU036 debe decidir si la
  aprobación cambia la razón a `AN` o si `asistio=1` + `SJC` representa otro
  estado público; hasta entonces es `CONTRACT_CONFLICT` para ese flujo.
- La documentación de activación UTC y el catálogo de HU de Infra presentan
  evidencias que requieren alineación antes del cierre global. El work item no
  elige entre esas fuentes ni cambia DB/Infra: es `BLOCKED_BY_MISSING_EVIDENCE`
  para el cierre global UTC hasta que el owner contractual aporte la decisión
  versionada y la evidencia de despliegue correspondiente.

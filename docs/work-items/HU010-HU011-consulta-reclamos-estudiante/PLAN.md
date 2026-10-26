---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# PLAN — HU010/HU011 (consulta): el estudiante consulta sus solicitudes de revisión

## 1. Identidad y objetivo

- **Historias:** HU010 (registrar solicitud de revisión) y HU011 (adjuntar soporte), en su parte de **consulta por el estudiante**: ver sus propias solicitudes, su estado, la respuesta del docente y el soporte adjunto.
- **Work Item ID:** `HU010-HU011-consulta-reclamos-estudiante`
- **Fecha:** 2026-10-26
- **Base Git:** `develop` (DB `b8d07bf`, backend `19268de`, frontend `d592bff`); rama `feature/HU010-HU011-consulta-reclamos-estudiante`.
- **Clase de cambio:** funcional aditivo (nueva lectura). **Variable principal:** `GET /api/v1/estudiante/reclamos` deja de responder 501.
- **Autorización:** el owner del dominio de reclamos/soportes (LB-004D) autorizó solo el `GET` (2026-10-26). `DELETE /estudiante/reclamos/{id}` sigue bloqueado por TD-059 y HU014 por TD-060.

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| `GET /api/v1/estudiante/reclamos` responde 501 (`FeatureUnavailableException`) | `EstudiantePortalController#consultarReclamos` |
| Radicar (`POST /api/v1/asistencias/revisiones`), subir/descargar soporte (`/api/v1/archivos/**`) y bandeja/detalle/resolución del docente existen | WI `LB-004-stateless-serverless-readiness`, `HU035-HU036-bandeja-detalle-reclamos-docente` |
| `usp_consultar_solicitudes_revision_asistencia_paginado` existe pero **no** filtra por ejecutor (no sirve para el estudiante) | SP en `schema/stored-procedures/` |
| `usp_consultar_solicitudes_revision_docente` (R2) filtra por titularidad del docente dentro del SP | SP LB-004D |
| El frontend no consulta la lista del estudiante; solo muestra `estadoSolicitudRevision` por sesión (LB-004D-F3) | `attendance-claim.service.ts`, `student-courses` |

### Criterios de aceptación

1. Un estudiante autenticado obtiene **solo sus** solicitudes, paginadas y ordenadas por fecha descendente, con estado, justificación, respuesta del docente, sesión, grupo, asignatura y metadatos del soporte adjunto.
2. Puede filtrar por estado (`P`/`A`/`R`).
3. Un usuario sin estudiante institucional recibe 403; otro estudiante nunca ve solicitudes ajenas (0 filas).
4. Paginación fuera de rango se acota (página mínima 1, tamaño 1..100) y un estado desconocido ⇒ 400, igual que la bandeja del docente.
5. Los contratos de LB-004D (radicar, soportes, docente) no cambian.

## 2. Alcance

| Capa | Cambio | Archivos |
|---|---|---|
| DB | SP de lectura NUEVO `usp_consultar_solicitudes_revision_estudiante` (propiedad en el SP), permisos aditivos, prueba y exclusión de lectura en `test_contract_closure.sql` | `schema/stored-procedures/usp_consultar_solicitudes_revision_estudiante.sql` (NUEVO), `schema/security/rol_asistencias_runtime_hu010_hu011.sql` (NUEVO), `test/test_consultas_reclamos_estudiante.sql` (NUEVO), `test_suite.sql`, `test/test_contract_closure.sql` |
| Backend | Feature `estudiante/consultarreclamos` (puerto, caso de uso, interactor), query port + repository JPA (native EXEC), `GET /estudiante/reclamos` real | NUEVOS bajo `application/features/estudiante/consultarreclamos/`, `SolicitudRevisionEstudianteQueryPort`, `SolicitudRevisionEstudianteJpaRepository`, `SpSolicitudRevisionEstudianteEntity`; modificado `EstudiantePortalController` (solo el método `GET /reclamos`) y su wiring |
| Frontend | `AttendanceClaimService.getReclamosEstudiante`, mapper, pantalla «Mis Reclamos» (`/app/estudiante/reclamos`) con filtro de estado, paginación y descarga del soporte | NUEVOS: componente `student-claims`; modificados: servicio, mapper, rutas, menú, interceptor mock |

**No alcance:** `DELETE /estudiante/reclamos/{id}` (TD-059), HU014 / categoría (TD-060), cambios a tablas, a SP congelados, a `SecurityConfig` (la regla `/api/v1/estudiante/**` ya existe), a contratos LB-004D, al repo Infra.

## 3. Consumidores, seguridad y observabilidad

- Consumidor único: frontend del estudiante.
- Seguridad: capa HTTP `/api/v1/estudiante/**` (ESTUDIANTE) + caso de uso exige estudiante institucional + propiedad en SQL (`Estudiante.usuario = ejecutor`).
- Observabilidad: correlación propagada al SP (`@idCorrelacion`); errores técnicos por `JpaQueryExecutor`.

## 4. Decisiones

- **D1:** Mismo patrón que R2 del docente (tablas base + filtro de propiedad en el SP), para coherencia con LB-004D; no se usa `SESSION_CONTEXT`.
- **D2:** Estado de soporte adjunto con el parámetro de catálogo `ARCHIVO.ESTADO_ADJUNTO` (sin literal).
- **D3:** Datos personales mínimos: el estudiante ve sus solicitudes; no se exponen datos del docente.

## 5. Riesgos, rollback y parada

- Riesgo bajo: todo es aditivo. Rollback: revertir commits (el SP y el permiso son nuevos).
- Parada: si se requiere cambiar una tabla o un SP congelado, detener y consultar.

## 6. Definition of Ready

READY: objetivo, criterios, AS-IS/TARGET, archivos, consumidores, seguridad, riesgos, rollback y pruebas definidos; sin `CONTRACT_CONFLICT` abiertos; deuda conocida TD-059/TD-060 fuera de alcance.

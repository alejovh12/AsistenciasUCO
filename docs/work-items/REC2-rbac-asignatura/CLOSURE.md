---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-29
---

# CLOSURE — REC2: el backend envía el ejecutor a los SP de asignatura

## Contexto

La auditoría de la BD (2026-10-28, severidad alta) detectó que `usp_actualizar_asignatura` y `usp_toggle_estado_asignatura` no recibían `@idUsuarioEjecutor`. La BD los corrige en su work item `REC2-rbac-asignatura`:

- el ejecutor pasa a ser obligatorio (`GEN_002`);
- se exige el perfil `COORDINADOR` (`SEC_001`);
- se exige la titularidad del programa de la asignatura (`SEC_002`).

Esto completa en la BD lo que `P0-SEC-001` ya aplicaba en el caso de uso.

## Cambio en el backend

- `AsignaturaCommandPort`: `actualizarAsignatura(..., UUID usuarioEjecutor)` y `toggleEstadoAsignatura(UUID, UUID usuarioEjecutor)`.
- `AsignaturaJpaRepository`: los dos `EXEC` envían `@idUsuarioEjecutor = :idUsuarioEjecutor`.
- `GestionarAsignaturaUseCaseImpl`: pasa el ejecutor autenticado que ya resolvía.
- No cambian las rutas HTTP, los DTO ni las respuestas. El `ForbiddenException` del caso de uso sigue siendo la primera barrera.
- Pruebas actualizadas:
  - unitarias: `GestionarAsignaturaUseCaseImplTest` y `AsignaturaJpaCommandPatternTest`, que ahora verifican que el ejecutor llega al SP;
  - de integración: `SqlStoredProcedureContractIT` (firma), `AcademicUserCommandsSpParityIT`, `AcademicUserCommandsSuccessParityIT` y el oráculo JDBC.
- `docs/contracts/HTTP_AS_IS_MATRIX.md`: la mutación de asignaturas queda solo para el COORDINADOR titular.

## Despliegue

Hay que desplegar **junto con** el PR de BD `fix/REC2-rbac-asignatura`, la BD primero:

- este backend contra la BD vieja falla porque el SP no acepta el parámetro;
- el backend viejo contra la BD nueva responde `GEN_002`.

Rollback: revertir ambos PR juntos.

## Nota para Alejo (PDP)

Este cambio no modifica la política PEP/PDP. La regla de la BD es la misma del caso de uso (coordinador del programa de la asignatura); solo agrega defensa en profundidad.

## Validación

- `mvn verify` (2026-10-29): `Tests run: 2661, Failures: 0, Errors: 0`, `All coverage checks have been met`, `BUILD SUCCESS`.
- BD (`fix/REC2-rbac-asignatura`): réplica del DB Quality Gate con `TOTAL_EXECUTED=368`, `FAILED=0` y `DB GATE PASS` (RA-01..RA-04).
- Pendiente: los IT contra SQL Server real (`SqlStoredProcedureContractIT` y las paridades de asignatura) corren en el pipeline con BD. Se ajustaron a la firma nueva.

# MAINT-003L — Cierre

## Resultado

`PARCIAL — PENDIENTE` para cierre funcional completo. Backend, SQL Server, Keycloak y HTTP real están preparados y verificados (PASS). La validación visual en el frontend real de AsistenciasUCO fue ejecutada manualmente por el usuario para el coordinador y el docente de 300 estudiantes (PASS reportado); los docentes de 1000 y 5000 no tienen validación visual (`NOT_RUN`).

## Alcance entregado y decisiones

- Tres cuentas E2E independientes: `docente.paginacion300`, `docente.paginacion1000`, `docente.paginacion5000`.
- Cada cuenta conserva `sub` propio, claim `idUsuario` del docente sintético y únicamente client role `DOCENTE`.
- Se añadieron dos horarios reservados; el horario preexistente de 300 se reutilizó sin modificación.
- Se añadió backup/rollback, preparación idempotente, cleanup limitado y validación HTTP sanitizada.
- Se alinearon los scripts DEV DB para crear horarios deterministas y limpiar horarios de grupos sintéticos antes de borrar grupos.

## Deuda

Ninguna deuda técnica nueva. La validación visual pendiente es una limitación de ambiente, no un cambio autorizado en frontend.

## Validación manual

Ver `VALIDATION.md`:

- HTTP real: PASS (300/1000/5000, ownership 403, directorio coordinador 200, docente 403).
- Maven `verify`: dos intentos. 2026-10-13 `INTERRUPTED` (Surefire sin progreso observable en `CrearDecanoUseCaseImplTest`; no se declaró PASS); 2026-10-14 PASS, exit `0`, 1534/1534 tests, ArchUnit PASS, JaCoCo LINE 90.63 % / BRANCH 80.51 %. El resultado vigente es el segundo; el primero se conserva como intento histórico.
- Visual Angular (manual, ejecutada y reportada por el usuario después del cierre técnico): login de coordinador, paginación y búsqueda de estudiantes por nombre y por documento PASS; login de `docente.paginacion300` y visualización de su grupo de 300 estudiantes PASS.
- Visual Angular de `docente.paginacion1000` y `docente.paginacion5000`: `NOT_RUN`; no se afirma.

## ADR relacionados

No aplica: no cambió una decisión duradera de arquitectura, contrato o proveedor.

## Elementos pendientes y bloqueos

Completar en el frontend real de AsistenciasUCO el login y la navegación de `docente.paginacion1000` y `docente.paginacion5000`, verificando que cada uno visualiza su grupo y la tabla paginada de 1000/5000. Coordinador y docente 300 ya fueron validados visualmente por el usuario.

## Cambios fuera de alcance

No se tocaron `docente.prueba`, el realm bootstrap, el backend productivo, OpenAPI, estudiantes, matrículas, grupos, asistencias, sesiones, grupos reales, despliegue ni merge.

## Condición de parada

No declarar DONE hasta registrar la validación visual de los docentes de 1000 y 5000 (o una decisión explícita del owner que la excluya del alcance).

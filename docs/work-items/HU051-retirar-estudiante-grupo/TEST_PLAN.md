---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# TEST_PLAN — HU051: retiro de un estudiante del grupo

## Fuentes

[PLAN.md](PLAN.md) (reglas R1-R6 de John), [CONTRACT.md](CONTRACT.md), `SecurityConfig` (`/api/v1/docente/**`), `infra/security-baseline/access-catalog.json`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| R-01 | R1, R2, R3 | Titular retira con motivo | A activo con asistencia, período vigente | EXEC | estado EG, cupos, asistencia, auditoría | `RET_005`; EG = I; cupos +1; asistencia intacta; `AuditoriaEvento` con motivo sin espacios extremos | borrar la matrícula, perder historial, motivo sin auditar | DB real |
| R-02 | Existencia | Retirar dos veces | A ya inactivo | EXEC | mensaje | `RET_002` | doble retiro silencioso | DB real |
| R-03 | R6 | Otro docente / coordinador / estudiante | — | EXEC | mensaje y estado | `RET_001` ×3; EG sigue A | rol ≠ titularidad | DB real |
| R-04 | R3 | Motivo vacío, espacios, nulo, 501 | — | EXEC | mensaje y estado | `RET_004` ×4; EG sigue A | motivo opcional | DB real |
| R-05 | R4 | Período terminado ayer / último día | fechaFin = ayer / hoy local | EXEC | mensaje | `RET_003`; el último día sí permite | comparación UTC o `<=` | DB real |
| R-06 | Existencia | Grupo inexistente | — | EXEC | mensaje | `RET_001` | error técnico | DB real |
| R-07 | Validación | Ejecutor nulo | — | EXEC | estado | 0 y sin cambios | retiro anónimo | DB real |
| R-08 | R5 | Destinatarios | tras el retiro | EXEC lectura | filas | 3 filas ESTUDIANTE/DOCENTE/COORDINADOR del programa; otro docente 0 filas | destinatarios fuera de ámbito | DB real |
| R-09 | Validación | Destinatarios con ejecutor nulo | — | EXEC | error | `THROW` | 0 filas silenciosas | DB real |
| UC-01 | Flujo | Retiro + aviso | docente institucional | execute | orden de llamadas | retira (motivo normalizado) → consulta destinatarios → notifica | avisar antes de confirmar | Unit |
| UC-02 | R5 | Contenido del aviso | — | execute | `Notificacion` | tipo, 3 roles, motivo, grupo y asignatura en el mensaje y datos | aviso incompleto | Unit |
| UC-03 | R6 | Usuario no docente | scope vacío | execute | excepción | 403 sin tocar la DB | confiar solo en el rol | Unit |
| UC-04 | R3 | Motivo nulo/vacío/espacios/501 y 500 | — | execute | excepción | 400; 500 caracteres sí | límites mal | Unit |
| UC-05 | Validación | Actor/grupo/estudiante nulos o DTO nulo | — | execute | excepción | 400 sin DB | NPE | Unit |
| UC-06 | Consistencia | La DB rechaza | port lanza | execute | notificación | sin aviso | avisar un retiro fallido | Unit |
| UC-07 | Mejor esfuerzo | Falla el aviso | notificador lanza | execute | resultado | el retiro no falla | 500 tras retiro exitoso | Unit |
| UC-08 | R5 | Sin destinatarios | lista vacía | execute | notificación | no se notifica | aviso vacío | Unit |
| RP-01..05 | Persistencia | binding, ejecutor nulo, mapeo, vacío, fallo técnico | EM mock | métodos | SQL/params/proyección | `EXEC` nombrados + correlación; `DatabaseOperationException` | SP equivocado | Unit |
| CL-01..03 | Errores | `RET_001..004` | — | classify | código | 404/404/409/400 | 500 genérico | Unit |
| CT-01..04 | HTTP | ok, sin cuerpo, 403, 404 e id inválido | port stub | POST | status | 200 / delega null / 403 / 404 / 400 | defaults en el adapter | MockMvc |
| NT-01..03 | Privacidad | adaptador de log | — | notificar/describir | línea | sin correo ni nombre; tipo y roles | fuga de PII al log | Unit |
| CAT | Seguridad | catálogo vs Spring vs `SecurityConfig` | fila nueva | `AccessCatalogConsistencyTest` | — | consistente | endpoint sin regla | Unit |
| RT-01..07 | UI | modal y padrón | HttpTestingController | interacción | DOM/HTTP | motivo obligatorio ≤ 500, POST con motivo recortado, error del backend visible, botón solo en activos | retirar sin motivo, botón en inactivos | Frontend unit |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES (DB) / NO (backend, frontend)
- ENVIRONMENT: SQL Server 2022 desechable (réplica del job DB Quality Gate).
- MOCK_SUFFICIENT: YES para backend y frontend.
- WHY: titularidad, período, auditoría y cupo viven en la DB.

## Negative / Boundary Coverage

Ejecutor nulo, no docente, otro docente, coordinador, estudiante, grupo inexistente, sin matrícula activa, motivo nulo/vacío/espacios/500/501, período terminado ayer vs. último día, fallo del aviso, sin destinatarios, id no UUID.

## Side Effects / Rollback

Un `UPDATE` de estado + un `INSERT` de auditoría en la misma transacción; las pruebas DB corren dentro de una transacción con ROLLBACK.

## Pruebas RED requeridas

- DB: `test/test_usp_retirar_estudiante_grupo.sql` — fallo esperado: SP inexistentes.
- Backend: `RetirarEstudianteGrupoUseCaseImplTest`, `RetiroEstudianteGrupoJpaRepositoryTest`, `DocenteGrupoEstudianteControllerTest`, `DbFailureClassifierRetiroEstudianteTest`, `LogNotificacionAdapterTest`, conteo 46 en `JpaRepositoryBeanUniquenessTest` — fallo esperado: compilación (clases inexistentes).
- Frontend: `teacher-retiro-estudiante-modal.component.spec.ts`, caso HU051 de `teacher-grupo-estudiantes.component.spec.ts` — fallo esperado: componente/salida inexistentes.

## RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | DB `6fd5413` · backend `0569311` · frontend `73d1119` |
| Archivos de test | ver "Pruebas RED requeridas" |
| Commit del estado RED | DB `64c63fe` · backend `e6d1d88` · frontend `e5afbbb` (solo pruebas) |
| Comando | backend `./mvnw -B -ntp -q test-compile` · DB réplica del gate · frontend `npm run test:ci` |
| Exit code | backend `1` (re-ejecutado sobre `e6d1d88` en un worktree desechable); DB y frontend: fallo determinista por SP/componente inexistente |
| Fallo esperado | ausencia del comportamiento (no fallo ambiental) |

## Congelación

Ninguna prueba cambió después de los commits RED.

## Integración y E2E

DB contra SQL Server 2022 real. E2E manual en MV-008 (pendiente del componente de seguridad) y, en modo baseline, tras reaprovisionar el PDP con la fila nueva del catálogo.

## Falsos positivos que deben evitarse

R-03 refuta un SP que valide solo el rol; R-05 refuta comparar en UTC o con `<=`; R-01 refuta un `DELETE`; NT-01 refuta escribir PII en el log; UC-07 refuta devolver error tras un retiro confirmado.

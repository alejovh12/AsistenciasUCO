---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# TEST_PLAN — HU053/HU054/HU055: solicitudes de ingreso a un grupo

## Fuentes

[PLAN.md](PLAN.md), [CONTRACT.md](CONTRACT.md), diseño aprobado (D1, D2, D4; D3/D5 descartados).

## Behavioral Matrix

| ID | Requirement | Scenario | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|
| S-01 | HU053 | El estudiante solicita | `SIG_006`, fila P sin resolución | solicitud creada en otro estado | DB real |
| S-02 | Reglas | duplicada abierta, ya matriculado, no estudiante, grupo inexistente, período terminado | `SIG_005`, `SIG_004`, `SIG_001`, `SIG_002`, `SIG_003` | duplicados / grupos cerrados | DB real |
| S-03 | HU053 | otros tres estudiantes solicitan | 3 éxitos | — | DB real |
| S-04 | Ámbito y validación | otro docente, acción inválida, rechazo sin motivo | `SIG_007`, `SIG_009`, `SIG_010` | rol ≠ titularidad | DB real |
| S-05 | HU053 | aceptar con el único cupo | `SIG_011`, `A`, matrícula activa enlazada, `docenteResolutor`, cupo 0 | aceptar sin matricular | DB real |
| S-06 | HU055 | aceptar sin cupo / reintentar | `SIG_012` → `E`; luego `SIG_013` | sobrecupo | DB real |
| S-07 | HU054 | rechazar con motivo / resolver de nuevo | `SIG_014` → `R` con motivo recortado; `SIG_008` | motivo perdido / doble resolución | DB real |
| S-08 | HU055 + D2 | se libera cupo (HU051) y el docente acepta a la **segunda** de la lista | `A` para la elegida; la primera sigue `E` | orden estricto forzado | DB real |
| S-09 | Consultas | bandeja (orden, posición, cupo, filtro, ámbito) y mis solicitudes | según contrato; otro docente 0 filas; solo las propias | fuga de ámbito | DB real |
| S-10 | HU055 | el estudiante retira (ajena, propia, dos veces) y vuelve a solicitar | `SIG_007`, `SIG_015` → `X`, `SIG_008`, `SIG_006` | índice único bloquea re-solicitud | DB real |
| S-11 | Avisos | destinatarios (docente, propio estudiante, terceros) | ESTUDIANTE + DOCENTE / 0 filas | fuga de correos | DB real |
| S-12 | Validación | consultas con ejecutor nulo | `THROW` ×3 | 0 filas silenciosas | DB real |
| RS-01..07 | HU053/054/055 | resolver (aceptar → ACEPTADA/EN_ESPERA, rechazar con motivo, 400, 403, rechazo DB sin aviso, aviso fallido) | según contrato; aviso solo al estudiante | aviso a destinatarios equivocados | Unit |
| SI-01..03 | HU053 | solicitar (id generado, aviso al docente, 403, 400) | según contrato | — | Unit |
| RW-01..03 | HU055 | retirar (aviso al docente, 400, 403) | según contrato | — | Unit |
| CD-01..04 / CE-01..03 | Consultas | normalización de estado, paginación acotada, mapeo, 403 | según contrato | estado libre al SP | Unit |
| RG-01..06 | Persistencia | binding de los 6 SP, extra `codigoEstadoResultante`, fechas UTC (LocalDateTime y Date), fallo técnico | según contrato | SP equivocado | Unit (EM mock) |
| CTD-01..03 / CTE-01..04 | HTTP | bandeja, resolver, 403/404/409, solicitar, mis solicitudes, retirar | según contrato | — | MockMvc |
| CLS-01..04 | Errores | `SIG_*` | 403/404/409/400 | 500 genérico | Unit |
| CAT | Seguridad | catálogo de acceso vs Spring vs `SecurityConfig` | consistente con 4 filas nuevas | endpoint sin regla | Unit |
| SV/SE/TS/MK | UI | servicio, página del estudiante, pestaña del docente, mock | ver TEST_PLAN del frontend | — | Frontend unit |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES (DB) / NO (backend, frontend)
- ENVIRONMENT: SQL Server 2022 desechable (réplica del job DB Quality Gate).
- MOCK_SUFFICIENT: YES para backend y frontend.

## Negative / Boundary Coverage

Ejecutor nulo, no estudiante, no docente, otro docente, otro estudiante, grupo inexistente, período terminado, duplicada, ya matriculado, ya resuelta, sin cupo (P → E y E sigue E), acción inválida, motivo vacío/espacios/501, estado desconocido, paginación 0/500.

## Limitación conocida

No hay prueba automática de **dos sesiones concurrentes** aceptando el último cupo: la suite DB corre en una sola sesión `sqlcmd`. La protección es el `UPDLOCK, HOLDLOCK` sobre la fila del grupo (patrón ya usado en `usp_eliminar_periodo_academico`); se valida en MV.

## RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | DB `a01a3de` · backend `06aa560` · frontend `df925f2` (rama apilada sobre HU051) |
| Commit del estado RED | DB `c9c6b9a` · backend `9e357dc` · frontend `1fa63b0` (solo pruebas) |
| Comando | DB réplica del gate · backend `./mvnw -B -ntp -q test-compile` · frontend `npm run test:ci` |
| Fallo esperado | tabla/SP, clases y componentes inexistentes (ausencia del comportamiento) |

## Congelación

Ajuste técnico posterior a RED (sin cambiar aserciones): en `SolicitudIngresoGrupoJpaRepositoryTest` el stub `List.<Object>of(new Object[]{...})` expandía el arreglo como varargs; se reemplazó por `Collections.singletonList(new Object[]{...})`, que es lo que devuelve JPA para una fila.

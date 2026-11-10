---
status: active
type: work-item
scope: database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-10
---

# VALIDATION — HU018 y HU019

Corrida local del 2026-11-10 (madrugada). El código va en el mismo árbol de trabajo que las correcciones de `CERT-CULM001-CORRECCIONES`, así que las corridas las incluyen.

| Capa | Comando | Resultado |
|---|---|---|
| BD | `deploy_schema.ps1` + `test/prepare_lb004d_fixture.ps1` + `test_summary.ps1` en un contenedor SQL Server 2022 limpio (mismo orden que el CI) | `TOTAL_EXPECTED=530`, `TOTAL_EXECUTED=708`, `PASSED=707`, `FAILED=0`, `SKIPPED=1` (permitido), `CRITICAL_MISSING=0`, `DB GATE PASS`. Pasan los 9 casos nuevos |
| Backend | `.\mvnw.cmd -B -o clean verify` (Java 25) | `BUILD SUCCESS`: 4221 tests, 0 fallos, 0 errores, 0 omitidos; "All coverage checks have been met" |
| Frontend | Pasos de `npm run verify`, uno por uno | 2691 de 2691 SUCCESS; statements 93.06 %, branches 79.38 %, functions 93.51 %, lines 94.04 %; realtime line 95.26 % y branch 86.81 %; build de producción exit 0 |
| Catálogo de rutas | `provision_asistencias.py catalog` | 198 endpoints en el catálogo, 0 sin regla, 0 obsoletos |
| PDP del lab | `provision_asistencias.py pdp` (sin `--prune`) y `verify` | 0 recursos faltantes y 0 concesiones faltantes: la ruta nueva quedó registrada. `clean: false` por lo que ya había antes de este trabajo: concesiones sobrantes (ADMINISTRADOR 59, DECANO 2, COORDINADOR 9, DOCENTE 6, ESTUDIANTE 6) y 34 recursos huérfanos de rutas retiradas. Revocarlas pide `--prune`, que necesita la aprobación de John |

Pruebas nuevas del backend: 23 de HU018/HU019 (`ConsultarPlanEstudioEstudianteUseCaseImplTest` 7, `PlanEstudioEstudianteJpaRepositoryTest` 4, `EstudiantePlanEstudioControllerTest` 4, `ConsultarMateriasEstudianteHorasProgramadasTest` 4 y `HorasProgramadasEstudianteJpaRepositoryTest` 4). Las otras 4 de la diferencia con la corrida anterior (4194) son de `MessageCatalogJpaMarcadoresSecuencialesTest`, del work item de correcciones.

## Pruebas de los scripts de seguridad

`python -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"`: 61 pruebas. En este equipo fallan por el entorno, no por el cambio: desde la ruta real fallan 3 por las rutas largas de Windows (`FileNotFoundError`), y desde la unidad `subst X:` fallan otras 4 porque comparan rutas de dos unidades distintas. Cada prueba pasa en uno de los dos entornos, y `catalog` (que lee las mismas fuentes) pasa.

## Revisión en pantalla (2026-11-10, 03:26–03:43)

Con el estudiante y el administrador de prueba del lab, la BD local con la migración 30 y el backend con el jar de la corrida de arriba. El detalle está fuera de los repos, en la sección "Segunda pasada" de `informes/CERTIFICACION_FASE3_CULM001_2026-11-10/RECERTIFICACION.md`.

| Ítem | Resultado |
|---|---|
| HU019, barra de horas | **PASA.** La tarjeta de Arquitectura de Software muestra "64% del curso dictado · 1.33 h asistidas · 6.97 h dictadas a la fecha · 10.97 h programadas" y "3 créditos". `GET /api/v1/estudiante/materias` → 200 con `horasProgramadas: 10.97` y `sesionesProgramadas: 6`. En la BD, `uv_cumplimiento_sesion_grupo` da 6 sesiones y 10.966666 h. Texto alternativo: "Asististe a 1.33 h de 6.97 h dictadas. El grupo tiene 10.97 h programadas." |
| HU018, plan de estudios | **PASA.** `GET /api/v1/estudiante/plan-estudio` → 200. "Ingenieria de Sistemas · Plan de estudios 2024 · Semestres 8 · Asignaturas 1 · Créditos del plan 3 · Cursas hoy 1 asignatura · 3 créditos". El semestre 8 abre con ARQ-402, "3 créditos · Obligatoria · Específica", "Cursando" y "Grupo: Grupo 001 - Arq Software". Coincide con `uv_asignatura_plan_estudio` y `uv_semestre_plan_estudio`. El acordeón cierra y abre; la consola no muestra errores |
| Privacidad | **PASA.** Con la sesión del administrador, `/app/estudiante/mis-materias/plan` lleva a "403 · No tienes acceso a esta sección" y no sale ninguna petición al plan. El 403 del backend lo cubren `EstudiantePlanEstudioControllerTest` y la regla `/api/v1/estudiante/**` de `SecurityConfig` |

Nota de los datos del lab: el plan local solo tiene registrado el semestre 8, así que hay un único acordeón y "Semestres 8" es el semestre más alto (regla MAX).

## Desviación

Las pruebas se escribieron junto con el código, sin un commit RED separado.

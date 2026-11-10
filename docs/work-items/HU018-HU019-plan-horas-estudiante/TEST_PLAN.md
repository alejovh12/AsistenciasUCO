---
status: active
type: work-item
scope: database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-10
---

# TEST_PLAN — HU018 y HU019

Las pruebas salen de las reglas y del contrato del [PLAN](PLAN.md). Son archivos nuevos: no se modificó ninguna prueba existente para hacerlas pasar, salvo el conteo de repositorios de `JpaRepositoryBeanUniquenessTest` (69 → 71).

## Backend

| ID | Archivo | Conducta esperada |
|---|---|---|
| PE-UC-01 | `ConsultarPlanEstudioEstudianteUseCaseImplTest` | Agrupa las filas por plan y semestre y conserva el orden que entrega el SP |
| PE-UC-02 | ídem | Los totales salen de las asignaturas; `totalSemestres` es el semestre más alto; una asignatura sin créditos suma 0 |
| PE-UC-03 | ídem | Un semestre sin asignaturas queda con la lista vacía y sigue contando para `totalSemestres` |
| PE-UC-04 | ídem | El componente se traduce (ESP, BAS, INS); el grupo solo se informa si `cursando` es verdadero |
| PE-UC-05 | ídem | Sin estudiante responde `ForbiddenException` y no consulta el plan |
| PE-UC-06 | ídem | Sin filas no hay planes; las dependencias son obligatorias |
| PE-UC-07 | ídem | El interactor entrega el contrato con los totales |
| PE-JPA-01..04 | `PlanEstudioEstudianteJpaRepositoryTest` | Ejecuta el SP con el usuario y la correlación; lee por nombre; conserva los nulos del semestre vacío; el ejecutor es obligatorio; `VAL_001` se traduce a validación |
| PE-C-01..04 | `EstudiantePlanEstudioControllerTest` | 200 con planes, semestres y asignaturas; 200 con lista vacía; 403 sin estudiante; dependencias obligatorias |
| HP-UC-01 | `ConsultarMateriasEstudianteHorasProgramadasTest` | Cada materia lleva las horas y sesiones programadas de su grupo; sin fila queda en nulo, sin tocar los indicadores ni los créditos |
| HP-UC-02 | ídem | Sin materias no se consultan las horas programadas |
| HP-UC-03 | ídem | El constructor sin puerto deja la carga en nulo; el puerto no puede ser nulo |
| HP-UC-04 | ídem | Los constructores cortos del dominio y del DTO dejan la carga en nulo |
| HP-JPA-01..04 | `HorasProgramadasEstudianteJpaRepositoryTest` | Ejecuta el SP; lee por nombre; redondea a 2 decimales; lista vacía; ejecutor obligatorio; `VAL_001` se traduce |
| — | `AccessCatalogConsistencyTest` (existente) | La ruta nueva tiene su entrada en `access-catalog.json` |
| — | `JpaRepositoryBeanUniquenessTest` (existente) | 71 repositorios, uno por puerto |

## Base de datos

`test/test_hu018_hu019_plan_horas_estudiante.sql`, `test/test_hu018_hu019_migraciones.sql` (con `test/test_hu018_hu019.ps1`) y un caso en `test/test_public_objects.sql`. Los nueve identificadores están en el work item homónimo del repositorio de BD.

## Frontend

Las pruebas de la barra de horas y de la pantalla del plan están en el work item homónimo del frontend.

## Validación manual

Con el estudiante de prueba del lab: abrir "Mis materias", comprobar la barra de horas contra `uv_resumen_asistencia_estudiante_grupo` y `usp_consultar_horas_programadas_estudiante`, y abrir la pestaña "Plan de estudios" para comparar con la malla que ve el coordinador.

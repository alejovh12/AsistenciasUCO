---
status: active
type: plan
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W2B - PLAN

Contrato: [CONTRACT.md](CONTRACT.md). Orden de mezcla: BD -> backend -> frontend (cada PR es compatible con el anterior y el siguiente).

| Fase | Entregable | Pruebas |
| :--- | :--- | :--- |
| BD | `uv_materia_estudiante`, `usp_consultar_resumen_materias_estudiante`, `@idGrupo` y orden cronologico en `usp_consultar_sesiones_materia_estudiante`, permiso del runtime | `test/test_w2b_estudiante_resumen.sql` (`W2B_*`) en `test_suite.sql` y `test_summary.ps1` |
| Backend | `consultarPagina` (puerto, caso de uso, interactor) con total real y `grupoId`; feature `consultarresumenmaterias`; `GET /estudiante/materias/resumen`; alta en `access-catalog.json` y `HTTP_AS_IS_MATRIX.md` | `EstudiantePortalControllerTest`, `ConsultarSesionesMateria*Test`, `ConsultarResumenMateriasEstudianteFeatureTest`, `ResumenMateriasEstudianteJpaRepositoryTest`, `SesionMateriaEstudianteJpaRepositoryTest`, `SqlStoredProcedureContractIT`, `AcademicQueryJpaParityIT` |
| Frontend | Sesiones del estudiante paginadas en servidor (se elimina `sesiones-orden.util`); resumen del tablero y de "Mis Materias"; modal del docente sin acciones por defecto | specs de servicio, mapper, simulador y componentes; E2E con el simulador |

## Pruebas actualizadas por cambio formal de regla (no relajadas)

- `SesionMateriaEstudianteJdbcBaseline` (referencia de paridad JDBC): `ORDER BY` pasa a `inicio, numero, id`, el orden que ahora declara el SP; la comparacion campo a campo no cambia.
- `SqlStoredProcedureContractIT`: el SP de sesiones suma `@idGrupo`; se registra el SP nuevo.
- Los `when/verify` de `queryPort.consultarSesionesMateria(...)` pasan a la sobrecarga con `idGrupo` (nulo) porque el caso de uso ahora la usa.
- Frontend: la prueba de orden/paginacion en cliente de `student-course-detail` y la de filtro por `idGrupo` en cliente de `student-courses` afirmaban el comportamiento viejo; se reemplazan por las
  que afirman el nuevo contrato (la vista no ordena ni filtra; el padre pide cada pagina y el grupo al servidor). Se elimina `sesiones-orden.util.spec.ts` junto con la utilidad.

## Verificacion

Ver `VALIDATION.md` (se completa al cerrar las compuertas).

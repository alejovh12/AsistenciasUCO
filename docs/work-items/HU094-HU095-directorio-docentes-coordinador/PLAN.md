---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# PLAN — HU094/HU095: ficha del docente con historial y consolidado del directorio docente del coordinador

## 1. Identidad y objetivo

- **Historias:**
  - HU094 "Buscar la información de un profesor en particular". El catálogo la llama "Ficha docente con historial de grupos dictados".
  - HU095 "Ver la lista de todos los profesores". El catálogo la llama "Directorio consolidado con estado de vinculación".
- **Work Item ID:** `HU094-HU095-directorio-docentes-coordinador`
- **Fecha:** 2026-10-27
- **Base Git:** `develop`, con la rama `feature/HU094-HU095-directorio-docentes-coordinador` en cada repo:
  - DB `fec4503`
  - backend `84ac59d`
  - frontend `f010db3`
- **Clase de cambio:** funcional aditivo de solo lectura: 2 SP de consulta y 2 endpoints GET. Ninguna tabla cambia.

### Alcance acordado con John (2026-10-27)

| # | Tema | Decisión |
|---|---|---|
| A1 | HU094 | Ficha del docente con los datos del vínculo, un **resumen de carga** e **historial paginado** de los grupos que dictó o dicta en el programa, en todos los períodos y con filtro opcional por período. La búsqueda ya la cubre HU063. |
| A2 | HU095 | **Opción A** (recomendada, aprobada): consolidar el directorio del programa con contadores de activos, inactivos y por tipo de vinculación, más un filtro por tipo de vinculación. Se descartó un directorio institucional, porque rompe la regla "el coordinador solo ve su programa". |
| A3 | Ámbito | Solo el programa del coordinador autenticado. Un docente sin vínculo con ese programa responde igual que uno inexistente (404). |

### AS-IS comprobado

| Hecho | Evidencia |
|---|---|
| El directorio del programa ya existe y está certificado: tarjetas, estado, tipo de vinculación y número de grupos | HU062/HU089: `usp_consultar_docentes_programa`, `GET /api/v1/coordinador/docentes` |
| El botón "Ver materias y horarios" filtraba en el navegador solo los cursos actuales del coordinador, sin historial | `CoordinatorDocentesComponent#verFichaDocente` (`CourseService.getCoordinatorCourses`) |
| `usp_consultar_docentes_programa` ya admite `@idTipoVinculacion` e `@idDocente` | SP |
| `usp_resolver_programa_coordinador_interno` resuelve el programa del ejecutor; lanza `KPI_001` si no coordina | HU065-HU070 |

### Criterios de aceptación

1. **HU094 — ficha:** el coordinador abre la ficha de un docente de su programa y ve:
   - los datos del vínculo;
   - el resumen de carga: grupos dictados, períodos, grupos vigentes y estudiantes activos;
   - el historial de grupos del más reciente al más antiguo, con el estado del período (VIGENTE, FINALIZADO o PRÓXIMO), los cupos y las cifras de matrícula.
2. **HU094 — vínculo inactivo:** un docente con vínculo inactivo conserva su historial visible.
3. **HU094 — errores y paginación:**
   - un docente de otro programa o inexistente responde 404;
   - un usuario sin programa a cargo responde 403;
   - una paginación fuera de rango responde 400.
4. **HU095 — consolidado:** el directorio muestra el total, los activos y los inactivos del programa, y un contador por tipo de vinculación. Usa el mismo criterio de "activo" del directorio: vínculo, cuenta y programa activos.
5. **HU095 — filtro:** el contador de un tipo, o el selector, filtra el directorio en el servidor y vuelve a la página 1. El consolidado se relee tras vincular, editar o cambiar el estado de un docente.

## 2. Alcance

| Capa | Cambio | Archivos |
|---|---|---|
| DB | SP `usp_consultar_historial_grupos_docente_programa` (HU094); SP `usp_consultar_resumen_docentes_programa` (HU095); permisos aditivos; 2 pruebas | `schema/stored-procedures/` (2 NUEVOS), `schema/security/rol_asistencias_runtime_hu094_hu095.sql`, `test/test_usp_consultar_historial_grupos_docente_programa.sql`, `test/test_usp_consultar_resumen_docentes_programa.sql`, `test_suite.sql`, `test/test_contract_closure.sql` |
| Backend | Features `coordinador/consultarfichadocente` y `coordinador/consultarresumendocentes`; puertos `HistorialGruposDocenteQueryPort` y `ResumenDocentesProgramaQueryPort`; `DocenteProgramaReporteJpaRepository`; `CoordinadorDocentesReporteController` (`GET /api/v1/coordinador/docentes/{idDocente}/ficha` y `GET /api/v1/coordinador/docentes/resumen`); `GEN_001` → 404 en el historial; 2 filas en `access-catalog.json`; conteo de repositorios 47 → 48 | NUEVOS + `DocenteProgramaWiringConfiguration`, `DbFailureClassifier`, `access-catalog.json`, `JpaRepositoryBeanUniquenessTest` |
| Frontend | `CoordinatorDocenteFichaModalComponent` reemplaza el modal que filtraba cursos en el cliente; contadores, chips y selector de tipo de vinculación en `CoordinatorDocentesComponent`; `getFichaDocente` y `getResumenDocentes`; mock | `features/coordinator/coordinator-docentes/**`, `core/services/coordinator-management.service.ts`, `core/models/docente-programa.model.ts`, `mock-backend.interceptor.ts` |

**No alcance:**
- tablas u objetos congelados;
- `SecurityConfig`: la regla `/api/v1/coordinador/**` ya cubre las rutas;
- un directorio institucional de todos los docentes (descartado en A2);
- el horario y aula de cada grupo dentro de la ficha (sigue en las pantallas de grupos y horarios);
- el repo Infra.

## 3. Consumidores, seguridad y observabilidad

- **Consumidor:** pantalla "Gestión de Docentes" del coordinador (`/app/coordinador/docentes`).
- **Seguridad**, en capas:
  1. `/api/v1/coordinador/**` (COORDINADOR o ADMINISTRADOR).
  2. PEP del Security Baseline con las 2 filas nuevas del catálogo.
  3. La Application exige un programa a cargo (`InstitutionalScopePort`).
  4. Los SP resuelven el programa del ejecutor en tablas base y nunca lo reciben del cliente.
- **Datos personales:** la ficha reutiliza los campos que el directorio ya expone (documento, nombre, correo y vínculo). No agrega datos nuevos.
- **Observabilidad:** la correlación se propaga a ambos SP. Las operaciones `consultarHistorialGruposDocente` y `consultarResumenDocentesPrograma` se registran en `JpaQueryExecutor`.

## 4. Decisiones

- **D1:** La cabecera de la ficha reutiliza `usp_consultar_docentes_programa` con `@idDocente` (mismo contrato que el directorio). El SP nuevo solo devuelve el historial y su resumen. Si el directorio no devuelve el docente, el caso de uso responde 404 antes de consultar el historial.
- **D2:** Las columnas `resumen*` del historial agregan todo el historial del docente en el programa (no dependen del filtro ni de la página) y se repiten en cada fila. Sin filas, el resumen es cero.
- **D3:** El historial incluye vínculos inactivos: la ficha es de consulta y el historial no se pierde al inactivar.
- **D4:** El consolidado de HU095 se cuenta en la DB por tipo de vinculación. Incluye los tipos activos sin docentes (en cero) y los inactivos que aún tienen vínculos. Los totales se suman en Application.
- **D5:** Un controlador propio (`CoordinadorDocentesReporteController`) y un repositorio propio (`DocenteProgramaReporteJpaRepository`), para no ampliar los constructores de `CoordinadorDocentesController` y `DocenteProgramaJpaRepository` ni romper sus pruebas.
- **D6:** `GEN_001` se clasifica como 404 solo para la operación del historial. En otras operaciones sigue sin clasificar.

## 5. Riesgos, rollback y parada

- **Riesgo PDP:** las 2 filas nuevas del `access-catalog.json` deben replicarse en el PDP del Security Baseline (`provision_asistencias.py`). **Coordinar con Alejo** al integrar.
- **Desviación visible:** la ficha ya no muestra horario ni aula de los cursos del período (los traía el modal anterior desde el portal del docente). Se reemplaza por el historial completo del servidor.
- **Rollback:**
  - revertir los commits;
  - `DROP PROCEDURE` de los 2 SP;
  - `REVOKE` de los permisos;
  - borrar las 2 filas del catálogo.

  No hay cambios de datos.

## 6. DoR

READY:
- el alcance A1-A3 fue confirmado por John el 2026-10-27 (HU095 opción A);
- no hay tablas nuevas;
- los objetos congelados quedan intactos.

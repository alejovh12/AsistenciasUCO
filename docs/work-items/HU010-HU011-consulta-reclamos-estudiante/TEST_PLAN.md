# TEST_PLAN — HU010/HU011 (consulta): Mis Reclamos del estudiante

## Fuentes

- Requisito: HU010/HU011 (consulta de las solicitudes de revisión propias), alcance acotado por Alejo: SOLO `GET /api/v1/estudiante/reclamos`.
- [PLAN.md](PLAN.md) y [CONTRACT.md](CONTRACT.md) de este work item.
- DB: `usp_consultar_solicitudes_revision_estudiante` (nuevo, NON_GOLDEN_PATH_EXTENSION), espejo de R2 `usp_consultar_solicitudes_revision_docente` (LB-004D).
- Seguridad: ownership en persistencia (`Estudiante.usuario = ejecutor`) + `InstitutionalScopePort.findEstudianteIdByUsuario` en Application.
- Fuera de alcance: `DELETE /estudiante/reclamos/{id}` (TD-059), categoría/HU014 (TD-060), contratos LB-004D (radicación, archivos, bandeja docente).

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| UC-01 | Autorización | Usuario sin estudiante institucional | scope vacío | execute | excepción, query port | 403 `ForbiddenException`, sin consulta | confiar solo en el rol | Unit |
| UC-02 | Validación | Actor nulo | — | execute | excepción | `ValidationException`, sin consulta | NPE / consulta con null | Unit |
| UC-03 | Paginación | Sin parámetros | 1 fila total=7 | execute | args del port, total | `(usuario, null, 1, 10)`, total 7 | defaults distintos / total = size | Unit |
| UC-04 | Filtros | " aprobada ", 0, 500 / "r", 3, 0 | — | execute | args del port | `("A",1,100)` / `("R",3,10)` | sin normalizar / sin acotar | Unit |
| UC-05 | Validación | estado "BORRADA" | — | execute | excepción | 400 `ValidationException`, sin consulta | estado libre al SP | Unit |
| UC-06 | Mapeo | fila con soporte | — | execute | DTO | `PENDIENTE`, UTC sesión, url `/api/v1/archivos/{id}`, bytes | URL de storage / código crudo | Unit |
| UC-07 | Mapeo | sin soporte / sin filas | — | execute | DTO | soporte `null`; total 0 y lista vacía | soporte vacío no nulo | Unit |
| RP-01 | Persistencia | binding | correlación en contexto | consultar | SQL + parámetros | `EXEC dbo.usp_consultar_solicitudes_revision_estudiante` con los 5 parámetros nombrados | SP equivocado (docente) | Unit (EM mock) |
| RP-02 | UTC v2 | procedencia nula | 2 filas | consultar | instantes | UTC solo con procedencia; soporte UTC | afirmar UTC sin procedencia | Unit |
| RP-03 | Paginación | total nulo | — | consultar | total | 0 | NPE | Unit |
| RP-04 | Errores | `PersistenceException` | — | consultar | excepción | `DatabaseOperationException` | fuga de excepción JPA | Unit |
| CT-01 | Contrato HTTP | GET con filtros | port stub | GET `?estado=P&pagina=2&tamano=5` | JSON | campos del contrato, fechas UTC `Z`, sin `estudianteId` | exponer datos del estudiante / formato de fecha | Controller (MockMvc) |
| CT-02 | Contrato HTTP | sin parámetros | — | GET | DTO delegado | nulls (defaults en Application) | defaults duplicados en el adapter | Controller |
| CT-03 | Autorización | sin estudiante | port lanza Forbidden | GET | status | 403 | 500 / 200 vacío | Controller |
| E-01 | Ownership | propias con soporte | fixture A/B | EXEC como A | filas | 2 filas de A, soporte ATTACHED, total 2 | filtro por docente / sin filtro | DB real |
| E-02 | Orden/paginación | 2 filas | — | página 1/2 tamaño 1 | orden | fecha DESC, nombre, id | orden no determinista | DB real |
| E-03 | Estado | filtro `A` | — | EXEC | filas | solo la aprobada | filtro ignorado | DB real |
| E-04 | Ownership | estudiante B | — | EXEC como B | filas | solo la de B | fuga cruzada | DB real |
| E-05 | Ownership | docente/coordinador | — | EXEC | filas | 0 | rol ≠ dueño | DB real |
| E-06 | Paginación | 0/0 | — | EXEC | filas | acotado a 1/10 (no PAG_001) | THROW por página | DB real |
| E-07 | Soportes | parámetro ARCHIVO.ESTADO_ADJUNTO cambiado | — | EXEC | idArchivoSoporte | NULL | literal `'ATTACHED'` quemado | DB real |
| E-08a | Compatibilidad | resultset | — | describe_first_result_set | columnas | mismas columnas y tipos que R2 | divergencia de contrato | DB real |
| E-08b | Validación | ejecutor nulo | — | EXEC | error | THROW VAL_001 | 0 filas silenciosas | DB real |
| SC-01..03 | UI | lista, filtro, paginación | HttpTestingController | interacción | params/DOM | estado/pagina/tamano correctos, texto | paginación local | Frontend unit |
| SC-04 | UI | 403 | — | flush 403 | estado | lista vacía + toast | lista previa visible | Frontend unit |
| SC-05..07 | Soportes | descarga / 404 / sin soporte | — | descargar | petición | `GET /archivos/{fileId}` blob; error y warning | URL de storage | Frontend unit |
| SC-08 | Mock | modo mock | `USE_MOCKS` | servicio | respuesta | forma `ApiListResponse` con total | mock divergente | Frontend unit |

Regresión docente: la suite completa (`DocentePortalController*`, `SolicitudRevisionDocente*`, `test_usp_consultar_*_docente.sql`) debe seguir verde; no se modificó ningún archivo de la bandeja docente.

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES (DB) / NO (backend, frontend)
- ENVIRONMENT: SQL Server 2022 desechable (réplica del job DB Quality Gate).
- MOCK_SUFFICIENT: YES para backend (EM mock + MockMvc) y frontend (HttpTestingController).
- WHY: el ownership y el resultset viven en el SP; se prueban contra el motor real.

## Negative / Boundary Coverage

Actor nulo, usuario sin estudiante, docente/coordinador, estudiante ajeno, estado desconocido, página 0/tamaño 0/500, total nulo, procedencia nula, sin soporte, parámetro de adjunto cambiado, error técnico JPA, 403 y 404 en UI.

## Side Effects / Rollback

Solo lectura. El test DB corre dentro de una transacción con ROLLBACK (E-08b fuera de la transacción porque THROW la aborta).

## Pruebas RED requeridas

- Backend: `ConsultarReclamosEstudianteUseCaseImplTest`, `SolicitudRevisionEstudianteJpaRepositoryTest`, `EstudiantePortalControllerTest` (CT-01..03). Fallo esperado: compilación (clases/puertos inexistentes) — el comportamiento no existe.
- DB: `test/test_consultas_reclamos_estudiante.sql`. Fallo esperado: `Could not find stored procedure 'dbo.usp_consultar_solicitudes_revision_estudiante'`.
- Frontend: `student-claims.component.spec.ts`. Fallo esperado: módulo `./student-claims.component` y `getReclamosEstudiante` inexistentes.

## RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | backend `19268de` · db `b8d07bf` · frontend `d592bff` |
| Archivos de test | ver "Pruebas RED requeridas" |
| SHA-256 o commit del estado RED | backend `c16cb54` · db `e40edcf` · frontend `f01f953` (solo pruebas, sin implementación) |
| Comando | backend `./mvnw -B -ntp test-compile` · db `test_summary.ps1` · frontend `npm run test:ci` |
| Exit code | backend `1` (re-ejecutado sobre `c16cb54` en un worktree desechable, ver [VALIDATION.md](VALIDATION.md)); DB y frontend: fallo determinista por SP/módulo inexistente (no re-ejecutados aparte) |
| Fallo esperado | símbolos/SP/módulo inexistentes (ausencia del comportamiento, no fallo ambiental) |

## Congelación

Pruebas congeladas en los commits RED anteriores; ninguna prueba se modificó después de RED.

## Integración y E2E

DB contra SQL Server 2022 real (contenedor desechable, sin puerto publicado). E2E manual del portal estudiante queda en MV-008 (en espera del componente de seguridad).

## Falsos positivos que deben evitarse

- E-04/E-05 refutan un SP que filtre por docente o no filtre.
- E-07 refuta un literal `'ATTACHED'` quemado.
- E-08a refuta divergencias de columnas con R2.
- CT-01 verifica que `estudianteId` NO se exponga.
- UC-01/UC-05 verifican que no se consulta la DB.

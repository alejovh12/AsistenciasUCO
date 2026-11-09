---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-09
---

# BE-FASE-20261109 — Corrección integral Backend (paginación, consistencia, seguridad)

Base: `develop` = `f26c5e934c0b8778890424961887b3a1932e1546` (PR #102). DB y Frontend solo lectura; Infra solo lectura (se leyeron
las ramas `develop` e `integracion/CULM-001`: sin decisión adicional sobre sesión futura, EX/SJC con `asistio=1` ni ventana de reclamo).
No repite la auditoría general: consolida lo confirmado por cuatro revisiones de solo lectura y lo ejecutado en esta fase.

## 1. Quality Gate de `develop` (Backend CI → SonarCloud)

| Dato | Evidencia (SonarCloud API pública, 2026-11-09) |
|---|---|
| Check fallido | `Backend Quality Gate` (paso *SonarQube Cloud analysis and Quality Gate*) y `SonarCloud Code Analysis`, SHA `f26c5e93` |
| Condición en ERROR | `new_reliability_rating` = 4 (D) frente a umbral 1 (A). Cobertura 92,8 % (≥80), seguridad A, mantenibilidad A, duplicación 1,4 %, hotspots 100 % |
| Histórico | `alert_status` OK hasta el 2026-11-06; cambió a Red el 2026-11-07 00:59 (`72a1675e`). `bugs`: 3 → 5 |
| Causa | Dos issues `java:S9352` CRITICAL/HIGH en `SmtpNotificacionAdapterConfiguration.java:68-69` (repositorio y catálogo de plantillas declarados también en `AdminNotificacionAdapterConfiguration`). Fecha de creación 2026-10-30: la regla apareció en el perfil después de esa fecha |
| Otros abiertos de fiabilidad (sin cambio en esta fase) | `java:S8786` ×2 (tests ArchUnit, MEDIUM), `java:S5998` ×2 (`ValidationPatterns`, MEDIUM), `javabugs:S2259` (`SecurityErrorResponseWriter:49`), `java:S8688` ×3 (INFO). Existían con Gate verde: no se atribuyen al fallo |
| Corrección | PR `fix/sonar-reliability-notificacion-beans`: `@Primary` en los tres beans SMTP + guard de reflexión `NotificacionBeansDesambiguacionTest` (RED reproduce exactamente `plantillaRenderer(PlantillaNotificacionRepository)` y `(PlantillaPorDefectoCatalogo)`) |
| Estado | `CORREGIDO_NO_DESPLEGADO`. El Gate solo se confirma con el análisis del PR/merge en SonarCloud: `NOT_RUN` hasta entonces |

No se rebajó ningún umbral ni se excluyó código. Si tras el análisis persiste D por otra issue, el siguiente candidato son las de la tabla anterior (cada una con su RED).

## 2. Corrección implementada: sesiones de materia del estudiante acotadas a la página

- ID: PAG-EST-SESIONES. Contrato: sin cambio HTTP (`GET /api/v1/estudiante/materias/{materiaId}/sesiones`, golden path no lo incluye; ver HTTP_AS_IS_MATRIX).
- Problema confirmado: el caso de uso leía **toda** la asistencia estudiante×asignatura (HQL con dos left join que multiplican filas) *antes* de pedir la página al SP, y la procedencia de **cada grupo completo** filtrando en Java. Además corría aun con página/tamaño inválidos.
- Causa / capa: Application (orden de lecturas) + Infrastructure (HQL sin acotar). Símbolos: `ConsultarSesionesMateriaEstudianteUseCaseImpl.execute/procedencias`, `AsistenciaSesionEstudianteJpaRepository`, `SesionProcedenciaJpaRepository`.
- Cambio: primero la página (SP, valida ámbito y PAG_001); después, solo con los ids de esa página, `consultarAsistenciaSesiones(idEstudiante, idAsignatura, idsSesion)` y `SesionProcedenciaQueryPort.consultarSesionesPorIds`. HQL `in :idsSesion` en bloques de 1000 (tope 2100 de SQL Server); página vacía = cero consultas; se filtran filas de sesiones fuera de la página y procedencias de otro grupo. No hay SP ni SQL nuevo.
- RED: `ConsultarSesionesMateriaEstudianteAcotadoTest` (4/4 fallaban por el motivo esperado antes del cambio). GREEN: 4/4 + migración de 28 pruebas existentes del caso de uso a las nuevas firmas (misma semántica).
- Adaptador: `SesionesEstudianteAcotadasJpaTest` (aislamiento, deduplicación, bloques 1000/1000/500, HQL sin `idGrupo`).
- SQL Server real (solo lectura, datos vivos): `SesionesEstudianteAcotadasSqlServerIT` 4/4 PASS, 0 skipped — la lectura acotada a una sesión devuelve solo esa sesión y menos filas que la completa; con todos los ids coincide con la completa; ids de sesiones ajenas = 0 filas; procedencia por ids = procedencia del grupo filtrada, sin traer el resto del grupo. `AsistenciaSesionEstudianteJpaParityIT` 3 ejecutadas, 1 skipped (preexistente: no hay datos vivos del caso).
- El método de lectura completa `consultarAsistenciaSesiones(idEstudiante, idAsignatura)` se conserva solo como oráculo de paridad en ITs; el caso de uso ya no lo usa.
- Estado: `CORREGIDO_NO_DESPLEGADO`. Falta validación HTTP real con token (ver §6).

## 3. Paginación: estado por endpoint (revisión de solo lectura, código verificado)

Ya correcto (paginación en SQL con total real; no reimplementar): `GET /api/v1/estudiantes`; grupos de coordinador; sesiones/asistencias de coordinador; sesiones, estudiantes, aforo e indicadores de decano; consultas transversales; docentes de programa; resumen docente; tomas; reclamos estudiante/docente (clamp 1..100); oferta de grupos del estudiante; festivos; cola de notificaciones. Tamaño 1..100 validado en `ConsultaPaginadaRules`.

Pendientes (todos requieren decidir contrato salvo los marcados): 

| Id | Endpoint / símbolo | Problema confirmado | ¿Cambia API? | Estado |
|---|---|---|---|---|
| B4 | `GET /grupos/{id}/asistencias` (`AsistenciaJpaRepository`) | SP con `@tamanoPagina=NULL` + consolidación en Java; filas = estudiantes×sesiones | Sí | `BLOQUEADO_POR_CONTRATO` |
| B1 | `GET /coordinador/estudiantes` (`EstudianteProgramaJpaRepository`) | `getResultList()` sin límite; total = tamaño de lista | Sí | `BLOQUEADO_POR_CONTRATO`. `/api/v1/estudiantes` ya paginado es la alternativa vigente |
| B3 | `GET /grupos/{id}/estudiantes` | lista completa deliberada (decisión A2-C3), 5000 filas validadas en MAINT-003L | Sí | `BLOQUEADO_POR_CONTRATO` |
| B7 | `GET /decano/sesiones` (`ConsultarFacultadDecanoUseCaseImpl.conHorario`) | N+1: hasta 100 consultas HQL (una por grupo de la página) y filtro en Java | **No** | `PENDIENTE` (usa `consultarSesionesPorIds` ya creado) |
| B7b | coordinador: `ConsultarAsistenciaProgramaUseCaseImpl` | mismo patrón por grupo | **No** | `PENDIENTE` |
| B8 | decano: `ConsultarAsignaturasPlanUseCaseImpl` | un `count` por asignatura (`canDecanoAccessPrograma`) | No | `PENDIENTE` |
| B9 | `GestionarAsignaturaUseCaseImpl` | carga todas las asignaturas del plan para un `anyMatch` | No | `PENDIENTE` (BAJA) |
| B2 | `GET /api/v1/docentes` | `findAll` sin ámbito ni límite | Sí | `BLOQUEADO_POR_CONTRATO` |
| B5 | `GET /grupos` | página drenada a lista completa (100 en 100) | Sí | `BLOQUEADO_POR_CONTRATO` |
| B6/B10 | `GET /decano/grupos`, `/coordinador/asignaturas` | sin paginación | Sí | `BLOQUEADO_POR_CONTRATO` |
| A1c | sesiones materia estudiante | `total` de la respuesta = tamaño de página | Sí | `BLOQUEADO_POR_CONTRATO` |

`CUBIERTO_POR_CULM001` (no reimplementar): `PaginacionRequestValidator` y `PAG_001` (400 si `pagina<1` o `tamano` fuera de 1..100), 9 listados admin/decano a `ApiPageResponse`, `/decano/sesiones` con `idGrupo`, estudiantes de programa del coordinador paginados. Cualquier cambio en esos controladores/`GlobalExceptionHandler`/`ApiErrorCatalog`/OpenAPI debe esperar al merge de `integracion/CULM-001` (el golden path pasa a 1.6.0 y cambia el SHA256).

No se repitió la prueba 300/1000/5000: la evidencia de MAINT-003L sigue vigente y esta fase no cambió el endpoint de estudiantes.

## 4. Consistencia académica (Bloque C) — sin cambio de código

Contrato aprobado y vigente: `LB-004D-STUDENT-SESSION-ATTENDANCE-CONTRACT.md` (cualquier combinación fuera de contrato → `NO_DISPONIBLE`) y KPIs HU065 (falta = `asistio=0` ∧ `SJC`; EX no cuenta; futuras no cuentan).

| Caso | Hallazgo | Estado |
|---|---|---|
| Detalles 6352 (`SJC`, `asistio=1`) y 6353 (`EX`, `asistio=1`) | El Backend responde `NO_DISPONIBLE`, no reclamable; mostrar presente/justificada sería inventar semántica | `BLOQUEADO_POR_CONTRATO` — transferencia DB: reclasificar/normalizar los dos detalles (con sus reclamos) o declarar la combinación inválida (CHECK/SP) |
| Sesión futura 9/12/2026 con toma y registros | `estadoTemporal` **no** es FUTURA/EN_CURSO/FINALIZADA: es `EstadoHorarioSesion {CONFIRMADA, INDETERMINADA}` (confirmación de la procedencia UTC). No hay indicador de sesión futura en la respuesta; una futura con `SJC` aparece AUSENTE y `reclamable=true` | `BLOCKED_BY_MISSING_EVIDENCE` — Producto debe definir ventana de reclamo/sesión futura (solicitud nº4 de LB-004D); alternativa DB: no devolver detalles de sesiones no iniciadas |
| Sesión cancelada | `NO_DISPONIBLE` y no reclamable (regla de lectura solo en código/tests; la spec "B.1 §3.2.4" no está en `docs/`) | `RESUELTO` en código; documentar spec |
| `reclamable` vs `plazoJustificacionVigente` | `reclamable` ignora el plazo; combinarlos cambia contrato | `BLOQUEADO_POR_CONTRATO` |
| Excel `ReporteAsistenciaController.estadoPresentacion` | `asistio=true` → "ASISTIO" aunque la razón sea SJC/EX (el estudiante ve `NO_DISPONIBLE`) | `BLOQUEADO_POR_CONTRATO` (política de presentación) |
| `AsistenciaProgramaJpaRepository:105` | `asistio` nulo → `false` | `BLOQUEADO_POR_CONTRATO` (columna normalmente no nula) |
| Resumen docente / horas | El backend solo transporta `uv_resumen_asistencia_estudiante_grupo`; cómo cuenta 6352/6353 depende de la vista | `BLOCKED_BY_MISSING_EVIDENCE` (definición de la vista) |

## 5. Seguridad y ámbitos (Bloque D) — hallazgos verificados, sin cambio de código

Verificado correcto: la identidad del actor sale siempre del JWT (`AuthenticatedUserResolver`), ningún request de lectura acepta id de usuario/estudiante/docente como actor; ownership de `GET /sesiones/{id}` validado en Java para docente, coordinador, decano y estudiante; reclamos con ajeno=404. Vínculo OIDC→identidad: `ExternalIdentityResolver` (issuer+subject, email verificado, coincidencia única y activa).

| Id | Hallazgo | Riesgo | Estado |
|---|---|---|---|
| H1 | `GET /docentes`, `/docentes/{id}`, `/docentes/{id}/asignaciones`: COORDINADOR/ADMIN sin ámbito (el caso de uso ni recibe actor) | ALTO | `BLOQUEADO_POR_CONTRATO` (matriz lo lista sin ownership; el ámbito exige SP o deprecar a favor de `/coordinador/docentes`) |
| H2 | Excel de asistencia: ADMIN activo accede a cualquier grupo; hay una prueba que fija ese comportamiento | MEDIO | `BLOCKED_BY_MISSING_EVIDENCE` (modelo multi-institución) |
| H3 | Multirrol docente+coordinador/admin recibe 403 en `ConsultarAsistenciasPorGrupo`/`ConsultarEstudiantesGrupo` (fail-closed) | MEDIO funcional | `PENDIENTE` decisión de producto (el reporte ya lo resuelve) |
| H4 | Asistencias/estudiantes de grupo: ámbito de coordinador/admin solo en el SP (no leído) | MEDIO | `BLOCKED_BY_MISSING_EVIDENCE` (cuerpo del SP) |
| H5 | `GET /sesiones/{id}`: 404 inexistente vs 403 ajeno (oráculo de existencia, UUID no enumerable) | BAJO | `BLOQUEADO_POR_CONTRATO` |
| H6 | Estudiante retirado conserva lectura en `GET /sesiones/{id}` | BAJO-MEDIO | `BLOQUEADO_POR_CONTRATO`/DB (estados de matrícula) |
| H8/H9 | OpenAPI sobre-declara 404 en v2 por grupo y no cubre el portal estudiante/coordinador | BAJO | `BLOQUEADO_POR_CONTRATO` (cambia SHA256 del OpenAPI) |

Coordinador `uco-coordinador`: el ámbito de programa se resuelve por `uv_coordinador.idUsuario` (un programa; activo primero) y el de grupo por `uv_grupo_programa`; no se asumió equivalencia. **No se revalidó contra el usuario real** porque requiere autenticación con credenciales que esta fase no usa: `NOT_RUN`.

## 6. Validación, HTTP real y despliegue

Ver `VALIDACION.md` en esta carpeta (se completa con los comandos y resultados reales).

## Seguimiento de PR y ajustes de Sonar (2026-11-09)

- [PR #117](https://github.com/alejovh12/AsistenciasUCO/pull/117): configuracion SMTP; dos issues de fiabilidad `java:S9352` corregidas en la rama, test de contexto Spring añadido y correccion posterior de comentario `java:S1135` (`32a3414f`). Nuevos CI/Sonar por verificar.
- [PR #118](https://github.com/alejovh12/AsistenciasUCO/pull/118): sesiones del estudiante acotadas; ajustes posteriores de Mockito `java:S6068` (`404f49ca`) y comentario `java:S1135` (`a1046b26`). Nuevos CI/Sonar por verificar.
- [PR #119](https://github.com/alejovh12/AsistenciasUCO/pull/119): documentacion; fusionar solo con la secuencia y evidencia compatibles.
- Ninguno de estos ajustes de mantenibilidad equivale a validacion HTTP autenticada o despliegue en 8080; ambos siguen `NOT_RUN`.

## 7. Transferencias y decisiones pendientes

- DB (responsable DB): detalles 6352/6353; sesión 9/12/2026 y registros SJC de sesiones no iniciadas; cuerpo y ámbito de `usp_consultar_sesiones_materia_estudiante`/SP de ámbito para evidencia; definición de `uv_resumen_asistencia_estudiante_grupo` ante combinaciones fuera de contrato.
- Producto/Contratos: ventana de reclamo y sesión futura; paginación de B1–B6, B10 y `total` real en sesiones del estudiante; ámbito de `/docentes`; política multirrol; presentación del Excel.
- CULM-001: cualquier cambio en controladores de paginación, SSE, KPIs por horas, cancelación por inasistencia, soportes múltiples o contratos espera su merge; PR #96 (HU156) sigue abierto y con conflictos, no se tocó.

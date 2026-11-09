---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-09
---

# BE-CIERRE-20261109 — Cierre Backend: lecturas acotadas, reconciliación CULM-001 y decisiones pendientes

Continúa [BE-FASE-20261109-correccion-integral](../BE-FASE-20261109-correccion-integral/REPORTE.md); no repite su auditoría.
Base: `develop` personal = `28da4fd7` (PR #117, #118, #119). DB, Frontend e Infra solo lectura en este work item.

## 1. Quality Gate de `develop` (SHA `28da4fd7`)

Fuentes públicas, 2026-11-09. SonarCloud `project_status` de la rama `develop`: **OK** (fiabilidad A, seguridad A, mantenibilidad A,
cobertura 92,8 %, duplicación 1,4 %, hotspots 100 %), análisis del SHA `28da4fd7` a las 08:19 UTC. GitHub check-runs del mismo SHA:
`Backend Quality Gate`, `SonarCloud Code Analysis`, `ArchUnit Architectural Guardrails`, `CodeQL Java Analysis`,
`Trivy Repository…` y `Trivy Production Docker Image Scan` = success; `Dependency Review` = skipped. Cierra el pendiente de
[BE-FASE-20261109 §1](../BE-FASE-20261109-correccion-integral/REPORTE.md). Las issues abiertas no atribuidas al fallo (S8786, S5998,
S2259, S8688) no se tocaron.

## 2. Correcciones Backend (sin cambio de HTTP, puertos, SQL ni DB)

| Id | Rama | Commits | Qué cambia |
|---|---|---|---|
| B7 `PAG-DEC-SESIONES` | `fix/decano-sesiones-procedencia-por-ids` | `c52db755` | `ConsultarFacultadDecanoUseCaseImpl.conHorario`: de una consulta por grupo de la página (hasta 100) a una sola `SesionProcedenciaQueryPort.consultarSesionesPorIds` con los ids de la página (sin nulos ni repetidos, orden de la página). Solo se aplica la procedencia del mismo id y mismo grupo; grupo nulo no coincide. |
| B7b `PAG-COORD-SESIONES` (sobrelectura, no N+1) | `fix/coordinador-sesiones-procedencia-por-ids` | `44081cb4`, `fa390a28` | `ConsultarAsistenciaProgramaUseCaseImpl.conHorario`: ya no lee todas las sesiones del grupo; lee solo las de la página. Ámbito, cancelación, UTC y total intactos. |
| B8 `PAG-DECANO-PLAN` | `fix/decano-asignaturas-plan-ambito-por-programa` (recuperada de `fix/decano-asignaturas-plan-un-chequeo-por-programa`, `914604ea`, que sigue intacta) | `7a436416`, `bf7b457d` | `ConsultarAsignaturasPlanUseCaseImpl`: `canDecanoAccessPrograma` una vez por programa distinto (3+N consultas → 3+P), corta en el primero denegado; mismo 403 y mensaje. |

Las tres ramas parten de `origin/develop` `28da4fd7`, son independientes (archivos distintos) y se pueden fusionar en cualquier orden.
La IT SQL Server nueva solo está en la rama B7 (la de B7b depende de ella para la evidencia de datos vivos).

### Evidencia (2026-11-09, Windows, Java 25, wrapper Maven)

| Paso | Resultado |
|---|---|
| RED B7/B7b (tests escritos por el tester antes de tocar producción, `mvnw test`) | B7: 7/8 nuevos fallan + 2 migrados; B7b: 7/10 nuevos fallan + 3 migrados; causa: `conHorario` llama `consultarSesionesPorGrupo` y nunca `consultarSesionesPorIds`. Las guardas (página vacía, 403, `idGrupo` nulo) pasan antes y después. |
| RED B8 (test de `914604ea` contra el código de `develop`) | 3/5 fallan (`mismo_programa…`, `cada_programa_distinto…`, `plan_grande…`); las guardas de 403/coordinador pasan antes y después. |
| GREEN | Los 52 tests de las cuatro clases B7/B7b + 20 clases ArchUnit/wiring: 0 fallos. B8: `ConsultarAsignaturasPlan*` y reglas de arquitectura OK. |
| `mvnw verify` completo (por rama) | B8 3619, B7 3622, B7b 3624 tests; 0 fallos, 0 errores, 0 omitidas; `jacoco:check` cumplido; BUILD SUCCESS. |
| IT SQL Server real, solo lectura (`-Pintegration`, BD viva) | `SesionesProcedenciaPaginaMultigrupoSqlServerIT`: página de ≥2 grupos por ids == lectura por grupo filtrada a la página → **PASS**. Caso de sesiones **canceladas**: **SKIP honesto** (`assumeTrue`: no hay canceladas en los datos vivos) → la cancelación solo está cubierta a nivel unitario. `SesionesEstudianteAcotadasSqlServerIT` 4/4 PASS. |
| Auditoría independiente (agente `05-auditor`, mismo proveedor de IA, sin ejecutar nada) | PASS condicionado: 0 BLOCKER, 0 MAJOR, 5 MINOR. Corregidos: estilo `distinct()` (B8, `bf7b457d`) y `assertThrows` explícito (B7b, `fa390a28`). Aceptados: RED+impl en el mismo commit (trazabilidad por git limitada; el RED original está en este reporte), IT sin >1000 ids ni orden (página máx. 100 → un bloque), sin test de `idPrograma` nulo. |

### Límites declarados

- CI remoto y SonarCloud de estas ramas: **NOT_RUN** (no hay PR abierto; no se publicó nada).
- HTTP real autenticado, despliegue en 8080 y navegador: **NOT_RUN** (ver §6).
- No se midió el número de sentencias SQL ni tiempos: la mejora se demuestra por forma de la llamada (una sola consulta por ids) y por filas recuperadas.
- Cuando `origin/integracion/CULM-001` llegue a `develop`: `ConsultarFacultadDecanoUseCaseImpl.consultarSesiones` gana `UUID idGrupo` (F-DEC-014, commit `d15a03c4`); los tests de B7 usan la firma de 6 parámetros y deberán adaptarse (conflicto semántico, no textual). Además `GET …/planes-estudio/{id}/asignaturas` pasa a `AsignaturaPlanController`: B8 queda como puente y pasa a código muerto tras ese merge.

## 3. Decisiones pendientes (no se implementó nada; ningún control se desactivó)

### DEC-DOCENTES-01 — ámbito de `GET /api/v1/docentes`, `/{id}`, `/{id}/asignaciones` (H1, ALTO) — `DECISION_REQUIRED`

Hecho: los tres casos de uso no reciben actor ni ámbito; devuelven id, idUsuario, cédula y nombre (y asignaciones) de todos los docentes a
cualquier COORDINADOR o ADMINISTRADOR (`SecurityConfig:139`). `access-catalog.json:127-131` declara «alcance del programa del coordinador»
y el código no lo aplica. No están en OpenAPI ni en el golden path; CULM-001 no los cambia. Frontend solo usa `GET /docentes` en el selector de
usuarios de `admin-decanos` (ADMIN) y `dean-coordinadores` (DECANO, que hoy recibe 403 y cae a datos simulados).

Opciones (a Producto y Seguridad; elegir una):
- **A. Deprecar** a favor de `/coordinador/docentes` (+`/candidatos`), `/admin/docentes`, `/decano/docentes`. Frontend: selector por rol (DTO `idDocente`, paginado), eliminar `registerTeacherFromUser` (ruta retirada). DB: nada. Riesgo: la exposición persiste durante la deprecación.
- **B. Exigir ámbito vía SP**: pasar el actor a los casos de uso y a `DocenteRepositoryPort`; el responsable DB define el SP (`idUsuarioEjecutor`). Cambia el contenido (menos filas) sin cambiar la forma; definir 403/404 fuera de ámbito.
- **C. Restringir a ADMINISTRADOR** (`SecurityConfig:139`, `access-catalog.json`, `RbacSecurityFilterChainTest`, `runtime-security-provider-architecture.md:537`). No resuelve el alcance multi-institución del ADMIN.

Pregunta de Seguridad: ¿el ADMIN debe ver solo su institución? Sin respuesta B y C no cierran el hallazgo. Mientras tanto H1 = `BLOQUEADO_POR_CONTRATO`.

### DEC-LISTADOS-01 — paginación de listados que hoy responden lista completa — `DECISION_REQUIRED`

| Id | Endpoint | Estado |
|---|---|---|
| B1 | `GET /coordinador/estudiantes` | `CUBIERTO_POR_CULM001` solo en `origin/integracion/CULM-001` (`ApiPageResponse`, PAG_001); la rama de la organización no lo tiene |
| B6 | `GET /decano/grupos` | ídem (`AsignaturaPlanController`) |
| B10 | `GET /coordinador/asignaturas` | ídem |
| B3 | `GET /grupos/{id}/estudiantes` | `BLOQUEADO_POR_CONTRATO`: lista completa declarada (OpenAPI, DR-010 opción A) |
| B4 | `GET /grupos/{id}/asistencias` | `BLOQUEADO_POR_CONTRATO`: «lista completa, sin paginación» en el golden path; paginar exige OpenAPI/golden path nuevos |
| B5 | `GET /grupos` | `BLOQUEADO_POR_CONTRATO`: lista por drenaje de 100 en 100; decidir lista o página |
| A1c | `total` de sesiones del estudiante | `BLOQUEADO_POR_CONTRATO`: hoy `total = tamaño de página`; el SP ya devuelve el total real; el FE lo ignora (`datos.length`) |

Aviso: el envelope paginado (`elementos`/`totalElementos`) rompe al Frontend actual, que lee `datos`/`total` en `/coordinador/estudiantes`, `/decano/grupos` y `/grupos`; debe desplegarse en la misma ventana. B9 (`GestionarAsignaturaUseCaseImpl`, `anyMatch` sobre el plan completo) sigue `PENDIENTE` de baja prioridad. Ninguno de estos cambia con SP: queda para el responsable DB.

## 4. Consistencia académica (Bloque C del prompt) — sin cambios respecto al reporte anterior

No hay evidencia nueva que cambie [BE-FASE-20261109 §4](../BE-FASE-20261109-correccion-integral/REPORTE.md): detalles 6352 (`SJC`, `asistio=1`) y 6353 (`EX`, `asistio=1`) → `BLOQUEADO_POR_CONTRATO` (transferencia DB); sesión futura del 9/12/2026 con registros, ventana de reclamo y registros de 2040 → `BLOCKED_BY_MISSING_EVIDENCE` (Producto/DB); KPI vs directorio vs matrícula y horas → depende de la definición de las vistas (DB). No se modificaron históricos ni se inventaron estados. Sesión cancelada y UTC: `RESUELTO` en código y tests.

## 5. Reconciliación con `integracion/CULM-001` (solo lectura, `git log/diff`, sin simular merges)

| Ítem | Backend | Frontend | DB | Estado en develop personal |
|---|---|---|---|---|
| A retiro de solicitudes, B1 sesiones/festivos, B2 tomas QR, golden path 1.3.0, D.1 plazo de excusas | #97–#101 | E-tokens, E2, A/B1/B2/D1 | migraciones ≤16 | **Ya en develop** (PR #102) |
| B2 asignaturas N:M | #107 `0f5afa72`, PDP #108, contrato 1.4.0 #109 | #67 | migraciones 17–18 | Solo integración (dependencia dura de capas) |
| C.1 alertas y cancelaciones | #114, #115 (contrato 1.5.0) | #68 | migración 19 | Solo integración |
| D.2 soportes | #106 | #66 | 21 | Solo integración |
| D.3/D.6 parámetros y auditoría | #104 | #64 | 22, 25 | Solo integración |
| D.4 admisión | #103 | #63 | 23 | Solo integración |
| D.5 carga masiva | #110 | #70 | 24 | Solo integración |
| D.8 indicadores por horas | #112 | #69 | 27 | Solo integración |
| D.9 hallazgos y SSE | #111, #113, #116 (contrato 1.6.0) | #71 | 28 | Solo integración; en develop solo la base FIX-001 |
| Manifiestos post-freeze | — | — | 67 entradas vs 7; drift de 2 hashes del manifiesto de freeze (`usp_generar_sesiones_grupo`, `usp_registrar_asistencias_sesion`) | Solo integración; DB #105 lo corrige y está sin fusionar |

Hechos: `origin/integracion/CULM-001` (`575c0db2`) es superconjunto de `org/integracion/CULM-001` (`8803383a`, solo D.3/D.4/D.6). OpenAPI: develop = 1.3.0 (`6b0408e1…`), integración = 1.6.0 (`0cf11fd2…`). El Backend de integración exige las migraciones DB 17–28 en la BD viva: **no es posible un merge parcial** ni desplegar solo el Backend. No se recomienda merge masivo: requiere paquete SQL del responsable DB, plan de despliegue conjunto y aprobación.

PR abiertos: **Backend #96** (HU156, `dac9a189`), **Frontend #55** y **DB #105**: los tres con conflictos (`dirty`). #96 y #55 están **obsoletos** (los endpoints de solicitudes de matrícula ya los retiró `050229e4`, en develop; solo aportarían el work item `RETIRO-STUBS-MATRICULA-COORDINADOR` y filas documentales) y contradicen D.4, que implementa HU156 como admisión por el administrador (`usp_admitir_estudiante`): **falta una decisión explícita sobre HU156** antes de cerrar o fusionar. Recomendación: cerrar #96 y #55 como superados solo tras esa decisión y rescatando la documentación; #105 debe re-basarse sobre la integración y revisarse por el responsable DB. Ninguno se tocó.

## 6. Bloqueos y NOT_RUN de esta fase

- **Inventario de 113 hallazgos y guiones `SCREEN-TESTS-*` de CULM-001: no están en ningún repositorio ni en disco** (búsqueda en refs de los tres repos y en Downloads/Desktop/Documents/OneDrive). Parecen vivir fuera de git (orquestador de CULM-001). `BLOCKED_BY_MISSING_EVIDENCE`: hace falta que el responsable los aporte (ruta o rama). Hasta entonces el registro por ID solo cubre los IDs de este reporte y de [BE-FASE-20261109](../BE-FASE-20261109-correccion-integral/REPORTE.md).
- Infra (`AsistenciasUCO-Infra`) no está clonada en disco; solo se dispone del directorio `infra/` del Backend.
- Despliegue local, HTTP real GET por rol y navegador: ver §8 (el inicio de sesión en pantalla de los cinco roles sigue `NOT_RUN`).

## 7. Estado por ítem

| Id | Estado |
|---|---|
| B7 sesiones Decano | `CORREGIDO_NO_DESPLEGADO` (rama local, `verify` verde; CI/HTTP/navegador NOT_RUN) |
| B7b sesiones Coordinador | `CORREGIDO_NO_DESPLEGADO` |
| B8 asignaturas del plan | `CORREGIDO_NO_DESPLEGADO`; superado por CULM-001 (solo en `origin/integracion`) |
| PAG-EST-SESIONES (#118) | `CORREGIDO_NO_DESPLEGADO` → en develop; HTTP real NOT_RUN |
| Quality Gate develop | `RESUELTO_VERIFICADO` (Sonar OK + 6 checks success en `28da4fd7`) |
| B1, B6, B10 | `CUBIERTO_POR_CULM001` (solo rama `origin/integracion/CULM-001`) |
| B3, B4, B5, A1c, H1, H5, H6 | `BLOQUEADO_POR_CONTRATO` |
| H2, H4, 6352/6353, sesión futura, ventana de reclamo, vistas de horas | `BLOQUEADO_POR_CONTRATO` / `BLOCKED_BY_MISSING_EVIDENCE` (DB/Producto) |
| B9 | `PENDIENTE` (baja prioridad) |
| HTTP real GET por rol (5 roles, local) | ver §8 |
| Navegador, 5 roles | `PENDIENTE_E2E` (login en pantalla pendiente del usuario) |

## 8. Despliegue local y HTTP real (2026-11-09, autorizado por el usuario)

**Despliegue.** Rama local no publicada `chore/despliegue-local-cierre-20261109` = `origin/develop` + merge local de las tres ramas del §2 y de esta rama documental (sin conflictos). `mvnw -DskipTests package` → jar copiado a `backend-run/AsistenciasUCO-20261109042500-8080.jar` (SHA-256 idéntico al de `target/`). Se detuvo solo el PID del Backend anterior (jar del 08/11) que ocupaba 8080 y se arrancó el nuevo con `start-backend-baseline.ps1` (provider baseline): `/actuator/health` = UP, `/api/v1/auth/me` sin token = 401. Keycloak, SQL Server, MinIO, ClamAV, PDP/PEP y GreenMail no se tocaron. Angular: el `ng serve` existente en 4200 (mismo repo Frontend, observa el árbol de trabajo) sirve la rama `fix/listados-error-carga-decano-admin` (el bundle contiene «Funcionalidad no disponible»); `env.js`: `USE_MOCKS=false`, API `:8080/api/v1`. La SPA carga la pantalla de login sin errores de consola.

**GET real con token PKCE por rol** (lab, solo lectura, sin imprimir claves ni datos personales):

| Rol | Resultado |
|---|---|
| `uco-coordinador` | `/auth/me` 200; **ámbito corregido**: `/coordinador/grupos` 200 con 7 grupos; `/coordinador/grupos/{id}/sesiones` (B7b) 200, 3 sesiones, las 3 `CONFIRMADA`; `/coordinador/estudiantes` 200, 5007. `GET /docentes` 200 (H1 sigue expuesto: sin ámbito) |
| `uco-decano` | `/auth/me` 200; `/decano/sesiones` y `/decano/estructura` → **403 FORBIDDEN** (`exigirDecano`: el usuario de laboratorio **no tiene facultad asignada**); `/decano/docentes` y `/decano/asignaturas` 200 con 0 filas. **B7 y B8 no se pudieron ejercitar por HTTP** (`BLOQUEADO_POR_ENTORNO`: requiere asignar facultad al decano de laboratorio o credenciales de un decano con facultad) |
| `uco-administrador` | `/admin/auditoria` 200 (10 de 18); `/admin/estudiantes` 200 (10 de 5007, 4,0 s) |
| `uco-docente` | `/auth/me` 200; `/docente/horarios` 200 (3 filas) |
| `uco-estudiante` | `/auth/me` 200; `/estudiante/reclamos` 200 (6); **`/estudiante/materias` 200 pero tarda ~64-68 s** (3 mediciones, `durationMs` 67845/64600/64505) |

**Hallazgo PERF-EST-MATERIAS (DB, no Backend).** Ejecutando por separado en la BD (solo lectura, mismo usuario): `usp_consultar_materias_estudiante` 31 ms; **`usp_consultar_resumen_asistencia_estudiante` 63 774 ms** (1 fila). El caso de uso `ConsultarMateriasEstudianteUseCaseImpl` hace una sola llamada a cada SP; la latencia es del SP/vista de resumen (con 5007 estudiantes). Transferencia al responsable DB: revisar plan de ejecución y las vistas que usa el resumen por estudiante. Una pantalla que lo llame supera cualquier timeout razonable del cliente. Estado: `BLOQUEADO_POR_ENTORNO`/DB.

Tampoco se verificó por HTTP la rama del Decano ni el flujo con página >1 de B7; los casos se cubren con unitarias, ArchUnit e IT del adaptador. El inicio de sesión en pantalla de los cinco roles, responsive, teclado/foco, SSE, QR/PIN, formularios y los guiones `SCREEN-TESTS-*` (no localizados) siguen `NOT_RUN`.

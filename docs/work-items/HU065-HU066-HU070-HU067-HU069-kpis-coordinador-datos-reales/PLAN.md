---
status: frozen-implemented
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# PLAN — KPIs y alertas con datos reales (Ola 1: Coordinador) — HU065 / HU066 / HU070 / HU067 / HU069

Fase 01: Planificación y Definition of Ready (DoR), según `GUIA_METODICA_IMPLEMENTACION_HU.md`, `HOJA_DE_RUTA_JERARQUIA_ROLES.md`, `docs/governance/DEFINITION_OF_READY.md` y `AGENTS.md`.
Reemplaza el enfoque de las ramas locales `feature/kpis-coordinador-p1-p2` y `feature/ficha-bitacora-estudiante-coordinador-p3` (ver §3).

---

## 1. Identidad y objetivo

- **Work Item ID:** `HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales`
- **Base Git:** `develop` @ `1b62498`.
- **Actor:** Coordinador de programa (`COORDINADOR`). El `ADMINISTRADOR` pasa el filtro de rutas (`/api/v1/coordinador/**`) pero el caso de uso lo rechaza con 403 si no tiene programa de coordinador (mismo criterio que D9 de HU076).
- **Historias de usuario:**
  - **HU065** Cronograma de sesiones ejecutadas vs programadas del programa.
  - **HU066** Semáforo de alerta temprana de deserción (umbral 15 % / 20 %).
  - **HU070** Cálculo de permanencia por estudiante (Regular, Riesgo, Pérdida).
  - **HU067** Bitácora cronológica de asistencias de un estudiante.
  - **HU069** Ficha integral del estudiante (contacto y faltas).
- **Objetivo:** que toda cifra que ve el Coordinador salga de la DB (SSOT), calculada con la regla institucional del 20 %, con el ámbito (programa del coordinador) forzado dentro de los SP. **Cero valores simulados**: si el dato no existe, la respuesta lo dice (`null`/ausente), no lo inventa.

### Criterios de aceptación

| ID | Criterio |
|---|---|
| CA-01 | `GET /api/v1/coordinador/indicadores` devuelve, para el programa del coordinador y el período vigente: total de estudiantes con matrícula activa, conteo por permanencia (Regular/Riesgo/Pérdida), tasa de permanencia, sesiones programadas, vencidas, ejecutadas y vencidas sin registro, y % de cumplimiento. Todo calculado en DB. |
| CA-02 | `GET /api/v1/coordinador/alertas-tempranas` lista paginada (`pagina`, `tamano`) de estudiantes del programa con su % de inasistencia y semáforo; filtra por `semaforo` y `nombre`. Orden: mayor % primero. |
| CA-03 | La permanencia de un estudiante se clasifica con los umbrales del catálogo de parámetros (`ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA`, `ASISTENCIA/UMBRAL_ALERTA_PERDIDA`), no con números en Java ni en T-SQL. |
| CA-04 | `GET /api/v1/coordinador/estudiantes/{idEstudiante}/bitacora` devuelve las asistencias del estudiante en los grupos del programa, paginadas, en orden cronológico descendente, con grupo, asignatura, sesión, asistió/no asistió y razón. |
| CA-05 | `GET /api/v1/coordinador/estudiantes/{idEstudiante}/ficha` devuelve identificación, nombre, correo, estado y resumen de asistencia por asignatura (sesiones, asistencias, faltas sin justa causa, excusas, % de inasistencia, permanencia). Solo campos que existen en la DB. |
| CA-06 | Un estudiante que no pertenece al programa del coordinador responde **404** (`GEN_001` "Estudiante"): no se revela que existe. |
| CA-07 | Un usuario sin ámbito (rol distinto o ejecutor sin programa) no obtiene filas: 403 por RBAC o 0 filas fail-closed, nunca datos de otro programa. |
| CA-08 | La UI no calcula ni estima: muestra lo que entrega el backend, con estados vacío/carga/error y mensajes del servidor. Sin `setInterval`, sin datos estáticos. |
| CA-09 | Las cifras de la UI coinciden con una consulta de control directa sobre las tablas (prueba de conciliación, ver TEST_PLAN). |

---

## 2. AS-IS verificado (develop, 2026-10-24)

| Dimensión | Evidencia | Consecuencia |
|---|---|---|
| **Modelo de asistencia** | `Sesion(grupo, fechaHoraInicio/Fin)`, `Asistencia(estudianteGrupo, sesion)`, `DetalleAsistencia(asistio, razonCausa)`; catálogo `RazonCausa` (`AN` asistencia normal, `SJC` sin justa causa, `EX` excusa, `CPVP`, `CPI`); `EstadoEstudianteGrupo` (`A`, `I`, `CI`, `CVP`, `F`). Vistas `uv_sesion`, `uv_sesion_v2` (con `procedenciaTemporal`), `uv_asistencia`, `uv_detalle_asistencia`, `uv_estudiante_grupo`. | Hay datos reales suficientes para todo el alcance de la ola 1. |
| **Umbrales** | `CatalogoParametro`: `ASISTENCIA/UMBRAL_ALERTA_ADVERTENCIA = 15`, `UMBRAL_ALERTA_PERDIDA = 20`. | SSOT existente; se consume, no se duplica. |
| **Cálculo previo reutilizable** | `usp_consultar_estudiantes_grupo_paginado` ya agrega faltas (`asistio = 0 AND SJC`), excusas (`EX`) y asistencias por `EstudianteGrupo`; `usp_consultar_asistencias_estudiante_paginado` da el historial de un estudiante **en un grupo** (sin ámbito de programa). | Se generaliza el patrón en una vista de resumen; no se reescribe la lógica desde cero. |
| **Inconsistencia de reglas** | `usp_ejecutar_cierre_masivo_periodo` cancela con `>= 3 inasistencias` (número fijo) mientras la regla institucional y `UMBRAL_ALERTA_PERDIDA` hablan del 20 %. | No se toca en esta ola; se documenta como deuda (D3) y los KPI usan el 20 %. |
| **Ámbito** | `uv_auth_estudiante`, `uv_auth_grupo`, `uv_auth_sesion`, `usp_validar_titularidad_jerarquica_interno` (`PROGRAMA`), `InstitutionalScopePort.findProgramaIdByCoordinadorUsuario`; patrón de contexto de ejecutor de `usp_consultar_grupos_paginado`/`usp_consultar_programas_academicos`. | El ámbito se resuelve dentro del SP. |
| **Datos de contacto** | `Usuario`: tipo y número de identificación, nombres, apellidos, `correo`. **No hay teléfono.** | La ficha no incluye teléfono (D5). |
| **Backend** | Sin endpoints de KPIs en `develop`. HU068 certificada (`GET /coordinador/estudiantes`). `/api/v1/coordinador/**` → `COORDINADOR` o `ADMINISTRADOR` (`SecurityConfig`). | Sin cambio de seguridad. |
| **Frontend** | `coordinator-students.component.ts` (HU068) y `coordinator-management.service.ts` existen; no hay vistas de KPIs ni ficha. | Se agregan, sin tocar HU068. |

### Hallazgo que motiva este plan (ramas locales previas)

Las ramas `feature/kpis-coordinador-p1-p2` (HU065/066/070) y `feature/ficha-bitacora-estudiante-coordinador-p3` (HU067/069) **no se aprueban**:

1. Los repositorios calculan con constantes: riesgo = 5 %, pérdida = 2 %, sesiones ejecutadas = 88 %, ausentismo = 4,8 fijo; la ficha y la bitácora son registros escritos en el código (asignaturas, profesores, promedio 89,6).
2. No hay SP ni vistas: se consulta con HQL desde el repositorio, contra la regla "consultas filtradas por SP".
3. La ficha usa atributos inexistentes (`nombre`, `correo`, `documentoIdentidad`, `telefono`, `programa` en `UvEstudianteIdentidadEntity`) y un `catch (Exception ignored)` que siempre cae a datos inventados.
4. Sin validación de ámbito: cualquier coordinador obtiene la ficha de cualquier `estudianteId`.
5. Las pruebas son mocks triviales y el `VALIDATION.md` declara cumplimiento sin E2E ni DB.

**Qué se rescata:** la estructura de DTO de salida (nombres de campos), el wiring en archivos propios, la idea visual (tarjetas, línea de tiempo, modal) y la ruta `/coordinador/indicadores`. **Qué se descarta:** los repositorios, los valores fijos y los tests. Las ramas quedan intactas hasta que el reemplazo se integre; entonces se marcan como superadas.

---

## 3. TARGET

### 3.1 Base de datos (SQL Server)

| Objeto | Tipo | Responsabilidad |
|---|---|---|
| `uv_resumen_asistencia_estudiante_grupo` | Vista | Por `EstudianteGrupo`: sesiones del grupo ya vencidas, registros de asistencia, asistencias, faltas sin justa causa (`SJC`), excusas (`EX`) y `porcentajeInasistencia` (denominador definido en D1). Una fila por matrícula. |
| `uv_cumplimiento_sesion_grupo` | Vista | Por `Sesion`: `estaVencida`, `tieneRegistroAsistencia` e `idPrograma` (vía asignatura → semestre del plan → plan → programa, igual que `uv_grupo_programa`). Base de HU065. |
| `ufn_clasificar_permanencia(@porcentaje)` | Función escalar | Devuelve el código `REGULAR` / `RIESGO` / `PERDIDA` comparando con los dos parámetros del catálogo. Única implementación de la regla. |
| `usp_consultar_kpis_coordinador` | SP de consulta (1 fila) | HU065 + HU070 agregados del programa del ejecutor. |
| `usp_consultar_alerta_temprana_programa` | SP de consulta paginado | HU066 (+HU070 por estudiante). |
| `usp_consultar_bitacora_estudiante_programa` | SP de consulta paginado | HU067. |
| `usp_consultar_ficha_estudiante_programa` | SP de consulta (1 fila) | HU069 cabecera. |
| `usp_consultar_resumen_asistencia_estudiante_programa` | SP de consulta | HU069 resumen por asignatura. |

Reglas comunes (de `AGENTS.md` §3.3): solo lectura sobre vistas `uv_*`/`uv_auth_*`, contexto de ejecutor aplicado y **siempre restaurado** (éxito y error), `@idUsuarioEjecutor` y `@idCorrelacion` obligatorios, paginación con `OFFSET/FETCH` y `COUNT(*) OVER()`, tamaño máximo del parámetro `PAGINACION/TAMANO_MAXIMO`, sin GUIDs ni textos quemados, mensajes por catálogo. Sin `SET XACT_ABORT ON` (solo lectura, no se debe invalidar la transacción del llamador).

### 3.2 Backend (Spring Boot)

Features nuevas bajo `application/features/coordinador/`: `consultarkpis`, `consultaralertatemprana`, `consultarbitacoraestudiante`, `consultarfichaestudiante` (estructura `primaryports` / `usecase` como `decano/crearprograma`). Puertos `secondaryports/academic/{Kpis,AlertaTemprana,BitacoraEstudiante,FichaEstudiante}QueryPort`. Adapters JPA con `EXEC` nativo + entidades `Sp*Entity` (sin `@Immutable`, regla ArchUnit existente). Controllers nuevos y separados (`CoordinadorIndicadoresController`, `CoordinadorFichaEstudianteController`) y wiring propio (`CoordinadorIndicadoresWiringConfiguration`). Sin `SecurityConfig`.

### 3.3 Frontend (Angular)

Servicio `coordinator-indicators.service.ts` (Signals), componentes en `features/coordinator/coordinator-indicators/` (tarjetas KPI, tabla de alertas con `PaginationComponent` y filtro semafórico) y modal de ficha con línea de tiempo en `coordinator-students` (solo se agrega el disparador, sin alterar HU068). Sin cálculo, sin polling.

---

## 4. Alcance y no alcance

**Alcance:** las 5 HU para el rol Coordinador sobre el período académico vigente (selector de períodos pasados: D8).

**No alcance:** ola 2 (Decano) y ola 3 (Administrador) — ver §8; cambiar el cierre masivo; administrar umbrales (HU154); notificaciones; exportaciones; realtime (D9).

---

## 5. Clase de cambio y perímetro de rutas

- **Change class:** `BEHAVIOR_CHANGE` (funcionalidad nueva) + `CONTRACT_CHANGE` aditivo en DB y HTTP.
- **PRIMARY_VARIABLE:** definición única y verificable de "inasistencia" y de "sesión ejecutada" calculadas en DB.
- **ALLOWED_PATHS:**
  - `GestioAsistenciaBackend/docs/work-items/HU065-HU066-HU070-HU067-HU069-kpis-coordinador-datos-reales/**`
  - `GestioAsistenciaDB/schema/views/uv_resumen_asistencia_estudiante_grupo.sql`, `uv_cumplimiento_sesion_grupo.sql`
  - `GestioAsistenciaDB/schema/functions/ufn_clasificar_permanencia.sql`
  - `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_{kpis_coordinador,alerta_temprana_programa,bitacora_estudiante_programa,ficha_estudiante_programa,resumen_asistencia_estudiante_programa}.sql`
  - `GestioAsistenciaDB/schema/seed/18_mensajes_kpis_coordinador.sql` (archivo propio, no `04_mensaje.sql`)
  - `GestioAsistenciaDB/test/test_usp_consultar_*_coordinador*.sql` (nuevos)
  - Backend: `application/features/coordinador/{consultarkpis,consultaralertatemprana,consultarbitacoraestudiante,consultarfichaestudiante}/**`, `application/secondaryports/academic/**` (puertos y proyecciones nuevos), adapters y entidades `Sp*` nuevos, los dos controllers y el wiring nuevo, y sus tests.
  - Frontend: servicio, modelos y componentes nuevos; solo el punto de entrada (botón "Ficha") en `coordinator-students.component.ts`.
- **ARCHIVOS CALIENTES (solo al final, commit aparte):** `DbFailureClassifier`, `HTTP_AS_IS_MATRIX.md`, `test_suite.sql`, `test_contract_closure.sql` (excepción `READ_QUERY_RESULTSET`), `JpaRepositoryBeanUniquenessTest` (inventario), `HU-CATALOG.md`.
- **FORBIDDEN_PATHS:** contratos WORM (`openapi-golden-path.yaml`, `BACKEND_GOLDEN_PATH_CONTRACT.*`, `DB_BASELINE_CONTRACT.*`), `uv_sesion`/`uv_asistencia`/`uv_detalle_asistencia` (vistas congeladas: se consumen, no se alteran), `SecurityConfig`, `dean-faculty` y `dean-infraestructura`, `usp_ejecutar_cierre_masivo_periodo`, las ramas locales `kpis-*`/`ficha-*` (no se modifican).
- **SECURITY_IMPACT:** ámbito por programa en DB; sin PII fuera de la ficha (nombre, identificación, correo); sin datos de otros programas; sin secretos.
- **OBSERVABILITY_IMPACT:** `CorrelationIdContext.get()` → `@idCorrelacion`; logs sin PII.
- **ROLLBACK:** revertir PR de Front → Back → DB; todo es aditivo (vistas, función y SP nuevos).

---

## 6. Decisiones requeridas (bloquean READY)

| ID | Pregunta | Opciones | Recomendación |
|---|---|---|---|
| **D1** | ¿Cuál es el denominador del % de inasistencia de una matrícula? | (a) Sesiones **vencidas** del grupo (fin ≤ ahora) que tienen registro de asistencia. (b) Todas las sesiones programadas del grupo. (c) Registros de asistencia del propio estudiante. | **(a)**: evita penalizar sesiones futuras o aún no registradas y se alinea con "sesiones dictadas". Se documenta la fórmula en el CONTRACT y se concilia en pruebas. |
| **D2** | ¿Qué cuenta como falta? | (a) `asistio = 0` con razón `SJC`; `EX` (excusa) no cuenta. (b) Todo `asistio = 0`. | **(a)**: es lo que ya hace `usp_consultar_estudiantes_grupo_paginado` y respeta la resolución de excusas del docente. |
| **D3** | La regla vigente del cierre masivo (≥ 3 faltas) difiere del 20 %. ¿Se alinea aquí? | (a) No: se reporta como deuda y los KPI usan el 20 %. (b) Se cambia el cierre masivo. | **(a)**; abrir un work item aparte, porque cambia estados reales de matrícula. |
| **D4** | ¿Cuándo una sesión está "ejecutada"? | (a) Vencida y con ≥ 1 registro de asistencia. (b) Solo vencida. | **(a)**; las vencidas sin registro se informan por separado (`sesionesVencidasSinRegistro`), que es el indicador de incumplimiento. |
| **D5** | La ficha pide "contacto", pero no existe teléfono. | (a) Sin teléfono (solo correo e identificación). (b) DDL para teléfono (otra HU). | **(a)**. |
| **D6** | Estudiante en varios grupos: ¿cómo se clasifica a la persona? | (a) Por matrícula (estudiante×grupo) y la persona toma la **peor** clasificación entre sus matrículas activas. (b) Solo por matrícula. | **(a)** para el semáforo y KPI; el detalle por grupo queda en la ficha. |
| **D7** | Manejo del tiempo (contrato UTC): sesiones con procedencia temporal indeterminada. | (a) Se usa `uv_sesion_v2`; las de procedencia indeterminada no se cuentan como vencidas y se informan en `sesionesSinProcedenciaTemporal`. (b) Se asume una zona. | **(a)** y confirmar con las pruebas de contrato UTC existentes antes del RED. |
| **D8** | ¿Período? | (a) Solo el vigente (por `grupoEstaHablitado`). (b) Parámetro `idPeriodoAcademico` opcional. | **(b)** con defecto = vigente; habilita HU142. |
| **D9** | ¿Evento SSE al registrar asistencia para refrescar KPIs? | (a) No, la pantalla consulta al abrir y bajo demanda. (b) Sí. | **(a)** en esta ola (sin polling). |
| **D10** | Base y ramas. | (a) Ramas desde `develop` en worktrees propios; las ramas `kpis-*` quedan intactas. (b) Rehacer sobre ellas. | **(a)**. |

---

## 7. Riesgos y dependencias

| ID | Riesgo | Mitigación |
|---|---|---|
| R1 | Pocos datos en la BD de desarrollo para ver cifras no triviales. | Los tests DB crean sus propios fixtures (programa, grupos, sesiones, asistencias) dentro de la transacción; el E2E documenta el fixture. |
| R2 | Costo de agregación por programa con muchas sesiones. | Vista de resumen por matrícula + índices existentes; plan de ejecución revisado en la fase 05 (AUDIT). |
| R3 | Semántica temporal (UTC, procedencia). | D7 y pruebas explícitas de sesión futura/vencida/indeterminada. |
| R4 | Choque con la ficha/bitácora de otros roles (docente ya ve estudiantes de su grupo). | Los SP nuevos no reemplazan a `usp_consultar_*_grupo_*`; solo agregan la vista por programa. |
| R5 | Divergencia entre la cifra de KPI y la de la tabla de estudiantes (HU068). | CA-09: prueba de conciliación contra consulta directa. |

**Dependencias:** HU068 y HU072 certificadas (ámbito de programa), HU076-078 (el programa existe con coordinador). No depende de IAM-001.

---

## 8. Hoja de ruta de las olas (fuera del alcance de esta fase, con bloqueos de datos)

| Ola | HU | Estado de datos en DB hoy | Condición para hacerlas "reales" |
|---|---|---|---|
| 2 Decano | HU102 ausentismo por facultad, HU103 comparativa entre programas, HU106 retención, HU144 aforo/grupos activos | Derivables de las mismas vistas de resumen (agregando por programa/facultad) y de `uv_estadistica_grupo` (aforo). | Reutilizan `uv_resumen_asistencia_estudiante_grupo`; mismos SP con ámbito de facultad. Ola 2 empieza cuando la ola 1 esté en `develop`. |
| 3 Admin | HU133 asistencia global, HU138 ratio sesiones con asistencia, HU139 comparativa por facultad, HU135/HU136 censos (conteos) | Derivables. | Misma base de vistas con ámbito de institución. |
| 3 Admin | HU134 tendencias temporales | Derivable (serie por semana/mes de `Sesion`/`DetalleAsistencia`), requiere definir granularidad. | Decisión de producto. |
| 3 Admin | HU140 censo por sedes, HU141 escalafón docente, HU129 horas contratadas | **No existen en el modelo** (no hay tabla de sedes, escalafón ni horas contratadas; `Docente` solo tiene `usuario`). | Requieren DDL y su propia HU de captura de datos. Hasta entonces: `BLOCKED_BY_MISSING_DATA`, sin simulación. |

---

## 9. Secuencia de ejecución (fases 02–06)

| Fase | Repo | Commits atómicos (Conventional Commits, `Refs: HU065…`) |
|---|---|---|
| 02 CONTRACT | Backend (docs) | `docs(hus): congelar contrato de kpis del coordinador` |
| 03 RED | DB → Backend → Frontend | `test(db/sp): …`, `test(backend/usecase): …`, `test(frontend/coordinador): …` |
| 04 GREEN | DB → Backend → Frontend | `feat(db/views): …`, `feat(db/sp): …`, `feat(backend/…): …`, `feat(frontend/coordinador): …` |
| 05 AUDIT | los 3 | `VALIDATION.md` (incluye conciliación y plan de ejecución) |
| 06 CLOSURE | Backend + Infra | archivos calientes en commit aparte; `HU-CATALOG.md`; marcar las ramas previas como superadas |

Un PR por repo, aprobado por el owner antes del merge. Sin push ni PR sin orden expresa.

---

## 10. Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md). Orden RED → GREEN → VALIDATE por repo, DB → Backend → Frontend. Cobertura del código nuevo ≥ 80 % (Sonar), objetivo 100 % en casos de uso y validadores.

## 11. Rollback y stop conditions

- **Rollback:** §5.
- **TEST_CONTRACT_CONFLICT:** si una prueba existente falla por el cambio; se documenta, nunca se relaja.
- **BLOCKED_BY_MISSING_EVIDENCE:** si D7 no puede confirmarse con las pruebas UTC existentes.
- **STOP:** D1–D10 sin aprobar; cualquier valor simulado detectado en una revisión (CA-08/CA-09).

## 12. Definition of Ready (DoR)

| Criterio | Estado | Evidencia / pendiente |
|---|---|---|
| 1. Objetivo y alcance delimitados | ✅ | §1, §4 |
| 2. AS-IS con evidencia | ✅ | §2 (verificado contra `develop`) |
| 3. Contratos identificados | ✅ (borrador) | [CONTRACT.md](CONTRACT.md) |
| 4. Matriz TDD inicial | ✅ | [TEST_PLAN.md](TEST_PLAN.md) |
| 5. Sin conflicto con contratos WORM | ✅ | Solo objetos y rutas nuevos |
| 6. Decisiones del owner | ❌ | D1–D10 (recomendaciones en §6) |
| 7. Archivos calientes | ✅ (con regla) | §5: solo al final, en commit aparte |
| 8. Rollback viable | ✅ | §5 |

**Dictamen DoR:** `NOT_READY`: pendiente solo de la aprobación del owner sobre D1–D10.

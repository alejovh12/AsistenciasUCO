---
status: frozen-implemented
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-24
---

# TEST_PLAN — HU065 / HU066 / HU070 / HU067 / HU069: KPIs y alertas del Coordinador con datos reales

**Estado:** `FROZEN-IMPLEMENTED`. Las pruebas se escriben en la fase 03, en orden **DB → Backend → Frontend**, sobre ramas `feature/HU065-HU066-HU070-HU067-HU069-kpis-coordinador` creadas desde `develop` en worktrees propios.

## Fuentes

- [PLAN](PLAN.md) (CA-01…CA-09, D1–D10) y [CONTRACT](CONTRACT.md) (`frozen-implemented`; las pruebas se derivan de él y se congelan con él).
- Skills: `uco-testing`, `uco-persistencia`, `uco-contratos`, `uco-seguridad`, `uco-backend-pagination-utc`.
- Patrones: `test/test_usp_consultar_programas_academicos.sql` (consulta paginada, contexto restaurado), `test_usp_consultar_grupos_paginado.sql`, `ProgramaAcademicoJpaRepositoryTest`, `DecanoProgramaControllerTest`, `scratch/test_hu076_hu077_hu078.ps1` (E2E).

> **Principio rector:** cada prueba construye sus propios datos y verifica **cifras exactas** calculadas a mano. Una prueba que pasaría con valores por defecto o aproximados es un falso positivo y se rechaza.

---

## 1. Matriz de comportamiento

### 1.1 Cálculo (DB)

Fixture base `F` (dentro de la transacción de cada prueba): programa P1 con coordinador C1; grupo G1 (asignatura A1) y G2 (asignatura A2) en el período vigente; 10 sesiones en G1 (6 vencidas, 4 futuras); 5 estudiantes E1…E5 matriculados en G1.

| ID | Requisito | Precondición | Acción | Resultado esperado |
|---|---|---|---|---|
| T-DB-01 | Falta = `asistio=0` y `SJC` | E1 con 1 falta `SJC` en 5 sesiones ejecutadas | `uv_resumen_asistencia_estudiante_grupo` | `faltasSinJustaCausa=1`, `sesionesEjecutadas=5`, `porcentajeInasistencia=20.00` |
| T-DB-02 | La excusa no es falta (D2) | E2 con 3 `EX` y 0 `SJC` | idem | `faltasSinJustaCausa=0`, `excusas=3`, `0.00` |
| T-DB-03 | Una inasistencia sin razón `SJC` ni `EX` no suma | E3 con `asistio=0` y razón distinta | idem | falta solo si `SJC`; documentado con el caso |
| T-DB-04 | Denominador = vencidas con registro (D1) | 6 vencidas, 5 con registro, 4 futuras | idem | `sesionesEjecutadas=5` (ni 6 ni 10) |
| T-DB-05 | Sesión vencida sin registro no entra al denominador y sí a `sesionesVencidasSinRegistro` | la sexta vencida sin registro | `usp_consultar_kpis_coordinador` | `sesionesVencidasSinRegistro=1` |
| T-DB-06 | Sesión futura nunca es vencida | 4 futuras | `uv_cumplimiento_sesion_grupo` | `estaVencida=0` |
| T-DB-07 | Procedencia temporal indeterminada (D7) | 1 sesión con `procedenciaTemporal NULL` y fin pasado | `usp_consultar_kpis_coordinador` | no cuenta como vencida; `sesionesSinProcedenciaTemporal=1` |
| T-DB-08 | Clasificación por umbrales del catálogo | % = 14.99 / 15 / 19.99 / 20 | `ufn_clasificar_permanencia` | `REGULAR` / `RIESGO` / `RIESGO` / `PERDIDA` |
| T-DB-09 | Los umbrales salen del catálogo, no del código | cambiar `UMBRAL_ALERTA_ADVERTENCIA` a 10 (en la transacción) | idem con 12 % | `RIESGO`; al revertir vuelve a `REGULAR` |
| T-DB-10 | Falta un parámetro de umbral | se desactiva `UMBRAL_ALERTA_PERDIDA` | `usp_consultar_kpis_coordinador` | falla con `KPI_002`; nunca asume un valor |
| T-DB-11 | Matrícula `CI` es `PERDIDA` | E4 en estado `CI` con 0 % | función | `PERDIDA` |
| T-DB-12 | Divisor cero | grupo sin sesiones ejecutadas | vista y KPIs | `porcentajeInasistencia=0.00`; `porcentajeCumplimiento IS NULL` |

### 1.2 KPIs (HU065, HU070)

| ID | Requisito | Acción | Resultado esperado |
|---|---|---|---|
| T-KPI-01 | Conteos exactos del fixture | `usp_consultar_kpis_coordinador` como C1 | `totalEstudiantes=5`; `regulares + riesgo + perdida = 5`; cada cifra igual a la calculada a mano; `tasaPermanencia = (reg+riesgo)/5×100` |
| T-KPI-02 | Persona en dos grupos toma la peor clasificación (D6) | E5 en G1 (REGULAR) y G2 (PERDIDA) | cuenta una vez, en `estudiantesPerdida`; `totalEstudiantes` no se duplica |
| T-KPI-03 | Cumplimiento | 5 ejecutadas de 6 vencidas | `porcentajeCumplimiento=83.33`; `porcentajeProgresoCalendario=60.00` (6/10) |
| T-KPI-04 | Período por defecto y explícito (D8) | sin `@idPeriodoAcademico` / con el de un período anterior | vigente / el solicitado; período inexistente → `PER_001` |
| T-KPI-05 | Sin período vigente | ningún grupo habilitado | una fila con ceros y porcentajes `NULL` (no es error) |
| T-KPI-06 | Eco de umbrales | idem T-KPI-01 | `umbralAdvertencia=15`, `umbralPerdida=20` (los del catálogo) |
| T-KPI-07 | **Conciliación (CA-09)** | comparar cada cifra con una consulta directa sobre `EstudianteGrupo`/`Asistencia`/`DetalleAsistencia`/`Sesion` | idénticas |

### 1.3 Alerta temprana (HU066)

| ID | Requisito | Acción | Resultado esperado |
|---|---|---|---|
| T-ALT-01 | Paginación y orden | 12 estudiantes, página 2, tamaño 5 | 5 filas, `totalRegistros=12`, orden por % descendente |
| T-ALT-02 | Filtro semafórico | `@semaforo='ROJO'` / `'AMBAR'` / `'VERDE'` | solo esa clasificación; combinable con `@filtroTexto` |
| T-ALT-03 | Semáforo inválido | `@semaforo='AZUL'` | `GEN_002` |
| T-ALT-04 | Filtro de texto literal | `@filtroTexto='%'` | no actúa como comodín |
| T-ALT-05 | Paginación inválida | página 0, tamaño 0, tamaño > máximo, NULL | `PAG_001` (6 casos); la transacción del llamador sigue viva |
| T-ALT-06 | Contexto restaurado | contexto previo establecido | igual después, con éxito y con error |

### 1.4 Bitácora y ficha (HU067, HU069)

| ID | Requisito | Acción | Resultado esperado |
|---|---|---|---|
| T-BIT-01 | Bitácora cronológica | E1 en G1 y G2 con sesiones intercaladas | orden `fechaHoraInicio DESC`; trae grupo, asignatura, `asistio` y razón |
| T-BIT-02 | Sesión vencida sin registro del estudiante | E1 sin fila en la sesión 3 | `asistio IS NULL` (no `0`) |
| T-BIT-03 | Paginación y total | 25 sesiones, página 3, tamaño 10 | 5 filas, `totalRegistros=25` |
| T-FIC-01 | Cabecera real | E1 | identificación, nombres, correo y programa iguales a `Usuario`/`Programa`; **sin** teléfono |
| T-FIC-02 | Resumen por asignatura | E1 en G1 y G2 | una fila por matrícula con los mismos números que la vista de resumen |
| T-FIC-03 | Estudiante ajeno o inexistente (CA-06) | E9 de otro programa / GUID aleatorio | ambos `GEN_001` con el mismo mensaje; ninguna fila |

### 1.5 Ámbito y seguridad

| ID | Requisito | Acción | Resultado esperado |
|---|---|---|---|
| T-SEC-01 | Otro coordinador | C2 de P2 ejecuta los 5 SP sobre datos de P1 | solo ve P2; los estudiantes de P1 no aparecen ni por id |
| T-SEC-02 | Ejecutor no coordinador | decano / docente / estudiante / usuario sin perfil | `KPI_001` o 0 filas fail-closed |
| T-SEC-03 | Ejecutor y correlación nulos | NULL | `VAL_001` / `CORR_001` |
| T-SEC-04 | HTTP: roles | JWT de decano, docente, estudiante | `403` en `SecurityFilterChain` (`RbacSecurityFilterChainTest`) |
| T-SEC-05 | HTTP: administrador sin programa | JWT admin | pasa el filtro y el caso de uso responde `403` |
| T-SEC-06 | HTTP: sin token | — | `401` |

### 1.6 Backend (capas)

| ID | Requisito | Resultado esperado |
|---|---|---|
| T-BE-01 | Casos de uso resuelven el programa del actor | sin programa → `ForbiddenException`, sin llamar al puerto |
| T-BE-02 | Validación de forma antes del puerto | `pagina<1`, `tamano<1`, `semaforo` inválido, UUID nulo → `ValidationException` |
| T-BE-03 | Mapeo fiel | proyección → dominio → DTO → respuesta sin transformar cifras ni redondear de nuevo |
| T-BE-04 | `null` se conserva | porcentajes y `asistio` nulos llegan como `null` al JSON |
| T-BE-05 | Adapter | `EXEC` con parámetros nombrados, `idCorrelacion` propagado, `PAG_001` → 400, otros fallos → técnicos |
| T-BE-06 | Códigos | `KPI_001` → 403, `KPI_002` → 500 (`DbFailureClassifierTest`) |
| T-BE-07 | Controllers | 200/400/401/403/404, envelope `ApiDataResponse`/`ApiListResponse`, defaults de paginación |
| T-BE-08 | Wiring | POJOs cableados (`CoordinadorIndicadoresWiringConfigurationTest`) |
| T-BE-09 | Arquitectura | ArchUnit sin excepciones nuevas; entidades `Sp*` sin `@Immutable`; inventario de repositories actualizado |
| T-BE-10 | **Anti-simulación** | prueba de arquitectura/texto que falla si el adapter o el caso de uso nuevo contienen `Math.round(… * 0.`, `new …DTO(` con literales numéricos o `catch (Exception ignored)` |

### 1.7 Frontend

| ID | Requisito | Resultado esperado |
|---|---|---|
| T-FE-01 | Servicio | `HttpTestingController`: rutas, query params, signals actualizados solo con respuesta exitosa; errores 400/401/403/404/500 propagados |
| T-FE-02 | Tarjetas KPI | muestran exactamente lo recibido; `null` → "Sin datos" (nunca 0 ni valor por defecto) |
| T-FE-03 | Tabla de alertas | `PaginationComponent` emite página y consulta; filtro semafórico y de texto; estado vacío/carga/error con el mensaje del backend |
| T-FE-04 | Ficha y línea de tiempo | abre desde `coordinator-students`, pagina la bitácora, 404 muestra el mensaje del servidor |
| T-FE-05 | Sin cálculo en cliente | revisión por prueba: no existen fórmulas de porcentaje ni clasificación en los componentes |
| T-FE-06 | HU068 intacta | los specs de `coordinator-students` existentes pasan sin modificarse (salvo el botón nuevo) |

### 1.8 Sistema (E2E)

`scratch/test_hu065_hu066_hu070_hu067_hu069.ps1` con JWT reales de `coordinador.sistemas`:

1. Crea (por SQL) un programa de prueba, grupos, sesiones y asistencias con cifras conocidas; limpia al final.
2. `GET /coordinador/indicadores` → 200 y cada cifra = la calculada a mano.
3. `GET /coordinador/alertas-tempranas?semaforo=ROJO` → solo estudiantes en `ROJO`; paginación inválida → 400.
4. Bitácora y ficha de un estudiante del programa → 200 con los números esperados; de otro programa → 404.
5. Token de decano/docente/estudiante → 403; admin → 403; sin token → 401.
6. **Conciliación SQL** de la respuesta contra consultas directas (CA-09).

---

## 2. Pruebas RED a crear por capa

| Capa | Archivos | Comando |
|---|---|---|
| DB (`GestioAsistenciaDB/test/`) | `test_uv_resumen_asistencia_estudiante_grupo.sql`, `test_ufn_clasificar_permanencia.sql`, `test_usp_consultar_kpis_coordinador.sql`, `test_usp_consultar_alerta_temprana_programa.sql`, `test_usp_consultar_bitacora_estudiante_programa.sql`, `test_usp_consultar_ficha_estudiante_programa.sql` | `sqlcmd` sobre cada archivo; todos con fixtures propios dentro de `BEGIN TRAN … ROLLBACK` y verificación final de `@@TRANCOUNT = 0` |
| Backend | `…/coordinador/{consultarkpis,consultaralertatemprana,consultarbitacoraestudiante,consultarfichaestudiante}/usecase/impl/*Test`, interactores, controllers (MockMvc), adapters JPA (patrón `EXEC` + mapeo), `DbFailureClassifier*Test`, wiring, `RbacSecurityFilterChainTest` (casos nuevos, archivo caliente), prueba anti-simulación | `.\mvnw.cmd test` |
| Frontend | `coordinator-indicators.service.spec.ts`, specs de tarjetas, tabla de alertas, ficha y línea de tiempo | `npm run test:ci` |
| E2E | script de §1.8 | PowerShell con backend en puerto propio |

Los `:r` de `test_suite.sql`, la excepción en `test_contract_closure.sql`, el contador de repositories y `HTTP_AS_IS_MATRIX.md` se tocan **solo al final**, en commit aparte.

**Cobertura:** casos de uso, validadores y mapeos al 100 % de líneas y ramas; el código nuevo ≥ 80 % (Sonar); frontend: cada componente y servicio nuevo con su spec.

---

## 3. Efectos secundarios y rollback

- Las pruebas DB no dejan datos (rollback). El E2E crea filas reales y las elimina en un `finally`; no usa los GUID del seed.
- Aplicación a la BD local compartida: solo los objetos nuevos de esta rama (vistas, función y SP), sin `deploy_schema.ps1` completo.

## 4. RED_SNAPSHOT (se completa en la fase 03)

| Repo | Commit base | Archivos de prueba | Comando | Exit code | Falla esperada |
|---|---|---|---|---|---|
| GestioAsistenciaDB | — | §2 | `sqlcmd` por archivo | — | Los objetos no existen |
| GestioAsistenciaBackend | — | §2 | `mvnw test` | — | Faltan clases, puertos y rutas |
| GestioAsistenciaFrontend | — | §2 | `npm run test:ci` | — | Falta el servicio y los componentes |

## 5. Reglas de congelamiento

Después del RED las pruebas no se modifican. Cualquier cambio exige un registro `TEST_CONTRACT_CONFLICT` en `VALIDATION.md` con su justificación (error genuino de la prueba o cambio formal del contrato) y aprobación del owner. Prohibido relajar aserciones, borrar casos, silenciar validaciones o *mockear* para evitar un fallo real.

## 6. Falsos positivos a evitar

- Fixtures que reutilizan GUID o datos del seed: cada prueba crea los suyos.
- Aserciones del tipo "mayor que 0" o "no nulo" donde corresponde una cifra exacta.
- Probar el ámbito solo en el caso de uso: también se prueba en DB (T-SEC-01, T-FIC-03).
- Pruebas de reloj que dependen de la hora real: las sesiones se crean con fechas relativas (`SYSUTCDATETIME() ± n`) y lejos del borde (≥ 1 día).
- Un E2E que valida solo el código HTTP: debe conciliar las cifras con SQL.
- Un mock del puerto que devuelve el mismo DTO que el test espera: la prueba real del cálculo es la de DB y la conciliación.

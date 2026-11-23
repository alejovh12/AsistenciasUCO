---
status: draft
type: work-item
scope: backend-db-frontend
owner: backend-team
last-reviewed: 2026-11-19
---

# CONTRACT CONFLICTS — SES-GEN-01

Registro de `CONTRACT_CONFLICT`, `BLOCKED_BY_MISSING_EVIDENCE`, hallazgos de seguridad y erratas del [PLAN](PLAN.md) detectados al armar la [CONTRACT_MATRIX](CONTRACT_MATRIX.md) y el [CONTRACT_PROPOSAL](CONTRACT_PROPOSAL.md). **Ninguno queda resuelto aquí**: el agente de contratos no elige quién cambia ni corrige por suposición. Fuentes: Backend `2fdc7a0f`, DB `develop @ dadf8dc` (instantánea del repo, no del SQL Server vivo), Frontend `0328526`. Sin secretos.

## 1. CONTRACT_CONFLICT (dos fuentes autoritativas se contradicen)

| ID | Fuente A | Fuente B | Ámbito | Resuelve | Efecto mientras tanto |
|---|---|---|---|---|---|
| **CC-01** Flujo F3 (el docente titular crea su grupo y define el horario inicial) | PLAN decisión 4 («el flujo F3 … debe quedar alineado») y sus fuentes: work items `F2-precarga-sesiones/PLAN.md` y `F3-docente-crea-grupo/PLAN.md` (siguen `status: active`), comentarios del Frontend (`course.service.ts` L257 «COORDINADOR o el propio DOCENTE»; L344 «el docente solo crea, F3»), mapeo `HOR_005 → CONFLICT` en `DbFailureClassifier` | **DEC-065 (4), ya aplicada en las tres capas**: `SecurityConfig` L234-236 (`POST /api/v1/grupos` solo `COORDINADOR`; «retira la excepción F3 del docente»), `access-catalog.json` L184, `RbacSecurityFilterChainTest` L63 y L428-436 (`GET /docente/oferta-creacion-grupos` retirado, 404 sin handler), `usp_crear_grupo` (cabecera: «solo el COORDINADOR crea grupos»; commit `17c27a2`) | HTTP, seguridad y persistencia | Responsable (confirmar que F3 queda retirado y marcar los work items F2/F3 como superados); John (destino de la rama `HOR_005` de `usp_guardar_horarios_grupo`, que sigue aceptando docente titular pero no tiene camino HTTP) | La decisión 4 se aplica solo al coordinador. No se diseña ningún camino docente |
| **CC-02** Roles y ámbito de `POST/PUT /api/v1/grupos` | `access-catalog.json` L184 («solo de los programas de su coordinación (Application + usp_crear_grupo…)») y L193 (`PUT` roles `ADMINISTRADOR`, `COORDINADOR`; «COORDINADOR solo de su programa (Application + SP)»); `SecurityConfig` L238 (`PUT` admite `ADMINISTRADOR`) | `usp_crear_grupo` PASO 1.5 y `usp_actualizar_grupo` PASO 1.5 exigen **solo el perfil `COORDINADOR`** (un `ADMINISTRADOR` sin ese perfil recibe `SEC_001` ⇒ 403) y **ninguno** valida el programa de la asignatura/grupo; `CrearGrupoUseCaseImpl` y `ActualizarGrupoUseCaseImpl` tampoco. El ámbito solo lo valida `usp_guardar_horarios_grupo` (`SEC_002`), y solo cuando hay franjas | Seguridad / PDP / persistencia | John (SP) + responsable (rol de `ADMINISTRADOR`, DR-05) + auditoría de seguridad | Ver `SECURITY_FINDING-GRP-001` (§3). Con `franjas` obligatorias el hueco de **creación** queda cubierto de facto; el de **edición sin franjas** sigue abierto |
| **CC-03** Documentación AS-IS desfasada respecto del código | `HTTP_AS_IS_MATRIX.md` filas 226-227 (`POST/PUT /api/v1/coordinador/periodos-academicos`); fila 83 (`GET /api/v1/coordinador/asignaturas` → `AsignaturaQueryPort`, `uv_asignatura`, `ApiListResponse<AsignaturaDTO>`); `HU096-HU097-HU098/CONTRACT.md` (`POST` ⇒ `200 {datos:null}`); no hay fila funcional para `POST/PUT /api/v1/grupos` (solo inventario, filas 272-273) | Código: `AdminPeriodoAcademicoController` (`/api/v1/admin/periodos-academicos`, `POST` ⇒ 201 con `PeriodoCreadoResponse`; el comentario de `CoordinadorPortalController` L129-131 lo confirma), `AsignaturaPlanController.asignaturasCoordinador` (`usp_consultar_asignaturas_coordinador_paginado`, `ApiPageResponse<AsignaturaPlanDTO>`), `GrupoController` | Documentación HTTP | backend-team | No bloquea el diseño; **bloquea el cierre** («documentación desactualizada bloquea el cierre, no autoriza adaptar el código a ella») |
| **CC-04** `numeroSemana` 1..16 frente al SP congelado | PLAN B2b y decisión 2b (1..16) | PLAN «no alcance» y cabecera de `usp_precargar_sesiones_grupo`/`usp_generar_sesiones_grupo_v2`: `usp_generar_sesiones_grupo` está **congelado** y contiene la misma expresión (`DATEDIFF(week, …) + 1`, L305) | Persistencia | Responsable + John (descongelar para corregir, o aceptar que `/generacion` pueda dar 17) | La corrección de la precarga (H-08) no alcanza a las sesiones creadas por `/generacion` |
| **CC-05** Texto de `VAL_002` | Uso en los SP de grupo, período y ámbito (`'Asignatura'`, `'Grupo'`, `'PeriodoAcademico'`, `'0','365'`) | Semilla `schema/seed/04_mensaje.sql` L24 y L157: «El número de identificación debe tener entre {} y {} dígitos» | Catálogo de mensajes (DB) | John (código propio o corrección del catálogo) | Mensajes engañosos para el usuario; el backend lo clasifica 400. Basado en la semilla estática: no se comprobó el catálogo vivo |

## 2. BLOCKED_BY_MISSING_EVIDENCE

| ID | Qué falta | Dónde se buscó | Qué bloquea | Quién lo aporta |
|---|---|---|---|---|
| **BM-01** Valores de B7 (duración mínima/máxima de franja, ventana horaria institucional) | `schema/seed/03_parametro.sql`, los demás `schema/seed/*parametro*`, `migrations/` (incl. la tabla de parámetros editables de `22_culm001_d3`), `schema/stored-procedures/` | Códigos `HOR_007/008` y el SP de previsualización en B7 | Responsable (valores) y John (parámetros) |
| **BM-02** Unidad de la hora de `horasSemanales` y decimales | PLAN, `subject-form.component.ts` (entero 1..20), docs de arquitectura históricos del repo DB (mencionan `@horasSemanales INT` en un diseño no desplegado) | Tipo de la columna, regla B5 exacta, `horasEsperadasMateria` | Responsable + John |
| **BM-03** Definición de B6 (franjas por semana «coherentes con la materia») | PLAN B6: no hay dato de «veces por semana» en la materia; solo `horasSemanales` | Sin dato no hay regla contratable; B6 se reduce a B5 + B7 + «≥ 1 franja» | Responsable (¿se quiere un máximo de franjas por semana?) |
| **BM-04** Clientes de `POST /api/v1/grupos` distintos del FE | Frontend (`crearGrupo` es el único llamador), scripts del backend (`infra/`, evidencias: solo GET), pruebas del backend (sí postean sin `franjas`) | La aprobación del cambio de comportamiento de `franjas` obligatorias (DR-02) no puede afirmar «único consumidor» con certeza | Responsable / backend-team |
| **BM-05** Dato de cohorte (B12) | No existe entidad de cohorte/semestre-grupo | Advertencia de cruce entre asignaturas del mismo semestre | Fuera de esta versión hasta que exista el dato |
| **BM-06** Prueba real de B2b (`numeroSemana` = 17) | Solo lectura estática de `usp_precargar_sesiones_grupo_interno` L174 y `usp_generar_sesiones_grupo` L305; no hay `SET DATEFIRST`/CHECK. **No se ejecutó nada** | Confirmar o descartar el defecto antes de corregir | Tester/DB en BD desechable (guardas `e2e_safety`), nunca la BD viva |
| **BM-07** Estado del SQL Server vivo | Solo el repo DB `develop @ dadf8dc` | Que los SP/semillas leídos sean los desplegados (p. ej. catálogo de `VAL_002`) | Responsable / John (paquete DB desplegado) |

## 3. SECURITY_FINDING-GRP-001 (tipo: ámbito de autorización; sin secretos)

- **Archivos**: `usp_crear_grupo.sql`, `usp_actualizar_grupo.sql` (repo DB); `CrearGrupoUseCaseImpl.java`, `ActualizarGrupoUseCaseImpl.java` (backend); `infra/security-baseline/access-catalog.json` L184/L193 (texto de ownership).
- **Hecho**: un usuario con perfil `COORDINADOR` pasa el PEP y los SP **sin que se compruebe que la asignatura o el grupo pertenezcan a un programa que coordina**. `POST /grupos` sin `franjas` crea el grupo; `PUT /grupos/{id}` sin `franjas` puede cambiar nombre, código, docente titular y cupo de grupos de otro programa. Con `franjas`, `usp_guardar_horarios_grupo` devuelve `SEC_002` y la transacción del backend revierte todo.
- **Mitigación parcial prevista**: decisión 4 (franjas obligatorias) cierra la creación; regla B1/H-05 y H-06 la cierran en la DB.
- **Severidad** (propuesta, a confirmar): media. Requiere credenciales de coordinador; afecta integridad de datos académicos ajenos.
- **No verificado**: que el SQL Server vivo no tenga otra barrera (p. ej. permisos por vista); es lectura del repo.
- **Quién**: John (SP), backend-team (catálogo de accesos), seguridad (auditoría).

## 4. Verificación de lo «no verificado» del PLAN

| Punto del PLAN | Resultado | Evidencia |
|---|---|---|
| **B4** unicidad de código por asignatura + período | **Existe para el código, no para el nombre; sin índice**. `usp_crear_grupo` PASO 5 (L149) y `usp_actualizar_grupo` PASO 4 hacen un `EXISTS` sobre `uv_grupo (idAsignatura, idPeriodoAcademico, codigo)` y responden `VAL_006` (409). `dbo.Grupo` solo tiene PK: sin índice único (carrera posible entre dos coordinadores). El nombre puede repetirse | `usp_crear_grupo.sql`, `usp_actualizar_grupo.sql`, `schema/tables/Grupo.sql`, `DbFailureClassifier` (`VAL_006`) |
| **B7** parámetro de ventana horaria institucional | **No existe** parámetro de ventana, duración mínima/máxima ni franja. La única regla de franja es `horaFin > horaInicio` (`HOR_004`). Parámetros relacionados existentes: `TIEMPO/ZONA_HORARIA_*`, `GRUPO/CAPACIDAD_MAXIMA_DEFECTO`, `ASIGNATURA/CREDITOS_MAXIMO`, `SESION/*` (motivos de cancelación), `ASISTENCIA/*`; `MATRICULA_QR/DURACION_MAXIMA_MINUTOS` no es de horarios | grep de `schema/seed/*`, `migrations/` (ver BM-01) |
| **¿`usp_actualizar_grupo` / `usp_crear_grupo` cubren B3?** | **Sí**. Crear: `usp_validar_docente_exista_por_id_interno` + `usp_validar_docente_vinculo_activo_programa_interno` (`DOC_009`) en PASO 4/4B. Editar: ambos en PASO 3/3B **solo si el titular cambia** (a propósito: editar cupo u otros datos de un grupo con titular sin vínculo activo sigue permitido) y, en 3C, el cruce de horario del nuevo titular (`HOR_002`). Un docente sin ningún vínculo no se bloquea (cabecera del SP de vínculo) | los dos SP; `usp_validar_docente_vinculo_activo_programa_interno` |
| B1 (asignatura activa y vinculada, programa del coordinador) | **No cubierta**: crear solo comprueba que la asignatura exista en `uv_asignatura` | `usp_crear_grupo` PASO 2 |
| B2 (período de la institución, no terminado ni cerrado) | **No cubierta**; además hay sustitución silenciosa por el período más reciente sin filtro de institución | `usp_crear_grupo` PASO 3 |
| B9 (cruce con estudiantes inscritos al cambiar franjas) | **Hueco confirmado** | `usp_guardar_horarios_grupo` PASO 6 |
| B2b (`numeroSemana` 17) | **Hipótesis consistente con el texto, no probada** (BM-06); la expresión también está en el SP congelado | ver CC-04 |

## 5. Erratas del PLAN (no se edita el PLAN; se reportan)

| ID | Texto del PLAN | Realidad comprobada |
|---|---|---|
| E-1 | Decisión 4: «El flujo F3 (el docente titular crea su grupo y define el horario inicial, HOR_005) debe quedar alineado» | F3 está retirado en backend, catálogo, pruebas y `usp_crear_grupo` (CC-01) |
| E-2 | Decisión 1c: «hoy no hay escritura de asignaturas en este alcance» | Existe: `POST/PUT /api/v1/coordinador/planes-estudio/{id}/asignaturas…` con `usp_crear_asignatura/usp_actualizar_asignatura`; el FE ya tiene el campo «Horas Semanales Presenciales», aunque no lo envía (C-03) |
| E-3 | AS-IS: «Ejecutan la precarga el COORDINADOR del programa y el DOCENTE titular» | Cierto en el SP; por HTTP solo el coordinador (a través de `POST/PUT /grupos`), y debe coordinar **todos** los programas del grupo |
| E-4 | AS-IS: «El contrato HTTP de crear/editar grupo del coordinador debe localizarse» | Localizado: `POST /api/v1/grupos` y `PUT /api/v1/grupos/{id}` (`GrupoController`), fuera del OpenAPI y de la tabla principal de la matriz AS-IS |
| E-5 | TARGET y contratos: «el coordinador/administrador elige la fecha de inicio» | Solo el `ADMINISTRADOR` crea/edita períodos (`/api/v1/admin/**`); el coordinador solo los lee |
| E-6 | B4, B7, B3 marcados «no verificado»/«verificar» | Resueltos en §4 |
| E-7 | Tabla de contratos: «HTTP/DB crear y editar período… (solo Administrador)» y la matriz AS-IS lista rutas de coordinador | Correcto lo del administrador; las filas de la matriz AS-IS son erróneas (CC-03) |
| E-8 | B2b: «No probado… calcular `DATEDIFF(DAY, inicio, fecha) / 7 + 1`» | Falta anotar que el mismo defecto está en el SP congelado y que la precarga calcula el día de la semana sin depender del idioma pero `numeroSemana` sí depende de `DATEFIRST` (CC-04) |

## 6. Otros hallazgos (no son conflicto entre fuentes)

- **MISSING_IN_CONSUMER**: `SES_005` (hora local inexistente por DST) no está en `DbFailureClassifier`: respondería 500 `ERR_DB_UNCLASSIFIED`.
- **MISMATCH menor (FE ↔ BE)**: el FE puede enviar `franjas: []` (editor vaciado) y documenta «vacío = sin horario»; el backend responde 400 (`HOR_004`) y revierte (A-05). El FE lee `mensajeUsuario` en el nivel superior mientras el backend lo anida en `datos` (cae a un texto de UI).
- **Riesgo R-GEN-1**: `POST /api/v1/sesiones/grupo/{grupoId}/generacion` (solo DOCENTE, SP congelado) genera también las ranuras **pasadas** del período, que la decisión 6 excluye de la precarga; los totales de la previsualización dejarían de coincidir si el docente lo ejecuta (E-02 de la matriz).
- **Lectura posicional estricta**: añadir columnas a los SP de asignaturas o a la tupla de la precarga rompe al backend (conteo exacto); toda columna nueva exige la secuencia expandir/contraer del CONTRACT_PROPOSAL §4.1.
- **Docs/estado de aprobación**: la matriz y la propuesta están en `DRAFT`; no hay evidencia de aprobación del contrato.

## 7. Resolución

Ninguna fila anterior tiene resolución ni aprobación versionada todavía. Se registrará aquí (con fecha, aprobador y referencia) cuando el responsable o el dueño DB decida. Trabajo independiente que puede continuar sin esperar: pruebas de B2b en BD desechable (BM-06), mapeo de `SES_005`, y la preparación de la regla de 112 días una vez resueltas DR-03/04/09.

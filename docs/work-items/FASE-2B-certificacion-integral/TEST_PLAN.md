# TEST_PLAN — FASE 2B: certificación integral

## Fuentes

Prompt autorizado de Fase 2B; PLAN.md; HTTP_AS_IS_MATRIX; OpenAPI canónico 1.7.0 para las operaciones cubiertas; BACKEND_GOLDEN_PATH_CONTRACT; SecurityConfig/runtime-security-provider-architecture; PC-puntos-control-tomas; INT-SES-20261104; decisiones y scripts DB de DEC-066; work items HU005-HU016, LB-004D y DEC-073. Las fuentes DB externas describen el contrato de su ámbito; los conflictos listados en PLAN.md no se resuelven por este plan.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| OPS-01 | Arranque seguro | SQL, Keycloak, PEP, PDP, PEP health, Frontend y Backend disponibles | Respaldo verificado; worker candidates 0 o control documentado; dispatcher outbox y evaluador de alertas con override explícito | Arrancar solo servicios existentes por scripts oficiales | Health, puertos, pantalla login y cambios de datos de worker acotados | Arranque solo con condiciones cumplidas; sin consumo outbox ni efecto inesperado | Arrancar con worker que pueda modificar filas compartidas o enviar notificación | OPERACIONAL |
| SES-01 | Sesiones del docente | Listar grupos propios y horarios | Login manual DOCENTE; requests GET | Abrir grupos/horarios en UI | Grupos, sesiones, estados y requests GET | Solo ámbito propio; registrar estados que existan, sin inferir cobertura inexistente | Mostrar grupo ajeno o estado diferente al contrato | Navegador real |
| SES-02 | Tiempo y estados | Ver sesiones futuras, en curso, finalizadas y canceladas | Fixture existente de solo lectura; zonas/fechas visibles sin PII | Consultar calendario y detalle | Hora mostrada, estado, zona y error HTTP si aplica | Representación coincide con contrato aplicable; los estados ausentes quedan NOT_RUN | Convertir zona o fecha sin evidencia, ocultar cancelación o atribuir horario al cliente | Navegador real |
| SES-03 | Apertura/cierre y medianoche | Apertura anticipada, cierre y cruce de medianoche | Contrato DB y expectativa resueltos; fixture seguro | Escribir/editar/cerrar sesión | UI, HTTP, código, persistencia | Solo según contrato aprobado. Hoy bloqueado por conflicto DEC-072/SES_028 y SES_003 | UI que acepte pero servidor/DB rechace, o endpoint de cierre que aparente persistir sin hacerlo | Navegador + provider, NOT_RUN actual |
| TOM-01 | DEC-066 máximo 20 | Crear tomas hasta 20 y probar 21 | Fixture aislado con cleanup; regla DB versionada | Abrir 20, confirmar 20 admitida; intentar 21 | HTTP/status/código; conteo SQL antes/después | 20 aceptada, 21 rechazada según contrato | Límite visual distinto del server, aceptar 21 o rechazar 20 | Navegador + DB, NOT_RUN actual |
| TOM-02 | Ventana temporal | Probar inicio-15, borde inicial, fin+30 y borde final | Contrato DB que define anclajes y reloj | Crear toma en cada borde con control de tiempo | Respuesta real y estado persistido | Inclusividad/exclusividad exacta según contrato owner | Sustituir la regla por tolerancia visual o cálculo Java no autorizado | Navegador + DB, BLOCKED |
| TOM-03 | Toma MANUAL / QR | Registro, idempotencia, límite y cierre | Sesión, estudiantes y limpieza de laboratorio | Registrar en UI manual y QR, repetir y cerrar | UI/HTTP, registros, duplicados y estado | Reglas AS-IS de toma/QR | Bloqueo solo visual, duplicar detalle, registrar en toma cerrada | Navegador + DB, NOT_RUN actual |
| TOM-04 | TOM_012 | Corregir toma anterior tras abrir la siguiente | Dos tomas seguidas en fixture seguro; HTTP autenticado | Enviar corrección autorizada desde herramientas del navegador | 409 TOM_012 y persistencia sin cambio | Error del servidor y cero mutación; botón disabled no es evidencia | Aceptar escritura tardía aunque UI la bloquee | Navegador + DB, NOT_RUN actual |
| ATT-01 | Minutos y estados | Consultar resumen de puntos y sesión del estudiante | Login manual, proyección existente legible | Abrir resumen/sesión en UI | Minutos, estados PRESENTE/AUSENTE/JUSTIFICADA/PARCIAL/NO_DISPONIBLE y GET response | Coincide con consulta real; estado no se deriva del frontend | Minutos calculados solo visualmente o estado incorrecto | Navegador real; DB lectura |
| MAT-01 | Matrícula | Consultar grupo/cupo/estudiante; explorar registro manual/QR | Login/rol que permita leer; una escritura requeriría fixture seguro | Solo consultar UI; no registrar ni consumir token | Cupo/estado y GET response | Solo lectura observable; POST queda NOT_RUN hasta fixture | Simular matrícula local o cambiar datos compartidos | Navegador real |
| REV-01 | Reclamos y titularidad | Consultar bandeja/detalle con DOCENTE y sesiones relacionadas con ESTUDIANTE | Login manual de cada rol; evidencia sin PII | Abrir listados y detalle | HTTP GET, status, ownership y campos de toma | Titular ve recurso dentro de ámbito; recursos ajenos se ocultan según contrato | Mostrar reclamo ajeno o usar identidad desde formulario | Navegador real |
| REV-02 | Radicar reclamo por sesión/toma | Categoría, plazo y soporte | DEC-073 y regla de soporte por categoría resueltas; fixture seguro; archivo de laboratorio | POST desde sesión del ESTUDIANTE | Status/código, soporte, toma y persistencia | Solo conforme a contrato dictaminado | Reclamo duplicado, categoría/soporte/plazo inválido o identidad tomada del body | Navegador + DB, NOT_RUN actual |
| REV-03 | Resolver PRESENTE/JUSTIFICADA/rechazo | Tres solicitudes independientes | Fixture seguro y contrato de decisión completo | PATCH como docente titular para cada resultado | Status, código, asistencia posterior y evidencia SQL | PRESENTE→AN, JUSTIFICADA→EX; rechazo conserva asistencia, conforme a contrato | Acción que solo actualice UI, persistencia errónea o resolución ajena | Navegador + DB, NOT_RUN actual |
| SEC-01 | Auth y ownership | Sin auth, rol equivocado, titular/ajeno, recurso inexistente | Cambios de rol con login manual; no reutilizar tokens | Navegar rutas y observar respuesta real | 401/403/404, código, correlation ID | Precedencia y ownership según AS-IS; no inventar resultado fuera de contrato | Bypass por frontend, diferencia de ownership o IDOR | Navegador real |
| OBS-01 | Correlación | Request real por rol | Sesión vigente | Leer response header/campo de error de correlation ID sin inspeccionar auth headers | UUID correlation ID saneado | Correlación está presente según contrato | Perder correlación o confundirla con JWT | Navegador real |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES para afirmar login real, lectura DB, ownership persistido, efectos de minutos/tomas/reclamos o comportamiento de provider.
- ENVIRONMENT: tres repositorios personales originales en snapshots indicados; SQL Server y Security Baseline existentes; GreenMail local; Backend con JDK 25 y Frontend real servido en loopback.
- MOCK_SUFFICIENT: NO para E2E, SQL/SP, identidad real ni ownership.
- WHY: un mock, un botón deshabilitado o una lectura simulada no certifican API, DB ni autenticación. La suite Maven verde solo certifica el alcance de pruebas ejecutadas.

## Negative / Boundary Coverage

- DEC-066 20/21; ventana temporal con bordes inclusivos/exclusivos solo tras obtener contrato DB.
- TOM_012 con persistencia antes/después.
- 401/403/404 y ownership de grupo/toma/reclamo por rol real.
- Estado HTTP y código para ventanas de asistencia, sesión cancelada/cerrada y duplicados, sin rebasar lo versionado.
- No solicitar ni inspeccionar tokens/cookies/Authorization; no cambiar el usuario sin nueva solicitud manual.

## Side Effects / Rollback

- Alcance actual: GET y salud únicamente. Outbox dispatcher/evaluador de alertas se controlan con overrides por proceso y se documentan; los otros jobs se arrancan solo si el preflight de elegibles permanece vacío y no se crean nuevas sesiones/tomas/ventanas.
- No POST/PATCH/PUT/DELETE ni uploads; no usar fixtures históricos ni helpers con DELETE.
- Si se observa cualquier escritura inesperada, cambio de token/toma, consumo de outbox o correo fuera de GreenMail: parar servicios iniciados y clasificar escenario como FAIL/BLOCKED, nunca restaurar ni borrar datos.
- Limpieza: Ctrl+C en las sesiones de Backend/Frontend; comprobar listener cerrado. Ninguna limpieza de DB está autorizada.

## Pruebas RED requeridas

NO APLICA al alcance DOCUMENTATION_ONLY actual: no se modifica código. Si se demuestra un defecto Backend, detener el caso afectado y anexar requirement, contrato, test específico, hash RED causal y aprobación antes de implementación.

## RED_SNAPSHOT

NO APLICA mientras el alcance sea solo documental/read-only. Una corrida de navegador con error no se convierte automáticamente en RED ni autoriza cambiar el contrato.

## Congelación

No se congela TARGET nuevo. Los contratos AS-IS enumerados permanecen inmutables durante esta fase. Cualquier contradicción nueva se registra como CONTRACT_CONFLICT o BLOCKED_BY_MISSING_EVIDENCE con owner y fuente.

## Integración y E2E

E2E requiere Backend y Frontend reales, Integrated Browser de Codex, login manual del usuario por rol, SQL Server snapshot indicado en modo lectura y protección de efectos de workers. Las escrituras se mantienen NOT_RUN hasta que owner DB/QA identifique fixture aislado y cleanup no destructivo. No se asume que backup autorice restaurar o mutar datos.

## Falsos positivos que deben evitarse

- Botón disabled no demuestra TOM_012 ni rechazo de servidor.
- Mocks de controller no demuestran HTTP/SQL real.
- Respuesta 200 no demuestra persistencia; validar con lectura posterior solo si se autorizó una escritura.
- Un error de contrato no es defecto Backend sin dictamen.
- No tener sesión o fixture no es FAIL del producto.
- No guardar capturas ni requests que expongan PII, credenciales, token, cookie o adjunto.

## Contrato AS-IS confirmado por revisión estática (subtarea D)

- Lectura de sesiones de grupo en la UI: `GET /api/v2/sesiones/grupo/{grupoId}` requiere `DOCENTE`; el caso de uso aplica ámbito del docente mediante `InstitutionalScopePort`. El detalle v2 `GET /api/v2/sesiones/{sesionId}` diferencia 404 inexistente y 403 ajena. Estos tests estáticos no prueban el provider real.
- Existe inconsistencia V1/V2: `GET /api/v1/sesiones/{sesionId}` usa `authenticated()` y admite relaciones adicionales; la UI del snapshot 91f89f5 usa v2 para listado. Un resultado de una versión no se extrapola a otra.
- Contrato de errores AS-IS: ausencia/credencial inválida 401; rol insuficiente 403; recurso inexistente 404; validación funcional 400; conflicto 409; fallo técnico inesperado 500 con envelope seguro. El código interno desconocido cae en `ERR_DB_UNCLASSIFIED`; no se debe reproducirlo artificialmente contra la DB compartida.
- `X-Correlation-Id` es opcional: el filtro acepta UUID canónico distinto de cero o genera uno, expuesto en header y cuerpo. En navegador solo se anotará status y correlation ID saneados; nunca `Authorization`, cookies ni token.
- Escrituras de sesión v2 exigen RFC3339 con offset y fin posterior al inicio. Cruce de medianoche, `SES_028` y ownership dependiente del provider siguen bloqueados por contrato/evidencia; cierre manual `POST /api/v1/sesiones/cierres` está en DB como SES_003 no soportado (501), con request Frontend legado incompatible. No invocar esos comandos desde la sesión compartida.
- Fuentes consultadas para esta revisión: `SecurityConfig.java`, `CorrelationIdFilter.java`, `GlobalExceptionHandler.java`, `SesionV2ConsultaController.java`, `ConsultarSesionesPorGrupoV2UseCaseImpl.java`, `SesionV2RequestParser.java`, `SesionController.java`, `HTTP_AS_IS_MATRIX.md` y el Frontend fijado en el PLAN. Tests relacionados existentes: `SesionV2ConsultaControllerTest` y `SesionV2ConsultaControllerEndpointTest`; no se ejecutaron durante el análisis D y no equivalen a provider/E2E real.

## Reclamos y tomas: límites AS-IS de las subtareas B/C/E

- Reclamo vigente por sesión/toma: `POST /api/v1/asistencias/revisiones` devuelve 202; `tomaId` es opcional y la identidad se resuelve desde el JWT. Resolución docente: `PATCH /api/v1/docente/reclamos/{id}` con `APROBADA`/`RECHAZADA`; aprobación `PRESENTE` traduce a `AN`, `JUSTIFICADA` a `EX`, y aprobar sin resultado deja la decisión de DB. Recurso ajeno/inexistente en detalle docente se oculta como 404; al resolver, una titularidad fallida mapeada por DB es 403.
- Errores observados en mapeo/tests para tomas y reclamos incluyen `ERR_CATEGORIA_JUSTIFICACION_INVALIDA`, `ERR_SOPORTE_OBLIGATORIO_CATEGORIA`, `ERR_MAXIMO_SOPORTES_RECLAMO`, `ERR_SOPORTE_RECLAMO_DUPLICADO`, `ERR_TOMA_NO_RECLAMABLE`, `ERR_RECLAMO_TOMA_DUPLICADO`, `ERR_PLAZO_JUSTIFICACION_VENCIDO` y `ERR_APROBACION_PRESENTE_SIN_TOMA` (409). Son evidencia de código/contratos, no resultado del provider real.
- Bloqueos: DEC-073 aprobado no se encontró versionado; `HU005-HU016` conserva una ruta de estudiante retirada; `TECHNICAL_DEBT.md` y fuentes anteriores difieren sobre cierre de TD-060; soporte obligatorio por categorías MED/ACA/LAB contradice el soporte opcional de LB-004D; vínculo durable adjunto→reclamo→sesión→docente permanece bloqueado por `DR-LB004-DB-002`. No fijar expectativas de texto de rechazo, soporte, archivo o plazo por código aislado.
- DEC-066 aprueba 20 tomas máximo; no se encontró E2E que pruebe 20/21. La ventana SQL usa sus predicados propios, pero no hay evidencia suficiente aquí para certificar los anclajes exactos solicitados `[inicio-15, fin+30)`. `TOM_012` requiere dos tomas y comprobar 409 más no mutación; sin fixture seguro, queda NOT_RUN aunque un botón aparezca deshabilitado.
- Fuentes: `docs/work-items/PC-puntos-control-tomas/{PLAN,VALIDATION}.md`, `docs/work-items/HU005-HU016/{CONTRACT,VALIDATION}.md`, `docs/work-items/LB-004-stateless-serverless-readiness/{OWNERSHIP_DECISION,HTTP_CONTRACT_TARGET}.md`, `docs/baseline/TECHNICAL_DEBT.md` y `HTTP_AS_IS_MATRIX.md`. La inspección de fixtures confirma que los grupos históricos 9105/9106 y seed 001 no tienen cleanup seguro; no usar.

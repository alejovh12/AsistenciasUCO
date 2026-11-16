---
status: active
type: work-item
scope: backend
owner: backend-team
created: 2026-11-14
---

# FASE 2A — Arranque controlado, altas individuales e IDENTITY_DRIFT

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-11-14 / Backend / `develop` `9a72fce693efffada6a55b2c6176e45e3031d43c`.
- Frontend snapshot: `develop` `91f89f5681c560703f94e7feda0205e1f35f5513` (solo lectura).
- DB snapshot: `develop` `39da95388eb4606991e99af94f060a898bf38593` (solo lectura; preservar `.env.example` y `.env.template` locales).
- Objetivo: comprobar el arranque seguro del Backend y recorrer en navegador integrado las altas individuales y contratos de correo/ámbito por rol; reproducir y corregir defectos propios del Backend demostrados con evidencia y regresión.
- Criterios de aceptación: (1) el arranque solo ocurre tras verificar servicios, respaldo, jobs, efectos y procedimiento de parada; (2) cada escenario tiene rol, ruta visual, request/status/código/correlation ID y persistencia cuando corresponda, o queda `NOT_RUN` con causa/responsable; (3) se separa Backend de Frontend, DB/SP y proveedores; (4) cada corrección Backend tiene contrato fijado, RED inalterado por implementación, GREEN y regresión; (5) ningún secreto, credencial, token o PII real aparece en evidencia.
- Skills y fuentes: `uco-baseline`, `uco-arquitectura`, `uco-seguridad`, `uco-observabilidad`; AGENTS, `SOURCE_OF_TRUTH`, `DEFINITION_OF_READY`, `API_DESIGN_RULES`, contratos HTTP aplicables, `TESTING_STANDARD`, `VALIDATION_RUNBOOK`, arquitectura Security Baseline y manual validation ledger.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Backend inicia desde la copia personal en `develop` | Git `9a72fce693efffada6a55b2c6176e45e3031d43c` | Confirmado al crear este PLAN; no se infiere estado de runtime. |
| Frontend y DB están en las bases comunicadas | Git frontend `91f89f5681c560703f94e7feda0205e1f35f5513`; Git DB `39da95388eb4606991e99af94f060a898bf38593` | Frontend limpio; DB tiene cambios locales en `.env.example` y `.env.template`, preservarlos. |
| Backend tiene estado local que debe preservarse | Git status `develop...origin/develop`, `?? .claude/launch.json`, `?? .codex/` | No editar, borrar, limpiar ni incluir en cambios del work item. |
| Base personal SQL Server está ejecutándose | `docker ps` y listeners locales | `sql_server_asistencias` publica 1433. No reiniciar ni modificar DB/fixtures sin procedimiento seguro y documentado. |
| Security Baseline y otros servicios locales están arriba | `docker ps` | PEP/PDP/Keycloak local, Redis, GreenMail, MinIO, ClamAV y observabilidad arriba; estado funcional de health/decisiones pendiente de verificar con endpoints/documentación correctos. No implica que Backend esté arriba. |
| Puerto 8080 disponible; frontend 4200 sin listener | `Get-NetTCPConnection -State Listen` | Backend está detenido al inicio de la fase; confirmar antes del arranque. |
| Java predeterminado no cumple Java 25 | `java -version` | Host resuelve OpenJDK 17.0.18; localizar JDK 25 sin imprimir secretos antes de iniciar Backend. |
| Altas mutables solo son seguras con ambiente desechable controlado | `infra/security-baseline/README.md`, scripts `e2e_safety.py`, `e2e_provisioning.py`, `e2e_profile_assignment.py` | Los scripts E2E rechazan SQL personal/no desechable y exigen tres guardas. No ejecutarlos contra DB de desarrollo ni marcarla desechable. Requiere confirmar si existe ambiente de prueba ya autorizado y procedimiento de limpieza. |

## TARGET

Recorrido funcional completo con evidencia real de navegador integrado, sujeto a disponibilidad de sesión humana por rol y de fixture autorizado para writes. Errores propios de Backend corregidos con TEST_PLAN/RED aprobado y validación. Todo escenario no ejecutable queda explícitamente `NOT_RUN`; no se declara fase cerrada mientras queden pruebas requeridas sin resolver.

## Clase de cambio y alcance de rutas

- Change class: `DOCUMENTATION_ONLY` para este PLAN y la preparación/inspección inicial; `BEHAVIOR_CHANGE` solo mediante enmienda de alcance con archivo existente, defecto reproducible, contrato y snapshot RED identificados.
- PRIMARY_VARIABLE: porcentaje de escenarios de aceptación con resultado concluyente y evidencia trazable (`PASS` o `FAIL`); `NOT_RUN` permanece fuera de ese numerador.
- ALLOWED_PATHS: `docs/work-items/FASE-2A-altas-identity-drift/**`; además, por enmienda F2A-DEF-001, `src/test/java/co/edu/uco/asistenciasuco/application/features/coordinador/vinculardocenteprograma/usecase/impl/VincularDocenteProgramaUseCaseImplTest.java` para el RED, y `src/main/java/co/edu/uco/asistenciasuco/application/features/coordinador/vinculardocenteprograma/usecase/impl/VincularDocenteProgramaUseCaseImpl.java` solo después de conservar el snapshot RED aprobado. Por F2A-DEF-002, `AltaRequestLengthParityTest.java` contiene el RED inmutable; la implementación autorizada tras auditoría comprende `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/validation/LongitudTrasTrim.java`, `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/request/AdmitirEstudianteRequest.java`, `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/request/VincularDocenteProgramaRequest.java` y el record `CrearDecanoRequest` en `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/admin/AdminPortalController.java`; actualizar la nota de uso en `docs/architecture/input-validation.md`.
- FORBIDDEN_PATHS: Frontend y repo DB; SQL, esquema, migraciones y SP; `SecurityConfig`, catálogo de acceso, realm, `.env*`, compose/volúmenes y componentes externos de mensajes/parámetros; `.claude/launch.json`, `.codex/`; secretos, datos personales reales; otras rutas no enumeradas en ALLOWED_PATHS.
- CONTRACTS: requests/responses/error/status del OpenAPI canónico para operaciones cubiertas; para otras, `HTTP_AS_IS_MATRIX.md` + evidencia de código/tests; seguridad según fuentes activas y catálogo de acceso vigente; persistencia solo según contrato DB vigente.
- PROVIDERS: SQL Server runtime, Security Baseline/Keycloak/PDP/PEP, MinIO/ClamAV y GreenMail existentes, únicamente si el preflight los confirma. No arrancar proveedores externos de mensajes ni parámetros.
- EXTERNAL_ENVIRONMENT: repositorios Frontend/DB e Infra son de lectura; no instalar, crear ni reconstruir clones, worktrees, contenedores o bases.
- SECURITY_IMPACT: validar 401/403, RBAC/scope, identidad y provisioning; no ampliar permisos ni alterar seguridad externa. No capturar credenciales/cookies/JWT.
- OBSERVABILITY_IMPACT: registrar correlation ID y logs saneados para cada operación; nunca incluir secretos/PII.
- TEST_LEVEL_REQUIRED: E2E de navegador integrado para comportamiento UI/API; contract/component/unit e integración SQL solo para un defecto Backend reproducible y dentro de fixture segura. `mvn verify` según DoR/alcance final.
- ROLLBACK: detener únicamente el Backend iniciado por el procedimiento oficial; preservar SQL, contenedores/proveedores ya existentes, datos, volúmenes, artefactos y cambios locales. Cada corrección tendrá reversión por diff acotado, sin reset/stash/clean.
- CONSUMERS: frontend personal en el SHA indicado; DB personal y Security Baseline actuales como providers/owners externos.
- STOP_CONDITIONS: efectos de jobs no acotados; respaldo ausente o no verificable; proveedor/fixture inestable; discrepancia autoritativa; falta de usuario de prueba o limpieza segura para altas válidas; sesión expirada; evidencia insuficiente para atribuir fallo a Backend.

## Alcance

- Inspeccionar precondiciones de operación, backup Fase 1, jobs de arranque y efectos laterales.
- Arrancar Backend solo con Java 25 y script oficial de Security Baseline, después de documentar preflight y parada.
- Verificar management health, autenticación, SQL runtime, Frontend en navegador integrado y escenarios solicitados de administrador, decano, coordinador y estudiante cuando el usuario esté autenticado.
- Validar `GET /api/v1/configuracion/registro-estudiante`, dominio institucional y rechazos de correos por API real.
- Investigar `IDENTITY_DRIFT`, roles, scopes, correlación, logging e idempotencia.
- Corregir defectos demostrados propios del Backend con enmienda de PLAN, contrato y RED aprobado.

## No alcance

- HU170, HU171 y HU177; cambios en Frontend, DB/SP/migraciones, Infra remota, seguridad externa, catálogos/proveedores de mensajes y parámetros.
- Crear cuentas o vínculos sin mecanismo autorizado para identificarlos, controlarlos y limpiarlos.
- Repetir migraciones, `--prune`, aprovisionamiento PDP o cambios de datos/fixtures de la línea base.
- Iniciar proveedores de mensajes o parámetros, desplegar o fusionar PRs.
- Declarar PASS desde pruebas por terminal solamente para escenarios interactivos.

## Archivos afectados

- EXISTENTES: `AGENTS.md`, documentación autoritativa usada para operar y los archivos de código/tests concretos que se registren por enmienda antes de una corrección.
- NUEVOS: `docs/work-items/FASE-2A-altas-identity-drift/PLAN.md`; `TEST_PLAN.md`, `VALIDATION.md` y `CLOSURE.md` al iniciar/finalizar sus etapas.
- RETIRAR: ninguno.

## Contratos y consumidores afectados

DOMAIN / HTTP / PERSISTENCE / SECURITY / REALTIME: no se cambia contrato en el arranque ni en validación; cada endpoint/DTO/regla se cita desde contrato vigente o AS-IS comprobado. Frontend consume sin autoridad para redefinir HTTP; el owner DB conserva autoridad de persistencia. Cualquier conflicto detiene únicamente el escenario afectado.

## Riesgos y dependencias

- Jobs de token, alertas, festivos y outbox podrían mutar datos, llamar proveedores o generar notificaciones. El preflight debe inspeccionar código/configuración efectiva sin revelar valores; no iniciar si no se descartan efectos externos peligrosos.
- La DB personal no es desechable y contiene cambios locales ajenos; operaciones de alta podrían dejar cuentas o vínculos residuales.
- Para pruebas UI por rol se requiere autenticación manual del usuario en navegador integrado; nunca solicitar contraseña en chat.
- Java 25 y endpoints de health exactos deben verificarse antes del arranque.
- El frontend debe coincidir con la versión indicada; cambios posteriores requieren solo verificación del delta.

## Test plan

`TEST_PLAN.md` fijará matriz requisito/contrato/caso/resultado esperado y escenarios seguros antes de las pruebas; cada defecto confirmado tendrá su propia evidencia RED inmutable antes de implementación. El diagnóstico de arranque y las pruebas manuales no cambian contratos ni sustituyen la validación automatizada aplicable.

## Rollback y stop conditions

No iniciar Backend hasta que el responsable del preflight confirme servicios, respaldo existente verificable, efectos de jobs y detención segura. No ejecutar operaciones de alta exitosas mientras no exista un procedimiento seguro de fixture/identificación/limpieza. Ante `CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT` o `BLOCKED_BY_MISSING_EVIDENCE`, registrar fuentes, escenario afectado, evidencia requerida y owner; continuar solo trabajo independiente.

## Deuda conocida y validación manual

- MV-002: provisioning y roles con IdP real, pendiente previo; ampliar evidencia solo con ambiente permitido y sesión humana.
- Nuevos escenarios de Fase 2A quedarán en `VALIDATION.md` y ledger manual únicamente si corresponden a validación externa inevitable.
- TD asociada: se determinará por evidencia; no inferir/cerrar deuda histórica en este PLAN.

## Definition of Ready

- Estado inicial: `READY` para inspección de solo lectura, formalización documental y análisis operativo/contractual autorizado.
- `NOT_READY` para iniciar Backend hasta completar preflight (servicios/health, backup verificable, jobs y efectos, Java 25 y rollback).
- `NOT_READY` para una corrección de código hasta identificar contrato aplicable, consumidor, archivo/símbolo, requisito, TEST_PLAN, RED snapshot, fixture y rollback específicos, con conflictos relevantes en cero.
- Autoridad: solicitud del usuario de 2026-11-14 para Fase 2A y correcciones de defectos Backend demostrados; este PLAN no concede permiso para cambiar DB, Frontend o providers externos.

## Enmienda F2A-DEF-001 — reintento con identidad contradictoria

- Hallazgo a reproducir: `VincularDocenteProgramaUseCaseImpl.vincularPersonaNueva` encuentra una persona por correo y, si ya está vinculada al programa, devuelve el vínculo sin comparar el documento de la solicitud. El test actual solo cubre el reintento con la misma persona.
- Requirement/contract: `docs/work-items/HU062-HU089-HU063-HU090-HU091-vinculacion-docente/CONTRACT.md`, criterio CA-07 (correo o documento de otra persona se rechaza) y decisión D14 (reintento tras fallo IAM solo con la misma persona). No cambia contrato HTTP ni contrato DB.
- RED: el tester añade un caso aislado en el test permitido; given email y vínculo de docente existentes, when se envía otro número de documento con el mismo email, then conflicto funcional, sin nuevo vínculo y sin llamada a `IdentityProviderPort`. Hash/comando/fallo esperado quedan en `TEST_PLAN.md` antes de GREEN.
- Clase de cambio: `BEHAVIOR_CHANGE`; archivo productivo existente comprobado en la ruta permitida arriba. No requiere cambio SQL/provider/contrato público.
- DoR: `READY` para el cambio de comportamiento acotado: el usuario exige comprobar conflictos de identidad; el RED independiente falla por aceptación falsa; el código funcional público AS-IS queda fijado por `DocenteErrorCode.ERR_PERSONA_YA_REGISTRADA` y no se cambia contrato HTTP. Los CONTRACT HU062 son drafts de apoyo y no se presentan como aprobación.
- Rollback: revertir solo el diff de ese archivo productivo; conservar el test de regresión aprobado.

## Enmienda F2A-DEF-002 — paridad de longitud tras trim en altas individuales

- Hallazgo candidato a reproducir: los requests de admisión de estudiante (`AdmitirEstudianteRequest`), alta de docente (`VincularDocenteProgramaRequest`) y alta de decano (`AdminPortalController.CrearDecanoRequest`) declaran `@Size` sobre el texto crudo. Sus validators propios comparan `trim().length()`. Por la norma activa `docs/architecture/input-validation.md`, Bean Validation no puede rechazar un valor que el validator propio acepta; los patrones admiten espacios alrededor. No se extiende el hallazgo a altas de coordinador, que no tienen el equivalente normativo/validator identificado.
- Requisito: `docs/architecture/input-validation.md`, sección “Bean Validation (ROL-06, decision G de John, 2026-11-12)”: la primera barrera replica reglas simples y nunca rechaza texto que acepta el validator propio; las reglas de longitud se contrastan en nombre y correo para cada uno de los tres requests citados.
- Valores límite AS-IS: estudiante y decano — nombre 50, correo 255; docente — nombre 50, correo 100. `trim().length() == max` con espacios alrededor debe ser válido en ambas barreras; `trim().length() == max + 1` debe ser inválido en ambas.
- Alcance RED: prueba aislada con Jakarta Bean Validation y el validator propio de cada request, datos sintéticos, sin HTTP, persistencia ni proveedores. No alterar contrato HTTP, validadores productivos ni otros requests.
- TEST_PLAN: tres filas F2A-DEF-002 en la matriz; archivo permitido para RED: `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/validation/AltaRequestLengthParityTest.java`. Snapshot, comando, JDK, exit/count y causa quedaron registrados en `TEST_PLAN.md`.
- DoR de la etapa RED: `READY` para añadir únicamente esa prueba; en ese punto la implementación no quedaba autorizada y requería snapshot causal, revisión independiente y avance de etapa explícito. El avance GREEN de abajo completa esos pasos.
- Clase de cambio: `BEHAVIOR_CHANGE` confirmado por RED e implementado en la etapa GREEN, sin cambio de contrato público, DB, frontend, infraestructura o wiring.
- Rollback: retirar únicamente el archivo de prueba añadido si el auditor determina `TEST_CONTRACT_CONFLICT`; conservar esta trazabilidad y no modificar producción.

### Avance a GREEN — 2026-11-15

- RED completo y revisado: `TEST_PLAN.md` registra base, comando, JDK 25, hash `1C30F3E256DA2C7F2783DBCDE9AB927A7FD90DF56C2E37192BCC329F07BCF0D2` y 6 casos (3 fallos causales en máximos acolchados para los cuatro nombres y correo; 3 guardas max+1 aprobadas para los mismos cinco campos). La auditoría independiente aprobó esta versión del test; el hash y reporte Surefire se verificaron en el workspace.
- DoR: `READY` para implementación acotada en las rutas productivas enumeradas en `ALLOWED_PATHS`. Requisito: `docs/architecture/input-validation.md` ROL-06; los tres validators propios aceptan longitud tras `trim()`, mientras `@Size` mide el texto crudo. No cambia endpoint, JSON, status, campo, código público, DB ni provider; el código de error de longitud debe seguir siendo `FIELD_INVALID_LENGTH`.
- Estrategia: agregar una constraint `LongitudTrasTrim` en Infrastructure Validation que aplique la misma métrica `trim().length()`; reemplazar solo los `@Size` de nombre/correo en las tres solicitudes citadas, con `payload = BeanValidationCodes.InvalidLength.class`. Mantener intacto el RED aprobado. No extender a alta de coordinador ni a otros requests sin evidencia.
- Documentación: actualizar `docs/architecture/input-validation.md` para indicar cuándo corresponde `@LongitudTrasTrim` y reservar `@Size` para contratos que midan el texto crudo; la regla de paridad ROL-06 no cambia.
- Validación requerida para GREEN: ejecutar el RED sin alterar el test, regresión aplicable y `clean verify` con Java 25; registrar salida, cobertura y límites en `VALIDATION.md`. E2E/navegador sigue bloqueado por el preflight de arranque; no crear cuentas.
- Rollback: revertir exclusivamente `LongitudTrasTrim.java` y los cambios de constraints en los tres requests si falla una regresión; conservar el test y snapshots para diagnóstico.

### GREEN completado — 2026-11-15

- `LongitudTrasTrim` y los tres request surfaces aplican ahora `trim().length()` a los cuatro nombres y correo, con payload `InvalidLength`. La auditoría independiente aprobó la implementación y confirmó que se preservan obligatoriedad/opcionalidad, mensajes, JSON, status previsto, clasificación de error y límites de rutas.
- GREEN focal: 6/6 tests. `clean verify`: 4539 tests/0 fallos/0 errores/0 skips; JaCoCo 94.92 % líneas / 86.53 % ramas. Evidencia y límites en `VALIDATION.md`; el test RED/hash de `TEST_PLAN.md` permaneció intacto.
- Este avance completa la corrección automatizada; no cierra la Fase 2A. El preflight de inicio y las pruebas HTTP live, navegador, roles, IDENTITY_DRIFT y providers siguen pendientes.

### Adenda de preflight — 2026-11-15

- `BLOCKED_BY_MISSING_EVIDENCE-2A-002` queda resuelto para el respaldo: se encontró y verificó el archivo esperado en `%USERPROFILE%\.asistencias-security-lab\db-backups\gestionasistenciadb_antes_migrar_20261114-173254.bak`; el detalle y la ruta de recuperación están en `VALIDATION.md`.
- El preflight de workers ya tiene evidencia de solo lectura: sin tokens QR vigentes, ventanas de matrícula vigentes ni tomas abiertas; festivos activos cargados para todo el horizonte 2026–2027 configurado. El arranque está autorizado solo con los overrides por proceso documentados en `VALIDATION.md` que impiden evaluación global de alertas y consumo/purga del outbox; no se desactiva silenciosamente ni se modifica configuración persistente.
- Al momento de esta adenda el Backend todavía no se había iniciado. Confirmar puerto 8080, salud/autenticación y efecto real de jobs después del arranque antes de cerrar `OPS-01/02`. Las altas, el login manual y el navegador de Fase 2A continúan pendientes; no se amplía este PLAN para encubrirlas como completadas por la Fase 2B.

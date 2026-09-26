---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Línea base técnica activa

La vertical patrón es [asistencia en lote + consulta + realtime](GOLDEN_PATH_ASISTENCIA.md), decidida en [ADR-001](../adr/ADR-001-golden-path-asistencia.md). Esta es la única secuencia activa; los roadmaps del [archivo](../archive/README.md) no ordenan trabajo nuevo.

| Fase | Alcance | Gate de salida | Estado |
|---|---|---|---|
| LB-000 | Gobernanza y documentación | Jerarquía única, routers, deuda, trazabilidad, sin cambios funcionales y validación registrada | GOVERNANCE COMPLETE; TECHNICAL BUILD GATE PASS (TD-029 cerrada por [TECH-001](../work-items/TECH-001-restaurar-gate-arquitectura/CLOSURE.md)); ver [cierre](../work-items/LB-000-gobernanza-documentacion/CLOSURE.md) |
| LB-001 | Golden Path + Contract First (ver descomposición abajo) | AS-IS/consumidores/DB comprobados; plan y contrato aprobados; tests previstos, sin conflictos relevantes | **CLOSED / FROZEN**: LB-001A/B/B.1/B.3/B.4 DONE; DB↔backend y backend↔frontend ALIGNED; MV-001 PASS reportado; LB-001C C.1/C.1A/C.2/C.3 PASS; OpenAPI Contract First, PATCH canónico y Swagger UI runtime canónica. No se inicia otra fase automáticamente |
| LB-001D | Governance Hardening | C.1 sincroniza documentación/skills/agentes/Azure; C.2 corrige seguridad del webhook y aísla Cloud Integration | **C.1 CLOSED** ([reporte](../work-items/LB-001D-governance-hardening/LB-001D.1-REPORT.md)); **C.2 CLOSED** ([reporte](../work-items/LB-001D-governance-hardening/LB-001D.2-REPORT.md)): TD-051/052/053 resueltas; TD-054/DR-AZ-001 sigue `DECISION_REQUIRED`; MV-003 PENDIENTE (evidencia parcial Azure real). **LB-001D: CLOSED / FROZEN**. Branch coverage 70.37 % (cerca del gate 70 %; no es fallo, sin tests cosméticos ni cambio de umbral). SQL_INTEGRATION: NOT_RUN_BY_ENVIRONMENT (obligatoria en LB-002) |
| LB-002 | Piloto JDBC → JPA | [Estrategia incremental](../persistence/JDBC_TO_JPA.md), paridad real y rollback; puertos y contratos preservados | **READY** (LB-001D cerrada); NOT STARTED; requiere integración SQL Server real |
| LB-003 | Quality Gate Golden Path | Unit/application, ArchUnit, contrato, SQL Server real, E2E y [DoD](DEFINITION_OF_DONE.md) | NO INICIADA |
| LB-004 | Stateless / Serverless readiness | Estado funcional fuera de filesystem/heap de réplica; storage y configuración/secrets externos; evaluar caché según necesidad | NO INICIADA |
| LB-005 | Realtime distribuido | Provider neutral, decisión por ADR y pruebas multi-instancia/costo/operación | NO INICIADA |
| LB-006 | IaC / CD / cloud | Despliegue reproducible, imágenes inmutables, observabilidad, promoción y rollback | NO INICIADA |
| LB-007 | Replicar patrón | Golden Path cerrado antes de extender JPA/contract-first a otras verticales | NO INICIADA |

No combinar JPA, cambio de negocio, contrato, realtime y cloud en una tarea sin límites. Dividir por una variable principal y dependencias explícitas. Ninguna fase autoriza automáticamente la siguiente.

## Descomposición conceptual de LB-001

| Sub-fase | Alcance | Clase de cambio | Estado |
|---|---|---|---|
| LB-001A | Alineación DB ↔ backend del Golden Path ([protocolo](../integration/CONTRACT_ALIGNMENT_PROTOCOL.md)) | CONTRACT_ANALYSIS | DONE / CONTRACT ALIGNED ([cierre](../work-items/LB-001A-db-backend-asistencia/CLOSURE.md)) |
| LB-001B | Alineación backend ↔ frontend / contrato HTTP | CONTRACT_ANALYSIS | ANALYSIS COMPLETE / HTTP-FRONTEND CONTRACT NOT FROZEN; `NOT_READY_FOR_LB001C` hasta resolver DR-002/003/005/006/007/008/009/010 (DR-001 y DR-004 ya `RESOLVED — Opción B`, ver LB-001B.1). **Actualización LB-001B.4:** DR-001/002/004/005/006/007/008/009 RESOLVED, DR-003 mapping RESOLVED (timezone HTTP en TD-005/LB-001C); DR-010 RESOLVED — Opción A (LB-001B.4A: listas completas AS-IS, sin paginación) ([cierre](../work-items/LB-001B-backend-frontend-asistencia/CLOSURE.md)) |
| LB-001B.1 | DB source of truth — retiro de `descripcion/aula/tipo` del contrato de creación/actualización de `Sesion` (backend); resolución formal de DR-001/DR-004 (Opción B) | CONTRACT_CHANGE + BEHAVIOR_CHANGE | **DONE (backend + frontend 1A/1B/1C)** — ver [LB-001B.1 CLOSURE](../work-items/LB-001B.1-db-source-of-truth-cleanup/CLOSURE.md) y evidencia frontend `LB-001B.1C-final-contract-cleanup/CLOSURE.md` en `AsistenciasUCO-Frontend` |
| LB-001B.3 | Backend alignment against frozen DB baseline (`DB_BASELINE_CONTRACT.md`) — Sesion SP signatures, horario docente, dominio de asistencia, UTC persistencia, mapeo de errores DB | CONTRACT_CHANGE + BEHAVIOR_CHANGE | **DONE / SUPERSEDED BY [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md)** (el `CONTRACT_CONFLICT` de mapeo de errores DB y el tercer lector UTC se resolvieron en LB-001B.4). Estado histórico al cierre parcial: **IN_PROGRESS / PARTIAL — sub-alcance desbloqueado DONE, work item completo NOT_READY.** `idDocente` retirado de `usp_crear_sesion`/`usp_actualizar_sesion` (MATCH, GREEN, auditado); `aula` retirada de `uv_horario_docente` (MATCH, GREEN, auditado — cambia el JSON de `GET /api/v1/docente/horarios`, riesgo frontend no verificado); UTC de Sesion resuelto en 2 de 3 llamadores confirmados (tercero, `SesionMateriaEstudianteSqlServerAdapter`, con `TEST_CONTRACT_CONFLICT` sin resolver); `mvn verify` BUILD SUCCESS 936/936, ArchUnit PASS, JaCoCo PASS (LINE 85,62 %/BRANCH 70,51 %), todo reproducido de forma independiente por 05-auditor. **Mapeo determinista de errores DB (`SEC_001/SEC_002/ATT_*/SES_003/SES_004/GEN_002`) permanece `CONTRACT_CONFLICT` sin resolver, por decisión explícita del usuario (2026-09-23) de continuar solo con el alcance desbloqueado.** DB no modificada; frontend no modificado; OpenAPI/JPA/Redis/serverless no iniciados. Ver [LB-001B.3 CLOSURE](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md) |
| LB-001B.4 | Cierre final backend ↔ DB congelada: canal `DBCODE`, mapping de errores por código, retiro de ghost fields (Grupo, HorarioEstudiante), UTC en los lectores de `Sesion`, wire format `occurredAt`; contrato backend del Golden Path | CONTRACT_CHANGE + BEHAVIOR_CHANGE | **DONE FINAL — DB↔BACKEND GOLDEN PATH ALIGNED** (incluye 4A SESSION CONTRACT POLISH: `tema` y `docente` retirados de create/update de `Sesion`; TD-042 CLOSED; DR-010 RESOLVED Opción A; 4B SESSION NAME LENGTH ALIGNMENT: `Sesion.nombre` 1..50 alineado con DB `NVARCHAR(50)`, [TD-048](TECHNICAL_DEBT.md#td-048) CLOSED; DB↔BACKEND ALIGNED). `mvn -B -ntp verify` (JDK 25, 4B) 970 tests 0F/0E/0S, ArchUnit 20/20, JaCoCo LINE 86,48 %/BRANCH 70,93 %; (histórico 4A: 967 tests; pre-4A: 959 tests, LINE 87,89 %/BRANCH 71,60 %); Golden Path IT 22/22 (`AsistenciaRepositorySqlServerIT` 6/6 + `GoldenPathSqlStoredProcedureContractIT` 16/16, 0 skips); contrato DB SHA-256 `45e48c5a...9aec` MATCH. **`verify -Pintegration` completo NOT_GREEN (53 tests, 6 fallos, 2 skips) solo por [TD-043](TECHNICAL_DEBT.md#td-043), fuera del Golden Path.** Estado histórico al cierre de B.4: READY FOR FRONTEND VERIFICATION: YES; READY FOR LB-001C: NO y MV-001 PENDIENTE; esas precondiciones quedaron posteriormente satisfechas antes de iniciar C.1. IT/Golden Path con DB no re-ejecutados en 4A/4B (sin DB; persistence/SP sin cambios en 4B). Contrato: [BACKEND_GOLDEN_PATH_CONTRACT](../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md). Ver [CLOSURE](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md) |
| LB-001C | Freeze Contract First: OpenAPI + contratos realtime | CONTRACT_CHANGE | **CLOSED / FROZEN — C.1, C.1A, C.2 y C.3 PASS**. OpenAPI 3.1.2 canónico con 9 operaciones (GET 5, POST 2, PATCH 1, PUT legacy/deprecated 1, DELETE 0), SHA `72a3097b...6da54`; contrato backend SHA `9b4830b...8c62`; contrato DB consumido SHA `45e48c5a...9aec`. Swagger UI 5.32.15 offline consume solo el YAML canónico, sin `/v3/api-docs`, habilitada en local/dev y deshabilitada por defecto/producción. C.3: `mvn verify` 993/993, ArchUnit 67/67, JaCoCo LINE 86,71 % / BRANCH 70,98 %, HTTP/browser PASS. [Cierre final](../work-items/LB-001C-openapi-contract-first/CLOSURE.md). No se inicia otra fase automáticamente |

Secuencia cerrada: LB-001A → LB-001B → LB-001B.1 → LB-001B.3 → LB-001B.4 → LB-001C.1/C.1A → LB-001C.2 (PASS) → LB-001C.3. LB-001C queda CLOSED/FROZEN. No se crean carpetas ni se inician microfases sin autorización. LB-002 sigue siendo el piloto JDBC → JPA (NOT STARTED).

**Nota sobre el repo frontend (actualización 2026-09-22):** `AsistenciasUCO-Frontend` contiene `LB-001B.1A-frontend-contract-corrections/CLOSURE.md`, `LB-001B.1B-db-source-of-truth-cleanup/CLOSURE.md` y `LB-001B.1C-final-contract-cleanup/CLOSURE.md`. La nota anterior que decía que `LB-001B.1B` no tenía `CLOSURE.md` queda sustituida por esta evidencia. La validación frontend de 1C reporta `npm run verify` PASS, 184/184 specs, coverage realtime PASS y build production PASS. No se falsifica fecha histórica: esta actualización se registra el 2026-09-22 al sincronizar la documentación.

**NO JPA ANTES DE CERRAR EL CONTRATO DE PERSISTENCIA DEL GOLDEN PATH.**

## Gate técnico general y clases de cambio

Estado actual: `TECHNICAL_BUILD_GATE = PASS` desde [TECH-001](../work-items/TECH-001-restaurar-gate-arquitectura/CLOSURE.md) ([TD-029](TECHNICAL_DEBT.md#td-029) cerrada, CF-002 resuelto). Un build base rojo no impide `DOCUMENTATION_ONLY` ni `CONTRACT_ANALYSIS`. Impide iniciar implementación (`CONTRACT_CHANGE`, `REFACTOR`, `BEHAVIOR_CHANGE`, `PERSISTENCE_MIGRATION` —JPA incluido— e `INFRASTRUCTURE`) hasta su resolución o una excepción explícita aprobada (regla que se aplica si el gate vuelve a fallar); ver [DoR](../governance/DEFINITION_OF_READY.md).

## Evoluciones conservadas del roadmap anterior

Estas opciones no son capacidades implementadas ni una selección de proveedor aprobada:

| Tema | Tratamiento |
|---|---|
| Idempotencia de commands; estrategia transaccional multi-adapter | Evaluar contrato/reintentos y consistencia antes de diseñar implementación; [TD-015](TECHNICAL_DEBT.md#td-015) |
| CQRS ligero y Redis cache-aside | Propuestas históricas: solo evaluar con necesidad medida; no introducir CQRS, Redis ni puertos especulativos |
| Métricas de fallos de auditoría, validación e idempotencia | Evolución operacional de LB-003/LB-006; mantener logs/metrics/traces/correlation actuales |
| Parameterized/property-based/mutation tests | Técnicas a seleccionar por riesgo; no son gates configurados actualmente |
| Empaquetado limpio, SBOM, image/dependency scanning | LB-006; CI ya construye imagen y ejecuta CodeQL/Dependency Review, no afirmar SBOM/scanning adicional existente |
| Contrato común SecurityErrorResponseWriter/ApiErrorResponse y exposición Actuator | [TD-021](TECHNICAL_DEBT.md#td-021) y [TD-022](TECHNICAL_DEBT.md#td-022) |
| ObjectHelper/CrosscuttingException | Limpieza oportunista limitada al código afectado, [TD-025](TECHNICAL_DEBT.md#td-025) |
| Outbox y mensajería durable | LB-005, condicionados por contrato DB y ADR; [TD-011](TECHNICAL_DEBT.md#td-011) |
| Cobertura frontend 80 % mencionada por el paquete | Objetivo histórico pendiente de acuerdo con su repositorio; no es gate comprobado del backend |

Los adapters Azure existentes de vault/catálogos son evidencia del checkout; no demuestran despliegue cloud, CD ni E2E. LB-001D.1 documenta el AS-IS, sin cambiar runtime ni operar recursos Azure.

## Próxima instrucción

LB-001C cerró la gobernanza de estilo y congeló el contrato Golden Path; LB-001C.1A aplicó un
hardening posterior al review sobre [OpenAPI 3.1.2](../contracts/openapi/openapi-golden-path.yaml),
sin cambiar el wire. C.2 se registra como PASS reportado por la autorización humana: 9 operaciones
y cero consumidores frontend PUT; no autoriza retirar compatibilidad. C.3 publica Swagger UI
offline sobre el mismo YAML, sin generación desde annotations ni `/v3/api-docs`. El PUT de sesión
y una futura migración del wire temporal siguen siendo candidatos de fase separada; no se cambian
por este cierre. El cierre final registra OpenAPI Contract First PASS, PATCH canónico, PUT
deprecated, Swagger UI PASS, 9 operaciones y los hashes contractuales vigentes. [TD-043](TECHNICAL_DEBT.md#td-043) permanece fuera del Golden Path y mantiene el
perfil de integración completo en rojo. No iniciar JPA, generación Angular ni otro work item sin
autorización. LB-001D.1 solo sincroniza gobernanza; LB-001D.2 cerró el hardening de seguridad del webhook Azure y el aislamiento de Cloud Integration (LB-001D cerrada/congelada; LB-002 READY).

## Ledger activo de estado técnico

| Elemento | Estado | Fuente |
|---|---|---|
| TECHNICAL_BUILD_GATE | PASS (`mvn verify`, 932 tests, JaCoCo dentro de gates) | [TECH-001 VALIDATION](../work-items/TECH-001-restaurar-gate-arquitectura/VALIDATION.md) |
| TD-029 / CF-002 | CERRADA / RESUELTO | [TECH-001 CLOSURE](../work-items/TECH-001-restaurar-gate-arquitectura/CLOSURE.md) |
| ME-001 (evidencia DB del Golden Path) | CLOSED, con evidencia certificada externamente (abajo). El historial de FINDINGS de LB-000 no se reescribe | este ledger |
| TECH-001 | DONE; revisión humana APROBADA | [TECH-001 CLOSURE](../work-items/TECH-001-restaurar-gate-arquitectura/CLOSURE.md) |
| LB-001A | DONE / CONTRACT ALIGNED; persistence contract golden path READY_FOR_FREEZE | [LB-001A CLOSURE](../work-items/LB-001A-db-backend-asistencia/CLOSURE.md) |
| LB-001B.1 (backend) | DONE — DR-001/DR-004 `RESOLVED — Opción B`; `descripcion/aula/tipo` retirados del contrato de creación/actualización de `Sesion`; DB no modificada; `verify` BUILD SUCCESS 933/933 | [LB-001B.1 CLOSURE](../work-items/LB-001B.1-db-source-of-truth-cleanup/CLOSURE.md) |
| LB-001B.1C (frontend) | DONE — contrato frontend de `Sesion` alineado; `ClassSession` sin ghost fields; `ClassSessionDTO`/`CourseMapper`/`CrearSesionRequest` muertos eliminados; `npm run verify` PASS 184/184; global coverage lines 61.22%, branches 42.58%, functions 46.48%; realtime coverage lines 95.62%, branches 87.50% | `AsistenciasUCO-Frontend/docs/work-items/LB-001B.1C-final-contract-cleanup/CLOSURE.md` |
| LB-001B.3 (backend) | **DONE / SUPERSEDED BY LB-001B.4** (histórico: PARTIAL al 2026-09-23, 936/936) | [LB-001B.3 CLOSURE](../work-items/LB-001B.3-backend-db-alignment/CLOSURE.md) |
| LB-001B.4 (backend) | **DONE FINAL — DB↔BACKEND GOLDEN PATH ALIGNED** (TD-048 cerrada en 4B); `verify` 970/970 (4B), ArchUnit 20/20, JaCoCo LINE 86,48 %/BRANCH 70,93 % (4A); Golden Path IT 22/22 sin skips (corrida LB-001B.4, no repetida en 4A); `-Pintegration` completo NOT_GREEN por TD-043 (no Golden Path). Estado histórico al cierre de B.4: READY FOR FRONTEND VERIFICATION: YES; READY FOR LB-001C: NO; MV-001 pendiente. Superado por las precondiciones de entrada y el cierre de C.1. | [LB-001B.4 CLOSURE](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md), [VALIDATION](../work-items/LB-001B.4-final-backend-contract-closure/VALIDATION.md) |
| LB-001C.1 | **DONE — API STYLE + OPENAPI/SWAGGER GOLDEN PATH BASELINE**; 8 operaciones; parser/ref/SHA/conformance PASS; `mvn verify` 975/975; ArchUnit 67/67; JaCoCo LINE 86,73 %/BRANCH 70,98 %; producción/DB/frontend/Keycloak sin cambios | [CLOSURE](../work-items/LB-001C-openapi-contract-first/CLOSURE.md), [VALIDATION](../work-items/LB-001C-openapi-contract-first/VALIDATION.md) |
| LB-001C.1A | **DONE — OPENAPI CONTRACT HARDENING POSTERIOR AL REVIEW**; LocalSessionDateTime sin `date-time`, realtime/error mantienen instantes, server portable, OAS-07/OAS-08 completos; OpenAPI 10/10; `mvn verify` 980/980; producción/DB/frontend/Keycloak sin cambios | [REPORTE](../work-items/LB-001C-openapi-contract-first/LB-001C.1A-REPORT.md), [VALIDATION](../work-items/LB-001C-openapi-contract-first/VALIDATION.md), [RED](../work-items/LB-001C-openapi-contract-first/LB-001C.1A-RED-SNAPSHOT.md) |
| LB-001C.2 | **PASS REPORTADO EXTERNO** en la autorización C.3: OpenAPI 3.1.2, 9 operaciones (GET 5, POST 2, PATCH 1, PUT legacy/deprecated 1, DELETE 0), SHA `72a3097b...6da54`, consumidores frontend PUT 0. Este checkout conserva evidencia local C.2A; el PASS global C.2 no se presenta como reproducido aquí | orden humana C.3 y [C.2A REPORT](../work-items/LB-001C-openapi-contract-first/LB-001C.2A-REPORT.md) |
| LB-001C.3 | **DONE — SWAGGER UI RUNTIME SOBRE OPENAPI CANÓNICO**; Swagger UI 5.32.15 offline, UI/YAML condicionados, sin `/v3/api-docs`, API protegida; RED causal 35 tests/4 fallos → GREEN 35/35; `mvn verify` final 993/993; JaCoCo LINE 86,71 % / BRANCH 70,98 %; hash runtime/canónico MATCH; navegador PASS con aviso no bloqueante de dialecto | [REPORTE](../work-items/LB-001C-openapi-contract-first/LB-001C.3-REPORT.md), [VALIDATION](../work-items/LB-001C-openapi-contract-first/LB-001C.3-VALIDATION.md), [RED](../work-items/LB-001C-openapi-contract-first/LB-001C.3-RED-SNAPSHOT.md) |
| TD-030 | RESOLVED BY FROZEN DB BASELINE (LB-001B.4): `SEC_001/SEC_002/EST_004 -> 403/FORBIDDEN` | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-030) |
| TD-036 | CLOSED (LB-001B.4): mapping DBCODE formal, fail-closed | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-036) |
| TD-005 | CLOSED_FOR_GOLDEN_PATH (LB-001C.1); wire ISO local sin offset congelado AS-IS, sin declarar política temporal global cerrada | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-005) |
| TD-042 | CLOSED (LB-001B.4A): `RealtimeEventResponseSpringJsonTest` con `@SpringBootTest` | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-042) |
| TD-048 | CLOSED (LB-001B.4B): backend alineado con DB `NVARCHAR(50)` (`Sesion.nombre` 1..50) | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-048) |
| TD-043 | ABIERTA, NON_GOLDEN: 3 SP inexistentes; `-Pintegration` completo NOT_GREEN | [TECHNICAL_DEBT](TECHNICAL_DEBT.md#td-043) |

Evidencia DB ↔ backend del Golden Path, certificada externamente (reportada por el responsable, no reproducida en el repositorio backend): DB GATE 76 PASS, 0 FAIL, 1 SKIP autorizado; CRITICAL_MISSING=0; SQL_ERROR_COUNT=0; @@TRANCOUNT=0. Backend: `SqlStoredProcedureContractIT` 20/20 y `AsistenciaRepositorySqlServerIT` 6/6. Esta evidencia sustentó LB-001A/B.4. LB-001C.1 congela únicamente el contrato HTTP/SSE Golden Path; no revalida DB ni resuelve TD-043 fuera de alcance.

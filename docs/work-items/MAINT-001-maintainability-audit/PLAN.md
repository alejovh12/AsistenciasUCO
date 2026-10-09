---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---

# MAINT-001 — auditoría de mantenibilidad/clean code basada en evidencia

## Propósito
Inspeccionar deuda técnica en el backend JPA-only / LB-004 posterior al merge PR #15 sin reemplazar arquitectura ni interrumpir PR #16 security. Referencia de develop revisado: `551594179c2de582cbe62b4490276879d1ffc886` (merge #15); 20 suites de arquitectura / 82 tests pasan en run de PR #16, no garantizan maintainability total.

## Clasificación, restricciones y gobernanza
- Cambio actual: DOCUMENTATION_ONLY, sin modificaciones Java/DB/Angular/pom/OpenAPI ni tests.
- Fuente de verdad: AGENTS.md → SOURCE_OF_TRUTH → DoR → skill → test plan → contratos.
- Sonar: comentario PR #15 histórico tenía **206 New issues** con Quality Gate PASSED y Maintainability A; **reglas, líneas y distribución de los 206 NO RECUPERADAS** en esta auditoría. No calificar cada candidato de `Sonar issue`.
- No tomar número 206 como medida actual de `develop`, ni atribuir seguridad/criticidad sin claves.
- No refactor masivo para subir índice: cada microfase requiere baseline, frontera, tests RED/contract y validación independiente.
- Permitido en microfases posteriores con DoR READY: pruebas y cambios Java específicos, nunca sin CI/JDK25; commit de implementación separado del PR de Trivy.

## Candidatos identificados en código real develop

| ID | Prioridad | Clase/observación | Riesgo/hipótesis | Definición de cierre |
|---|---|---|---|---|
| M01 | P1 | `JpaQueryExecutor.execute` captura `PersistenceException, IllegalStateException, IllegalArgumentException, ArithmeticException` alrededor de TODO el `Supplier` | Excepción de proyección/arithmetic puede convertirse indebidamente en `DatabaseOperationException` | Test RED: error de mapeo no traducido como error DB; JPA persistence sí y causa preservada. Decidir alcance del catch con implementador |
| M02 | P1 | `ParameterCatalogJpaRepository.getParameter` usa `catch(Exception)` y caché `ConcurrentHashMap` sin invalidador en la clase | Puede capturar errores ajenos a DB; política de invalidez/cache entre réplicas no explicitada | Identificar contrato catálogo/TTL/fuente de verdad; tests error provider vs programming, invalidación y concurrencia. No cambiar política sin decisión |
| M03 | P1 | `EstudianteJpaRepository` usa `dto.page()*dto.size()` en `int`, construye filtros opcionales con condiciones null | Posible overflow y coste de búsqueda/paginación bajo tamaños no acotados | Revisar validator de controller/input; negativa extremos y carga sobre SQL; si ya acotado -> CLOSED_BY_EXISTING_CONTRACT |
| M04 | P2 | `ArchivoController.subirArchivo` construye respuesta con `HashMap<String,Object>` | Menor seguridad de tipos y OpenAPI más difícil de versionar | Diseñar DTO de salida versionado, validar consumidores FE y contract tests; no cambiar wire shape |
| M05 | P2 | `ArchivoController.descargarArchivo` `Content-Disposition: inline`, sin decisión de `nosniff` | Comportamiento browser MIME/sniffing requiere hardening y compatibilidad | Decisión security+frontend con prueba HTTP; no cambiar por intuición |
| M06 | P2 | Records con `byte[]` poseen semántica equals/hashCode por contenido pero exponen buffer mutable | Mutabilidad accidental y aliasing, coste de copia | Identificar owners/ciclo de vida; contrato inmutabilidad; pruebas mutable alias/copia antes de tocar |
| M07 | P2 | `ParameterCatalogJpaRepository.getParameterAs` convierte cualquier Boolean no reconocido a `false` | Datos institucionales inválidos pueden quedar invisibles | Decisión contrato de catálogo + tests true/false/invalid; no alterar baseline por defecto |
| M08 | P2 | Pila de errores `GlobalExceptionHandler`/`DbExceptionTranslator` | Cambios ad hoc pueden romper códigos HTTP, correlación o saneamiento | Matriz excepción→HTTP y traza; tests 400/403/404/409/500 y de mensaje técnico no expuesto |
| M09 | P1 | Tests IT DB: `DocenteRepositorySqlServerIT` reportó un skip por falta de fixture en auditoría previa | Caso de paridad no ejecutado pese a suite verde | Fixture académico mínimo y rerun SQL real, registrar skip=0 para alcance afectado |

## Sonar: procedimiento para obtener deuda real
1. Acceder a proyecto SonarCloud `alejovh12_AsistenciasUCO` **branch develop**, no PR histórico; exportar issues abiertos y clasificar tipos mantenibilidad (CODE_SMELL), impacto, severidad, ruleKey, fichero, línea, esfuerzo, deuda, fecha y owner.
2. Obtener métricas: maintainability rating, technical debt ratio, code smells, complexity/cognitive complexity, duplicaciones, quality gate status. Registrar URL, SHA/análisis.
3. Priorización: bugs/security que no pasan gate antes de smells; dentro de maintainability: lógica compleja que dificulta cambios / repetición / acoplamiento / tests débiles vs detalles de formato sin impacto.
4. Relacionar issue Sonar concreto con M01–M09, **solo si el rule key/path coinciden**. Si Sonar no accesible, marcar `ISSUE_DETAILS_UNAVAILABLE`, sin especular.
5. Reducir deuda por micro-PR con contrato y tests, guardar baseline de diff y nuevo scan. Medir efectos de cambio y paridad.

## Secuencia de ejecución segura
- MAINT-00: exportar Sonar + matriz concreta y decisión de prioridad, sin cambio runtime.
- MAINT-01: pruebas RED de excepciones JPA/catalog; separar error funcional, persistencia, programming; restaurar cobertura de propagación y logs.
- MAINT-02: validar pagination/overflow y política boolean/cache mediante contratos y pruebas; hacer fix solo si RED confirma riesgo.
- MAINT-03: DTO response y HTTP headers + aliasing bytes condicionado por contratos/consumidores.
- MAINT-04: fixture DB para IT omitido; independencia del owner DB para cambios semilla/fixturing.
- MAINT-05: auditoría cierre; ArcUnit/Sonar/JaCoCo/CodeQL/Trivy actualizado, documentación y merge autorizados.

## Definition of Ready / Done
READY: inventario/análisis. Implementación: NOT_READY hasta tests y autoridad de microfase. Criterios: GREEN local JDK25, -Pintegration cuando corresponda, Sonar issues cerrados en nuevo SHA, ningún contrato roto, Sonar Quality Gate y ArchUnit PASS. No mover BD durante este work item para desbloquear fixture sin autorización.

---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Estándar de testing — AsistenciasUCO

## Objetivo

Los tests son especificaciones ejecutables. No existen para "hacer subir JaCoCo" ni para confirmar
a posteriori la implementación escogida.

## Flujo obligatorio

1. criterios de aceptación;
2. contrato;
3. matriz de tests;
4. tests RED;
5. implementación;
6. tests GREEN;
7. refactor;
8. validación completa.

El tester no modifica producción. El implementador no modifica tests RED.

## Gates actuales comprobados

Fuente ejecutable: [pom.xml](../../pom.xml) y [.github/workflows](../../.github/workflows/backend-ci.yml).

- Java `[25,26)` por Maven Enforcer.
- JaCoCo BUNDLE, COVEREDRATIO: **LINE >= 0.80; BRANCH >= 0.70**, en fase verify.
- ArchUnit/JUnit en Surefire, dentro de verify; reportes en `target/surefire-reports`.
- Failsafe `**/*IT.java` (excepto `**/*CloudIntegrationIT.java`) solo con perfil `integration`, configurado con `application-integration`. Cloud Integration Azure: perfil `azure-integration` (solo `**/*CloudIntegrationIT.java`, sin Surefire ni JaCoCo).
- CI: clean verify, existencia de reportes JaCoCo/JAR y Docker build. Sonar análisis + espera de Quality Gate solo para PR/push develop; el umbral remoto de New Code no está versionado aquí.
- CodeQL Java y Dependency Review (PR, high+) en workflow de seguridad. Protección de ramas y resultados remotos requieren evidencia externa.
- Integración SQL workflow manual; no required check hasta disponer de DB CI reproducible.
- LB-001C.1 agrega gate OpenAPI al `mvn verify` mediante tests: Swagger Parser 3.1, resolución de
  `$ref`, SHA-256 y conformance crítica controller/DTO. No hay generación de código ni dependencia
  runtime. El E2E frontend sigue siendo evidencia manual/externa en MV-001.

Cobertura es un gate, no el objetivo principal.

Coverage is necessary but not sufficient. La cobertura no compensa negativos ausentes, falta de integración requerida, efectos no verificados, asserts débiles ni mocks excesivos.

## Protocolo e integridad

REQUIREMENT → CONTRACT → TEST_PLAN → RED TEST → IMPLEMENTATION → GREEN → VALIDATION.
Prohibido IMPLEMENTATION → tests diseñados para confirmar esa implementación. En un bug o cambio funcional RED falla por la condición esperada, no por falta de red/DB. Guardar hash/commit de RED aprobado. El implementador no lo modifica; `TEST_CONTRACT_CONFLICT` vuelve a contratos/tester con dictamen del auditor y una nueva evidencia RED.

### RED_SNAPSHOT

Todo TEST_PLAN con RED incluye una sección `RED_SNAPSHOT` con, cuando aplique: base commit; archivos de test; SHA-256 o commit del estado RED; comando; exit code; fallo esperado. El auditor compara el snapshot RED contra la versión usada para aprobar GREEN; una diferencia no justificada es `TEST_CONTRACT_CONFLICT`.

[TECH-001](../work-items/TECH-001-restaurar-gate-arquitectura/TEST_PLAN.md) precede esta política: posee evidencia RED documental, pero no snapshot criptográfico; no se altera retroactivamente.

Para documentación sin cambio funcional, registrar NO APLICA RED con motivo; no crear tests Java cosméticos.

## Behavior-driven test design

Cada test debe declarar o permitir justificar:

- requirement o behavior que protege;
- Given / precondición y estado;
- When / acción;
- Then / observable esperado;
- implementación equivocada que haría fallar.

No es obligatorio usar sintaxis BDD literal en nombres Java. Sí es obligatorio probar comportamiento observable y evitar tautologías. Para tests críticos, el tester debe preguntarse: **«¿Qué implementación equivocada haría fallar este test?»**. Esta disciplina no introduce todavía un framework de mutation testing ni PIT.

Todo `TEST_PLAN` funcional incluye como mínimo:

| ID | Requirement | Scenario | Input/State | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|
| | | | | | | | |

## Estándares por tipo de comportamiento

### Commands

Un command no queda probado solo por status exitoso o ausencia de excepción. Cuando aplique debe verificar resultado, side effect, estado persistido, ausencia de side effect ante error, rollback, publicación de evento solo después del éxito, idempotencia exigida por contrato y error semántico. `assertDoesNotThrow` nunca es evidencia única.

### Queries

Cubrir cuando aplique: 0 rows, 1 row, N rows, filtros, filtros opcionales null, mapping, nullability, duplicados, errores del provider y autorización/scope. Verificar ordering solo si es contractual. No inventar paginación.

### Endpoints protegidos

Matriz mínima cuando aplique:

1. sin credencial;
2. credencial malformada o inválida;
3. identidad válida con rol incorrecto;
4. rol válido con scope institucional/ownership incorrecto;
5. rol y scope válidos.

`401` es Authentication; `403` es Authorization. No cambiar el expected para acomodar la implementación.

### Persistencia y migración JDBC → JPA

- Un repository mock no certifica persistencia.
- Un `EntityManager` mock no certifica JPA.
- H2 no certifica comportamiento específico de SQL Server.
- Cuando mapping/query depende de SQL Server, se requiere integración real contra SQL Server.
- Toda migración JDBC → JPA exige `PARITY TEST`: mismo fixture, misma semántica, misma proyección, baseline JDBC, candidato JPA y comparación field-by-field; commands incluyen errores, side effects y rollback.

## Niveles

### Unit — Domain/Application

- Java/JUnit puro cuando la clase no necesita framework.
- Mockito solo para colaboradores reales del Use Case.
- Verificar happy path, negativos y ausencia de side effects.
- Para colecciones, usar varios elementos cuando se prueba no-N+1 o comportamiento por lote.

### Application / use-case

Requisitos funcionales, autorización y orden/ausencia de efectos mediante puertos; positivos, bordes y errores. No probar solo la implementación escogida ni mockear el SUT.

### Architecture

[Suite ArchUnit](../../src/test/java/co/edu/uco/asistenciasuco/architecture): dirección de capas, neutralidad, controllers, Composition Root y consistencia path/package. Un fallo de arquitectura es un gate fallido, no deuda que permita declararlo verde.

### Adapter unit/contract

- mapeos correctos;
- parámetros al SP/repository;
- traducción de errores;
- no filtrar excepciones técnicas al cliente.

### Integration — SQL Server actual y JPA futura

JPA todavía no está implementado. Existen IT SQL Server bajo persistence/sqlserver, incluidos AsistenciaRepositorySqlServerIT, GrupoRepositorySqlServerIT, SqlStoredProcedureContractIT y UsuarioPasswordHashSqlServerIT. Comprobar fixtures/ambiente y revisar assumptions/skips; una suite omitida no certifica integración.

### Requisitos de integración JPA futura

No mockear `EntityManager`/repository Spring Data como evidencia de que JPA funciona. Usar una
prueba de integración contra el motor/contrato SQL Server cuando la query, SP o mapping depende de
SQL Server.

Validar:

- nombres reales de tabla/vista/columna;
- tipos/UUID;
- proyecciones;
- paginación/filtros;
- transacción/rollback;
- SP real cuando corresponde.

### HTTP contract

Verificar request/response/status/error contra OpenAPI. Un controller test no reemplaza un contract
test de la especificación.

### E2E Golden Path

Mínimo:

```text
Auth -> POST lote -> commit SQL -> evento realtime -> GET consulta -> datos coherentes
```

Frontend E2E se agrega cuando el ambiente esté automatizable.

## Clasificación de tests

| Nivel | Definición |
|---|---|
| Unit | sin infraestructura real; SUT aislado con colaboradores mínimos |
| Component / Application | caso de uso o componente con colaboradores controlados |
| Architecture | reglas ArchUnit y dirección de dependencias |
| Contract | OpenAPI, schemas, compatibilidad y conformance |
| Integration | DB o provider real dentro de un ambiente controlado |
| Cloud Integration | Azure real u otro cloud real, con perfil/comando explícito y evidencia sanitizada |
| E2E | frontend/backend/DB/IdP reales según el alcance declarado |

Un test que consume Azure real no debe ejecutarse implícitamente como suite unitaria/normal. Se nombra `*CloudIntegrationIT`, corre con Failsafe solo mediante `-Pazure-integration` (solo lectura) y está excluido de `mvn verify` y de `-Pintegration` (SQL Server); no se aísla con skips/assumptions. Sin ambiente Azure falla o se registra `VALIDATION_BLOCKED_BY_ENVIRONMENT`, nunca PASS. Mocks no certifican Azure. `CloudIntegrationClassificationTest` protege la convención detectando wiring Azure + `.getSecret(` + endpoint operacional (variables `AZURE_*_ENDPOINT`); no detecta pruebas Cloud sin esas señales.

## Anti-patrones bloqueantes

- tests que modifican el SUT para poder probarlo sin justificar arquitectura;
- `assertTrue(true)`;
- `assertDoesNotThrow` como única evidencia de un command con side effects;
- `try/catch` que consume excepción;
- `lenient()` general para esconder stubs incorrectos;
- `any()` en todas las verificaciones cuando importan valores;
- `times(0)` sin verificar el efecto que realmente no debe ocurrir;
- test de repository con repository mockeado;
- desactivar un test fallido sin deuda/razón;
- cambiar expected status porque la implementación devolvió otro;
- sleeps arbitrarios para realtime.
- declarar integración Azure/provider real como PASS basándose únicamente en mocks (`EVIDENCE_LAUNDERING`).

## Regla de regresión

Todo bug reproducible debe entrar primero como test que falla. Luego se corrige producción. El test
queda como regresión permanente salvo que cambie el contrato aprobado.

## Ejecución y resultados

Seguir [VALIDATION_RUNBOOK](VALIDATION_RUNBOOK.md); un mock HTTP/DB no demuestra E2E. Sin ambiente registrar `VALIDATION_BLOCKED_BY_ENVIRONMENT` con causa. Si falla una prueba real, registrar FAIL y el test, sin reclasificarlo como fallo ambiental. Para E2E indicar si incluye frontend real o únicamente backend/DB.

---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# Validación — LB-003 Quality Gate Golden Path

## Dictamen

Los gates técnicos locales del Golden Path están verdes. LINE 91.52 % / BRANCH 81.17 %. El ruleset
`Protect develop` y sus required status checks quedaron `OBSERVED / ACTIVE` por revisión humana
autenticada de GitHub. LB-003 queda `BLOCKED_PENDING_REMOTE_CI` porque los runs remotos observados
corresponden al commit base (`0bfc02a`) y no al checkout actual con los cambios acumulados de
LB-002/LB-003; [MV-004](../../baseline/MANUAL_VALIDATION_LEDGER.md) y
[TD-023](../../baseline/TECHNICAL_DEBT.md#td-023) permanecen `PARTIAL`/abiertos hasta esa ejecución.

No se modificó código de producción, tests, DB, frontend, contratos, configuración runtime ni
workflows. No se ejecutó el perfil global de integración.

## Contexto reproducible

| Campo | Valor sanitizado |
|---|---|
| Fecha | 2026-09-29, `America/Bogota` |
| Revisión base (HEAD) | `0bfc02a58a8abc894841cc1e9b31c002a716554e`; el checkout contiene cambios locales no confirmados |
| Rama local | `jose-valencia/lb-002.2a-jpa-command-plan` |
| Java del wrapper | Oracle Java 25 |
| SQL Server | contenedor `sql_server_asistencias`; SQL Server 16.0; base `gestionasistenciadb`; esquema `dbo`; aislamiento `READ_COMMITTED` |
| Configuración sensible | cargada desde entorno local; valores no impresos ni versionados |

El checkout ya contenía cambios de LB-002/LB-003 ajenos a esta certificación; se preservaron.

## Gate técnico normal

Comando efectivo, una vez con acceso a dependencias:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

| Resultado | Evidencia actual |
|---|---|
| Maven | `BUILD SUCCESS`, exit code 0; 2026-09-29 02:24:32 -05:00; 2 min 44 s |
| Surefire | 1426 tests; 0 failures; 0 errors; 0 skips; 256 reportes XML |
| JAR | presente |
| JaCoCo LINE | 7630 covered / 707 missed / 8337 total = **91.52 %** (gate >=80 %) |
| JaCoCo BRANCH | 1746 covered / 405 missed / 2151 total = **81.17 %** (gate >=70 %) |
| ArchUnit | 19 clases / 85 tests; 0 failures; 0 errors; 0 skips |
| OpenAPI | 4 clases / 16 tests; 0 failures; 0 errors; 0 skips |

Un primer intento quedó antes del modelo Maven por restricción de red del sandbox; no ejecutó tests
y no se cuenta como resultado del gate. La repetición autorizada fuera del sandbox es la corrida
actual registrada arriba. No se repitió `clean verify` después: solo cambió documentación.

## SQL Server Golden Path targeted

Comando final:

```powershell
.\mvnw.cmd -Pintegration verify "-Dit.test=GoldenPathSqlStoredProcedureContractIT,AsistenciaRepositorySqlServerIT,AsistenciaQueryJpaParityIT,AsistenciaCommandJpaParityIT,AsistenciaCommandTransactionBoundaryIT,AsistenciaCommandConcurrencyIT,AsistenciaCommandRealtimeIT"
```

Antes del comando final se retiraron **solo del proceso Maven** los selectores externos de provider
que el archivo local `.env` traía definidos; las credenciales DB permanecieron en el entorno sin
imprimirse. Esto deja que cada test configure explícitamente el provider que certifica.

| Clase | Tests | Failures | Errors | Skips |
|---|---:|---:|---:|---:|
| `GoldenPathSqlStoredProcedureContractIT` | 16 | 0 | 0 | 0 |
| `AsistenciaRepositorySqlServerIT` | 6 | 0 | 0 | 0 |
| `AsistenciaQueryJpaParityIT` | 7 | 0 | 0 | 0 |
| `AsistenciaCommandJpaParityIT` | 21 | 0 | 0 | 0 |
| `AsistenciaCommandTransactionBoundaryIT` | 3 | 0 | 0 | 0 |
| `AsistenciaCommandConcurrencyIT` | 3 | 0 | 0 | 0 |
| `AsistenciaCommandRealtimeIT` | 3 | 0 | 0 | 0 |
| **Total** | **59** | **0** | **0** | **0** |

Resultado Maven final: `BUILD SUCCESS`, exit code 0; 2026-09-29 02:31:23 -05:00; 2 min 47 s.
La paridad query reportó `FIELD_MISMATCH_COUNT=0` en todos los escenarios y la paridad command
`MISMATCH=0` en todos los escenarios.

El primer intento targeted sí alcanzó Surefire, pero el `.env` imponía los dos providers JPA sobre
tests que verifican defaults/composición; produjo 8 failures y 5 errors ambientales antes de
Failsafe. No fue un defecto del producto ni un resultado SQL. Se corrigió únicamente el proceso de
ejecución, sin editar `.env`, código o tests, y la corrida final anterior es la evidencia válida.

## Seguridad del Golden Path

| Caso | Expected | Evidencia | Resultado |
|---|---|---|---|
| autenticación ausente/inválida | 401 | `SecurityConfigTest` 12/12; `RbacSecurityFilterChainTest` 33/33 | PASS |
| autenticado sin autorización | 403 | controller/RBAC dentro de los 33 tests anteriores | PASS |
| DOCENTE ajeno / ownership incorrecto | 403 / `FORBIDDEN`; 0 write; 0 realtime | use case normal + repository/parity/realtime SQL targeted | PASS |
| rol y ownership válidos | éxito | RBAC + targeted SQL + MV-006 runtime real | PASS |

## Errores seguros

`DbExceptionTranslatorTest` pasó 54/54 y `GlobalExceptionHandlerTest` 17/17. Junto con los tests de
catálogo existentes, esto conserva el mapping contractual de `SEC_001`, `SEC_002`, `ATT_001`,
`ATT_002`, `ATT_003`, `SES_003`, `SES_004` y `GEN_002` sin convertirlos accidentalmente en 500 ni
duplicar `DbFailureClassifier`. Resultado: `PASS_PREVIOUS_EVIDENCE` reconfirmado por el gate actual.

## Correlation y auditoría

- Correlation request/context/MDC/response: `CorrelationIdFilterTest` 11/11 PASS.
- Propagación hacia SP, logs y evento cuando aplica: tests de adapters y la suite targeted PASS; el
  evento realtime se observa solo después de persistencia exitosa.
- Auditoría: `AuditInterceptorTest` 5/5 y `AuditEventJdbcRepositoryTest` 5/5 PASS prueban wiring y
  comportamiento unitario. La suite targeted invoca application/persistence sin el request HTTP y
  no observa una escritura/consulta real de `dbo.AuditoriaEvento` por correlation.

Clasificación honesta: `AUDIT: NOT_OBSERVABLE`. No se convierte en PASS. [TD-010](../../baseline/TECHNICAL_DEBT.md#td-010)
sigue abierta por DML directo y revisión de release DB. No bloquea el DoD Golden Path de LB-003:
la DoD operacional exige logs/metrics/traces/correlation y TD-010 declara explícitamente que no
bloquea la línea base actual; ampliar a un contrato/auditoría DB real sería otro alcance.

## Write/read y realtime

| Observable | Evidencia | Resultado |
|---|---|---|
| POST lógico -> persistencia -> readback/estado | repository, parity, transaction y concurrency IT | PASS |
| realtime solo después de éxito | `AsistenciaCommandRealtimeIT` | PASS |
| cero realtime en fallo | `AsistenciaCommandRealtimeIT` | PASS |
| dos clientes SSE | MV-006 | PASS_REPORTED_EXTERNAL |
| reconexión/reconvergencia | MV-006 | PASS, aproximadamente 25 s |
| HTTP como fuente de verdad | MV-006 | PASS |

El E2E manual no se repitió porque no apareció contradicción con MV-006.

## Deuda y evidencia remota

### TD-008

`CLOSED — LB-003`. La suite final cubrió SQL Server real 16.0, fixtures reales, 59/59, cero skips,
write/read/realtime; MV-006 aporta runtime/E2E trazable. Se actualizó
[TECHNICAL_DEBT](../../baseline/TECHNICAL_DEBT.md#td-008).

### TD-023 / MV-004

Comprobación read-only en GitHub:

- [Backend CI #43](https://github.com/alejovh12/AsistenciasUCO/actions/runs/36377960197), commit
  base `0bfc02a`: SUCCESS; `Backend Quality Gate` SUCCESS; Java 25, Maven clean verify, JaCoCo,
  SonarQube Cloud analysis/Quality Gate, JAR e imagen.
- [Backend Security #43](https://github.com/alejovh12/AsistenciasUCO/actions/runs/36377960116), misma
  revisión: SUCCESS; CodeQL Java PASS; Dependency Review `skipped` por evento `push`.
- [Backend Security PR #42](https://github.com/alejovh12/AsistenciasUCO/actions/runs/36376621627):
  CodeQL Java PASS y Dependency Review PASS en el PR inmediato.
- Revisión humana autenticada de GitHub (sesión con permisos de administración del repositorio)
  confirmó el ruleset `Protect develop`, `status: active`, `target: refs/heads/develop`: PR
  requerido, `deletion` bloqueada, `non-fast-forward` bloqueado, sin bypass; required status checks
  configurados: `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review`, `SonarCloud
  Code Analysis`. `RULESET_DEVELOP: OBSERVED / ACTIVE`; `REQUIRED_CHECKS_CONFIGURATION: OBSERVED`.
- Los runs remotos anteriores corresponden a `0bfc02a` y no incluyen los cambios locales
  acumulados de LB-002/LB-003 sobre los que se ejecutaron los gates de esta fase; por tanto no
  certifican el checkout actual frente a los 4 required checks.

`REMOTE_CI_FOR_CURRENT_CHECKOUT: PENDING`. MV-004 se actualizó a `PARTIAL / PENDING` con la
configuración observada; TD-023 queda `OPEN / PARTIAL` y sigue bloqueando el cierre de LB-003 hasta
que esos 4 checks corran sobre el commit/PR que contenga los cambios actuales. No se difiere a
LB-006 sin decisión humana.

### TD-043

Sin cambios: `OPEN / DEFERRED / NON-GOLDEN`. No se ejecutó el perfil global; su estado heredado es
`NOT_GREEN_TD043` y no invalida los 59 tests targeted verdes.

## Limitaciones

- No hay run remoto de `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review` ni
  `SonarCloud Code Analysis` sobre el checkout actual (commit/PR con los cambios de LB-002/LB-003).
- La auditoría DB real del request no es observable en el harness targeted.
- MV-001 y MV-006 son `PASS_REPORTED_EXTERNAL`, sin artefactos locales reproducibles.
- No se presume que una prueba no ejecutada haya pasado.


---
status: active
type: evidence-snapshot
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Procedencia — DB_BASELINE_CONTRACT.md (snapshot externo)

Evidencia externa según [CONTRACT_ALIGNMENT_PROTOCOL](../../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md) §"Identidad de la evidencia externa". Este archivo y `DB_BASELINE_CONTRACT.sha256` son un **SNAPSHOT trazable**, no una copia mantenida ni una fuente de verdad permanente dentro del backend. La fuente de verdad sigue siendo el repositorio DB.

## Estado vigente (actualizado en LB-002.0, 2026-09-26)

```text
DB_CURRENT_BRANCH: develop
DB_CURRENT_TIP: 9b2b993423fa920ef6c5971098622c5ff65c8b4c
DB_FINAL_CODE_FREEZE: dcc69f19ffe3c246b78fa1996299ad97df229bbf
DB_GOLDEN_PATH_CONTRACT_SHA: 45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec
BASELINE_MERGED_TO_DEVELOP: YES
GOLDEN_PATH_CONTRACT_CHANGED: NO
DB_DEVELOP_FROZEN: YES
SOURCE_OF_EVIDENCE: local filesystem documentation
```

- `SOURCE_OF_EVIDENCE` es documentación versionada leída por ruta local del filesystem del repositorio DB (solo lectura; sin git, sin GitHub/web, sin scripts ni SQL, ningún archivo DB modificado). No se afirma que GitHub se haya consultado para la DB.
- `docs/contracts/DB_DEVELOP_FREEZE_ADDENDUM.md` y `docs/work-items/DB-GP-001C-develop-final-freeze/{CLOSURE,REPORT,VALIDATION}.md` (DB, lectura local) declaran `develop` FROZEN en `FINAL_DEVELOP_COMMIT dcc69f19...` y el contrato Golden Path `UNCHANGED`.
- **Límite de trazabilidad:** la documentación DB local registra `dcc69f19...` (último commit de esquema/tests/gates; los posteriores son solo documentación) y remite el tip a `git rev-parse origin/develop`. El tip `9b2b993423fa920ef6c5971098622c5ff65c8b4c` **lo declara la orden humana de LB-002.0**; no aparece literalmente en la documentación local y no se verificó con git (prohibido en esta fase). No contradice ninguna fuente.
- Los hashes que verifica esta fase son los del contrato: DB local == snapshot backend == `45e48c5a...9aec` (ver [LB-002.0-RECONCILIATION](../../../work-items/LB-002-jpa-incremental/LB-002.0-RECONCILIATION.md)).

### Extensiones DB post-freeze (conocidas, sin adoptar)

```text
NON_GOLDEN_PATH_DB_EXTENSIONS:
  - uv_auth_*  (12 vistas)
  - ufn_obtener_usuario_ejecutor_contexto
  - ufn_obtener_perfil_usuario_contexto
  - usp_establecer_contexto_usuario_ejecutor
  - usp_consultar_grupos_paginado
STATE: KNOWN / DOCUMENTED / NOT_CONSUMED_BY_LB-002.1
```

Decisión y límites en [LB-002.0-DECISION](../../../work-items/LB-002-jpa-incremental/LB-002.0-DECISION.md). Estas extensiones no forman parte del contrato consumido por el backend; adoptarlas exige un work item propio.

## Identidad de la evidencia (captura histórica del snapshot)

- Repositorio origen: `gestion-asistencia-db` (`C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`), solo lectura, ningún archivo modificado en ese repo.
- Rama de la captura original: `feat/db-golden-path-baseline-freeze`, commit observado `99190f07436bc64299b7d3a35c8e4486f49f9cd6` (2026-09-23). *Histórico: en ese momento no estaba fusionada a `main`/`develop`; **superado** por el estado vigente de arriba (baseline en `develop`, congelado).*
- Archivo capturado: `docs/contracts/DB_BASELINE_CONTRACT.md` (y su `docs/contracts/DB_BASELINE_CONTRACT.sha256` acompañante).
- Fecha de recaptura para LB-001B.4: 2026-09-23.
- SHA-256 de `DB_BASELINE_CONTRACT.md`: `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec` — coincide exactamente con el `.sha256` publicado en el repo DB y con el recalculado tras la copia. Precheck LB-001B.4: **VERIFIED**; recomprobado en LB-002.0 contra el repo DB local.
- El propio documento se autodeclara `GENERATED_FROM_COMMIT: UNCOMMITTED_WORKTREE` (línea final del archivo) — generado antes de ser comiteado en `99190f0`; no afecta la integridad del hash SHA-256 verificado arriba, pero se registra como limitación de trazabilidad interna del repo DB.
- `REMOTE_COMMIT_CONTAINING_BASELINE`: `6882ac09f91cc5131030c56656997a23e1a75680`.
  Comprobación local del 2026-09-25: el commit existe en
  `origin/feat/db-golden-path-baseline-freeze`, desciende de `99190f0`, contiene
  `docs/contracts/DB_BASELINE_CONTRACT.md` y publica el SHA-256
  `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec` en su archivo acompañante.
  Esta evidencia posterior no reescribe el origen histórico del documento:
  `GENERATED_FROM_COMMIT: UNCOMMITTED_WORKTREE` permanece vigente como procedencia inicial.

## Limitaciones

- La identidad contractual es el hash del snapshot, porque el archivo se generó desde un worktree no comiteado.
- Bajo autorización posterior del usuario se leyeron también documentos `docs/work-items/DB-GP-001B-final-closure/{PLAN,VALIDATION,CLOSURE}.md` y, en LB-002.0, la documentación de freeze DB-GP-001C; no se abrió ni modificó SQL DB.
- Pendientes operacionales DB fuera del backend: rotación de credencial histórica y GitHub secret `MSSQL_SA_PASSWORD` (`EXTERNAL_PRECONDITION`, sin valores en este repo).
- No se copiaron secretos, cadenas de conexión ni datos personales.

---
status: active
type: evidence-snapshot
scope: backend
owner: backend-team
last-reviewed: 2026-09-25
---

# Procedencia — DB_BASELINE_CONTRACT.md (snapshot externo)

Evidencia externa según [CONTRACT_ALIGNMENT_PROTOCOL](../../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md) §"Identidad de la evidencia externa". Este archivo y `DB_BASELINE_CONTRACT.sha256` son un **SNAPSHOT trazable**, no una copia mantenida ni una fuente de verdad permanente dentro del backend. La fuente de verdad sigue siendo el repositorio DB.

## Identidad de la evidencia

- Repositorio origen: `gestion-asistencia-db` (`C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`), solo lectura, ningún archivo modificado en ese repo.
- Rama: `feat/db-golden-path-baseline-freeze`.
- Commit observado: `99190f07436bc64299b7d3a35c8e4486f49f9cd6`.
- **Estado de fusión: NO fusionada a `main`/`develop` de `gestion-asistencia-db` al momento de esta captura.** `main` está en `25cf2e8`/`8cb27fb` según remoto/local; la rama de freeze no es ancestro de `main`. Importada bajo autorización explícita del usuario (2026-09-23) como snapshot de trabajo para LB-001B.3, no como confirmación de que `main` ya congeló el baseline.
- Archivo capturado: `docs/contracts/DB_BASELINE_CONTRACT.md` (y su `docs/contracts/DB_BASELINE_CONTRACT.sha256` acompañante).
- Fecha de recaptura para LB-001B.4: 2026-09-23.
- SHA-256 de `DB_BASELINE_CONTRACT.md`: `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec` — coincide exactamente con el `.sha256` publicado en el repo DB y con el recalculado tras la copia. Precheck LB-001B.4: **VERIFIED**.
- El propio documento se autodeclara `GENERATED_FROM_COMMIT: UNCOMMITTED_WORKTREE` (línea final del archivo) — generado antes de ser comiteado en `99190f0`; no afecta la integridad del hash SHA-256 verificado arriba, pero se registra como limitación de trazabilidad interna del repo DB.
- `REMOTE_COMMIT_CONTAINING_BASELINE`: `6882ac09f91cc5131030c56656997a23e1a75680`.
  Comprobación local del 2026-09-25: el commit existe en
  `origin/feat/db-golden-path-baseline-freeze`, desciende de `99190f0`, contiene
  `docs/contracts/DB_BASELINE_CONTRACT.md` y publica el SHA-256
  `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec` en su archivo acompañante.
  Esta evidencia posterior no reescribe el origen histórico del documento:
  `GENERATED_FROM_COMMIT: UNCOMMITTED_WORKTREE` permanece vigente como procedencia inicial.

## Limitaciones

- No hay confirmación en esta sesión de que un DBA/owner haya fusionado `feat/db-golden-path-baseline-freeze` a `main`. La identidad contractual de LB-001B.4 es el hash del snapshot, porque el archivo se generó desde un worktree no comiteado.
- Bajo autorización posterior del usuario se leyeron también documentos `docs/work-items/DB-GP-001B-final-closure/{PLAN,VALIDATION,CLOSURE}.md` para verificar el baseline oficial y su estado; no se abrió ni modificó SQL DB.
- No se copiaron secretos, cadenas de conexión ni datos personales.

---
date: 2026-10-10
status: REVIEW_RECOMMENDATIONS_NO_MERGES
---
# Matriz de ramas y PR — no eliminar hasta checks/merge

## Observado (GitHub 2026-10-10)
Backend develop bfc4fd3f, frontend develop f32db2c2, DB develop f2871a95. Backend PRs abiertos:
- PR #18 `maint-001-jpa-pagination-safety` → develop, Draft, mergeable=true; 10 commits, HEAD 7e2827d1.
- PR #19 `maint-002-parameter-cache-key` → develop, Draft, mergeable=true; 1 commit, HEAD 3985b0ac.
- PR #20 `maint-003-backend-utc-pagination-readiness` → develop, Draft, mergeable=true; HEAD 126a2d1d, **trabajo temprano RED, no toda UTC v2**.
- PR #21 `codex/fase1-backend-validation` → develop, Open no Draft, mergeable=true; HEAD 27e6ed0.
- No PR DB para D06/CC-003G-01, ni PR de frontend para v2.
- `maint-003h-provider-contract-impl` acumula 43 commits sobre develop y no contiene los PR #18 ni #19 como ancestros; también supera en 38 commits PR #21. **No asumir compatibilidad solo por `mergeable` de sus PR contra el develop actual.**

## Orden seguro propuesto
1. PR #18 (paginación): revisar diff y reevaluar cobertura/CI contra HEAD actual; puede promocionarse y mergearse si checks nuevos PASS y aprobación. **No merge automático desde esta revisión.**
2. PR #19 (cache key): evaluar independientemente; sólo merge después de CI sin regresión. No confundir con introducción futura de Redis.
3. PR #21: comparar contra MAINT-003H antes de merge: PR #21 es ancestro del trabajo MAINT-003H según GitHub; verificar que no duplicate features/tests. Si se integra PR #21 a develop, verificar PR principal UTC contra el nuevo develop.
4. PR #20: designar `SUPERSEDED_BY_MAINT_003H` en descripción/comentario de manera visible después de abrir PR completo; no mergear RED antiguo para reemplazar funcionalidad real. Cerrar PR sólo con acuerdo y evidencia de absorción de tests.
5. DB primero: PR Draft `feat/utc-d06-post-freeze`→develop, luego PR Draft `feat/cc-003g-01-public-user-plan-providers`→`feat/utc-d06-post-freeze` (stack). Verificar manifests y approvals; tras merge D06, rebase/retarget segundo a develop sin duplicar historia.
6. Backend: PR Draft `maint-003f-utc-v2-implementation`→develop, luego `maint-003h-provider-contract-impl`→`maint-003f-utc-v2-implementation`, y `maint-003i-utc-v1-read-timezone-fix`→`maint-003h-provider-contract-impl` **solo después** de ejecutar tests de timezone. Alternativa: PR único final contra develop tras reconciliar PR #18/#19/#21 y comprobar todos sus commits y tests; evitar PR con 100+ archivos sin checklist.
7. Frontend: nuevo PR desde develop cuando compile/tests; no habilitar feature v2 hasta DEV DB+backend.
8. Eliminar ramas **remotas** solo una vez que su PR esté fusionado o formalmente supersedido, que sus commits sean alcanzables desde develop (o archivados por tag SHA verificable) y los worktrees locales desvinculados. Respetar ramas de Sergio, master/main y worktrees ajenos.

## Criterios de promoción a merge (cada repo)
- Branch limpia + revisión + checks CI de HEAD exacto: Surefire/Failsafe/SQL, SonarCloud, CodeQL/Trivy según pipelines, cobertura y control de secretos.
- Ningún conflicto con develop, contracts v1 preservados, D06/CC004 SP/roles disponibles, staging/DEV gates reales.
- Aprobar owner DB antes de migración permanente; no mezclar PR provider con datos de test destructivos.
- Si se fusiona, revalidar FRONTEND wire y sincronización de todos los consumidores; no eliminar v1 hasta migración.

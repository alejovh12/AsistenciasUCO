# HANDOFF — SEC-BASELINE-003

## Dictamen

**`PARTIAL_WITH_BLOCKERS`** (no confundir con `LOCAL_CERTIFIED` ni con `READY_FOR_PRODUCTION`).

El recorrido JWT → PEP/PDP → RBAC → titularidad → SQL **aislado** → HTTP está certificado en local para SSE, fail-closed
(PEP caído, PDP caído, respuestas anómalas), comportamiento estable con el flag OFF, privilegio mínimo SQL y la matriz
por roles. No es `LOCAL_CERTIFIED` porque 5 de 296 casos no son PASS y hay casos obligatorios `NOT_RUN`:

| No PASS | Tipo | Bloquea |
|---|---|---|
| DL-03 IDOR del reporte Excel | defecto AS-IS de AsistenciasUCO (también en `develop`) | sí — cualquier promoción |
| RB-admin en `/api/v1/decano/coordinadores` | defecto AS-IS (fail-closed) | no (decisión de política) |
| REV-02 / REV-04 revocación | límite de diseño | sí — producción |
| PEP-03 | defecto upstream (mitigado) | sí — producción |
| MFA, frontend/PKCE | `NOT_RUN` | sí — producción |

## Respuestas

1. **¿FIX-001 quedó integrado correctamente en el laboratorio?** Sí. Merge `8b50343` (sin conflictos ni duplicados;
   0 archivos en común), 42/42 pruebas focalizadas, `verify` 1571/0 con ArchUnit y JaCoCo 90,75/80,68 %, sin
   WebFlux/JDBC/JPA nuevos, flag OFF por defecto.
2. **¿E2E-13b pasó de 500 a 403 con PEP/PDP real?** Sí: 403 `FORBIDDEN` `application/json`, con `X-Decision-Id` real
   del PDP (la denegación es de la titularidad en Application), sin abrir suscripción (gauge sin cambios), en la DB
   principal (solo lectura) y en la aislada. También 400 `VALIDATION_ERROR` y 400 `INVALID_REQUEST` en JSON.
3. **¿El E2E completo tiene 100 % de casos PASS?** No: 291/296. Las suites SSE/E2E de SEC-BASELINE-002 sí son 100 %
   (25/25 ×3, fallos PEP/PDP 16/16, estable 9/9); los 5 no-PASS están arriba.
4. **¿Existe DB E2E realmente aislada?** Sí: `asistencias-e2e-sqlserver` (contenedor, puerto loopback 14333, volumen,
   red y credenciales exclusivos), copia `COPY_ONLY` verificada con huella idéntica, logins runtime/readonly de mínimo
   privilegio, denegaciones probadas (22/22) y guard por PID que demuestra que el backend solo tiene sesiones allí. El
   principal no cambió (snapshots idénticos). Salvedad: la copia no es 100 % sintética (SB3-22).
5. **¿Todos los roles y recursos fueron probados?** Los 5 roles + sin rol + docente ajeno, sí, en 14 rutas RBAC y en
   los flujos de sesiones v1/v2, asistencias, reclamos, DELETE, descargas, CORS/CSRF, SSE y volumen 300/1000/5000. **No**
   todos los recursos: ≈37 de ~110 operaciones; MFA y frontend `NOT_RUN` ([TEST_PLAN](TEST_PLAN.md#not_run)).
6. **¿Qué problemas dependen de Seguridad-UCO?** Issuer/IdP definitivo, fuente y aprovisionamiento de `idUsuario`,
   plantillas de recursos PDP, PEP-03, `environment`, audiencias del scope upstream, secretos literales del release,
   MFA, modelo de roles/permisos y su sincronización, imágenes firmadas, DR de sus componentes
   ([TECHNICAL_DEBT](TECHNICAL_DEBT.md#dependen-de-seguridad-uco-proveedor-o-contrato-institucional)).
7. **¿Qué deudas podemos resolver nosotros?** FIX-002 (IDOR), estrategia y parte de la revocación, secret manager de la
   credencial PEP, PKCE con el frontend, retención SSE, contrato del 503, `/emit`, observabilidad del lado backend,
   fixture 100 % sintético, endpoints 501, seed E2E del admin, política admin/decano, ampliar la matriz
   ([TECHNICAL_DEBT](TECHNICAL_DEBT.md#resolubles-por-asistenciasuco-sin-depender-de-seguridad-uco)).
8. **¿Es viable sustituir Keycloak 8081?** Técnicamente sí a medio plazo, por convivencia gradual (doble issuer
   temporal → PKCE → ventana de observación → retiro) descrita en el [MIGRATION_ADR](MIGRATION_ADR.md). **Hoy no**: el
   laboratorio no es un IdP productivo y faltan SB3-01/02/05/06/11.
9. **¿Qué falta para la migración institucional definitiva?** Acuerdos M1 del ADR con Seguridad-UCO; release upstream
   con plantillas y sin PEP-03; FIX-002; aprobación contractual del 503; modo audit-only; frontend PKCE; MFA; secret
   manager; decisión de revocación; observabilidad PEP/PDP; DR; ventana de observación y aprobación explícita del retiro.
10. **Acciones manuales del usuario:** ver abajo.

## Acciones manuales pendientes del usuario

1. **Revisar y abrir el PR en borrador** (no hay `gh` en la máquina): título y cuerpo en [PR_DRAFT](#pr-en-borrador);
   URL: `https://github.com/alejovh12/AsistenciasUCO/compare/develop...jose-valencia/experiment-security-baseline-mvc?expand=1`.
   **No fusionar**: el merge a `develop` no está autorizado.
2. Decidir sobre la tarea separada **FIX-002** (IDOR del reporte Excel; sugerida como tarea independiente).
3. Decidir la política ADMINISTRADOR en `/api/v1/decano/**` (SB3-27).
4. Decidir si se sincroniza el repositorio de la organización por PR *fast-forward* (`develop`, rama experimental,
   MAINT-003L); nunca `--force`.
5. Llevar a Seguridad-UCO la lista de dependencias del punto 6 y la propuesta del ADR.
6. Opcional: borrar el `.bak` `sec_baseline_003_e2e_*.bak` del principal, los 3 PDF de prueba del MinIO local y, con
   aprobación, la instancia SQL E2E ([ROLLBACK](ROLLBACK.md)).

## Cómo reproducir

```powershell
$lab = 'infra\security-baseline-lab\scripts'
& "$lab\new-e2e-sqlserver.ps1"; & "$lab\prepare-e2e-database.ps1"      # solo la primera vez (-ReplaceE2eDatabase para rehacer)
& "$lab\test-e2e-sql-permissions.ps1"                                 # 22/22
& "$lab\verify-academic-identity.ps1" -Mode Verify; & "$lab\verify-role-identities.ps1"
python -I "$lab\role_matrix_e2e.py" provision
.\mvnw.cmd -B -ntp -DskipTests package
& "$lab\start-backend-pep-lab.ps1" -IdentityMode SharedLab -DatasourceEnv "$env:USERPROFILE\.asistencias-security-lab\e2e-sql-runtime.env"
# en otra consola:
python -X utf8 -I "$lab\lab_matrix.py" e2e                 # 25/25
python -X utf8 -I "$lab\role_matrix_e2e.py" run            # exit 2: 151 PASS + 2 KNOWN_DEFECT
python -X utf8 -I "$lab\role_matrix_e2e.py" revocation     # exit 2: 3 PASS + 2 KNOWN_DESIGN_LIMIT
```

PEP/PDP caído, stub de fallos y modo estable: [VALIDATION](VALIDATION.md) y la guía de SEC-BASELINE-002.

## Estado al cerrar la sesión

- Rama `jose-valencia/experiment-security-baseline-mvc` con commits de este work item, empujada a `origin`; sin merge;
  `develop` intacto; frontend intacto; organización sin cambios.
- Backend 8083/8084 detenidos; laboratorio arriba (PEP y PDP rearrancados y sanos); `asistencias-e2e-sqlserver`
  arriba; Keycloak 8081 arriba y sin cambios; SQL principal sin cambios de datos.
- CI remoto: el push de `c44ffdd` a la rama experimental no dispara workflows (0 check-runs, 0 runs; los workflows se
  ejecutan en PR y en `develop`). Se ejecutará al abrir el PR en borrador; no se presume verde.
- `CLOSURE.md` no se crea: el work item no cumple la DoD (dictamen `PARTIAL_WITH_BLOCKERS`).

## PR en borrador

**Título:** `experiment(security): SEC-BASELINE-003 local certification and migration readiness (DRAFT — do not merge)`

**Cuerpo (propuesto):**

> Rama experimental Security Baseline + MVC, para revisión. **No fusionar**: el merge a `develop` no está autorizado y
> el dictamen es `PARTIAL_WITH_BLOCKERS`.
>
> - Incorpora FIX-001 (`72312d7`) mediante merge; E2E-13b pasa de 500 a 403 JSON con PEP/PDP reales.
> - SQL Server E2E aislado con logins de mínimo privilegio y guard por PID; principal sin cambios.
> - Matriz por roles con el proveedor real: 291/296 PASS; 2 defectos AS-IS (IDOR del reporte Excel → FIX-002), 2
>   límites de revocación, 1 defecto upstream (PEP-03).
> - ADR de migración propuesto (no aprobado) y deuda priorizada.
> - Sin cambios en `src/**` fuera de FIX-001; flag PEP OFF por defecto; OpenAPI canónico intacto.
>
> Documentación: `docs/work-items/SEC-BASELINE-003-local-certification-migration-readiness/`.
>
> 🤖 Generated with [Claude Code](https://claude.com/claude-code)

## Propuesta de PR de integración a `develop` (solo si se aprueba más adelante)

No corresponde todavía (no hay `LOCAL_CERTIFIED`). Cuando se levanten los bloqueos, integrar en PR separados: (1) FIX-002;
(2) filtro PEP opt-in + overlay 503 con aprobación contractual (sin el laboratorio); (3) herramientas de laboratorio en
`infra/security-baseline-lab/` si se decide versionarlas en `develop`. Cada uno con CI verde y aprobación explícita.

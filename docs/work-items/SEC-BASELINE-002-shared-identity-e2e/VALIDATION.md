# VALIDATION — SEC-BASELINE-002 (2026-10-14, Windows 11, JDK 25, Python 3.14)

Base: rama `jose-valencia/experiment-security-baseline-mvc` en `a33d8d0` + cambios de este work item (sin
cambios en `src/**`). Evidencia completa en [evidence/](evidence/). Una prueba no ejecutada no es PASS.

## Fase 1 — Preservación

| Comprobación | Resultado |
|---|---|
| Worktree `.w/sec-mvc` en la rama experimental, limpio, al día con origin (`a33d8d0`) | PASS |
| `develop` = `4082736`, sin cambios; checkout estable con cambios ajenos (MAINT-003L) no tocados | PASS |
| Keycloak 8081, backend 8080 (no estaba en ejecución), SQL Server: sin modificaciones | PASS |
| Sin importar `test-integracion-seguridad`; sin credenciales impresas | PASS |

## Ejecuciones

| Hora UTC (fin) | Comando | Exit | Resultado |
|---|---|---:|---|
| 13:00 | `verify-academic-identity.ps1 -Mode Verify` | 0 | 3/3 UUID `verified=True` (existe, activo, 1 Docente, grupo único, titular) |
| 13:00–13:03 | `verify-academic-identity.ps1 -Mode Snapshot` (antes de arrancar 8083) | 0 | `userUpdates=0`; checksums base |
| 13:01 | `python -I provision_shared_identity_e2e.py` | 0 | 5/5 combinaciones de claims PASS |
| 13:03 | `start-backend-pep-lab.ps1 -IdentityMode SharedLab` (1.er intento) | 1 | **FAIL de arnés**: PowerShell 5.1 convirtió un `WARNING` de stderr de la JVM en error terminante y mató el proceso tras la 2.ª petición. Corregido en el script; no es un fallo del backend |
| 13:04 | idem (corregido) | — | arranque en 8,3 s; descubrimiento OIDC del issuer del laboratorio correcto |
| 13:06–13:09 | `lab_matrix.py e2e` (corridas 1-3) | 1 | ajustes del **arnés** documentados: SSE requiere `grupoId` (contrato) y el stream no envía cabeceras hasta el primer heartbeat (25 s) |
| 13:10 | `lab_matrix.py e2e-pep-down` (con `docker stop asistencias-security-lab-pep-1`) | 0 | 4/4 PASS; PEP reiniciado, readiness `UP` |
| 13:11 | `lab_matrix.py e2e-fault` (8083 → `pep_fault_stub.py` :18089) | 0 | 7/7 PASS (fail-closed 503); correlation IDs coinciden backend ↔ stub |
| 13:12 | `lab_matrix.py e2e` (corrida final, PEP real) | 1 | 23 filas: 22 PASS, **1 FAIL (E2E-13b, SSE 500)** |
| 13:13 | `lab_matrix.py pep` | 2 | 13 PASS + PEP-03 `KNOWN_UPSTREAM_FAIL` |
| 13:13 | `verify-academic-identity.ps1 -Mode Snapshot` (después) | 0 | `userUpdates=0`; checksums idénticos |
| — | `lab_matrix.py selftest` | 0 | 5 tests del arnés OK |
| — | parser PowerShell (`verify-academic-identity.ps1`, `start-backend-pep-lab.ps1`) y `ast.parse` de 5 scripts Python | 0 | OK |
| 13:17 | `.\mvnw.cmd -B -ntp verify` (sin cambios de contrato) | 0 | 1565 tests, 0 fallos; JaCoCo OK |
| 13:22 | `.\mvnw.cmd -B -ntp verify` con 503 añadido al YAML canónico | 1 | 2 fallos: `OpenApiGoldenPathValidationTest.canonicalSha256MatchesExactYamlBytes`, `OpenApiEstudiantesDirectoryConformanceTest.responsesAreExactlyTheObservedStatusesWithSharedErrorEnvelope`. **Cambio revertido**; el 503 pasa a un overlay experimental. Gates no modificados |
| 13:27 | `.\mvnw.cmd -B -ntp verify` (final) | 0 | **1565 tests, 0 fallos, 0 skipped; LINE 90,74 % / BRANCH 80,64 % (≥ 80/70)**; ArchUnit OK |

Regresiones de SEC-BASELINE-001 incluidas en `verify`: `SecurityBaselinePepWiringTest` (13: filtro una sola vez en
la cadena, sin registro servlet global, HTTP/1.1 sin `Upgrade: h2c`, ASYNC sin bypass, 503 fail-closed…),
`SecurityBaselinePepPropertiesTest` (9), `SecurityBaselinePepClientTest` (6), `SecurityBaselinePepFilterTest` (3),
`SecurityConfigTest` (12), `RbacSecurityFilterChainTest` (40).

## Resultados por fase

| Fase | Resultado |
|---|---|
| 2 — IdP común de laboratorio | PASS: `iss`, `aud` (pdp + api), roles, `idUsuario`, `sub` independiente, RS256/`kid`/300 s |
| 3 — Identidad académica | PASS: verificación SQL previa a la asociación; 4 identidades (positiva, rol incorrecto, sin PDP, sin `idUsuario`) |
| 4 — Configuración 8083 | PASS: 4 `KEYCLOAK_*` de proceso; discovery/JWKS del issuer tal cual; validaciones intactas |
| 5 — Recursos PDP | PASS: 4 recursos exactos nuevos (2 ajenos concedidos a propósito), rol nuevo, sin wildcards ni admin |
| 6 — E2E | 30/31 peticiones por backend PASS + 3 precondiciones PEP PASS; **FAIL E2E-13b** (preexistente) |
| 7 — Aislamiento SQL | PARCIAL: 0 escrituras demostradas, pero sin DB aislada ni credencial de solo lectura |
| 8 — Arnés | PASS: exit codes 0/1/2, ALLOW = 204 + decisión válida, PEP-03 `KNOWN_UPSTREAM_FAIL`, selftest |
| 9 — Validación/documentación | PASS (`verify` final verde, docs y contrato experimental) |

## SQL — aislamiento

```text
before 13:03:01Z userUpdates=0 sesiones=16 asistencias=6350 matriculas=6302 horarios=3 usuarios=5011
       sesionChecksum=1820493127 asistenciaChecksum=-1201892611 matriculaChecksum=1435088 horarioChecksum=1008836604
after  13:13:55Z (idéntico)
```

## Secretos

Escaneo de 19 valores secretos (`lab.env`, `.env` del backend, `infra/keycloak/.env`) contra el work item y
`infra/security-baseline-lab/`: **0 coincidencias**. Tokens nunca impresos (solo resúmenes de claims).

## No ejecutado

- E2E del frontend (fuera de alcance).
- PDP detenido (solo se detuvo el PEP).
- CI remoto de esta rama: no se presume; se registrará tras el push si aplica.

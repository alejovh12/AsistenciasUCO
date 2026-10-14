# TEST_PLAN — SEC-BASELINE-002

Derivado del encargo (13 casos obligatorios), del OpenAPI canónico y del contrato de validación JWT del backend.
Un mock no certifica al proveedor: E2E-11 usa un PEP de inyección de fallos y se declara así.

## Niveles

| Nivel | Comando | Qué prueba |
|---|---|---|
| Unit/slice/ArchUnit | `.\mvnw.cmd -B -ntp verify` | 1565 tests, gates JaCoCo LINE ≥ 80 % / BRANCH ≥ 70 %; regresiones de doble registro del filtro, bypass y HTTP/1.1 (`SecurityBaselinePepWiringTest`, `SecurityBaselinePepPropertiesTest`, `SecurityBaselinePepClientTest`) |
| Arnés | `python -I infra\security-baseline-lab\scripts\lab_matrix.py selftest` | ALLOW ⇒ 204 + `X-Decision-Id` válido; `KNOWN_UPSTREAM_FAIL` nunca es PASS; exit codes 0/1/2 |
| PEP/PDP real | `lab_matrix.py pep` | contrato del proveedor sin backend |
| E2E real | `lab_matrix.py e2e` | casos 1-9, 12, 13 a través de 8083 |
| E2E real, PEP detenido | `lab_matrix.py e2e-pep-down` | caso 10 |
| E2E backend real + PEP de fallos | `lab_matrix.py e2e-fault` | caso 11 |
| Aislamiento SQL | `verify-academic-identity.ps1 -Mode Snapshot` antes/después | 0 escrituras |

## Casos

| # | ID | Identidad / token | Ruta | Esperado | Oráculo de capa |
|---|---|---|---|---|---|
| 1 | E2E-01a/b | `lab-e2e-docente-fixpag300` | horarios; estudiantes FIX-PAG-300 | 200; registros = SQL | `X-Decision-Id` + datos |
| 2 | E2E-02a/b (+pre) | idem | v2 sesiones / estudiantes de FIX-PAG-1000 (concedidas en PDP) | PEP directo 204; backend 403 | `X-Decision-Id` + log de Application |
| 3 | E2E-03 (+pre) | `lab-e2e-docente-sin-pdp` | horarios | 403 | sin `X-Decision-Id`, sin log de Application |
| 4 | E2E-04a/b | `lab-e2e-rol-incorrecto` (ESTUDIANTE, titular de FIX-PAG-1000) | horarios; v2 de su propio grupo | 403 | `X-Decision-Id`, sin log de Application |
| 5 | E2E-05 | `lab-e2e-sin-idusuario` | horarios | 401 | única diferencia: sin `idUsuario` |
| 6 | E2E-06 | cliente `wrong-aud` | horarios | 401 | única diferencia: `aud` |
| 7 | E2E-07 | `docente.paginacion300` del 8081 | horarios | 401 | issuer no configurado en 8083 |
| 8 | E2E-08 | positivo con firma alterada | horarios | 401 | firma |
| 9 | E2E-09 | ninguno | horarios | 401 | entry point |
| 10 | E2E-10a-d | positivo | horarios, SSE, sin token, actuator | 503 / 503 / 401 / 200 | `ACCESS_DECISION_UNAVAILABLE` |
| 11 | E2E-11a-g | positivo | 7 rutas, una por anomalía | 503 | fail-closed, sin datos, sin `X-Decision-Id` |
| 12 | E2E-12 | positivo | v2 sesiones FIX-PAG-300 | 200; ids/procedencia/instantes = SQL | datos |
| 13 | E2E-13a-e | positivo / sin PDP / ninguno | `stream?grupoId=` propio, ajeno, sin PDP, sin token, sin `grupoId` | 200 SSE / 403 / 403 / 401 / 400 | contrato OpenAPI |

Informativos: E2E-14 (literalidad de rutas exactas), E2E-15 (actuator público).

## Reglas

- Solo GET. Ningún endpoint de creación/modificación académica.
- Tokens en memoria; nunca impresos ni escritos.
- Un FAIL no se reclasifica: E2E-13b queda FAIL (defecto preexistente) y PEP-03 `KNOWN_UPSTREAM_FAIL`.

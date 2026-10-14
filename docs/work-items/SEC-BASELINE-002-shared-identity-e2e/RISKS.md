# Riesgos y controles — SEC-BASELINE-002

| Riesgo | Control | Estado |
|---|---|---|
| Tomar la identidad de laboratorio como integración institucional | Documentado como experimental en todos los artefactos; usuarios `lab-e2e-*` solo en el realm del laboratorio; decisiones pendientes en [IDENTITY_DECISION](IDENTITY_DECISION.md#decisiones-que-requieren-aprobación-institucional) | Controlado |
| Asociar un `idUsuario` sin procedencia | `provision_shared_identity_e2e.py` exige la evidencia SQL (< 24 h, `verified=true`); atributo editable solo por admin; patrón UUID | Controlado (asociación manual: solo laboratorio) |
| Confundir `sub` con el actor académico | `sub` = id Keycloak; `idUsuario` aparte; verificado `sub ≠ idUsuario` | Controlado |
| Relajar validaciones JWT | Solo cambian los 4 `KEYCLOAK_*` del proceso 8083; E2E-05…08 = 401 | Controlado |
| Resolución de `keycloak.localhost` | El JDK 25 de Windows la resuelve a loopback (127.0.0.1 primero); Keycloak solo publica en 127.0.0.1. En otro SO/JDK podría fallar el arranque (fail-fast, nunca un bypass) | Aceptado para laboratorio |
| `InstitutionalIssuerValidator` acepta además el issuer con `localhost` → `127.0.0.1` reemplazado (`http://keycloak.127.0.0.1:19090/…`) | Comportamiento preexistente; la firma sigue ligada al JWKS del issuer configurado, por lo que no permite tokens de otro emisor | Observación (sin cambio) |
| **Sin DB E2E aislada ni principal de solo lectura**; el backend usa `sa` | Solo GET; snapshot `sys.dm_db_index_usage_stats.user_updates` y `CHECKSUM_AGG` de Sesion/Asistencia/EstudianteGrupo/Horario antes y después: 0 escrituras, checksums idénticos. No se crean logins ni bases (prohibido modificar SQL Server) | Aislamiento **operacional**, no por privilegios — riesgo residual registrado |
| Datos del fixture cambiados por terceros | El fixture FIX-PAG-300 tiene 6 sesiones (MAINT-003L registró 4 + 1): el E2E compara con la lectura SQL del mismo día, no con valores históricos | Controlado |
| Rutas exactas en el PDP no escalan a grupos dinámicos | Documentado; sin wildcards; E2E-14 demuestra fail-closed ante variantes | Abierto upstream (plantillas `{x}` → 500) |
| PDP no ve la query string (SSE `grupoId`) | La titularidad del gateway SSE es la barrera; E2E-13b la demuestra | Abierto (diseño) |
| **SSE con `grupoId` ajeno responde 500 en vez de 403** | Acceso denegado igualmente; defecto preexistente de `GlobalExceptionHandler` con `Accept: text/event-stream`; tarea separada | Abierto (FAIL documentado) |
| PEP-03 upstream (200 sin decisión sin Bearer) | Cliente: sin Bearer no se consulta; ALLOW solo 204 + `X-Decision-Id`; E2E-11a | Mitigado localmente; abierto upstream |
| ROPC (Direct Access Grants) en `asistencias-lab-e2e` | Solo laboratorio, cliente confidencial, `fullScopeAllowed=false` | Prohibido fuera del laboratorio |
| Secretos en evidencia | Valores aleatorios en `lab.env` (ACL usuario); escaneo de 19 valores contra work item y scripts: 0 coincidencias | Controlado |
| Stub de fallos tomado como proveedor | Solo loopback, solo caso 11, declarado en matriz y runbook | Controlado |
| Muerte del backend por stderr en PowerShell 5.1 | `start-backend-pep-lab.ps1` deja de tratar stderr nativo como terminante y devuelve el exit code de la JVM | Corregido |
| `ACCESS_DECISION_UNAVAILABLE` sin aprobación contractual | Solo en la rama experimental, marcado `x-experimental` | Abierto (requiere decisión antes de merge) |

# Guía de prueba manual — SEC-BASELINE-002

Reproduce el E2E desde cero en Windows (PowerShell 5.1), en la raíz del worktree experimental. Requisitos: Docker
Desktop, JDK 25 en `JAVA_HOME`, Python 3 (`python -I`), SQL Server local con el fixture FIX-PAG cargado, el
laboratorio instalado según [SEC-BASELINE-001 LAB_RUNBOOK](../SEC-BASELINE-001-mvc-integration/LAB_RUNBOOK.md) y,
para E2E-07, la cuenta institucional `docente.paginacion300` en `infra\keycloak\.env` del checkout estable.
Nunca copie tokens ni contraseñas en tickets o chats.

```powershell
$lab = 'infra\security-baseline-lab'
$envFile = Join-Path $env:USERPROFILE '.asistencias-security-lab\lab.env'
$dc = @('--context','desktop-linux','compose','--project-name','asistencias-security-lab','--env-file',$envFile,'-f',"$lab\compose.lab.yaml")
docker @dc up -d --wait                                   # laboratorio arriba (sin -v jamás)
```

## 1. Verificación académica (solo lectura)

```powershell
& "$lab\scripts\verify-academic-identity.ps1" -Mode Verify      # exit 0 y verified=True para los 3 UUID
& "$lab\scripts\verify-academic-identity.ps1" -Mode Snapshot > "$env:TEMP\snap-before.json"
```

Si algún UUID sale `verified=False`, **deténgase**: no asocie la identidad.

## 2. Identidad de laboratorio (Keycloak 19090 + PDP)

```powershell
python -I "$lab\scripts\provision_shared_identity_e2e.py"         # todas las líneas claims: verdict PASS
```

Comprobación visual opcional: consola `http://keycloak.localhost:19090/admin` (credenciales de `lab.env`), realm
`security-baseline` → Users → `lab-e2e-docente-fixpag300` → Attributes (`idUsuario`) y Role mapping
(`asistencias-api DOCENTE`). Clients → `asistencias-lab-e2e` → Client scopes → Evaluate muestra el token generado
sin tener que copiarlo.

## 3. Backend 8083 con identidad compartida

```powershell
.\mvnw.cmd -B -ntp -DskipTests package
& "$lab\scripts\start-backend-pep-lab.ps1" -IdentityMode SharedLab    # deja la consola abierta
```

En otra consola: `Invoke-WebRequest http://127.0.0.1:8083/actuator/health` → 200.

## 4. Matrices

```powershell
python -I "$lab\scripts\lab_matrix.py" selftest     # exit 0
python -I "$lab\scripts\lab_matrix.py" pep          # exit 2: solo PEP-03 KNOWN_UPSTREAM_FAIL
python -I "$lab\scripts\lab_matrix.py" e2e          # exit 1 mientras exista el defecto E2E-13b (SSE 500)
```

E2E-13a tarda ~25 s (el stream no envía cabeceras hasta el primer heartbeat).

## 5. PEP inaccesible (caso 10)

```powershell
docker stop asistencias-security-lab-pep-1
python -I "$lab\scripts\lab_matrix.py" e2e-pep-down     # exit 0
docker start asistencias-security-lab-pep-1
Invoke-RestMethod http://127.0.0.1:18081/actuator/health/readiness   # status UP antes de seguir
```

## 6. Respuesta PEP sin decisión válida (caso 11)

Detenga el backend (Ctrl+C) y:

```powershell
Start-Process python -ArgumentList '-I', "$lab\scripts\pep_fault_stub.py", '18089'
& "$lab\scripts\start-backend-pep-lab.ps1" -IdentityMode SharedLab -PepUrl 'http://127.0.0.1:18089'
python -I "$lab\scripts\lab_matrix.py" e2e-fault        # exit 0, siete 503
```

Después detenga el backend y el stub, y vuelva al paso 3 si necesita el PEP real.

## 7. Aislamiento SQL

```powershell
& "$lab\scripts\verify-academic-identity.ps1" -Mode Snapshot      # userUpdates y checksums iguales al paso 1
```

## Prueba manual con navegador (opcional)

No hay frontend integrado. Para observar el 200 sin herramientas de terceros, use la matriz; para inspeccionar
claims, use *Client scopes → Evaluate → Generated access token* en la consola del 19090.

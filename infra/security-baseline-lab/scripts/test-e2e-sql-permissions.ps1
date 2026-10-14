<#
.SYNOPSIS
    SEC-BASELINE-003 - demuestra que los logins E2E tienen solo los permisos requeridos.

.DESCRIPTION
    SOLO contra el SQL E2E aislado (127.0.0.1:14333) con la marca de aislamiento; nunca contra el
    principal (de él solo se lee sys.server_principals para demostrar que los logins E2E no existen).
    Cada intento de escritura va dentro de BEGIN TRAN ... ROLLBACK y con WHERE 1=0 / valores que no
    podrían persistir: aun si un permiso estuviera mal concedido, no cambia ninguna fila. Se espera el
    error 229 (permiso denegado). Salida JSON por caso, sin secretos. Exit 0 si todo PASS.
#>
[CmdletBinding()]
param(
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env'),
    [string]$EvidenceFile
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath
$results = New-Object System.Collections.ArrayList

function Test-Case([System.Data.SqlClient.SqlConnection]$c, [string]$case, [string]$sql, [string]$expect) {
    $tx = $c.BeginTransaction()
    $outcome = 'ALLOWED'; $number = $null
    try {
        $cmd = $c.CreateCommand(); $cmd.Transaction = $tx; $cmd.CommandText = $sql; $cmd.CommandTimeout = 60
        $r = $cmd.ExecuteReader(); while ($r.Read()) { }; $r.Close()
    } catch [System.Data.SqlClient.SqlException] {
        $number = $_.Exception.Number
        $outcome = if ($number -in 229, 230, 262) { 'DENIED' } else { "ERROR" }
    } finally {
        try { $tx.Rollback() } catch { }
    }
    $verdict = if ($outcome -eq $expect) { 'PASS' } else { 'FAIL' }
    $row = [ordered]@{ case = $case; login = $c.ConnectionString -replace '.*User ID=([^;]+).*', '$1'; expected = $expect
        outcome = $outcome; sqlError = $number; verdict = $verdict }
    [void]$results.Add($row)
    $row | ConvertTo-Json -Compress
}

$checks = @{
    readonly = @(
        @('RO-01 SELECT vista uv_grupo', 'SELECT TOP 1 id FROM dbo.uv_grupo', 'ALLOWED'),
        @('RO-02 SELECT tabla base dbo.Usuario', 'SELECT TOP 1 id FROM dbo.Usuario', 'DENIED'),
        @('RO-03 INSERT dbo.AuditoriaEvento', 'INSERT INTO dbo.AuditoriaEvento DEFAULT VALUES', 'DENIED'),
        @('RO-04 UPDATE dbo.Usuario (WHERE 1=0)', 'UPDATE dbo.Usuario SET estado = estado WHERE 1 = 0', 'DENIED'),
        @('RO-05 DELETE dbo.Asistencia (WHERE 1=0)', 'DELETE FROM dbo.Asistencia WHERE 1 = 0', 'DENIED'),
        @('RO-06 EXEC usp_crear_sesion', 'EXEC dbo.usp_crear_sesion', 'DENIED'),
        @('RO-07 EXEC usp_registrar_asistencias_sesion', 'EXEC dbo.usp_registrar_asistencias_sesion', 'DENIED'),
        @('RO-08 CREATE TABLE', 'CREATE TABLE dbo.sec003_probe (id int)', 'DENIED'),
        @('RO-09 EXEC usp_crear_sesion_v2 (solo VIEW DEFINITION)', 'EXEC dbo.usp_crear_sesion_v2', 'DENIED'),
        @('RO-10 OBJECT_ID de los SP v2 visible (arranque del backend)', "SELECT CASE WHEN OBJECT_ID('dbo.usp_crear_sesion_v2','P') IS NOT NULL AND OBJECT_ID('dbo.usp_actualizar_sesion_v2','P') IS NOT NULL THEN 1 ELSE 1/0 END v", 'ALLOWED')
    )
    runtime = @(
        @('RT-01 SELECT vista uv_sesion_v2', 'SELECT TOP 1 * FROM dbo.uv_sesion_v2', 'ALLOWED'),
        @('RT-02 UPDATE dbo.Usuario (DENY explícito, WHERE 1=0)', 'UPDATE dbo.Usuario SET estado = estado WHERE 1 = 0', 'DENIED'),
        @('RT-03 INSERT dbo.Sesion (DENY explícito)', 'INSERT INTO dbo.Sesion DEFAULT VALUES', 'DENIED'),
        @('RT-04 DELETE dbo.Sesion (DENY, WHERE 1=0)', 'DELETE FROM dbo.Sesion WHERE 1 = 0', 'DENIED'),
        @('RT-05 UPDATE dbo.Asistencia (sin GRANT, WHERE 1=0)', 'UPDATE dbo.Asistencia SET id = id WHERE 1 = 0', 'DENIED'),
        @('RT-06 EXEC usp_crear_sesion_interno (DENY)', 'EXEC dbo.usp_crear_sesion_interno', 'DENIED'),
        @('RT-07 SELECT tabla base dbo.Grupo', 'SELECT TOP 1 id FROM dbo.Grupo', 'DENIED'),
        @('RT-08 CREATE TABLE', 'CREATE TABLE dbo.sec003_probe (id int)', 'DENIED')
    )
}
$logins = @{ readonly = @('asistencias_e2e_readonly', 'E2E_SQL_READONLY_PASSWORD'); runtime = @('asistencias_e2e_runtime', 'E2E_SQL_RUNTIME_PASSWORD') }
foreach ($kind in 'readonly', 'runtime') {
    $c = New-E2eConnection -Target E2E -Database $s.Database -User $logins[$kind][0] -Password $secrets[$logins[$kind][1]] -AppName "SEC-BASELINE-003-perm-$kind"
    try {
        Assert-E2eTarget $c
        foreach ($check in $checks[$kind]) { Test-Case $c $check[0] $check[1] $check[2] }
    } finally { $c.Close() }
}

# El principal no conoce los logins E2E (lectura de catálogo).
$backend = Import-KeyValueFile $BackendEnv
$p = New-E2eConnection -Target Principal -User $backend['SPRING_DATASOURCE_USERNAME'] -Password $backend['SPRING_DATASOURCE_PASSWORD'] -AppName 'SEC-BASELINE-003-readonly-check'
try {
    $n = (Invoke-E2eSql $p "SELECT COUNT(*) n FROM sys.server_principals WHERE name LIKE 'asistencias_e2e_%'").Rows[0].n
    $row = [ordered]@{ case = 'ISO-01 logins E2E inexistentes en el principal'; expected = 0; outcome = $n; verdict = $(if ($n -eq 0) { 'PASS' } else { 'FAIL' }) }
    [void]$results.Add($row); $row | ConvertTo-Json -Compress
} finally { $p.Close() }

# Datos sintéticos del fixture en la copia (lectura con el login de solo lectura).
$c = New-E2eConnection -Target E2E -Database $s.Database -User $logins.readonly[0] -Password $secrets[$logins.readonly[1]] -AppName 'SEC-BASELINE-003-fixture'
try {
    Assert-E2eTarget $c
    $t = Invoke-E2eSql $c "SELECT g.nombre, COUNT(eg.id) matriculas FROM dbo.uv_grupo g LEFT JOIN dbo.uv_estudiante_grupo eg ON eg.idGrupo = g.id WHERE g.nombre LIKE 'FIX-PAG-%' GROUP BY g.nombre ORDER BY g.nombre"
    foreach ($r in $t) {
        $expected = [int](($r.nombre -split ' ')[0] -replace 'FIX-PAG-', '')
        $row = [ordered]@{ case = "FIX-01 $($r.nombre)"; expected = $expected; outcome = [int]$r.matriculas; verdict = $(if ([int]$r.matriculas -eq $expected) { 'PASS' } else { 'FAIL' }) }
        [void]$results.Add($row); $row | ConvertTo-Json -Compress
    }
} finally { $c.Close() }

$fail = @($results | Where-Object { $_.verdict -ne 'PASS' }).Count
"TOTAL=$($results.Count) PASS=$($results.Count - $fail) FAIL=$fail"
if ($EvidenceFile) { $results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $EvidenceFile -Encoding utf8 }
exit $(if ($fail -eq 0) { 0 } else { 1 })

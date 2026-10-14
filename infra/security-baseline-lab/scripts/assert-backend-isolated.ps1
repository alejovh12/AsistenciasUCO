<#
.SYNOPSIS
    SEC-BASELINE-003 - guard previo a cualquier escritura E2E: el proceso que escucha en -Port solo tiene
    sesiones en el SQL E2E aislado, con un login de mínimo privilegio, y ninguna en el principal.

.DESCRIPTION
    Correlaciona el PID del backend (Get-NetTCPConnection) con sys.dm_exec_sessions.host_process_id (el
    driver JDBC lo envía). En el principal solo lee catálogos (SELECT). Exit 0 = aislado; 1 = no aislado.
    Imprime una línea JSON sin secretos.
#>
[CmdletBinding()]
param(
    [int]$Port = 8083,
    [string]$BackendEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\.env')
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath
$backend = Import-KeyValueFile $BackendEnv
$listen = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listen) { '{"isolated":false,"reason":"no hay proceso en el puerto"}'; exit 1 }
$processId = [int]$listen.OwningProcess
$sql = "SELECT login_name, DB_NAME(database_id) db, COUNT(*) n FROM sys.dm_exec_sessions WHERE host_process_id=@pid GROUP BY login_name, database_id"

$e = New-E2eConnection -Target E2E -User sa -Password $secrets['MSSQL_SA_PASSWORD'] -AppName 'SEC-BASELINE-003-isolation-guard'
try { $e2eRows = @((Invoke-E2eSql $e $sql @{ '@pid' = $processId }).Rows) } finally { $e.Close() }
$p = New-E2eConnection -Target Principal -User $backend['SPRING_DATASOURCE_USERNAME'] -Password $backend['SPRING_DATASOURCE_PASSWORD'] -AppName 'SEC-BASELINE-003-isolation-guard'
try { $principalRows = @((Invoke-E2eSql $p $sql @{ '@pid' = $processId }).Rows) } finally { $p.Close() }

$allowed = 'asistencias_e2e_runtime', 'asistencias_e2e_readonly'
$foreign = @($e2eRows | Where-Object { $_.login_name -notin $allowed -or $_.db -ne $s.Database })
$ok = ($e2eRows.Count -gt 0) -and ($foreign.Count -eq 0) -and ($principalRows.Count -eq 0)
[ordered]@{
    isolated = $ok; pid = $processId; port = $Port
    e2e = @($e2eRows | ForEach-Object { "$($_.login_name)@$($_.db) x$($_.n)" })
    principalSessionsOfPid = $principalRows.Count
    checkedAt = (Get-Date).ToUniversalTime().ToString('o')
} | ConvertTo-Json -Compress
exit $(if ($ok) { 0 } else { 1 })

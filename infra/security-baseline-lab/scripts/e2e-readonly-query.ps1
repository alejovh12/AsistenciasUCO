<#
.SYNOPSIS
    SEC-BASELINE-003 - consulta SELECT contra la DB E2E AISLADA con el login asistencias_e2e_readonly.
    La usa role_matrix_e2e.py para localizar identificadores que la API no expone (p. ej. el id de una
    solicitud de revisión: GET /api/v1/docente/reclamos responde 501). Salida: JSON (lista de filas).
    El permiso real lo impone SQL Server (rol rol_asistencias_e2e_lectura: solo SELECT sobre vistas).
#>
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Query, [string]$ParamsJson = '{}')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Query -notmatch '^\s*SELECT\b') { throw 'Solo SELECT' }
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath
$params = @{}
($ParamsJson | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $params['@' + $_.Name] = $_.Value }
$c = New-E2eConnection -Target E2E -Database $s.Database -User asistencias_e2e_readonly `
    -Password $secrets['E2E_SQL_READONLY_PASSWORD'] -AppName 'SEC-BASELINE-003-readonly-query'
try {
    Assert-E2eTarget $c
    $t = Invoke-E2eSql $c $Query $params
    $rows = @(foreach ($r in $t.Rows) {
        $o = [ordered]@{}
        foreach ($col in $t.Columns) { $v = $r[$col.ColumnName]; $o[$col.ColumnName] = $(if ($v -is [DBNull]) { $null } else { "$v" }) }
        $o
    })
    ConvertTo-Json -InputObject $rows -Compress -Depth 3
} finally { $c.Close() }

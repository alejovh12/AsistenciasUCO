Set-StrictMode -Version Latest

# SEC-BASELINE-003 - utilidades del SQL Server E2E AISLADO. Nunca imprimen valores secretos.
# El servidor principal (sql_server_asistencias, 127.0.0.1:1433) solo se toca en modo lectura,
# salvo el BACKUP ... WITH COPY_ONLY de prepare-e2e-database.ps1 (sin efecto en la cadena de log).

$script:E2eContainer = 'asistencias-e2e-sqlserver'
$script:E2eVolume = 'asistencias-e2e-sqlserver-data'
$script:E2eNetwork = 'asistencias-e2e-sql-net'
$script:E2eHost = '127.0.0.1,14333'
$script:E2eDatabase = 'gestionasistenciadb_e2e'
$script:E2eMarker = 'sec_baseline_003_isolated_e2e'
$script:PrincipalHost = '127.0.0.1,1433'
$script:PrincipalContainer = 'sql_server_asistencias'

function Get-E2eSqlSettings {
    return [pscustomobject]@{
        Container = $script:E2eContainer; Volume = $script:E2eVolume; Network = $script:E2eNetwork
        Host = $script:E2eHost; Database = $script:E2eDatabase; Marker = $script:E2eMarker
        PrincipalHost = $script:PrincipalHost; PrincipalContainer = $script:PrincipalContainer
        SecretsPath = (Join-Path $env:USERPROFILE '.asistencias-security-lab\e2e-sql.env')
    }
}

function Import-KeyValueFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw "No existe $Path" }
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $t = $line.Trim()
        if ($t.Length -eq 0 -or $t.StartsWith('#')) { continue }
        $i = $t.IndexOf('='); if ($i -lt 1) { continue }
        $map[$t.Substring(0, $i).Trim()] = $t.Substring($i + 1).Trim()
    }
    return $map
}

function New-E2eConnection {
    <# -Target E2E | Principal. Principal usa el login del .env del backend (solo para lectura/backup). #>
    param([ValidateSet('E2E', 'Principal')][string]$Target, [string]$Database = 'master',
          [string]$User, [string]$Password, [string]$AppName = 'SEC-BASELINE-003')
    $b = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
    $b['Data Source'] = if ($Target -eq 'E2E') { $script:E2eHost } else { $script:PrincipalHost }
    $b['Initial Catalog'] = $Database
    $b['User ID'] = $User; $b['Password'] = $Password
    $b['Encrypt'] = $true; $b['TrustServerCertificate'] = $true
    $b['Application Name'] = $AppName
    $b['Connect Timeout'] = 15
    $c = New-Object System.Data.SqlClient.SqlConnection $b.ConnectionString
    $c.Open()
    return $c
}

function Invoke-E2eSql {
    param([System.Data.SqlClient.SqlConnection]$Connection, [string]$Sql, [hashtable]$Parameters = @{},
          [int]$Timeout = 600, [switch]$NonQuery)
    $cmd = $Connection.CreateCommand(); $cmd.CommandText = $Sql; $cmd.CommandTimeout = $Timeout
    foreach ($k in $Parameters.Keys) { [void]$cmd.Parameters.AddWithValue($k, $Parameters[$k]) }
    if ($NonQuery) { return $cmd.ExecuteNonQuery() }
    $r = $cmd.ExecuteReader(); $t = New-Object System.Data.DataTable; $t.Load($r)
    return ,$t
}

function Assert-E2eTarget {
    <# Rechaza cualquier conexión que no sea el contenedor E2E con la marca de aislamiento. #>
    param([System.Data.SqlClient.SqlConnection]$Connection)
    if ($Connection.DataSource -ne $script:E2eHost) { throw "Conexión no aislada: $($Connection.DataSource)" }
    $t = Invoke-E2eSql $Connection "SELECT CAST(value AS nvarchar(200)) v FROM sys.extended_properties WHERE class=0 AND name=@n" @{ '@n' = $script:E2eMarker }
    if ($t.Rows.Count -ne 1) { throw "La base $($Connection.Database) no tiene la marca $($script:E2eMarker)" }
}

Export-ModuleMember -Function Get-E2eSqlSettings, Import-KeyValueFile, New-E2eConnection, Invoke-E2eSql, Assert-E2eTarget

# E2E-03 - HU102/HU103/HU106/HU144: KPIs, comparativa de programas y aforo de grupos de la facultad del decano.
# Prerrequisitos: backend de la rama feature/HU102-kpis-decano-datos-reales en -ApiUrl, Keycloak en :8081,
# SQL Server en Docker con la semilla canonica y los objetos de la rama feature/HU102 de GestioAsistenciaDB.
# Solo lectura: no crea ni modifica datos. Concilia la API contra la ejecucion directa de los SP y contra tablas.
param(
    [string]$ApiUrl = 'http://localhost:8090/api/v1',
    [string]$TokenUrl = 'http://127.0.0.1:8081/realms/asistencias-uco/protocol/openid-connect/token',
    [string]$Container = 'sql_server_asistencias',
    [string]$SqlPassword = 'TuPasswordSeguro123',
    [string]$UserPassword = 'Test1234!',
    [string]$Decano = 'decano.ingenieria@uco.edu.co',
    [string]$IdDecanoUsuario = 'E1F2A3B4-0000-0000-0000-000000000001'
)
$ErrorActionPreference = 'Stop'
$failures = 0
$skips = 0

function Get-Token([string]$user) {
    $body = @{ client_id = 'asistencias-uco-frontend'; grant_type = 'password'; username = $user; password = $UserPassword }
    (Invoke-RestMethod -Uri $TokenUrl -Method Post -Body $body).access_token
}

function Invoke-Api([string]$path, [string]$token) {
    $headers = @{ 'X-Correlation-Id' = [guid]::NewGuid().ToString() }
    if ($token) { $headers.Authorization = "Bearer $token" }
    try {
        $r = Invoke-WebRequest -Uri "$ApiUrl$path" -Method Get -Headers $headers -UseBasicParsing
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Json = ($r.Content | ConvertFrom-Json) }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        $content = (New-Object System.IO.StreamReader($resp.GetResponseStream())).ReadToEnd()
        $json = $null
        if ($content) { try { $json = $content | ConvertFrom-Json } catch { } }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Json = $json }
    }
}

function Invoke-Sql([string]$query) {
    $out = $query | docker exec -i $Container /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $SqlPassword -C -d gestionasistenciadb -s '|' -W
    @($out | Where-Object { $_ -and $_ -notmatch '^\(\d+ rows? affected\)' -and $_ -notmatch '^-+(\|-+)*$' })
}

function Check([string]$name, [bool]$cond, [string]$detail = '') {
    if ($cond) { Write-Host "[PASS] $name" -ForegroundColor Green }
    else { Write-Host "[FAIL] $name $detail" -ForegroundColor Red; $script:failures++ }
}

function Skip([string]$name, [string]$why) {
    Write-Host "[SKIP] $name ($why)" -ForegroundColor Yellow
    $script:skips++
}

function Compare-Columns($cols, $row, $api, [string[]]$campos, [string]$prefijo) {
    foreach ($campo in $campos) {
        $idx = [array]::IndexOf($cols, $campo)
        if ($idx -lt 0) { Check "$prefijo columna $campo presente en el SP" $false "columnas=$($cols -join ',')"; continue }
        $esperado = ($row -split '\|')[$idx]
        $actual = $api.$campo
        $coincide = if ($esperado -eq 'NULL') { $null -eq $actual } else { [decimal]$actual -eq [decimal]$esperado }
        Check "$prefijo $campo coincide con SP ($esperado)" $coincide "api=$actual"
    }
}

$td = Get-Token $Decano
$tc = Get-Token 'coordinador.sistemas@uco.edu.co'
$tdoc = Get-Token 'maria.rostagno@uco.edu.co'
$te = Get-Token 'carlos.zapata@uco.edu.co'
$ta = Get-Token 'admin.institucional@uco.edu.co'
$rutas = @('/decano/indicadores', '/decano/comparativa-programas', '/decano/aforo-grupos?pagina=1&tamano=5')

# --- T-E2E-01: seguridad ---
foreach ($r in $rutas) {
    Check "sin token => 401 en $r" ((Invoke-Api $r $null).Status -eq 401)
    foreach ($par in @(@('coordinador', $tc), @('docente', $tdoc), @('estudiante', $te))) {
        Check "$($par[0]) => 403 en $r" ((Invoke-Api $r $par[1]).Status -eq 403)
    }
}
$admin = Invoke-Api '/decano/indicadores' $ta
Check 'administrador sin facultad => 403 con mensaje del servidor' ($admin.Status -eq 403 -and $admin.Json.message) "status=$($admin.Status)"

# --- T-E2E-02/03: KPIs conciliados contra el SP ---
$k = Invoke-Api '/decano/indicadores' $td
Check 'indicadores 200' ($k.Status -eq 200) "status=$($k.Status)"
$d = $k.Json.datos
$sp = Invoke-Sql "SET NOCOUNT ON; DECLARE @c UNIQUEIDENTIFIER = NEWID(); EXEC dbo.usp_consultar_kpis_decano @idUsuarioEjecutor='$IdDecanoUsuario', @idCorrelacion=@c;"
Write-Host "SP kpis => $($sp -join ' // ')"
$cols = $sp[0] -split '\|'
Compare-Columns $cols $sp[1] $d @('totalProgramas', 'totalEstudiantes', 'estudiantesRegulares', 'estudiantesRiesgo', 'estudiantesPerdida', 'tasaRetencion',
    'sesionesEjecutadas', 'faltasSinJustaCausa', 'porcentajeAusentismo', 'porcentajeAsistencia', 'gruposActivos', 'cupoTotal', 'matriculadosActivos', 'tasaAforo') 'kpi'
Check 'regulares+riesgo+perdida = total' (($d.estudiantesRegulares + $d.estudiantesRiesgo + $d.estudiantesPerdida) -eq $d.totalEstudiantes)
Check 'umbrales provienen del catalogo' ($d.umbralAdvertencia -eq 15 -and $d.umbralPerdida -eq 20)
$totalProgDirecto = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(*) AS n FROM dbo.Programa p INNER JOIN dbo.Facultad f ON f.id = p.facultad INNER JOIN dbo.Decano dc ON dc.id = f.decano WHERE dc.usuario = '$IdDecanoUsuario';")[1]
Check "totalProgramas coincide con consulta de control sobre tablas ($totalProgDirecto)" ($d.totalProgramas -eq $totalProgDirecto)

# --- T-E2E-04: comparativa ---
$c = Invoke-Api '/decano/comparativa-programas' $td
Check 'comparativa 200' ($c.Status -eq 200)
$filas = @($c.Json.datos)
Check 'comparativa: una fila por programa de la facultad' ($filas.Count -eq $totalProgDirecto) "filas=$($filas.Count)"
$spC = Invoke-Sql "SET NOCOUNT ON; DECLARE @c UNIQUEIDENTIFIER = NEWID(); EXEC dbo.usp_consultar_comparativa_programas_facultad @idUsuarioEjecutor='$IdDecanoUsuario', @idCorrelacion=@c;"
$colsC = $spC[0] -split '\|'
Check 'comparativa: mismo numero de filas que el SP' ($filas.Count -eq ($spC.Count - 1))
if ($filas.Count -gt 0 -and $spC.Count -gt 1) {
    Compare-Columns $colsC $spC[1] $filas[0] @('totalEstudiantes', 'estudiantesRiesgo', 'estudiantesPerdida', 'tasaRetencion', 'sesionesEjecutadas', 'faltasSinJustaCausa', 'porcentajeAusentismo') 'comparativa[0]'
}
Check 'comparativa: suma de estudiantes por programa >= total de la facultad (una persona puede estar en 2 programas)' ((($filas | Measure-Object -Property totalEstudiantes -Sum).Sum) -ge $d.totalEstudiantes)

# --- T-E2E-05/06: aforo y cada valor de cada filtro ---
$a = Invoke-Api '/decano/aforo-grupos?pagina=1&tamano=5' $td
Check 'aforo 200' ($a.Status -eq 200)
Check 'aforo <= tamano' (@($a.Json.datos).Count -le 5)
$totalGruposDirecto = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(*) AS n FROM dbo.uv_grupo_programa gp INNER JOIN dbo.uv_programa p ON p.id = gp.idPrograma INNER JOIN dbo.uv_facultad f ON f.id = p.idFacultad INNER JOIN dbo.uv_decano_identidad di ON di.id = f.idDecano WHERE di.idUsuario = '$IdDecanoUsuario' AND gp.grupoEstaHablitado = 1 AND gp.idPeriodoAcademico = '$($d.idPeriodoAcademico)';")[1]
Check "aforo: total coincide con consulta de control ($totalGruposDirecto)" ($a.Json.total -eq $totalGruposDirecto) "api=$($a.Json.total)"
Check 'aforo: gruposActivos del KPI = total de grupos habilitados' ($d.gruposActivos -eq $totalGruposDirecto)
$p2 = Invoke-Api '/decano/aforo-grupos?pagina=2&tamano=1' $td
Check 'aforo: pagina 2 respeta el tamano' (@($p2.Json.datos).Count -le 1 -and $p2.Status -eq 200)
if ($totalGruposDirecto -ge 2) {
    Check 'aforo: paginas disjuntas' (@($a.Json.datos)[0].idGrupo -ne @($p2.Json.datos)[0].idGrupo)
} else { Skip 'aforo: paginas disjuntas' 'la semilla tiene menos de 2 grupos habilitados' }

$propio = $filas[0].idPrograma
Check 'filtro idPrograma propio => 200' ((Invoke-Api "/decano/aforo-grupos?pagina=1&tamano=5&idPrograma=$propio" $td).Status -eq 200)
Check 'filtro idPrograma inexistente => 404' ((Invoke-Api "/decano/aforo-grupos?pagina=1&tamano=5&idPrograma=$([guid]::NewGuid())" $td).Status -eq 404)
$ajeno = Invoke-Sql "SET NOCOUNT ON; SELECT TOP 1 p.id FROM dbo.Programa p INNER JOIN dbo.Facultad f ON f.id = p.facultad INNER JOIN dbo.Decano dc ON dc.id = f.decano WHERE dc.usuario <> '$IdDecanoUsuario';"
if ($ajeno.Count -ge 2) { Check 'filtro idPrograma de otra facultad => 404' ((Invoke-Api "/decano/aforo-grupos?pagina=1&tamano=5&idPrograma=$($ajeno[1])" $td).Status -eq 404) }
else { Skip 'filtro idPrograma de otra facultad => 404' 'la semilla no tiene programas de otra facultad (el caso esta cubierto por el test DB con fixture)' }
foreach ($v in @('true', 'false')) {
    $r = Invoke-Api "/decano/aforo-grupos?pagina=1&tamano=5&soloHabilitados=$v" $td
    Check "filtro soloHabilitados=$v => 200" ($r.Status -eq 200) "status=$($r.Status)"
    if ($v -eq 'true') { Check 'soloHabilitados=true solo lista grupos habilitados' ((@($r.Json.datos) | Where-Object { -not $_.grupoEstaHabilitado }).Count -eq 0) }
}
Check 'idPeriodoAcademico vigente => 200' ((Invoke-Api "/decano/indicadores?idPeriodoAcademico=$($d.idPeriodoAcademico)" $td).Status -eq 200)
Check 'idPeriodoAcademico inexistente => 404' ((Invoke-Api "/decano/indicadores?idPeriodoAcademico=$([guid]::NewGuid())" $td).Status -eq 404)
Check 'idPeriodoAcademico inexistente en comparativa => 404' ((Invoke-Api "/decano/comparativa-programas?idPeriodoAcademico=$([guid]::NewGuid())" $td).Status -eq 404)
Check 'idPeriodoAcademico inexistente en aforo => 404' ((Invoke-Api "/decano/aforo-grupos?pagina=1&tamano=5&idPeriodoAcademico=$([guid]::NewGuid())" $td).Status -eq 404)
Check 'pagina=0 => 400' ((Invoke-Api '/decano/aforo-grupos?pagina=0&tamano=5' $td).Status -eq 400)
Check 'tamano excesivo => 400' ((Invoke-Api '/decano/aforo-grupos?pagina=1&tamano=1000' $td).Status -eq 400)
Check 'tamano=0 => 400' ((Invoke-Api '/decano/aforo-grupos?pagina=1&tamano=0' $td).Status -eq 400)

Write-Host "Omitidos: $skips"
if ($failures -gt 0) { Write-Host "E2E FALLO: $failures verificaciones" -ForegroundColor Red; exit 1 }
Write-Host 'E2E OK' -ForegroundColor Green

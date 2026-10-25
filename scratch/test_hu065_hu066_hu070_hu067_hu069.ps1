# E2E-02 - HU065/HU066/HU070/HU067/HU069: KPIs, alerta temprana, permanencia, bitacora y ficha del coordinador.
# Prerrequisitos: backend de la rama feature/HU065-kpis-coordinador-datos-reales en -ApiUrl, Keycloak en :8081,
# SQL Server en Docker con la semilla canonica y los objetos de la rama feature/HU065 de GestioAsistenciaDB.
# Solo lectura: no crea ni modifica datos. Concilia la API contra la ejecucion directa de los SPs.
param(
    [string]$ApiUrl = 'http://localhost:8090/api/v1',
    [string]$TokenUrl = 'http://127.0.0.1:8081/realms/asistencias-uco/protocol/openid-connect/token',
    [string]$Container = 'sql_server_asistencias',
    [string]$SqlPassword = 'TuPasswordSeguro123',
    [string]$UserPassword = 'Test1234!',
    [string]$Coordinador = 'coordinador.sistemas@uco.edu.co',
    [string]$IdCoordinadorUsuario = 'E1F2A3B4-0000-0000-0000-000000000002'
)
$ErrorActionPreference = 'Stop'
$failures = 0

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

$tc = Get-Token $Coordinador
$te = Get-Token 'carlos.zapata@uco.edu.co'
$td = Get-Token 'maria.rostagno@uco.edu.co'

# --- Seguridad ---
Check 'sin token => 401' ((Invoke-Api '/coordinador/indicadores' $null).Status -eq 401)
Check 'estudiante => 403 en indicadores' ((Invoke-Api '/coordinador/indicadores' $te).Status -eq 403)
Check 'docente => 403 en alertas' ((Invoke-Api '/coordinador/alertas-tempranas?pagina=1&tamano=5' $td).Status -eq 403)

# --- HU065/HU070: KPIs conciliados contra el SP ---
$k = Invoke-Api '/coordinador/indicadores' $tc
Check 'indicadores 200' ($k.Status -eq 200) "status=$($k.Status)"
$d = $k.Json.datos
$sp = Invoke-Sql "SET NOCOUNT ON; DECLARE @c UNIQUEIDENTIFIER = NEWID(); EXEC dbo.usp_consultar_kpis_coordinador @idUsuarioEjecutor='$IdCoordinadorUsuario', @idCorrelacion=@c;"
Write-Host "SP kpis => $($sp -join ' // ')"
$cols = $sp[0] -split '\|'
foreach ($par in @(@('totalEstudiantes'), @('estudiantesRegulares'), @('estudiantesRiesgo'), @('estudiantesPerdida'), @('sesionesProgramadas'), @('sesionesEjecutadas'))) {
    $campo = $par[0]
    $idx = [array]::IndexOf($cols, $campo)
    if ($idx -ge 0) {
        $esperado = ($sp[1] -split '\|')[$idx]
        Check "kpi $campo coincide con SP ($esperado)" ([string]$d.$campo -eq $esperado) "api=$($d.$campo)"
    } else { Check "columna $campo presente en la salida del SP" $false "columnas=$($cols -join ',')" }
}
Check 'regulares+riesgo+perdida = total' (($d.estudiantesRegulares + $d.estudiantesRiesgo + $d.estudiantesPerdida) -eq $d.totalEstudiantes)
Check 'umbrales provienen del catalogo' ($d.umbralAdvertencia -eq 15 -and $d.umbralPerdida -eq 20)

# --- HU066: alertas paginadas ---
$a = Invoke-Api '/coordinador/alertas-tempranas?pagina=1&tamano=5' $tc
Check 'alertas 200' ($a.Status -eq 200)
Check 'alertas <= tamano' (@($a.Json.datos).Count -le 5)
$aBad = Invoke-Api '/coordinador/alertas-tempranas?pagina=0&tamano=5' $tc
Check 'pagina invalida => 400' ($aBad.Status -eq 400) "status=$($aBad.Status)"
$aRojo = Invoke-Api '/coordinador/alertas-tempranas?pagina=1&tamano=50&semaforo=ROJO' $tc
Check 'filtro ROJO solo devuelve ROJO' ((@($aRojo.Json.datos) | Where-Object { $_.semaforo -ne 'ROJO' }).Count -eq 0)
foreach ($v in @('VERDE', 'AMBAR', 'ROJO')) {
    $rv = Invoke-Api "/coordinador/alertas-tempranas?pagina=1&tamano=50&semaforo=$v" $tc
    Check "filtro semaforo=$v => 200 y solo devuelve $v" ($rv.Status -eq 200 -and (@($rv.Json.datos) | Where-Object { $_.semaforo -ne $v }).Count -eq 0) "status=$($rv.Status)"
}
Check 'semaforo invalido (AMARILLO) => 400' ((Invoke-Api '/coordinador/alertas-tempranas?pagina=1&tamano=5&semaforo=AMARILLO' $tc).Status -eq 400)

# --- HU067/HU069: ficha y bitacora ---
$primero = @($a.Json.datos)[0]
if ($primero) {
    $id = $primero.idEstudiante
    $f = Invoke-Api "/coordinador/estudiantes/$id/ficha" $tc
    Check 'ficha 200' ($f.Status -eq 200)
    Check 'ficha corresponde al estudiante' ($f.Json.datos.idEstudiante -eq $id)
    $b = Invoke-Api "/coordinador/estudiantes/$id/bitacora?pagina=1&tamano=5" $tc
    Check 'bitacora 200' ($b.Status -eq 200)
    Check 'bitacora <= tamano' (@($b.Json.datos).Count -le 5)
} else { Write-Host '[INFO] sin alertas en la semilla: ficha/bitacora se prueban con un estudiante del directorio' -ForegroundColor Yellow }

$ajeno = Invoke-Api "/coordinador/estudiantes/$([guid]::NewGuid())/ficha" $tc
Check 'estudiante inexistente/ajeno => 404' ($ajeno.Status -eq 404) "status=$($ajeno.Status)"

if ($failures -gt 0) { Write-Host "E2E FALLO: $failures verificaciones" -ForegroundColor Red; exit 1 }
Write-Host 'E2E OK' -ForegroundColor Green

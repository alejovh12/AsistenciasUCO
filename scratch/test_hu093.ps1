param(
    [string]$Api = 'http://127.0.0.1:18081/api/v1',
    [string]$Keycloak = 'http://127.0.0.1:8081',
    [string]$SqlContainer = 'sql_docentes',
    [string]$SqlPassword = $env:MSSQL_SA_PASSWORD,
    [string]$Documento = '960093001',
    [string]$Correo = 'e2e.hu093.docente@uco.edu.co'
)
# E2E HU093 (activar/inactivar el vinculo) con JWT real. Requiere el backend en otro puerto y SQL propio.
$ErrorActionPreference = 'Stop'
function Get-Token($user, $pass) {
    (Invoke-RestMethod -Uri "$Keycloak/realms/asistencias-uco/protocol/openid-connect/token" -Method Post -Body @{
        client_id = 'asistencias-uco-frontend'; grant_type = 'password'; username = $user; password = $pass }).access_token
}
function Call($method, $path, $token, $body) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        $r = if ($null -ne $body) { Invoke-WebRequest -Uri "$Api$path" -Method $method -Headers $h -Body ($body | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing }
             else { Invoke-WebRequest -Uri "$Api$path" -Method $method -Headers $h -UseBasicParsing }
        return @{ status = [int]$r.StatusCode; body = ($r.Content | ConvertFrom-Json) }
    } catch {
        $resp = $_.Exception.Response
        $content = $null
        if ($resp) { $content = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() }
        return @{ status = [int]$resp.StatusCode; body = $(if ($content) { $content | ConvertFrom-Json } else { $null }) }
    }
}
function Assert($cond, $msg) { if (-not $cond) { throw "E2E FAILED: $msg" } else { Write-Host "OK  $msg" -ForegroundColor Green } }
function Sql($q) { (docker exec $SqlContainer /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $SqlPassword -C -d gestionasistenciadb -h -1 -W -Q "SET NOCOUNT ON; $q" | Out-String).Trim() }

$coord = Get-Token 'coordinador.sistemas@uco.edu.co' 'Test1234!'
$decano = Get-Token 'decano.ingenieria@uco.edu.co' 'Test1234!'
$tipos = Call GET '/tipos-vinculacion' $coord $null
$cat = ($tipos.body | Where-Object codigo -eq 'CAT').id
$tipoCC = (Sql "SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion='CC'")

$alta = Call POST '/coordinador/docentes' $coord @{ idTipoIdentificacion = $tipoCC; numeroIdentificacion = $Documento; primerNombre = 'Estado'; primerApellido = 'Prueba';
    correo = $Correo; password = 'Docente2026!'; idTipoVinculacion = $cat }
Assert ($alta.status -eq 201) "Alta previa del docente de prueba (obtuvo $($alta.status))"
$id = $alta.body.datos.idDocente

$r1 = Call PATCH "/coordinador/docentes/$id/estado" $coord @{ estaActivo = $false }
Assert ($r1.status -eq 200 -and $r1.body.datos.estaActivo -eq $false) "PATCH inactivar responde 200 con estaActivo=false (obtuvo $($r1.status))"
$bd = Sql "SELECT CONCAT(dp.estado, '|', u.estado) FROM dbo.DocentePrograma dp INNER JOIN dbo.Docente d ON d.id = dp.docente INNER JOIN dbo.Usuario u ON u.id = d.usuario WHERE dp.docente = '$id'"
Assert ($bd -eq '0|1') "sqlcmd: vinculo inactivo (0) y persona activa (1): $bd"
Assert ([bool](Get-Token $Correo 'Docente2026!')) 'El docente SIGUE iniciando sesion en Keycloak (la cuenta no se deshabilita)'

$inact = Call GET '/coordinador/docentes?estaActivo=false' $coord $null
Assert ($inact.status -eq 200 -and (@($inact.body.datos | Where-Object { $_.idDocente -eq $id }).Count -eq 1)) 'El filtro estaActivo=false lista al docente inactivado'

$r2 = Call PATCH "/coordinador/docentes/$id/estado" $coord @{ estaActivo = $false }
Assert ($r2.status -eq 200) 'Inactivar de nuevo es idempotente (200)'

$r3 = Call PATCH "/coordinador/docentes/$id/estado" $coord @{ estaActivo = $true }
Assert ($r3.status -eq 200 -and $r3.body.datos.estaActivo -eq $true) 'PATCH reactivar responde 200 con estaActivo=true'

$vacio = Call PATCH "/coordinador/docentes/$id/estado" $coord @{}
Assert ($vacio.status -eq 400) 'PATCH sin estaActivo responde 400'
$sinVinculo = Call PATCH '/coordinador/docentes/11111111-2222-3333-4444-555555555555/estado' $coord @{ estaActivo = $false }
Assert ($sinVinculo.status -eq 404 -and $sinVinculo.body.code -eq 'ERR_DOCENTE_NO_VINCULADO_PROGRAMA') "Docente sin vinculo responde 404 con codigo de feature (obtuvo $($sinVinculo.status) $($sinVinculo.body.code))"
$forb = Call PATCH "/coordinador/docentes/$id/estado" $decano @{ estaActivo = $false }
Assert ($forb.status -eq 403) 'Decano recibe 403'
Write-Host 'E2E HU093 COMPLETO' -ForegroundColor Cyan


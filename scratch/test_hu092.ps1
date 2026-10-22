param(
    [string]$Api = 'http://127.0.0.1:18081/api/v1',
    [string]$Keycloak = 'http://127.0.0.1:8081',
    [string]$SqlContainer = 'sql_docentes',
    [string]$SqlPassword = $env:MSSQL_SA_PASSWORD,
    [string]$Documento = '960092001',
    [string]$Correo = 'e2e.hu092.docente@uco.edu.co'
)
# E2E HU092 (editar tipo de vinculacion y nombres) con JWT real. Requiere el backend en otro puerto y SQL propio.
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
$pla = ($tipos.body | Where-Object codigo -eq 'PLA').id
$tipoCC = (Sql "SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion='CC'")

$alta = Call POST '/coordinador/docentes' $coord @{ idTipoIdentificacion = $tipoCC; numeroIdentificacion = $Documento; primerNombre = 'Edgar'; primerApellido = 'Original';
    correo = $Correo; password = 'Docente2026!'; idTipoVinculacion = $cat }
Assert ($alta.status -eq 201) "Alta previa del docente de prueba (obtuvo $($alta.status))"
$id = $alta.body.datos.idDocente

$r1 = Call PUT "/coordinador/docentes/$id" $coord @{ idTipoVinculacion = $pla; primerNombre = 'Editado'; segundoApellido = 'Nuevo' }
Assert ($r1.status -eq 200) "PUT tipo + nombres responde 200 (obtuvo $($r1.status))"
Assert ($r1.body.datos.codigoTipoVinculacion -eq 'PLA' -and $r1.body.datos.primerNombre -eq 'Editado' -and $r1.body.datos.segundoApellido -eq 'Nuevo') 'La respuesta refleja los cambios releidos de la DB'
Assert ($r1.body.datos.correo -eq $Correo -and $r1.body.datos.numeroIdentificacion -eq [int]$Documento) 'Correo y documento no cambian'

$fila = Sql "SELECT CONCAT(primerNombre, '|', segundoApellido, '|', correo) FROM dbo.Usuario WHERE correo = '$Correo'"
Assert ($fila -eq "Editado|Nuevo|$Correo") 'sqlcmd confirma los nombres editados en Usuario'

$login = Get-Token $Correo 'Docente2026!'
Assert ([bool]$login) 'El docente sigue iniciando sesion en Keycloak (los nombres no afectan el login)'

$vacio = Call PUT "/coordinador/docentes/$id" $coord @{}
Assert ($vacio.status -eq 400) 'PUT sin campos responde 400'
$malTipo = Call PUT "/coordinador/docentes/$id" $coord @{ idTipoVinculacion = '15151515-1515-1515-1515-151515151515' }
Assert ($malTipo.status -eq 400 -and $malTipo.body.code -eq 'ERR_TIPO_VINCULACION_INVALIDO') "Tipo inexistente responde 400 con codigo de feature (obtuvo $($malTipo.status) $($malTipo.body.code))"
$sinVinculo = Call PUT '/coordinador/docentes/11111111-2222-3333-4444-555555555555' $coord @{ primerNombre = 'X' }
Assert ($sinVinculo.status -eq 404) "Docente sin vinculo responde 404 (obtuvo $($sinVinculo.status))"
$forb = Call PUT "/coordinador/docentes/$id" $decano @{ primerNombre = 'X' }
Assert ($forb.status -eq 403) 'Decano recibe 403'
Write-Host 'E2E HU092 COMPLETO' -ForegroundColor Cyan

param(
    [string]$Api = 'http://127.0.0.1:18081/api/v1',
    [string]$Keycloak = 'http://127.0.0.1:8081',
    [string]$SqlContainer = 'sql_docentes',
    [string]$SqlPassword = 'TuPasswordSeguro123',
    [string]$Documento = '960091001',
    [string]$Correo = 'e2e.hu091.docente@uco.edu.co'
)
# E2E HU062/HU063/HU089/HU090/HU091 con JWT real (Keycloak) contra un backend en otro puerto y SQL propio.
$ErrorActionPreference = 'Stop'
function Get-Token($user, $pass) {
    (Invoke-RestMethod -Uri "$Keycloak/realms/asistencias-uco/protocol/openid-connect/token" -Method Post -Body @{
        client_id = 'asistencias-uco-frontend'; grant_type = 'password'; username = $user; password = $pass }).access_token
}
function Call($method, $path, $token, $body) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        $r = if ($body) { Invoke-WebRequest -Uri "$Api$path" -Method $method -Headers $h -Body ($body | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing }
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
Assert ($tipos.status -eq 200 -and $tipos.body.Count -ge 4) 'GET /tipos-vinculacion devuelve el catalogo activo'
$cat = ($tipos.body | Where-Object codigo -eq 'CAT').id
$pla = ($tipos.body | Where-Object codigo -eq 'PLA').id
$tipoCC = (Sql "SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion='CC'")

$lista = Call GET '/coordinador/docentes?pagina=1&tamano=5' $coord $null
Assert ($lista.status -eq 200 -and $lista.body.exitoso) 'GET /coordinador/docentes responde 200 (antes 501)'

$nuevo = @{ idTipoIdentificacion = $tipoCC; numeroIdentificacion = $Documento; primerNombre = 'Eduardo'; primerApellido = 'Prueba';
            correo = $Correo; password = 'Docente2026!'; idTipoVinculacion = $cat }
$r1 = Call POST '/coordinador/docentes' $coord $nuevo
Assert ($r1.status -eq 201) "POST modo nuevo responde 201 (obtuvo $($r1.status))"
$idDocente = $r1.body.datos.idDocente
Assert ($r1.body.datos.codigoTipoVinculacion -eq 'CAT' -and $r1.body.datos.totalGruposPrograma -eq 0) 'El docente aparece con CAT y 0 grupos'

$login = Get-Token $Correo 'Docente2026!'
Assert ([bool]$login) 'El docente nuevo inicia sesion en Keycloak con la contrasena asignada'
$seg = $login.Split('.')[1].Replace('-', '+').Replace('_', '/')
while ($seg.Length % 4 -ne 0) { $seg += '=' }
$payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($seg))
Assert ($payload -match 'DOCENTE') 'El token del docente contiene el rol DOCENTE'

$r2 = Call POST '/coordinador/docentes' $coord $nuevo
Assert ($r2.status -eq 200 -or $r2.status -eq 201 -or $r2.status -eq 409) "Repetir el alta es idempotente o conflicto controlado (estado $($r2.status))"

$r3 = Call POST '/coordinador/docentes' $coord @{ idDocente = $idDocente; idTipoVinculacion = $pla }
Assert ($r3.status -eq 409) "Vincular dos veces el mismo docente responde 409 (obtuvo $($r3.status))"

$busq = Call GET "/coordinador/docentes?busqueda=$Documento" $coord $null
Assert ($busq.status -eq 200 -and $busq.body.total -ge 1) 'Busqueda por prefijo de cedula encuentra al docente'
$busq2 = Call GET '/coordinador/docentes?busqueda=Prueba' $coord $null
Assert ($busq2.status -eq 200 -and $busq2.body.total -ge 1) 'Busqueda por nombre encuentra al docente'

$cand = Call GET '/coordinador/docentes/candidatos?busqueda=ab' $coord $null
Assert ($cand.status -eq 400) 'Candidatos con busqueda corta responde 400'
$cand2 = Call GET '/coordinador/docentes/candidatos?busqueda=Prueba' $coord $null
Assert ($cand2.status -eq 200) 'Candidatos responde 200'

$bad = Call POST '/coordinador/docentes' $coord @{ idTipoVinculacion = $cat }
Assert ($bad.status -eq 400) 'POST sin modo responde 400'

$forb = Call GET '/coordinador/docentes' $decano $null
Assert ($forb.status -eq 403) "Decano en /coordinador/docentes responde 403 (obtuvo $($forb.status))"

$persist = Sql "SELECT COUNT(*) FROM dbo.uv_docente_programa WHERE idDocente = '$idDocente'"
Assert ($persist -eq '1') 'sqlcmd confirma exactamente un vinculo en uv_docente_programa'
Write-Host 'E2E HU091 COMPLETO' -ForegroundColor Cyan

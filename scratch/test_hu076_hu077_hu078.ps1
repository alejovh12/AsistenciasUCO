# E2E — HU076 + HU077 + HU078 (+ HU108 listado, HU023 catalogos): programa academico del decano.
# Prerrequisitos: backend de la rama feature/HU076-HU077-HU078-programa-academico (por defecto en :8090, NO el :8080 de otros agentes),
# Keycloak en :8081 (realm asistencias-uco) y SQL Server en Docker con la semilla canonica y los objetos de esta rama aplicados.
# Crea dos coordinadores libres y un programa reales y los elimina al terminar.
param(
    [string]$ApiUrl = 'http://localhost:8090/api/v1',
    [string]$TokenUrl = 'http://127.0.0.1:8081/realms/asistencias-uco/protocol/openid-connect/token',
    [string]$Container = 'sql_server_asistencias',
    [string]$SqlPassword = 'TuPasswordSeguro123',
    [string]$UserPassword = 'Test1234!',
    [string]$Decano = 'decano.ingenieria@uco.edu.co',
    [string]$Coordinador = 'coordinador.sistemas@uco.edu.co',
    [string]$Admin = 'admin.institucional@uco.edu.co'
)
$ErrorActionPreference = 'Stop'
$failures = 0

function Get-Token([string]$user) {
    $body = @{ client_id = 'asistencias-uco-frontend'; grant_type = 'password'; username = $user; password = $UserPassword }
    (Invoke-RestMethod -Uri $TokenUrl -Method Post -Body $body).access_token
}

function Invoke-Api([string]$method, [string]$path, [string]$token, $body = $null) {
    $headers = @{ 'X-Correlation-Id' = [guid]::NewGuid().ToString() }
    if ($token) { $headers.Authorization = "Bearer $token" }
    $params = @{ Uri = "$ApiUrl$path"; Method = $method; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $body) { $params.Body = ($body | ConvertTo-Json -Depth 5); $params.ContentType = 'application/json; charset=utf-8' }
    try {
        $r = Invoke-WebRequest @params
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Json = ($r.Content | ConvertFrom-Json) }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $content = $reader.ReadToEnd()
        $json = $null
        if ($content) { try { $json = $content | ConvertFrom-Json } catch { } }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Json = $json }
    }
}

function Expect([string]$label, [bool]$ok, [string]$detail) {
    if ($ok) { Write-Host "E2E_PASS:$label" } else { Write-Host "E2E_FAIL:$label $detail"; $script:failures++ }
}

function Invoke-Sql([string]$query) {
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("SET NOCOUNT ON; $query"))
    $out = docker exec $Container bash -c "echo $b64 | base64 -d | /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '$SqlPassword' -C -d gestionasistenciadb -h -1 -W"
    ($out | Out-String).Trim()
}

$sufijo = ([guid]::NewGuid().ToString('N')).Substring(0, 8)
$u1 = [guid]::NewGuid(); $u2 = [guid]::NewGuid(); $c1 = [guid]::NewGuid(); $c2 = [guid]::NewGuid()
$programaId = $null
try {
    Invoke-Sql @"
DECLARE @tid UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.TipoIdentificacion);
INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, correo, correoConfirmado, estado, password)
VALUES ('$u1', @tid, 400000000 + ABS(CHECKSUM(NEWID()) % 9999999), N'E2E', N'Uno', N'Coord', N'X', N'e2e.$sufijo.1@qa.test', 1, 1, N'x'),
       ('$u2', @tid, 410000000 + ABS(CHECKSUM(NEWID()) % 9999999), N'E2E', N'Dos', N'Coord', N'X', N'e2e.$sufijo.2@qa.test', 1, 1, N'x');
INSERT INTO dbo.Coordinador (id, usuario) VALUES ('$c1', '$u1'), ('$c2', '$u2');
"@ | Out-Null

    $tokenDecano = Get-Token $Decano
    $tokenCoord = Get-Token $Coordinador
    $tokenAdmin = Get-Token $Admin

    # Catalogos (HU023)
    $tipos = Invoke-Api GET '/tipos-programa' $tokenDecano
    Expect 'catalogo-tipos-200' ($tipos.Status -eq 200 -and @($tipos.Json).Count -ge 1) "status=$($tipos.Status)"
    $mods = Invoke-Api GET '/modalidades-programa' $tokenDecano
    $jors = Invoke-Api GET '/jornadas-programa' $tokenDecano
    Expect 'catalogo-modalidades-jornadas-200' ($mods.Status -eq 200 -and $jors.Status -eq 200 -and @($mods.Json).Count -ge 1 -and @($jors.Json).Count -ge 1) "mod=$($mods.Status) jor=$($jors.Status)"
    $tipoId = @($tipos.Json)[0].id
    $modId = @($mods.Json)[0].id
    $jorId = @($jors.Json)[0].id

    # HU076: alta feliz
    $nombre = "E2E Programa $sufijo"
    $alta = Invoke-Api POST '/decano/programas' $tokenDecano @{ nombre = $nombre; idTipoPrograma = $tipoId; idCoordinador = $c1.ToString(); idModalidad = $null; idJornada = $null }
    Expect 'HU076-alta-201' ($alta.Status -eq 201 -and $alta.Json.datos.nombre -eq $nombre -and $alta.Json.datos.estaActivo -eq $true) "status=$($alta.Status)"
    $programaId = $alta.Json.datos.id
    $vinculo = Invoke-Sql "SELECT idPrograma FROM dbo.uv_coordinador WHERE id = '$c1'"
    Expect 'HU076-coordinador-vinculado-en-db' ($vinculo -eq $programaId.ToUpper()) "idPrograma=$vinculo"

    # HU076: duplicados y reglas 1 a 1
    $dup = Invoke-Api POST '/decano/programas' $tokenDecano @{ nombre = $nombre.ToUpper(); idTipoPrograma = $tipoId; idCoordinador = $c2.ToString() }
    Expect 'HU076-nombre-duplicado-409' ($dup.Status -eq 409) "status=$($dup.Status)"
    $ocupado = Invoke-Api POST '/decano/programas' $tokenDecano @{ nombre = "Otro $sufijo"; idTipoPrograma = $tipoId; idCoordinador = $c1.ToString() }
    Expect 'HU076-coordinador-ocupado-409' ($ocupado.Status -eq 409) "status=$($ocupado.Status)"
    $tipoMalo = Invoke-Api POST '/decano/programas' $tokenDecano @{ nombre = "Tipo $sufijo"; idTipoPrograma = [guid]::NewGuid().ToString(); idCoordinador = $c2.ToString() }
    Expect 'HU076-tipo-invalido-400' ($tipoMalo.Status -eq 400) "status=$($tipoMalo.Status)"
    $sinNombre = Invoke-Api POST '/decano/programas' $tokenDecano @{ nombre = ' '; idTipoPrograma = $tipoId; idCoordinador = $c2.ToString() }
    Expect 'HU076-cuerpo-invalido-400' ($sinNombre.Status -eq 400) "status=$($sinNombre.Status)"

    # HU077: modalidad, jornada y reasignacion de coordinador
    $edit = Invoke-Api PUT "/decano/programas/$programaId" $tokenDecano @{ nombre = "$nombre editado"; idTipoPrograma = $tipoId; idCoordinador = $c2.ToString(); idModalidad = $modId; idJornada = $jorId }
    Expect 'HU077-edicion-200' ($edit.Status -eq 200 -and $edit.Json.datos.idCoordinador -eq $c2.ToString() -and $edit.Json.datos.idModalidad -eq $modId -and $edit.Json.datos.idJornada -eq $jorId) "status=$($edit.Status)"
    $libre = Invoke-Sql "SELECT COUNT(*) FROM dbo.uv_coordinador WHERE id = '$c1' AND idPrograma IS NULL"
    Expect 'HU077-coordinador-anterior-disponible-en-db' ($libre -eq '1') "libre=$libre"
    $ajeno = Invoke-Api PUT "/decano/programas/$([guid]::NewGuid())" $tokenDecano @{ nombre = 'X'; idTipoPrograma = $tipoId; idCoordinador = $c1.ToString() }
    Expect 'HU077-programa-inexistente-404' ($ajeno.Status -eq 404) "status=$($ajeno.Status)"

    # HU108: listado paginado
    $lista = Invoke-Api GET "/decano/programas?pagina=1&tamano=5&nombre=E2E%20Programa%20$sufijo" $tokenDecano
    Expect 'HU108-listado-200' ($lista.Status -eq 200 -and $lista.Json.total -eq 1 -and $lista.Json.datos[0].id -eq $programaId) "status=$($lista.Status) total=$($lista.Json.total)"
    $listaMala = Invoke-Api GET '/decano/programas?pagina=0' $tokenDecano
    Expect 'HU108-paginacion-invalida-400' ($listaMala.Status -eq 400) "status=$($listaMala.Status)"
    $listaGrande = Invoke-Api GET '/decano/programas?tamano=100000' $tokenDecano
    Expect 'HU108-tamano-excedido-400' ($listaGrande.Status -eq 400) "status=$($listaGrande.Status)"

    # HU078: estado
    $off = Invoke-Api PATCH "/decano/programas/$programaId/estado" $tokenDecano @{ estaActivo = $false }
    Expect 'HU078-desactivar-200' ($off.Status -eq 200 -and $off.Json.datos.estaActivo -eq $false) "status=$($off.Status)"
    $off2 = Invoke-Api PATCH "/decano/programas/$programaId/estado" $tokenDecano @{ estaActivo = $false }
    Expect 'HU078-idempotente-200' ($off2.Status -eq 200) "status=$($off2.Status)"
    $inactivos = Invoke-Api GET "/decano/programas?estaActivo=false&nombre=E2E%20Programa%20$sufijo" $tokenDecano
    Expect 'HU078-filtro-inactivos' ($inactivos.Status -eq 200 -and $inactivos.Json.total -eq 1) "status=$($inactivos.Status)"
    $on = Invoke-Api PATCH "/decano/programas/$programaId/estado" $tokenDecano @{ estaActivo = $true }
    Expect 'HU078-activar-200' ($on.Status -eq 200 -and $on.Json.datos.estaActivo -eq $true) "status=$($on.Status)"
    $sinEstado = Invoke-Api PATCH "/decano/programas/$programaId/estado" $tokenDecano @{}
    Expect 'HU078-estado-ausente-400' ($sinEstado.Status -eq 400) "status=$($sinEstado.Status)"

    # Seguridad
    $coordGet = Invoke-Api GET '/decano/programas' $tokenCoord
    Expect 'SEC-coordinador-403' ($coordGet.Status -eq 403) "status=$($coordGet.Status)"
    $adminGet = Invoke-Api GET '/decano/programas' $tokenAdmin
    Expect 'SEC-admin-sin-facultad-de-decano-403' ($adminGet.Status -eq 403) "status=$($adminGet.Status)"
    $sinToken = Invoke-Api GET '/decano/programas' $null
    Expect 'SEC-sin-token-401' ($sinToken.Status -eq 401) "status=$($sinToken.Status)"
}
finally {
    Invoke-Sql @"
DELETE FROM dbo.Programa WHERE coordinador IN ('$c1', '$c2');
DELETE FROM dbo.Coordinador WHERE id IN ('$c1', '$c2');
DELETE FROM dbo.Usuario WHERE id IN ('$u1', '$u2');
"@ | Out-Null
    Write-Host 'Fixtures E2E eliminados.'
}

if ($failures -gt 0) { Write-Host "E2E_RESULT:FAIL ($failures)"; exit 1 }
Write-Host 'E2E_RESULT:PASS'

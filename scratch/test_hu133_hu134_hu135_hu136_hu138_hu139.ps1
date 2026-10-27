# E2E-04 - HU133/HU134/HU135/HU136/HU138/HU139: KPIs, censo, comparativa de facultades y tendencia de ausentismo de la institucion.
# Prerrequisitos: backend de la rama feature/HU133-kpis-admin-datos-reales en -ApiUrl, Keycloak en :8081,
# SQL Server en Docker con la semilla canonica y los objetos de la rama feature/HU133 de GestioAsistenciaDB.
# Solo lectura: no crea ni modifica datos. Concilia la API contra la ejecucion directa de los SP y contra tablas.
param(
    [string]$ApiUrl = 'http://localhost:8090/api/v1',
    [string]$TokenUrl = 'http://127.0.0.1:8081/realms/asistencias-uco/protocol/openid-connect/token',
    [string]$Container = 'sql_server_asistencias',
    [string]$SqlPassword = 'TuPasswordSeguro123',
    [string]$UserPassword = 'Test1234!',
    [string]$Admin = 'admin.institucional@uco.edu.co',
    [string]$IdAdminUsuario = 'E1F2A3B4-0000-0000-0000-000000000000'
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

$ta = Get-Token $Admin
$td = Get-Token 'decano.ingenieria@uco.edu.co'
$tc = Get-Token 'coordinador.sistemas@uco.edu.co'
$tdoc = Get-Token 'maria.rostagno@uco.edu.co'
$te = Get-Token 'carlos.zapata@uco.edu.co'
$rutas = @('/admin/indicadores', '/admin/comparativa-facultades', '/admin/tendencia-ausentismo')

# --- T-E2E-01: seguridad ---
foreach ($r in $rutas) {
    Check "sin token => 401 en $r" ((Invoke-Api $r $null).Status -eq 401)
    foreach ($par in @(@('decano', $td), @('coordinador', $tc), @('docente', $tdoc), @('estudiante', $te))) {
        Check "$($par[0]) => 403 en $r" ((Invoke-Api $r $par[1]).Status -eq 403)
    }
}

# --- T-E2E-02/03: KPIs conciliados contra el SP y tablas ---
$k = Invoke-Api '/admin/indicadores' $ta
Check 'indicadores 200' ($k.Status -eq 200) "status=$($k.Status)"
$d = $k.Json.datos
$sp = Invoke-Sql "SET NOCOUNT ON; DECLARE @c UNIQUEIDENTIFIER = NEWID(); EXEC dbo.usp_consultar_kpis_institucion @idUsuarioEjecutor='$IdAdminUsuario', @idCorrelacion=@c;"
Write-Host "SP kpis => $($sp -join ' // ')"
$cols = $sp[0] -split '\|'
Compare-Columns $cols $sp[1] $d @('totalFacultades', 'totalProgramas', 'totalEstudiantes', 'estudiantesRegulares', 'estudiantesRiesgo', 'estudiantesPerdida', 'tasaRetencion',
    'sesionesEjecutadas', 'faltasSinJustaCausa', 'porcentajeAusentismo', 'porcentajeAsistencia', 'sesionesVencidas', 'sesionesConRegistro', 'porcentajeCumplimiento',
    'estudiantesConMatriculaActiva', 'estudiantesRegistrados', 'docentesConGrupoActivo', 'docentesRegistrados') 'kpi'
Check 'regulares+riesgo+perdida = total = con matricula activa' (($d.estudiantesRegulares + $d.estudiantesRiesgo + $d.estudiantesPerdida) -eq $d.totalEstudiantes -and $d.totalEstudiantes -eq $d.estudiantesConMatriculaActiva)
Check 'sesionesConRegistro <= sesionesVencidas' ($d.sesionesConRegistro -le $d.sesionesVencidas)
Check 'umbrales provienen del catalogo' ($d.umbralAdvertencia -eq 15 -and $d.umbralPerdida -eq 20)
$inst = $d.idInstitucion
$fac = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(*) AS n FROM dbo.Facultad WHERE institucion = '$inst';")[1]
$prog = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(*) AS n FROM dbo.Programa p INNER JOIN dbo.Facultad f ON f.id = p.facultad WHERE f.institucion = '$inst';")[1]
Check "totalFacultades coincide con consulta de control ($fac)" ($d.totalFacultades -eq $fac)
Check "totalProgramas coincide con consulta de control ($prog)" ($d.totalProgramas -eq $prog)
$regDirecto = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(DISTINCT ep.estudiante) AS n FROM dbo.EstudiantePrograma ep INNER JOIN dbo.Programa p ON p.id = ep.programa INNER JOIN dbo.Facultad f ON f.id = p.facultad INNER JOIN dbo.Estudiante e ON e.id = ep.estudiante INNER JOIN dbo.Usuario u ON u.id = e.usuario WHERE f.institucion = '$inst' AND u.estado = 1;")[1]
Check "estudiantesRegistrados coincide con consulta de control ($regDirecto)" ($d.estudiantesRegistrados -eq $regDirecto)
$faltasDirectas = [int](Invoke-Sql "SET NOCOUNT ON; SELECT COUNT(*) AS n FROM dbo.DetalleAsistencia da INNER JOIN dbo.Asistencia a ON a.id = da.asistencia INNER JOIN dbo.RazonCausa rc ON rc.id = da.razonCausa INNER JOIN dbo.EstudianteGrupo eg ON eg.id = a.estudianteGrupo INNER JOIN dbo.EstadoEstudianteGrupo es ON es.id = eg.estado INNER JOIN dbo.Sesion se ON se.id = a.sesion INNER JOIN dbo.Grupo g ON g.id = eg.grupo INNER JOIN dbo.Asignatura asg ON asg.id = g.asignatura INNER JOIN dbo.SemestrePlanEstudio spe ON spe.id = asg.semestrePlanEstudio INNER JOIN dbo.PlanEstudio pe ON pe.id = spe.planEstudio INNER JOIN dbo.Programa pr ON pr.id = pe.programa INNER JOIN dbo.Facultad fa ON fa.id = pr.facultad WHERE fa.institucion = '$inst' AND es.codigo = 'A' AND da.asistio = 0 AND rc.codigo = 'SJC' AND se.procedenciaTemporal IS NOT NULL AND se.fechaHoraFin <= SYSUTCDATETIME() AND g.periodoAcademico = '$($d.idPeriodoAcademico)';")[1]
Check "faltas SJC coinciden con consulta de control sobre tablas ($faltasDirectas)" ($d.faltasSinJustaCausa -eq $faltasDirectas)

# --- T-E2E-04: comparativa ---
$c = Invoke-Api '/admin/comparativa-facultades' $ta
Check 'comparativa 200' ($c.Status -eq 200)
$filas = @($c.Json.datos)
Check "comparativa: una fila por facultad de la institucion ($fac)" ($filas.Count -eq $fac) "filas=$($filas.Count)"
$spC = Invoke-Sql "SET NOCOUNT ON; DECLARE @c UNIQUEIDENTIFIER = NEWID(); EXEC dbo.usp_consultar_comparativa_facultades @idUsuarioEjecutor='$IdAdminUsuario', @idCorrelacion=@c;"
$colsC = $spC[0] -split '\|'
Check 'comparativa: mismo numero de filas que el SP' ($filas.Count -eq ($spC.Count - 1))
if ($filas.Count -gt 0 -and $spC.Count -gt 1) {
    Compare-Columns $colsC $spC[1] $filas[0] @('totalProgramas', 'totalEstudiantes', 'estudiantesRiesgo', 'estudiantesPerdida', 'tasaRetencion', 'sesionesEjecutadas', 'faltasSinJustaCausa', 'porcentajeAusentismo') 'comparativa[0]'
}
Check 'comparativa: suma de estudiantes por facultad >= total de la institucion' ((($filas | Measure-Object -Property totalEstudiantes -Sum).Sum) -ge $d.totalEstudiantes)

# --- T-E2E-05/06: tendencia y cada valor de cada parametro ---
foreach ($g in @('SEMANA', 'MES')) {
    $t = Invoke-Api "/admin/tendencia-ausentismo?granularidad=$g" $ta
    Check "tendencia granularidad=$g => 200" ($t.Status -eq 200) "status=$($t.Status)"
    $pts = @($t.Json.datos)
    if ($pts.Count -gt 0) {
        $sumSes = ($pts | Measure-Object -Property sesionesEjecutadas -Sum).Sum
        $sumFal = ($pts | Measure-Object -Property faltasSinJustaCausa -Sum).Sum
        Check "tendencia ${g}: la suma de sesiones coincide con el KPI ($($d.sesionesEjecutadas))" ($sumSes -eq $d.sesionesEjecutadas) "suma=$sumSes"
        Check "tendencia ${g}: la suma de faltas coincide con el KPI ($($d.faltasSinJustaCausa))" ($sumFal -eq $d.faltasSinJustaCausa) "suma=$sumFal"
        $ordenados = @($pts | Sort-Object inicioIntervalo)
        Check "tendencia ${g}: orden cronologico ascendente" ((($pts | ForEach-Object { $_.inicioIntervalo }) -join ',') -eq (($ordenados | ForEach-Object { $_.inicioIntervalo }) -join ','))
    } else { Skip "tendencia ${g}: conciliacion de sumas y orden" 'no hay sesiones ejecutadas en el periodo vigente de la semilla (cubierto por el test DB con fixture)' }
}
$def = Invoke-Api '/admin/tendencia-ausentismo' $ta
$sem = Invoke-Api '/admin/tendencia-ausentismo?granularidad=SEMANA' $ta
Check 'tendencia sin granularidad = SEMANA' (@($def.Json.datos).Count -eq @($sem.Json.datos).Count -and $def.Status -eq 200)
Check 'tendencia granularidad invalida => 400' ((Invoke-Api '/admin/tendencia-ausentismo?granularidad=ANIO' $ta).Status -eq 400)
foreach ($r in $rutas) {
    $sep = if ($r.Contains('?')) { '&' } else { '?' }
    Check "idPeriodoAcademico vigente => 200 en $r" ((Invoke-Api "$r${sep}idPeriodoAcademico=$($d.idPeriodoAcademico)" $ta).Status -eq 200)
    Check "idPeriodoAcademico inexistente => 404 en $r" ((Invoke-Api "$r${sep}idPeriodoAcademico=$([guid]::NewGuid())" $ta).Status -eq 404)
}

# --- T-E2E-07: administrador sin fila en Administrador ---
Skip 'usuario administrador sin fila en Administrador => 403' 'la semilla no tiene un usuario con rol ADMINISTRADOR sin fila en Administrador (cubierto por el test DB: KPI_004)'
# --- T-E2E-08: SSE ---
Skip 'SSE de asistencias para el administrador' 'el stream /realtime/stream exige grupoId y titularidad docente: no hay canal institucional (ver VALIDATION)'

Write-Host "Omitidos: $skips"
if ($failures -gt 0) { Write-Host "E2E FALLO: $failures verificaciones" -ForegroundColor Red; exit 1 }
Write-Host 'E2E OK' -ForegroundColor Green

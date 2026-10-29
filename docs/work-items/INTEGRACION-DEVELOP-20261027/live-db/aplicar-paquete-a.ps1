<#
.SYNOPSIS
    Paquete A (HU051 + HU053-HU055) sobre la base VIVA del laboratorio: inventario, respaldo verificado, aplicacion
    aditiva y verificacion. Lo ejecuta el USUARIO con el login sa del contenedor; el agente no tiene esa credencial.

.DESCRIPTION
    Fases (-Fase):
      Inventario  SOLO LECTURA: 00_inventario_solo_lectura.sql -> archivo con marca de tiempo (linea base o post-cambio).
      Respaldo    BACKUP DATABASE ... COPY_ONLY, COMPRESSION, CHECKSUM dentro del contenedor, RESTORE VERIFYONLY (NO restaura),
                  copia al host y SHA-256. Escribe respaldo-verificado.json.
      Aplicar     Solo si respaldo-verificado.json es de las ultimas 24 h y el archivo existe. Ejecuta, en orden y con
                  `sqlcmd -I -b`, EXCLUSIVAMENTE los 15 archivos del paquete A (las 5 FK de la tabla nueva salen de 01_fk_solicitud_ingreso_grupo.sql, no de zz_foreign_keys.sql). Todos son idempotentes
                  (IF NOT EXISTS / CREATE OR ALTER / MERGE solo de codigos nuevos). Se detiene en el primer error.
      Verificar   Inventario posterior + comparacion de conteos contra el ultimo inventario previo.

    NO ejecuta deploy_schema.ps1 (re-sembraria usuarios, titularidades y catalogos del laboratorio), NO restaura, NO borra,
    NO toca Keycloak ni el PDP. Pide la contrasena de sa con Read-Host -AsSecureString: no se guarda ni se imprime.

.EXAMPLE
    .\aplicar-paquete-a.ps1 -Fase Inventario
    .\aplicar-paquete-a.ps1 -Fase Respaldo
    .\aplicar-paquete-a.ps1 -Fase Aplicar
    .\aplicar-paquete-a.ps1 -Fase Verificar
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('Inventario', 'Respaldo', 'Aplicar', 'Verificar')][string]$Fase,
    [string]$Container = 'sql_server_asistencias',
    [string]$Database = 'gestionasistenciadb',
    [string]$DbRepo = 'C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db',
    [string]$Salida = 'C:\Users\josev\AsistenciasUCO-backups-20261027\sql'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $Salida | Out-Null

# Contenedor y base protegidos: solo el contenedor de laboratorio habitual y la base habitual.
if ($Container -ne 'sql_server_asistencias') { throw "Contenedor no previsto: $Container" }
if ($Database -ne 'gestionasistenciadb') { throw "Base no prevista: $Database" }
if (-not (docker ps --filter "name=^$Container$" --format '{{.Names}}')) { throw "El contenedor $Container no esta en ejecucion." }

$secure = Read-Host -Prompt "Contrasena de sa del contenedor $Container" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:SQLCMDPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }

function Invoke-Sql([string[]]$Argumentos) {
    $salida = docker exec -e SQLCMDPASSWORD $Container /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -I @Argumentos 2>&1
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd fallo ($LASTEXITCODE): $($salida -join [Environment]::NewLine)" }
    return $salida
}

# Archivos del paquete A (orden de dependencia). Rutas relativas a $DbRepo; todos aditivos e idempotentes.
$paqueteA = @(
    'schema\tables\SolicitudIngresoGrupo.sql',
    'LOCAL:01_fk_solicitud_ingreso_grupo.sql',
    'schema\seed\21_mensajes_retiro_estudiante_grupo.sql',
    'schema\seed\22_estados_solicitud_ingreso_grupo.sql',
    'schema\seed\23_mensajes_solicitud_ingreso_grupo.sql',
    'schema\stored-procedures\usp_retirar_estudiante_grupo.sql',
    'schema\stored-procedures\usp_consultar_destinatarios_retiro_estudiante_grupo.sql',
    'schema\stored-procedures\usp_registrar_solicitud_ingreso_grupo.sql',
    'schema\stored-procedures\usp_resolver_solicitud_ingreso_grupo.sql',
    'schema\stored-procedures\usp_retirar_solicitud_ingreso_grupo.sql',
    'schema\stored-procedures\usp_consultar_solicitudes_ingreso_estudiante.sql',
    'schema\stored-procedures\usp_consultar_solicitudes_ingreso_grupo_docente.sql',
    'schema\stored-procedures\usp_consultar_destinatarios_solicitud_ingreso_grupo.sql',
    'schema\security\rol_asistencias_runtime_hu051.sql',
    'schema\security\rol_asistencias_runtime_hu053_hu055.sql'
)

function Invoke-Inventario([string]$Etiqueta) {
    $sql = Join-Path $PSScriptRoot '00_inventario_solo_lectura.sql'
    docker cp $sql "${Container}:/tmp/00_inventario_solo_lectura.sql" | Out-Null
    $texto = Invoke-Sql @('-b', '-W', '-s', '|', '-i', '/tmp/00_inventario_solo_lectura.sql')
    $archivo = Join-Path $Salida ("inventario_{0}_{1}.txt" -f $Etiqueta, (Get-Date -Format 'yyyyMMdd_HHmmss'))
    $texto | Set-Content -Path $archivo -Encoding UTF8
    Write-Host "Inventario guardado en $archivo" -ForegroundColor Green
    return $archivo
}

try {
    switch ($Fase) {
    'Inventario' { Invoke-Inventario 'previo' | Out-Null }

    'Respaldo' {
        $marca = Get-Date -Format 'yyyyMMdd_HHmmss'
        $bak = "/var/opt/mssql/backup/{0}_pre_paqueteA_{1}.bak" -f $Database, $marca
        Invoke-Sql @('-Q', "BACKUP DATABASE [$Database] TO DISK = N'$bak' WITH COPY_ONLY, COMPRESSION, CHECKSUM, INIT, STATS = 25") | Out-Null
        Invoke-Sql @('-Q', "RESTORE VERIFYONLY FROM DISK = N'$bak' WITH CHECKSUM") | Out-Null   # verificacion: NO restaura nada
        $local = Join-Path $Salida (Split-Path $bak -Leaf)
        docker cp "${Container}:$bak" $local | Out-Null
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $local).Hash
        $info = Get-Item -LiteralPath $local
        [pscustomobject]@{ archivo = $local; bytes = $info.Length; sha256 = $hash; verificado = $true; momento = (Get-Date).ToString('o') } |
            ConvertTo-Json | Set-Content -Path (Join-Path $Salida 'respaldo-verificado.json') -Encoding UTF8
        Write-Host "Respaldo verificado: $local ($($info.Length) bytes) SHA-256 $hash" -ForegroundColor Green
        Write-Host 'Es solo una salvaguarda: NO se ejecuta RESTORE.' -ForegroundColor Yellow
    }

    'Aplicar' {
        $control = Join-Path $Salida 'respaldo-verificado.json'
        if (-not (Test-Path $control)) { throw 'Falta respaldo-verificado.json: ejecute -Fase Respaldo primero.' }
        $r = Get-Content $control -Raw | ConvertFrom-Json
        if (-not $r.verificado -or -not (Test-Path $r.archivo)) { throw 'El respaldo no esta verificado o el archivo no existe.' }
        if (((Get-Date) - [datetime]$r.momento).TotalHours -gt 24) { throw 'El respaldo verificado tiene mas de 24 h; genere uno nuevo.' }
        Invoke-Inventario 'previo-aplicar' | Out-Null
        Invoke-Sql @('-Q', 'IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N''' + $Database + ''') THROW 50000, ''Base inexistente'', 1;') | Out-Null
        foreach ($rel in $paqueteA) {
            $ruta = if ($rel.StartsWith('LOCAL:')) { Join-Path $PSScriptRoot $rel.Substring(6) } else { Join-Path $DbRepo $rel }
            if (-not (Test-Path $ruta)) { throw "No existe $ruta (verifique que el repo DB este actualizado frente a develop 48aafef1)." }
            $nombre = Split-Path $ruta -Leaf
            docker cp $ruta "${Container}:/tmp/$nombre" | Out-Null
            Write-Host "Aplicando $rel" -ForegroundColor Cyan
            Invoke-Sql @('-b', '-d', $Database, '-i', "/tmp/$nombre") | Out-Null
        }
        Write-Host 'Paquete A aplicado. Ejecute -Fase Verificar.' -ForegroundColor Green
    }

    'Verificar' {
        $post = Invoke-Inventario 'posterior'
        $previo = Get-ChildItem $Salida -Filter 'inventario_previo*.txt' | Sort-Object LastWriteTime | Select-Object -Last 1
        if ($previo) {
            Write-Host "Compare manualmente las secciones '3. Conteo' y '4. Preservacion' entre:`n  $($previo.FullName)`n  $post" -ForegroundColor Yellow
        }
    }
    }
} finally {
    # Limpieza garantizada incluso si Inventario/Respaldo/Aplicar/Verificar falla.
    Remove-Item Env:SQLCMDPASSWORD -ErrorAction SilentlyContinue
}

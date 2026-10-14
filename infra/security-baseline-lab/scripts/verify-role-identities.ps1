<#
.SYNOPSIS
    SEC-BASELINE-003 - verificación de SOLO LECTURA (login asistencias_e2e_readonly, DB E2E aislada) de las
    identidades académicas EXISTENTES que la matriz de roles asocia en el Keycloak del laboratorio.

.DESCRIPTION
    No inventa identidades. Fuente de cada idUsuario:
      * DOCENTE / COORDINADOR / ESTUDIANTE: identidades E2E institucionales ya existentes
        (E2E_*_ID_USUARIO de infra\keycloak\.env del checkout estable, solo lectura).
      * ADMINISTRADOR / DECANO: la ÚNICA fila de dbo.uv_administrador / dbo.uv_decano de la copia (la
        identidad E2E institucional del administrador no existe en la DB; se registra como hallazgo).
      * DOCENTE FIX-PAG-300/1000/5000: fixture DEV FIX-PAG (MAINT-003L), igual que SEC-BASELINE-002.
    Cada identidad debe existir, estar activa y tener exactamente una fila en la vista de su rol; si no,
    verified=false y provision_role_matrix_e2e.py se niega a asociarla. Escribe
    %USERPROFILE%\.asistencias-security-lab\role-identity-verification.json (UUIDs y conteos, sin datos
    personales).
#>
[CmdletBinding()]
param(
    [string]$InstitutionalEnv = (Join-Path $env:USERPROFILE 'AsistenciasUCO\AsistenciasUCO\infra\keycloak\.env'),
    [string]$OutFile = (Join-Path $env:USERPROFILE '.asistencias-security-lab\role-identity-verification.json')
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'E2eSql.psm1') -Force
$s = Get-E2eSqlSettings
$secrets = Import-KeyValueFile $s.SecretsPath
$inst = Import-KeyValueFile $InstitutionalEnv
$c = New-E2eConnection -Target E2E -Database $s.Database -User asistencias_e2e_readonly `
    -Password $secrets['E2E_SQL_READONLY_PASSWORD'] -AppName 'SEC-BASELINE-003-role-verification'
try {
    Assert-E2eTarget $c
    function One([string]$sql, [hashtable]$p = @{}) { $t = Invoke-E2eSql $c $sql $p; if ($t.Rows.Count -eq 0) { return $null }; return $t.Rows[0] }

    $unique = @{
        ADMINISTRADOR = One "SELECT COUNT(*) n, MIN(CAST(idUsuario AS char(36))) id FROM dbo.uv_administrador"
        DECANO        = One "SELECT COUNT(*) n, MIN(CAST(idUsuario AS char(36))) id FROM dbo.uv_decano"
    }
    $views = @{ ADMINISTRADOR = 'uv_administrador'; DECANO = 'uv_decano'; COORDINADOR = 'uv_coordinador'; DOCENTE = 'uv_docente'; ESTUDIANTE = 'uv_estudiante' }
    $active = @{ ADMINISTRADOR = 'estaActivoAdministrador'; DECANO = 'estaActivoDecano'; COORDINADOR = 'estaActivoCoordinador'; DOCENTE = 'estaActivoDocente'; ESTUDIANTE = 'estaActivoEstudiante' }
    $plan = @(
        @{ user = 'lab-e2e-administrador'; role = 'ADMINISTRADOR'; source = 'unica fila dbo.uv_administrador'; id = $(if ($unique.ADMINISTRADOR.n -eq 1) { $unique.ADMINISTRADOR.id } else { $null }) },
        @{ user = 'lab-e2e-decano'; role = 'DECANO'; source = 'unica fila dbo.uv_decano'; id = $(if ($unique.DECANO.n -eq 1) { $unique.DECANO.id } else { $null }) },
        @{ user = 'lab-e2e-coordinador'; role = 'COORDINADOR'; source = 'E2E_COORDINADOR_ID_USUARIO'; id = $inst['E2E_COORDINADOR_ID_USUARIO'] },
        @{ user = 'lab-e2e-docente-seed'; role = 'DOCENTE'; source = 'E2E_DOCENTE_ID_USUARIO'; id = $inst['E2E_DOCENTE_ID_USUARIO'] },
        @{ user = 'lab-e2e-estudiante'; role = 'ESTUDIANTE'; source = 'E2E_ESTUDIANTE_ID_USUARIO'; id = $inst['E2E_ESTUDIANTE_ID_USUARIO'] },
        @{ user = 'lab-e2e-docente-fixpag1000'; role = 'DOCENTE'; source = 'fixture FIX-PAG-1000'; id = 'de000003-0000-4000-8000-000000000002' },
        @{ user = 'lab-e2e-docente-fixpag5000'; role = 'DOCENTE'; source = 'fixture FIX-PAG-5000'; id = 'de000003-0000-4000-8000-000000000003' },
        @{ user = 'lab-e2e-revocable'; role = 'DOCENTE'; source = 'fixture FIX-PAG-300 (cuenta dedicada a revocación)'; id = 'de000003-0000-4000-8000-000000000001' },
        @{ user = 'lab-e2e-sin-rol'; role = 'DOCENTE'; source = 'E2E_DOCENTE_ID_USUARIO (sin client role en el token)'; id = $inst['E2E_DOCENTE_ID_USUARIO'] }
    )
    $identities = @()
    foreach ($item in $plan) {
        $row = [ordered]@{ username = $item.user; role = $item.role; source = $item.source; idUsuario = $null; verified = $false; checks = [ordered]@{} }
        if ($item.id) {
            $id = ([guid]$item.id).ToString()
            $row.idUsuario = $id
            $u = One "SELECT COUNT(*) n, MAX(CAST(estaActivoUsuario AS int)) activo FROM dbo.uv_usuario WHERE id=@id" @{ '@id' = [guid]$id }
            $r = One "SELECT COUNT(DISTINCT id) n, MAX(CAST($($active[$item.role]) AS int)) activo FROM dbo.$($views[$item.role]) WHERE idUsuario=@id" @{ '@id' = [guid]$id }
            $row.checks.usuarioExiste = ($u.n -eq 1)
            $row.checks.usuarioActivo = ($u.activo -eq 1)
            $row.checks.unaFilaDelRol = ($r.n -eq 1)
            $row.checks.rolActivo = ($r.activo -eq 1)
            $row.verified = -not ($row.checks.Values -contains $false)
        } else {
            $row.checks.idDisponible = $false
        }
        $identities += $row
    }
    $instAdmin = $inst['E2E_ADMIN_ID_USUARIO']
    $instAdminExists = (One "SELECT COUNT(*) n FROM dbo.uv_usuario WHERE id=@id" @{ '@id' = [guid]$instAdmin }).n -gt 0

    # Contexto académico para la matriz (identificadores, sin datos personales).
    $docSeed = [guid]$inst['E2E_DOCENTE_ID_USUARIO']; $estSeed = [guid]$inst['E2E_ESTUDIANTE_ID_USUARIO']
    $grupoSeed = One "SELECT COUNT(DISTINCT g.id) n, MIN(CAST(g.id AS char(36))) id FROM dbo.uv_grupo g JOIN dbo.uv_docente d ON d.id = g.idDocente WHERE d.idUsuario=@u" @{ '@u' = $docSeed }
    $est = One "SELECT COUNT(DISTINCT e.id) n, MIN(CAST(e.id AS char(36))) id FROM dbo.uv_estudiante e WHERE e.idUsuario=@u" @{ '@u' = $estSeed }
    $mat = One "SELECT COUNT(*) n, MIN(CAST(eg.idGrupo AS char(36))) grupo, MIN(eg.codigoEstadoEstudiante) estado FROM dbo.uv_estudiante_grupo eg WHERE eg.idEstudiante=@e" @{ '@e' = [guid]$est.id }
    $fix = Invoke-E2eSql $c "SELECT CAST(g.id AS char(36)) id, g.nombre, (SELECT COUNT(*) FROM dbo.uv_estudiante_grupo eg WHERE eg.idGrupo=g.id) matriculas FROM dbo.uv_grupo g WHERE g.nombre LIKE 'FIX-PAG-%' ORDER BY g.nombre"
    $context = [ordered]@{
        grupoDocenteSeed = $(if ($grupoSeed.n -eq 1) { $grupoSeed.id.ToLower() } else { $null })
        estudianteSeedId = $(if ($est.n -eq 1) { $est.id.ToLower() } else { $null })
        estudianteSeedMatriculas = $mat.n
        estudianteSeedGrupo = $(if ($mat.n -eq 1) { $mat.grupo.ToLower() } else { $null })
        estudianteSeedEstado = $(if ($mat.n -eq 1) { $mat.estado } else { $null })
        fixPag = @($fix | ForEach-Object { [ordered]@{ grupo = $_.id.ToLower(); nombre = $_.nombre; matriculas = [int]$_.matriculas } })
    }
    $context.estudianteEnGrupoDelDocenteSeed = ($context.estudianteSeedGrupo -eq $context.grupoDocenteSeed)
    $evidence = [ordered]@{
        workItem = 'SEC-BASELINE-003'; verifiedAt = (Get-Date).ToUniversalTime().ToString('o')
        source = "$($s.Host)/$($s.Database) (login asistencias_e2e_readonly, solo SELECT)"
        identities = $identities; context = $context
        findings = @([ordered]@{ id = 'E2E_ADMIN_ID_USUARIO'; existsInDb = $instAdminExists
            note = 'idUsuario del administrador E2E institucional (8081) frente a la copia de la DB' })
    }
    $evidence | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $OutFile -Encoding utf8
    foreach ($i in $identities) { "{0,-28} {1,-13} verified={2} {3}" -f $i.username, $i.role, $i.verified, (($i.checks.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ',') }
    "contexto: " + ($context | ConvertTo-Json -Compress -Depth 4)
    "E2E_ADMIN_ID_USUARIO existe en la DB: $instAdminExists"
    "Evidencia: $OutFile"
    if (@($identities | Where-Object { -not $_.verified }).Count -gt 0) { exit 1 }
} finally { $c.Close() }

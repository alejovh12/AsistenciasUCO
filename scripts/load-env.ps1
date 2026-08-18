param(
    [string]$Path = (Join-Path (Split-Path -Parent $PSScriptRoot) ".env")
)

if (-not (Test-Path -LiteralPath $Path)) {
    throw "No existe el archivo .env en la ruta esperada: $Path"
}

Get-Content -LiteralPath $Path | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -le 0) {
        return
    }

    $name = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1)
    if ([string]::IsNullOrWhiteSpace($name)) {
        return
    }

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

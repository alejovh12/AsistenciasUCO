$ErrorActionPreference = "Stop"

if (-not (Get-Command java -ErrorAction SilentlyContinue) -or
    -not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "Se requiere JDK 25 para los integration tests. Configura JAVA_HOME a JDK 25 y antepone su directorio bin al PATH de esta sesion."
}

$javaVersion = (& java --version | Select-Object -First 1).ToString()
$javacVersion = (& javac --version | Select-Object -First 1).ToString()
if ($javaVersion -notmatch '^(openjdk|java) 25([.+_-]\S*)?(\s|$)' -or
    $javacVersion -notmatch '^javac 25([.+_-]\S*)?(\s|$)') {
    throw "Se requiere JDK 25 para los integration tests. Configura JAVA_HOME a JDK 25 y antepone su directorio bin al PATH de esta sesion."
}

. "$PSScriptRoot\load-env.ps1"

$requiredVariables = @(
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "APP_DATABASE_EXPECTED_NAME"
)

foreach ($variable in $requiredVariables) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($variable, "Process"))) {
        throw "Falta la variable obligatoria $variable. Completa el archivo .env local sin versionarlo."
    }
}

.\mvnw.cmd verify -Pintegration

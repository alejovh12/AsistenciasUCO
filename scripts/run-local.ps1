$ErrorActionPreference = "Stop"

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

.\mvnw.cmd spring-boot:run

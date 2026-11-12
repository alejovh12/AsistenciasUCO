# scratch — scripts E2E de PowerShell

Scripts de apoyo para validar HU contra un backend, Keycloak y SQL Server locales. No traen contraseñas: se leen del entorno y, si falta alguna, el script falla indicando cuál.

| Variable | Scripts | Uso |
|---|---|---|
| `MSSQL_SA_PASSWORD` | todos | Contraseña del usuario `sa` del contenedor SQL Server. |
| `E2E_USER_PASSWORD` | todos | Contraseña de las cuentas de prueba sembradas (coordinador, decano, administrador). |
| `E2E_DOCENTE_PASSWORD` | `test_hu091.ps1`, `test_hu092.ps1`, `test_hu093.ps1` | Contraseña con la que el script crea el docente de prueba. |

Ejemplo (PowerShell), sin escribir los valores en el repo:

```powershell
$env:MSSQL_SA_PASSWORD = Read-Host 'SA'
$env:E2E_USER_PASSWORD = Read-Host 'Usuarios de prueba'
```

Los scripts Python de `infra/security-baseline/scripts` no usan contraseñas literales: reciben `--sql-password-file` y generan secretos aleatorios en memoria.

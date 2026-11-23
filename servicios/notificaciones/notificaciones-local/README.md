# Kit de laboratorio — Servicio de Notificaciones UCO

Levanta el servicio como contenedor `ext-notificaciones-servicio` en la red `asis-red`, con su BD `notificacionesdb` en el SQL Server
del laboratorio (`sql_server_asistencias`, alias `sqlserver`).

## Pasos

1. **SQL Server arriba** (`.\AsistenciasUCO-Infra\infra.ps1 stack up minimo` o cualquier escenario).
2. **Imagen**: `docker build -t notificaciones-uco:0.1.1 ..\notificaciones-uco` (o `docker compose build`).
3. **`.env`**: `Copy-Item .env.example .env` y reemplazar los valores `<...>`. Los del SMTP del laboratorio salen de los `.env` que ya
   existen en Infra/Backend (`SMTP_NOTIFICATION_*`); no los copie a ningún otro lugar.
4. **Crear la BD y el login** (una vez, o al recrear el volumen de SQL Server):
   `pwsh scripts/crear-bd.ps1` (usa `MSSQL_SA_PASSWORD` y `NOTIFICACIONES_DB_PASSWORD` de la sesión o de `.env`; las claves no se
   imprimen ni se pasan como argumento). `-Eliminar -Yes` borra la base y el login.
5. **Levantar**: `docker compose up -d`. Flyway crea las tablas y siembra los 22 tipos y sus plantillas de fábrica al primer arranque.
6. **Probar**:

```powershell
$k = @{ 'X-Api-Key' = '<NOTIFICACIONES_API_KEY>' }
Invoke-RestMethod http://localhost:18090/actuator/health                       # abierta
Invoke-RestMethod http://localhost:18090/api/v1/estado-canal -Headers $k
Invoke-RestMethod http://localhost:18090/api/v1/plantillas -Headers $k | Select-Object tipo, versionActiva
Invoke-RestMethod -Method Post http://localhost:18090/api/v1/correos-prueba -Headers $k -ContentType 'application/json' `
  -Body '{"tipo":"RETIRO_ESTUDIANTE_GRUPO","correoDestino":"<buzon-de-prueba@dominio>"}'
```

Con `SMTP_NOTIFICATION_TEST_MODE_ENABLED=true` todo correo se redirige a `SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO` y el asunto lleva
`[PRUEBA para ...]`. Con `NOTIFICACIONES_CANAL=log` el servicio no envía nada (solo registra cuántos destinatarios habría).

## Puertos y nombres

| Qué | Valor |
|---|---|
| Contenedor | `ext-notificaciones-servicio` |
| Puerto en el PC (solo loopback) | `127.0.0.1:18090` → `8080` |
| URL entre contenedores | `http://ext-notificaciones-servicio:8080` |
| Imagen | `notificaciones-uco:0.1.1` |
| Memoria | límite 450 MB (≈ 300 MB en reposo) |
| Salud del contenedor | `GET /actuator/health/readiness` (BD lista; el SMTP no cuenta) |

## Qué debe contener la sección `ext-notificaciones` del stack (fase F3, Infra)

Ver el informe de la fase F2: directorio `Complementos/Notificaciones/notificaciones-local`, compose `compose.yaml`
(+ overlay `@stack/secciones/ext-notificaciones.yml` solo con `container_name`/labels si hace falta), `envFiles: [".env"]`,
servicio `servicio`, salud `contenedor:ext-notificaciones-servicio` y `http:http://localhost:18090/actuator/health`,
`requiereImagenes: ["notificaciones-uco:0.1.1"]`, `requiere: ["asis-datos"]`, `memoriaMb: 450`.

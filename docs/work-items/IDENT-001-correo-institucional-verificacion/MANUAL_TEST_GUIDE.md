# IDENT-001 — Guía de pruebas manuales (entorno local)

> Solo para desarrollo local. Las credenciales de abajo son las canónicas de desarrollo de `AGENTS.md` §7.

## 1. Servicios y puertos

| Servicio | URL | Nota |
| :--- | :--- | :--- |
| Frontend (Angular `ng serve`) | http://localhost:4200 | Rama `feature/IDENT-001-correo-institucional-verificacion` |
| Backend (Spring Boot) | http://localhost:8080 | Proveedor de seguridad `legacy` (Keycloak local) |
| Keycloak | http://127.0.0.1:8081 | Realm `asistencias-uco` |
| SQL Server | localhost:1433 | BD `gestionasistenciadb` |

Ajustes locales que **no** se commitean:
- `public/assets/env.js` del frontend con `SECURITY_PROVIDER = 'legacy'` (el valor del repositorio es `baseline`, plataforma central que no existe en este equipo). Está marcado con `git update-index --skip-worktree`.
- Backend: `APP_ADAPTERS_SECURITY_PROVIDER=legacy`, catálogos y notificación por `sqlserver`/`outbox`/`smtp`, `SMTP_NOTIFICATION_PLATFORM_URL=http://localhost:4200`, y el secreto del cliente `asistencias-backend-admin` leído del propio Keycloak al arrancar.

## 2. Correo

- **Modo Gmail (por defecto):** el `.env` activa `SMTP_NOTIFICATION_TEST_MODE_ENABLED=true`, así que **todo** correo se redirige al buzón de pruebas configurado en `SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO`. Si la contraseña de aplicación fue revocada, el envío fallará y se reintentará desde el outbox.
- **Modo sumidero local (sin internet):** `GreenMailSink` en el puerto 3025 vuelca cada correo como `.eml`.

## 3. Casos manuales

Docente: `maria.rostagno@uco.edu.co` / `Test1234!` → Toma de asistencia → «+ Matricular» → «+ Registrar datos de nuevo alumno».

| # | Acción | Resultado esperado |
| :--- | :--- | :--- |
| M-01 | Abrir el formulario de alumno nuevo | Bajo «Correo electrónico institucional» aparece «Debe terminar en @uco.edu.co…» |
| M-02 | Registrar con `x@gmail.com` | Error «El correo del estudiante debe ser institucional y terminar en @uco.edu.co.»; no se llama al backend |
| M-03 | Registrar con `nombre@uco.edu.co` y clave de ≥ 12 caracteres | «Estudiante en Grupo registrado(a) exitosamente»; llega el correo «Verifique su correo institucional» |
| M-04 | Intentar iniciar sesión como ese estudiante antes de verificar | Rechazado (cuenta deshabilitada) |
| M-05 | Abrir el enlace del correo (`/verificar-correo#token=…`) | «Su correo fue verificado. Ya puede iniciar sesión.»; el token desaparece de la barra de direcciones |
| M-06 | Abrir de nuevo el mismo enlace en una pestaña nueva | «El enlace de verificación no es válido o ya venció.» |
| M-07 | «Info & Alumnos» → «Reenviar enlace» dentro de los 5 min del registro | Aviso «Debe esperar unos minutos antes de volver a enviar el enlace de verificación.» |
| M-08 | Igual que M-07 pasados 5 min, sobre un alumno sin verificar | Toast de éxito y correo nuevo; el enlace anterior deja de valer |
| M-09 | Registrar un estudiante que ya existe con otro dominio | Se vincula al grupo sin exigir dominio (la regla solo aplica a cuentas nuevas) |

## 4. Observaciones conocidas (no son defectos de IDENT-001)

1. La contraseña del realm exige **≥ 12 caracteres**; el backend acepta 8. Con menos de 12 el aprovisionamiento en Keycloak falla (500) pero la fila ya quedó creada en BD (usuario huérfano). Ver TD-079 y la alineación pendiente de la política.
2. La cuenta se crea con contraseña *temporal* (`UPDATE_PASSWORD`), de modo que un estudiante verificado no puede iniciar sesión por el flujo directo hasta que se resuelva ese paso (código previo a IDENT-001; ver TD-079).
3. El botón «Reenviar enlace» aparece en todas las filas; para un estudiante ya verificado el backend responde con su mensaje de rechazo.
4. Abrir en la misma pestaña dos enlaces distintos de `/verificar-correo` solo cambia el fragmento y no recarga la SPA; los enlaces del correo se abren en pestaña nueva, por lo que no afecta al flujo real.

# HU177 — recuperar contraseña con enlace de un solo uso

## Decisión

John, 2026-11-11: HU177 se hace con un flujo propio de la aplicación, no con la opción `resetPasswordAllowed` de Keycloak. Sigue el patrón de la verificación de correo (IDENT-001): un enlace de un solo uso que llega por correo, del que la base de datos solo guarda la huella SHA-256.

## Contrato HTTP

Dos rutas públicas nuevas, declaradas una por una (sin comodín `/public/**`):

| Ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /api/v1/public/recuperacion-clave/solicitud` | `{ "correo": "…" }` | Siempre 200 con el mensaje `RCL_001`, exista o no una cuenta con ese correo |
| `POST /api/v1/public/recuperacion-clave/confirmacion` | `{ "token": "…", "clave": "…" }` | 200 con `RCL_003`. 400 `RCL_002` para cualquier enlace que no sirve. 400 con el código de la política (`ERR_PASSWORD_REQUERIDO`, `ERR_PASSWORD_POLITICA_INVALIDA`, `ERR_PASSWORD_LONGITUD_INVALIDA`, `ERR_PASSWORD_IGUAL_IDENTIFICACION`) si la contraseña no la cumple |

Las dos responden 429 cuando una misma IP supera el límite por minuto. Ninguna respuesta incluye datos personales.

## Flujo

1. **Solicitud.** `SolicitarRecuperacionClaveUseCaseImpl` consulta `usp_consultar_solicitud_recuperacion_clave`. Si la base de datos devuelve un usuario (activo, único dueño del correo y fuera de la espera entre enlaces), encola el aviso `RECUPERACION_CLAVE`. Responde lo mismo en todos los casos.
2. **Correo.** Al entregar el aviso, el canal de correo genera el token (32 bytes aleatorios), registra su hash con `usp_emitir_token_recuperacion_clave` y arma el enlace `<plataforma>/restablecer-clave#token=<token>`. El token nunca pasa por el caso de uso ni queda en el outbox.
3. **Confirmación.** `RestablecerClaveUseCaseImpl` consulta el enlace sin consumirlo, valida la contraseña con `PasswordRegistroRule`, ubica la cuenta en el proveedor (primero por el vínculo vigente `ExternalIdentityLinkPort.findSujetosVigentes`; si no hay uno solo, por el atributo institucional), la fija y solo entonces consume el enlace. Si el proveedor falla, el enlace sigue vigente.

## Piezas

| Capa | Archivo | Qué hace |
|---|---|---|
| Application | `features/usuario/recuperarclave/**` | Los dos casos de uso, sus puertos de entrada e interactores |
| Application | `secondaryports/repository/RecuperacionClaveRepositoryPort` | Persistencia del enlace |
| Application | `secondaryports/identity/IdentityProviderPort.establecerClave` | Operación nueva y opcional del proveedor de identidad |
| Application | `secondaryports/notification/Notificacion.TIPO_RECUPERACION_CLAVE` | Tipo de aviso nuevo |
| Infrastructure | `RecuperacionClaveJpaRepository` | Llama a los tres SP; calcula el SHA-256 del token |
| Infrastructure | `KeycloakIdentityProviderAdapter.establecerClave` | Contraseña definitiva (no temporal) y cierre de las sesiones abiertas. `ProfileSyncingIdentityProvider` la delega |
| Infrastructure | `RecuperacionClaveController` y sus dos `request` | Las dos rutas públicas |
| Infrastructure | `RecuperacionClaveWiringConfiguration` | Composición y límite de intentos por IP |
| Infrastructure | `SecurityPublicEndpoints`, `SecurityConfig`, `infra/security-baseline/access-catalog.json` (`exempt`) | Las dos rutas quedan públicas y fuera del PEP |
| Infrastructure | `VerificacionCorreoMarcadorProvider.conEnlace` | El emisor de enlaces de un solo uso arma también el de recuperación |
| Infrastructure | `TokenVerificacionCorreoJpaRepository` | Acepta el SP de emisión por parámetro (misma firma) |
| Infrastructure | `VerificacionCorreoRateLimitFilter` | Acepta un conjunto de rutas; cada registro lleva su propio contador |
| Infrastructure | `PlantillaPorDefectoCatalogo`, `notification/templates/RECUPERACION_CLAVE.es.properties` | Tipo entregable y plantilla por defecto |
| Infrastructure | `AdministracionNotificacionJpaAdapter` | El envío de prueba del administrador no emite un enlace real de recuperación |
| Configuración | `application.yml` | `app.recuperacion-clave.limite-por-minuto` (5) y `app.recuperacion-clave.max-claves` (10000) |

## Reglas de seguridad

- La solicitud no revela si el correo existe.
- El token y la contraseña nunca se escriben en logs ni se guardan: la base de datos solo tiene el hash del token y la contraseña vive en el proveedor de identidad.
- Un enlace sirve una sola vez, vence por parámetro (`RECUPERACION_CLAVE/TTL_MINUTOS`) y se revoca al pedir otro.
- Cualquier enlace que no sirve responde igual (`RCL_002`).
- La contraseña nueva cumple la misma política que el registro: 8 a 255 caracteres, mayúscula, minúscula, número y carácter especial, sin espacios y distinta del número de identificación.
- Al cambiar la contraseña se cierran las sesiones abiertas de la cuenta.
- Límite por IP en las dos rutas (5 por minuto por defecto), más la espera entre enlaces en la base de datos.

## Cambios en componentes de otros equipos

Los dos son aditivos y no cambian el comportamiento existente:

- **Notificaciones (Sergio):** tipo y plantilla nuevos; `VerificacionCorreoMarcadorProvider` y `TokenVerificacionCorreoJpaRepository` se generalizaron para emitir un segundo enlace; el filtro de límite acepta más rutas.
- **Identidad (José Alejandro):** operación nueva `establecerClave` en el puerto y en el adaptador de Keycloak.

## Dependencias y despliegue

- Base de datos: migración `35_hu177_recuperacion_clave_up.sql`, antes que este backend.
- El envío real del correo depende del canal de notificaciones del ambiente (`app.adapters.notification.provider` y su SMTP). Con el proveedor que solo registra en el log, el correo no sale.
- El aviso solo se entrega a usuarios con el correo confirmado (regla del canal de correo).
- `platformUrl` del canal de correo debe apuntar al frontend, que publica la ruta `/restablecer-clave`.
- Las dos rutas son públicas: no necesitan aprovisionarse en el PDP.

## Rollback

Revertir el commit del backend y ejecutar el `down` de la migración 35.

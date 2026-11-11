# HU177 — validación

## Automatizada

| Comando | Resultado |
|---|---|
| `.\mvnw.cmd -B -o clean verify` (JDK 25), 2026-11-11 | `Tests run: 4372, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `BUILD SUCCESS` |

Pruebas nuevas (38 casos):

| Clase | Qué cubre |
|---|---|
| `SolicitarRecuperacionClaveUseCaseImplTest` | Respuesta uniforme; el aviso solo se encola si hay solicitante; el aviso no lleva el enlace ni el correo |
| `RestablecerClaveUseCaseImplTest` | Orden consultar → fijar la clave → consumir; la cuenta se ubica primero por el vínculo vigente de identidad y, si no hay uno solo, por el atributo institucional; `RCL_002` uniforme; política de contraseñas; el enlace sigue vigente si falla el proveedor de identidad |
| `RecuperacionClaveControllerTest` | Contrato HTTP de las dos rutas; los cuerpos no imprimen correo, token ni clave |
| `RecuperacionClaveJpaRepositoryTest` | El adaptador envía el SHA-256 del token y traduce las filas de los SP |
| `KeycloakIdentityProviderAdapterRecuperacionClaveTest` | Contraseña definitiva (`temporary=false`), cierre de sesiones y fallos controlados |
| `EnlaceRecuperacionClaveMarcadorProviderTest` | El enlace de recuperación usa su ruta y su registro; el de verificación de correo no cambia |
| `RecuperacionClaveRateLimitFilterTest` | Las dos rutas comparten el contador por IP; no limita otras rutas ni otros métodos |

`JpaRepositoryBeanUniquenessTest` pasa de 70 a 71 repositorios. Las pruebas existentes de la verificación de correo, del filtro de límite y del catálogo de acceso siguen pasando sin cambios.

## En pantalla (lab local, 2026-11-11)

Backend de esta rama contra el lab de Security Baseline 0.1.11, BD local con la migración 35 y frontend de la rama homónima. El equipo local no tiene servidor de correo configurado, así que el canal SMTP real del backend (`OUTBOX_NOTIFICATION_SENDER=smtp`) se apuntó a un buzón de pruebas en `127.0.0.1:2525` que guarda los correos en un archivo y no sale a Internet.

| Paso | Resultado |
|---|---|
| Login → "¿Olvidaste tu contraseña?" | Abre `/recuperar-clave` |
| Solicitud con un correo que no existe | `POST …/solicitud` → 200 con el mensaje uniforme; no se envió ningún correo |
| Solicitud con el correo del estudiante de prueba | 200 con el mismo mensaje; el buzón recibió un correo "Defina una contraseña nueva" con el saludo por nombre y el enlace `/restablecer-clave#token=…` |
| Abrir el enlace | La página borra el fragmento de la URL y no muestra el token; el botón se habilita solo cuando se cumplen los seis requisitos |
| Guardar con el enlace vencido (más de 60 minutos) | `POST …/confirmacion` → 400 `RCL_002` y la página ofrece "Pedir un enlace nuevo" |
| Pedir un segundo enlace | Llega un correo nuevo; en la BD el primero queda revocado |
| Guardar la contraseña con el enlace vigente | 200 "Su contraseña fue actualizada. Ya puede iniciar sesión." |
| Iniciar sesión en Keycloak | La contraseña anterior se rechaza; con la nueva entra a la aplicación como ESTUDIANTE |
| Reusar el enlace ya usado | 400 `RCL_002`. En la BD: 2 enlaces, 1 usado, 1 revocado, 0 pendientes |

**Defecto encontrado y corregido en esta revisión:** el primer intento de guardar respondió 500 porque el caso de uso buscaba la cuenta del proveedor solo por el atributo institucional, que las cuentas creadas fuera de la aplicación no tienen. Ahora la ubica primero por el vínculo vigente `issuer + subject`, igual que `AccesoCuentaService`. El enlace siguió vigente tras ese fallo, como está diseñado.

Al terminar se restauró la contraseña del usuario de prueba con `provision_asistencias.py users` y `verify-decisions` quedó limpio (1231 decisiones, 0 fallos).

## Sin validar

- El envío con un servidor de correo real. Depende de los datos SMTP de cada ambiente; el canal usado en la prueba es el mismo.

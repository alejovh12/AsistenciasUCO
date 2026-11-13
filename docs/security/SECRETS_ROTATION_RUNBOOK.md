---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# Runbook de rotación de secretos

Origen: SEC-10 de `informes/LISTA_CHEQUEO_VALIDACION_2026-11-12.md`, work item
[VAL-oleada3-baul-secretos](../work-items/VAL-oleada3-baul-secretos/PLAN.md). Los secretos, sus nombres en Key Vault y
sus dueños están en el [inventario](SECRETS_INVENTORY.md). Este runbook nunca lleva valores: ni en comandos, ni en
tickets, ni en evidencias.

## 1. Calendario

| Secreto | Cada cuánto | Quién rota | Reinicio |
|---|---|---|---|
| Contraseña de BD | 90 días | Dueño de BD | Reinicio escalonado |
| Secretos de cliente de Keycloak (legacy y baseline) | 90 días | Dueño de seguridad | Reinicio escalonado |
| Secreto de cliente del PDP y credencial del PEP | 90 días o la política del PDP | Dueño de seguridad, con Security Baseline | Reinicio escalonado |
| Claves de aplicación de MinIO | 90 días | Dueño de Infra | Reinicio escalonado |
| Contraseña SMTP | 180 días o la política del proveedor | Dueño de notificaciones | Reinicio escalonado |
| Token del webhook de Event Grid | 90 días | Dueño de Azure | Reinicio escalonado |
| API key de festivos | Según el proveedor | Dueño de Azure | No (se lee en caliente) |

Además, **cualquier secreto se rota de inmediato** si aparece en un log, un commit, una captura o un ticket, o si se va
del equipo alguien que lo conocía. El dueño de Azure revisa el calendario al inicio de cada mes; las fechas de
expiración se ponen en el propio secreto de Key Vault (`--expires`) para que Key Vault emita `SecretNearExpiry`.

## 2. Qué pasa en el backend cuando cambia un secreto

1. Se crea una versión nueva del secreto en Key Vault. Los consumidores siempre leen la versión vigente (sin número de
   versión).
2. Key Vault publica `Microsoft.KeyVault.SecretNewVersionCreated` en Event Grid.
3. Event Grid llama al webhook `POST /api/v1/internal/azure-events` con el header `aeg-sas-token`.
   `AzureEventGridWebhookController` pasa el evento a `ProcesarEventoAzureInputPort`; el caso de uso toma el nombre del
   secreto (`data.ObjectName` o el `subject`) y llama a `CatalogInvalidationPort.invalidateSecret`.
4. `CompositeCatalogInvalidationAdapter` invalida esa entrada en la caché de `AzureKeyVaultAdapter` (Caffeine, 50
   entradas, 5 minutos). La lectura siguiente va a Key Vault y trae la versión nueva. Lo prueba
   `RotacionSecretoInvalidacionCacheTest` (con `SecretClient` simulado). `SecretExpired` también invalida.
5. Si el evento no llega, la caché expira sola a los 5 minutos.

Alcance de la invalidación:

- **Secretos en caliente** (API key de festivos): toman la versión nueva tras el paso 4, sin reinicio.
- **Secretos de arranque** (BD, Keycloak, PDP/PEP, MinIO, SMTP, webhook): se leen una vez al iniciar la instancia y
  quedan enlazados al pool de conexiones o a los clientes. La invalidación no los cambia: **hay que reiniciar las
  instancias de forma escalonada** (una réplica a la vez, esperando que `/actuator/health/readiness` responda `UP`).
- La caché es local de cada instancia. Event Grid debe entregar a cada instancia o, si no, basta el reinicio escalonado.
  No hay invalidación distribuida.

## 3. Procedimiento general

Requisitos: `az login` con permiso de escritura de secretos sobre el baúl (rol *Key Vault Secrets Officer*), acceso al
sistema dueño de la credencial y una ventana acordada.

1. **Generar** la credencial nueva en el sistema dueño (BD, Keycloak, PDP, MinIO o proveedor SMTP) **sin retirar la
   anterior**, cuando el sistema permite dos credenciales a la vez.
2. **Guardar** la versión nueva en Key Vault, leyendo el valor desde un archivo temporal y no desde la línea de comandos:
   `az keyvault secret set --vault-name <baul> --name <nombre-del-inventario> --file <archivo-temporal> --expires <fecha>`.
   Borrar el archivo temporal.
3. **Confirmar** en el log del backend que llegó el evento (`Evento Azure recibido: eventType=Microsoft.KeyVault.SecretNewVersionCreated`); el log nunca muestra el valor.
4. **Reiniciar** las instancias de forma escalonada si es un secreto de arranque (tabla 1).
5. **Verificar** el uso real: readiness `UP` (BD), login y alta de usuario (Keycloak/PDP), subida y descarga de un
   soporte (MinIO), un correo de prueba en modo test (SMTP), una entrega de Event Grid aceptada (webhook).
6. **Retirar** la credencial anterior en el sistema dueño.
7. **Registrar** fecha, secreto (nombre), responsable y resultado, sin valores.

Rollback: mientras la credencial anterior no se haya retirado (paso 6), se publica otra vez el valor anterior como
versión nueva, sin mostrarlo: `az keyvault secret download --vault-name <baul> --name <nombre> --version <anterior> --file <archivo-temporal>`,
luego `az keyvault secret set ... --file <archivo-temporal>`, borrar el archivo y reiniciar. No sirve deshabilitar la
versión nueva: una lectura sin versión de un secreto cuya última versión está deshabilitada falla.

## 4. Particularidades

| Secreto | Nota |
|---|---|
| Contraseña de BD | SQL Server no admite dos contraseñas por login: crear un login nuevo con los mismos permisos, guardar usuario y contraseña nuevos en el baúl, reiniciar y luego deshabilitar el login anterior. En local la gestiona el repositorio DB. |
| Keycloak / PDP | Regenerar el secreto del cliente en la consola del realm. Si el realm no tiene rotación con secreto doble, la ventana entre el cambio y el reinicio produce fallos de aprovisionamiento (no de login de usuarios): hacerlo fuera de horario. |
| Credencial del PEP | La emite el PDP para la aplicación y ambiente; coordinar con Security Baseline. Con la credencial vieja inválida, el PEP responde 503 (fail-closed). |
| MinIO | Crear la clave de servicio nueva para la cuenta de aplicación, guardar las dos piezas en el baúl, reiniciar y borrar la clave anterior. Nunca usar las credenciales root. |
| Token del webhook | El filtro acepta un solo token. Orden: guardar el token nuevo en el baúl, reiniciar el backend y actualizar el header de entrega en la suscripción de Event Grid. Mientras tanto Event Grid recibe 401 y reintenta (hasta 24 h por defecto), así que no se pierden eventos; tras actualizar la suscripción, confirmar una entrega aceptada. |
| Local, test y CI | Usan `local_env`: los secretos vienen de `.env` (fuera de git) o de los secretos del CI. Rotar ahí es editar ese valor y reiniciar. |

## 5. Lo que este runbook no cubre

- Contraseñas de usuarios finales y su recuperación: son de Keycloak y Security Baseline.
- La prueba contra un Key Vault real (SEC-11): `.\mvnw.cmd -Pazure-integration verify` con `az login`; sigue pendiente.

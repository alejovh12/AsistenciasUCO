---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Azure Runtime Integration

## 1. Objetivo

Documentar el runtime Azure AS-IS de AsistenciasUCO sin convertir configuración desplegada ni valores externos en source of truth del repositorio. Esta norma gobierna Key Vault, App Configuration, Event Grid, caches locales, invalidación y evidencia operacional.

## 2. Alcance

Incluye capabilities, adapters, Composition Root, autenticación técnica, convenciones de claves, failure behavior, testing, seguridad y procedimiento de cambio. No crea IaC/CD, no certifica recursos desplegados, no modifica contratos HTTP/Golden Path y no autoriza operar Azure sin work item.

## 3. Diagrama lógico

```text
Application
   |
   +-- SecretVaultPort
   |      |
   |      +-- AzureKeyVaultAdapter
   |
   +-- ParameterCatalogPort
   |      |
   |      +-- AzureAppConfigParameterCatalogAdapter
   |
   +-- MessageCatalogPort
          |
          +-- AzureAppConfigMessageCatalogAdapter

Azure Event Grid
   |
   v
AzureEventGridWebhookController
   |
   v
ProcesarEventoAzureInputPort
   |
   v
ProcesarEventoAzureUseCase
   |
   v
CatalogInvalidationPort
   |
   +-- invalidateSecret
   +-- invalidateParameter
   +-- invalidateMessage
   +-- invalidateAll
```

Azure es un conjunto de providers/resources. La capability pertenece a la aplicación y se expresa mediante Ports neutrales.

## 4. Capability matrix

| Capability | Port/SPI | Provider | Adapter | Selector |
|---|---|---|---|---|
| Secret Vault | `SecretVaultPort` | Azure Key Vault | `AzureKeyVaultAdapter` | `app.adapters.vault.provider=azure_keyvault` |
| Parameter Catalog | `ParameterCatalogPort` | Azure App Configuration | `AzureAppConfigParameterCatalogAdapter` | `app.adapters.parameter-catalog.provider=azure_appconfig` |
| Message Catalog | `MessageCatalogPort` | Azure App Configuration | `AzureAppConfigMessageCatalogAdapter` | `app.adapters.message-catalog.provider=azure` |
| Cache invalidation | `CatalogInvalidationPort` | Event Grid input + composite local adapter | `CompositeCatalogInvalidationAdapter` | wiring de invalidación |

Alternativas actuales: `LocalEnvSecretVaultAdapter` para vault local, y adapters SQL Server para catálogos de parámetros/mensajes. La selección vive en Composition Root.

## 5. Authentication

Los clientes de Key Vault y App Configuration se construyen con `DefaultAzureCredential` dentro de Infrastructure. En desarrollo puede resolver una identidad de herramienta local como Azure CLI; en ambientes administrados debe usar la identidad autorizada correspondiente. Application no conoce la cadena de credenciales.

La autenticación del webhook Event Grid es una frontera distinta. No es Bearer JWT de la API de negocio y no reutiliza RBAC/claims institucionales.

## 6. Key Vault

Key Vault contiene conceptualmente secretos de infraestructura y configuración sensible requerida por runtime. Se documentan tipos y, solo cuando ya son contrato operacional público, nombres; nunca valores.

`AzureKeyVaultAdapter`:

- recibe `SecretClient` desde Composition Root;
- retorna vacío para nombre inválido o recurso no encontrado;
- lanza `SecretVaultException` ante fallos técnicos;
- invalida la entrada local ante respuestas 401/403 antes de propagar el fallo;
- soporta invalidación específica y total.

## 7. App Configuration — Parameters

App Configuration contiene parámetros funcionales/configurables y claves de configuración no secretas. La convención efectiva del adapter es `grupo:clave`, construida desde ambos argumentos. Ejemplo público permitido: `asistencias:asistencia:tolerancia-minutos`, resultante de grupo `asistencias:asistencia` y clave `tolerancia-minutos`.

Argumentos nulos/vacíos retornan vacío. Una clave inexistente retorna vacío; otros fallos del provider se traducen a `ParameterCatalogException`. La conversión tipada soporta los tipos básicos declarados por el Port y falla explícitamente para tipos no soportados.

## 8. App Configuration — Messages

Convención AS-IS:

- usuario: `messages:user:<CODIGO>`, primero label `es`; si no existe, fallback sin label;
- técnico: `messages:technical:<CODIGO>`, sin label.

Ausencia de mensaje produce fallback al código canónico en `getUserMessage`/`getTechnicalMessage`. Los placeholders se resuelven según el comportamiento del adapter (`{}` secuencial o `MessageFormat`). No se registran mensajes privados ni valores desplegados en esta norma.

## 9. Event Grid

Primary Adapter: `AzureEventGridWebhookController`.

```text
POST /api/v1/internal/azure-events
```

El controller soporta handshake de suscripción y lotes de eventos regulares. Delega cada evento a `ProcesarEventoAzureInputPort`; no llama adapters Azure secundarios.

El caso de uso procesa familias:

- `Microsoft.AppConfiguration.*`;
- `Microsoft.KeyVault.*`.

App Configuration invalida mensaje, parámetro o todo según la clave disponible. Key Vault invalida el secreto identificado. Algunos cambios de mensaje de usuario o parámetro publican hoy `MESSAGE_UPDATED` o `PARAMETER_UPDATED` mediante `RealtimePublisherPort`; su semántica futura está en DR-AZ-001 y no se decide aquí.

## 10. Cache policy

| Adapter | Cache | Maximum size | TTL after write |
|---|---|---:|---:|
| `AzureKeyVaultAdapter` | secretos | 50 | 5 minutos |
| `AzureAppConfigParameterCatalogAdapter` | parámetros | 1000 | 10 minutos |
| `AzureAppConfigMessageCatalogAdapter` | mensajes de usuario | 2000 | 30 minutos |
| `AzureAppConfigMessageCatalogAdapter` | mensajes técnicos | 2000 | 30 minutos |

Todas son Caffeine **LOCAL PROCESS CACHE**. No son cache distribuida, no comparten estado entre réplicas y su TTL no demuestra refresco inmediato.

## 11. Cache invalidation

Event Grid permite invalidación push mediante `CatalogInvalidationPort`:

- `invalidateSecret(name)`;
- `invalidateParameter(group, key)`;
- `invalidateMessage(code)`;
- `invalidateAll()` cuando App Configuration no identifica la clave.

`CompositeCatalogInvalidationAdapter` delega en los adapters Azure activos que soportan invalidación. Cada réplica debe recibir/procesar el evento para invalidar su cache local; esta documentación no afirma broadcast distribuido entre instancias.

## 12. Failure behavior

- Un recurso ausente se diferencia de un fallo técnico mediante `Optional`/excepciones del Port.
- No se sirve un valor inventado ante fallo del provider.
- Los fallos técnicos conservan causa para diagnóstico, pero no exponen secretos al cliente.
- Event Grid con evento no soportado o sin tipo se ignora; App Configuration sin clave identificable invalida todo; Key Vault sin nombre identificable no invalida.
- El webhook responde 401 si la credencial no está configurada (vacía/ausente/en blanco), falta o no coincide. La credencial se acepta solo en el header `aeg-sas-token`; no existe default funcional (`app.security.azure-events.webhook-token` ← `AZURE_EVENTGRID_WEBHOOK_TOKEN`, vacío por defecto) ni autenticación por query string (TD-051/TD-052, LB-001D.2). `permitAll` en `SecurityConfig` es solo de autorización: la autenticación la ejecuta `AzureEventGridAuthFilter`, que identifica la ruta de forma exacta (respetando context path).
- No existe garantía de invalidación distribuida, entrega exactamente una vez ni orden global.

## 13. Provider selectors

```text
app.adapters.vault.provider=azure_keyvault
app.adapters.parameter-catalog.provider=azure_appconfig
app.adapters.message-catalog.provider=azure
```

Endpoints y opciones del provider viven bajo `app.providers.*`; no se mezclan con selectors. Un provider configurado sin adapter/configuración válida debe fallar temprano, no hacer fallback silencioso.

## 14. Local/dev workflow

1. Seleccionar providers explícitos o conservar los defaults conocidos.
2. Configurar endpoints por variables de entorno sin copiar valores sensibles a documentos.
3. Autenticarse mediante un mecanismo soportado por `DefaultAzureCredential` y con permisos mínimos.
4. Ejecutar unit/component tests sin depender de Azure real.
5. Ejecutar Cloud Integration solo mediante perfil/comando explícito y ambiente autorizado.
6. Validar lectura, Event Grid, invalidación y refresco sin imprimir valores.
7. Retirar datos/recursos temporales y registrar evidencia sanitizada.

## 15. Production considerations

- Identidad administrada/credencial aprobada y least privilege.
- Endpoints y credenciales fuera del repositorio.
- Webhook sin default funcional, credencial solo en header seguro y rotación/rollback definidos.
- Event Grid entregado a cada instancia o mecanismo equivalente documentado.
- Timeouts, cuota, retry y degradación no se presumen: requieren evidencia/configuración propia.
- Caffeine sigue siendo local; una necesidad de coherencia distribuida exige decisión, no cambio implícito.

## 16. Testing strategy

- Unit: parsing de claves, fallback, formatting, invalidación y failure semantics con clientes controlados.
- Component/wiring: selector, fail-fast y Composition Root.
- Architecture: Azure SDK ausente de Domain/Application y controller dependiente de InputPort.
- Cloud Integration: Key Vault/App Configuration reales mediante perfil explícito; evidencia sanitizada y cero secretos.
- Webhook integration: autenticación, handshake, eventos y refresh observable.

La Cloud Integration es `AzureCloudIntegrationIT` (Failsafe, solo lectura de Key Vault/App Configuration mediante `SecretVaultPort`/`ParameterCatalogPort`/`MessageCatalogPort`). `mvn verify` no la ejecuta; se lanza con `.\mvnw.cmd -Pazure-integration verify`, con identidad `DefaultAzureCredential` (p. ej. `az login`) y `AZURE_KEYVAULT_ENDPOINT`/`AZURE_APPCONFIG_ENDPOINT` definidos. Sin ambiente la prueba **falla** (nunca se salta ni pasa en falso). Un PASS de esta prueba no certifica Event Grid real, invalidación end-to-end ni observabilidad: MV-003 sigue pendiente. Los tests con mocks/clientes controlados no certifican Azure real.

## 17. Observability

Registrar, cuando aplique: tipo de evento, subject sanitizado, correlation disponible, provider/capability, clasificación del fallo, resultado de procesamiento e invalidación. Nunca valor de secreto, credencial, token ni payload sensible. No se inventan métricas Azure inexistentes; AS-IS y TARGET se declaran por separado.

## 18. Security rules

- Azure SDK solo en Infrastructure.
- Secretos nunca en Application, logs, docs, tests o evidencia.
- External webhook auth no equivale a Bearer JWT de negocio.
- Credenciales de webhook sin defaults funcionales y nunca por query string.
- Permisos mínimos para lectura/operación requerida.
- No exponer nombres sensibles si no son contrato operacional público.
- Hallazgos se registran por archivo/tipo, nunca por valor.

## 19. Change procedure

### Antes de cambiar un parámetro

1. identificar capability;
2. identificar key;
3. revisar consumidores;
4. revisar fallback;
5. revisar tipo esperado;
6. revisar test;
7. cambiar Azure;
8. verificar Event Grid;
9. verificar invalidación;
10. verificar comportamiento runtime;
11. registrar evidencia sanitizada.

### Antes de cambiar un mensaje

1. confirmar código canónico;
2. distinguir user vs technical;
3. confirmar label;
4. revisar placeholders/`MessageFormat`;
5. revisar consumidores;
6. revisar fallback;
7. verificar invalidación de cache.

### Antes de cambiar un secreto

1. no registrar valor;
2. revisar consumidores;
3. planificar rotación;
4. revisar permisos;
5. verificar invalidación;
6. validar startup/runtime;
7. definir rollback.

## 20. What must never be documented

- valores de secretos, tokens, passwords, private keys o credenciales;
- headers de autenticación reales;
- dumps de configuración/Key Vault/App Configuration sin sanitizar;
- URLs con credenciales o query tokens;
- afirmaciones de despliegue, invalidación o Cloud Integration sin evidencia;
- valores externos presentados como source of truth del repositorio.

## 21. Known debts

- [TD-027](../baseline/TECHNICAL_DEBT.md#td-027): evidencia operacional Azure/telemetría; MV-003.
- [TD-051](../baseline/TECHNICAL_DEBT.md#td-051), [TD-052](../baseline/TECHNICAL_DEBT.md#td-052), [TD-053](../baseline/TECHNICAL_DEBT.md#td-053): RESUELTAS en LB-001D.2 (default funcional, credencial por query, aislamiento de Cloud Integration).
- [TD-054](../baseline/TECHNICAL_DEBT.md#td-054): decisión Azure→realtime; DR-AZ-001.

## 22. References

- [AGENTS.md](../../AGENTS.md)
- [Source of Truth](../governance/SOURCE_OF_TRUTH.md)
- [Composition Root](../architecture/adapter-composition-standard.md)
- [External Services](../architecture/external-services.md)
- [Testing Standard](../testing/TESTING_STANDARD.md)
- [Runtime Security](../security/runtime-security-provider-architecture.md)
- [Observability](../observability/README.md)
- [Technical Debt](../baseline/TECHNICAL_DEBT.md)
- [MV-003](../baseline/MANUAL_VALIDATION_LEDGER.md)
- [LB-001D.1 work item](../work-items/LB-001D-governance-hardening/LB-001D.1-PLAN.md)

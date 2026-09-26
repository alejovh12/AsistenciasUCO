---
name: uco-catalogos
description: Revisar o cambiar catálogos de mensajes y parámetros (MessageCatalogPort, ParameterCatalogPort) y sus providers sin acoplar capas.
---

# uco-catalogos

## Cuándo usar

Revisar o modificar `MessageCatalogPort`, `ParameterCatalogPort`, sus providers (SQL Server, Azure App Configuration), su wiring, caché, fallback o consumo de mensajes/parámetros desde cualquier capa.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo lo pertinente:

- [adapter-composition-standard.md](../../../docs/architecture/adapter-composition-standard.md): matriz de capabilities, selectores y Composition Root.
- [external-services.md](../../../docs/architecture/external-services.md): servicios externos y providers.
- [TECHNICAL_DEBT.md](../../../docs/baseline/TECHNICAL_DEBT.md): [TD-027](../../../docs/baseline/TECHNICAL_DEBT.md#td-027) (evidencia operacional Azure) y [TD-029](../../../docs/baseline/TECHNICAL_DEBT.md#td-029) (`GlobalExceptionHandler` → `MessageCatalogPort`).
- Código AS-IS: `application/secondaryports/catalog/`, `infrastructure/adapter/secondary/catalog/{sqlserver,azure}` y `infrastructure/config/adapters/catalog/**`. Selectores en `application.yml`: `app.adapters.message-catalog.provider` y `app.adapters.parameter-catalog.provider`.

## Reglas obligatorias

- Los puertos son neutrales: ningún tipo de Azure/SQL/Spring en la firma. La selección de provider vive solo en Composition Root, con `@ConditionalOnProperty`; no Service Locator ni condicionales de provider en Application.
- Un Primary Adapter (controller, filtro, handler HTTP) no depende directamente de un Secondary Port. Si necesita un mensaje, lo obtiene por un InputPort/servicio de Application; no ocultar la violación editando ArchUnit.
- Comprobar en el código, antes de afirmarlo, la caché (hoy `ConcurrentHashMap` en memoria sin TTL, con `clearCache()` en los adapters de mensajes), el fallback (por ejemplo, label/sin label en Azure) y el comportamiento fail-fast de la configuración. No asumir una política de refresco/invalidación que no exista.
- Ausencia de código/parámetro/mensaje: comportamiento explícito y observable; no devolver texto inventado ni exponer detalles técnicos o secretos al usuario.
- Sin secretos en catálogos, logs ni evidencia. Credenciales del provider vía vault/identidad gestionada.

## Archivos y cambios prohibidos

No cambiar contratos HTTP ni códigos/mensajes públicos sin decisión contractual. No crear providers, puertos ni caché distribuida especulativos. No modificar catálogo SQL/App Configuration desplegado desde este repositorio.

## Quality gates

ArchUnit (dirección de dependencias y controllers), tests de wiring por provider, tests de fallback/caché/ausencia, `verify` y cobertura según [DoD](../../../docs/baseline/DEFINITION_OF_DONE.md). Un mock no certifica el provider real; la evidencia operacional Azure sigue en TD-027.

## Evidencia esperada

Provider/selector afectado, archivo/símbolo de puerto y adapter, comportamiento de caché/fallback comprobado, resultado de wiring y ArchUnit, observabilidad preservada (logs/correlation) y referencia a TD abierta.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.

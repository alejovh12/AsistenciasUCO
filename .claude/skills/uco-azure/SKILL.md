---
name: uco-azure
description: Revisar o cambiar Azure Key Vault, App Configuration, Event Grid, DefaultAzureCredential, caches e invalidación cloud-backed.
---

# uco-azure

## Cuándo usar

Usar para Azure Key Vault, Azure App Configuration, Azure Event Grid, `DefaultAzureCredential`, invalidación, caches cloud-backed, webhook Azure, provider cloud y cambios operacionales Azure.

## Fuentes obligatorias

Leer, en orden:

- [AGENTS.md](../../../AGENTS.md)
- [SOURCE_OF_TRUTH.md](../../../docs/governance/SOURCE_OF_TRUTH.md)
- [azure-runtime-integration.md](../../../docs/integration/azure-runtime-integration.md)
- [adapter-composition-standard.md](../../../docs/architecture/adapter-composition-standard.md)
- [uco-catalogos](../uco-catalogos/SKILL.md)
- [uco-seguridad](../uco-seguridad/SKILL.md)
- [uco-observabilidad](../uco-observabilidad/SKILL.md)
- [uco-arquitectura](../uco-arquitectura/SKILL.md)

## Reglas obligatorias

- Port neutral en Application; adapter y SDK solo en Infrastructure; selección en Composition Root mediante provider selector.
- Documentar fallback, caché local, TTL/tamaño, invalidación, failure semantics y observabilidad antes de cambiar comportamiento.
- Exigir tests unitarios y wiring; integración con Azure real cuando el alcance la requiera, mediante perfil/comando explícito y evidencia sanitizada.
- No afirmar validación Azure real por mocks. `NOT_RUN` no es PASS.
- Cambios operacionales Azure requieren work item, consumidores, rollback, permisos y validación de invalidación/runtime.

## Prohibiciones

- Azure SDK en Domain o Application.
- Acoplar Use Cases a `ConfigurationClient`, `SecretClient`, Event Grid SDK o implementación de credenciales.
- Service Locator o selector tecnológico en Application.
- Secretos en logs, documentación, tests o evidencias.
- Tokens/credenciales en URL o defaults funcionales inseguros.
- Modificar recursos Azure sin work item autorizado.
- Hardcodear endpoints sensibles innecesariamente.
- Ejecutar cloud real dentro de la suite unitaria normal.
- Omitir provider selector, fallback, caché, invalidación, observabilidad o failure semantics.

## Evidencia esperada

Capability/Port, adapter, selector, configuración, cache policy, evento de invalidación, comportamiento de fallo, tests por nivel, ambiente/versión, cleanup/rollback y evidencia sanitizada. Para credenciales solo archivo/tipo del hallazgo, nunca el valor.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia ausente o contradicción aplicar los protocolos de AGENTS y detener únicamente el alcance dependiente.

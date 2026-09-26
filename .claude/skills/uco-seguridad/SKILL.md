---
name: uco-seguridad
description: Revisar JWT, roles, scopes institucionales, ownership, secretos e independencia del proveedor de identidad.
---

# uco-seguridad

## Cuándo usar

Revisar JWT, roles, scopes institucionales, ownership, secretos e independencia del proveedor de identidad.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [runtime-security-provider-architecture.md](../../../docs/security/runtime-security-provider-architecture.md)
- [keycloak-identity-provider.md](../../../docs/security/keycloak-identity-provider.md)
- [keycloak-service-account.md](../../../docs/security/keycloak-service-account.md)
- [README.md](../../../infra/keycloak/README.md)
- [TECHNICAL_DEBT.md](../../../docs/baseline/TECHNICAL_DEBT.md)

## Reglas obligatorias

Seguridad es capacidad; Keycloak es el provider actual. Separar runtime JWT de provisioning. Preservar issuer/audience/idUsuario, roles y 401/403. Layer 1 HTTP y ownership/InstitutionalScope son distintos; no inventar permisos ni confundir scope de token y alcance institucional. Secretos nunca en evidencia/logs/URLs.

## Archivos y cambios prohibidos

No lógica Keycloak/JWT en Domain/Application, no tokens por query, no ampliar RBAC/CORS ni modificar .env, realm, bootstrap o DB sin alcance autorizado. No imprimir ni rotar secretos al inspeccionar.

## Quality gates

Tests negativos JWT/RBAC/ownership y separación de provider; ArchUnit/verify y checks de seguridad CI aplicables.

## Evidencia esperada

Matriz de acceso y consumidores, pruebas 401/403 y titularidad, SECURITY_FINDING solo con archivo/tipo; MV-002 si E2E externo.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.

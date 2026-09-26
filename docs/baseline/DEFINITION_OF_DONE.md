---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Definition of Done — línea base

DONE exige todos los checks aplicables; compilar por sí solo no basta. Registrar PASS, FAIL, NO APLICA con motivo o `VALIDATION_BLOCKED_BY_ENVIRONMENT`. Un bloqueo o una prueba omitida relevante impide cerrar como DONE; registrarlo como deuda no sustituye el gate.

| Área | Evidencia de cierre |
|---|---|
| Alcance y contrato | Criterios cumplidos, contrato aprobado antes de modificar API, compatibilidad/consumidores comprobados; cero conflictos relevantes abiertos |
| RED → GREEN | TEST_PLAN derivado de requisito/contrato y matriz conductual; tests prueban observables, negativos y efectos aplicables; fallo RED por la causa esperada; implementador no alteró RED; correcciones del tester trazadas |
| Build | `mvn verify` o wrapper equivalente termina con exit 0; tests y paquete correctos |
| Arquitectura | ArchUnit pasa; Domain/Application sin dependencias tecnológicas prohibidas; providers en Composition Root; puertos neutrales |
| Coverage | Gates globales del [estándar](../testing/TESTING_STANDARD.md) cumplidos; reportar contadores reales |
| Persistencia / providers | Integración real cuando contrato, mapping o provider lo exigen; fixture/versión documentados y skips revisados; mocks no sustituyen integración; no esquema administrado por backend; paridad antes de retirar JDBC; sin dual-write; N+1/rollback evaluados si aplica |
| Seguridad | 401/403 y ownership relevantes cubiertos; hallazgos bloqueantes resueltos; cero secretos nuevos; cero dependencias prohibidas; no debilitar scanners, RBAC ni tests |
| Operación | Logs, metrics, traces, correlationId/traceId/spanId preservados; limitaciones de SSE y estado local explícitas |
| CI | Checks aplicables descritos en [.github/CI](../../.github/CI.md); resultado remoto identificado o pendiente, nunca inferido de Maven local |
| Documentación | Normas y matriz AS-IS actualizadas; contratos/hashes aplicables verificados; [ledger de deuda](TECHNICAL_DEBT.md) y [manuales](MANUAL_VALIDATION_LEDGER.md) actualizados; ADR si hay decisión duradera |
| Higiene de evidencia | Ningún test requerido omitido; datos/recursos temporales limpiados; evidencia sanitizada; ningún secret, skip oculto o resultado `NOT_RUN` presentado como PASS |
| Cierre | VALIDATION con evidencias y dictamen; CLOSURE con resultado, decisiones, pendientes y alcance real de la auditoría |

Para `DOCUMENTATION_ONLY` y `CONTRACT_ANALYSIS`, GREEN funcional, DB runtime y E2E pueden ser NO APLICA con justificación. Siguen siendo obligatorios: integridad documental, fuentes, evidencia, conflictos, diff controlado (sin código/config funcional fuera de las rutas permitidas), validación de enlaces y cero secretos. Ejecutar `verify` si el entorno lo permite y registrar su resultado real.

Registrar por separado la dimensión documental (`DOCUMENTATION_VALIDATION`) y el gate técnico general (`TECHNICAL_BUILD_GATE`). Si el gate técnico falla, la tarea documental puede cerrarse como documentalmente completa, sin afirmar build verde ni DONE integral.

Para tareas de implementación (`CONTRACT_CHANGE`, `REFACTOR`, `BEHAVIOR_CHANGE`, `PERSISTENCE_MIGRATION`, `INFRASTRUCTURE`) todos los gates técnicos aplicables de la tabla siguen siendo obligatorios.

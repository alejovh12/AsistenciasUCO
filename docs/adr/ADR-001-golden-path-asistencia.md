---
status: active
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# ADR-001 — Golden Path de la línea base

- **Estado:** Aceptado
- **Fecha:** 2026-09-20

## Contexto

Se necesita una vertical suficientemente avanzada para probar la línea base antes de expandir
historias de usuario.

## Decisión

Usar registro masivo de asistencias por sesión + consulta + realtime como caso patrón.

## Razones

- ya atraviesa seguridad, aplicación, persistencia, DB y realtime;
- invoca un SP cuyo uso y pruebas de atomicidad existen en el checkout; su despliegue se verifica por separado;
- permite demostrar JPA sin cambiar reglas de negocio;
- permite contract-first, filtros/listados y E2E;
- no exige en el primer paso coordinar una transacción distribuida con Keycloak.

## Consecuencias

Todas las decisiones nuevas de línea base se prueban primero en esta vertical. No se migra el resto
del sistema a JPA antes de que el patrón cierre sus gates.

## Alternativas consideradas

Migrar todas las verticales a la vez se descarta por alcance y dificultad para aislar regresiones. El provisioning de usuarios exige consistencia DB/IdP adicional; se conserva fuera del patrón inicial. No se afirma una evaluación experimental de esas alternativas.

## Alcance de la aceptación

Selección ratificada por la solicitud LB-000 del 2026-09-20. La implementación futura requiere [DoR](../governance/DEFINITION_OF_READY.md) y [evidencia del Golden Path](../baseline/GOLDEN_PATH_ASISTENCIA.md); este ADR no certifica E2E.

---
status: active
type: adr
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# ADR-002 — Migración incremental JDBC -> JPA

- **Estado:** Aceptado para piloto
- **Fecha:** 2026-09-20

## Contexto

El backend está aislado por puertos, pero la persistencia usa JDBC. Los adapters y tests consumen vistas y stored
procedures; la validación de su versión desplegada debe aportarse y el equipo backend no es dueño del schema.

## Decisión

Migrar vertical por vertical detrás de los puertos existentes. Empezar por queries JPA; conservar
SP complejos y llamarlos desde Infrastructure JPA durante el piloto.

## Rechazado

- Big Bang de todo JDBC.
- Reescribir SP y JPA en la misma fase.
- `ddl-auto=update`.
- entidades JPA en Domain.

## Criterio de salida

Un adapter JDBC solo se retira cuando existe paridad funcional/integración y todos los gates pasan.

## Consecuencias

JDBC y JPA pueden coexistir temporalmente detrás de puertos estables, sin dual-write productivo. Entidades y transacciones técnicas quedan en Infrastructure; Hibernate DDL solo validate/none y sin Open Session in View. El backend no administra esquema. Se conserva SP complejo, se valida paridad y se documenta rollback antes del retiro. Detalle único: [JDBC_TO_JPA](../persistence/JDBC_TO_JPA.md).

## Alcance de la aceptación

Estrategia ratificada por la solicitud LB-000 del 2026-09-20. Aceptada para planificar el piloto LB-002 tras LB-001; no autoriza implementarlo en LB-000. Sin selección de librerías/versiones nuevas ni nombres de clases definitivos.

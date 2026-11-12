---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# VALIDATION — VAL-oleada1-urgentes (backend)

| Alcance | Comando | Resultado |
|---|---|---|
| EST-02 / OBS-08 focalizado | `.\mvnw.cmd -B -o test -Dtest=ValidationPatternsTest,SecurityErrorResponseWriterTest,ManagementPortConfigTest` (JDK 25, Windows, 2026-11-12) | BUILD SUCCESS: 72 tests, 0 fallos, 0 errores, 0 skips |
| SEC-04 | Análisis sintáctico de los 7 `scratch/*.ps1` con el parser de PowerShell | 0 errores; 0 coincidencias de los valores literales anteriores |

Pruebas nuevas: `ValidationPatternsTest` (casos válidos/inválidos, contraste aleatorio de 60 000 + 160 000 entradas contra las expresiones anteriores sin diferencias, entradas de 200 000 caracteres con límite de 5 s); `SecurityErrorResponseWriterTest.nullRequestUriIsWrittenAsEmptyPathWithoutNullPointer`; `ManagementPortConfigTest` ajustada a la dirección configurable.

Limitaciones: los scripts PowerShell no se ejecutaron (requieren backend, Keycloak y SQL). El workflow y Prometheus no se probaron en ejecución. Las compuertas completas (`verify`, Sonar) quedan para el responsable.

## Compuertas y PDP (2026-11-12)

| Verificación | Resultado |
|---|---|
| BD compuerta limpia (migración 39 incluida) | 766 ejecutadas, 0 fallos; VAL_ROL05_01..04 PASS |
| Backend `mvnw -B -o clean verify` | 4404 pruebas, 0 fallos, BUILD SUCCESS |
| Frontend `npm run verify` | 2736 de 2736 SUCCESS; bundle inicial 520.38 kB |
| `provision_asistencias.py catalog` | 201 endpoints, 0 sin regla, 0 obsoletos |
| `provision_asistencias.py pdp --prune` (aprobado por John, decisión F) | Concesiones sobrantes revocadas; `verify` limpio (quedan 36 recursos huérfanos sin concesiones, que no abren acceso) |
| `provision_asistencias.py verify-decisions` | 1231 decisiones, 0 fallos |

El `--prune` se ejecutó solo en el laboratorio local. En el entorno compartido lo debe repetir José Alejandro.

---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-08
---
# QUALITY-PR15 — cobertura histórica y pruebas equivalentes

Fuente: XML JaCoCo auditado del PR #15; NO re-ejecutado en Q0. Sonar new coverage 48.4%, global JaCoCo 82.6%. Distintos denominadores.

| Prioridad | Clase | Líneas sin cubrir | Cobertura histórica | Trabajo |
|---|---|---:|---:|---|
| P1 | MinioFileStorageAdapter | 99 | 0% | unit sdk/errores/limites; IT proveedor |
| P1 | ClamAvMalwareScanAdapter | 47 | 0% | unit respuestas, timeout, stream; IT EICAR |
| P1 | AcademicViewJpaProjectionMapper | 46 | 8% | proyecciones completas/null |
| P2 | EstudianteJpaRepository | 39 | 29.1% | filtros, paginación, vacío |
| P2 | MessageCatalogJpaRepository | 34 | 12.8% | caché, errores, formatos |
| P1 | CoreViewJpaProjectionMapper | 27 | 3.6% | mapeos completos/null |
| P2 | ParameterCatalogJpaRepository | 27 | 12.9% | tipos y valores inválidos |
| P2 | InstitutionalScopeJpaRepository | 22 | 35.3% | allow/deny/errores |

## Equivalencia JDBC→JPA (rellenar desde diff y suites existentes)
| Capacidad | Test antiguo eliminado / SHA | Contrato | Test unit JPA | IT real | Resultado |
|---|---|---|---|---|---|
| Asistencia | INVENTARIAR | query, command, rollback | INVENTARIAR | INVENTARIAR | PENDIENTE |
| Sesión/Grupo | INVENTARIAR | SP, error, tx | INVENTARIAR | INVENTARIAR | PENDIENTE |
| Estudiante | INVENTARIAR | filtros, cardinalidad | INVENTARIAR | INVENTARIAR | PENDIENTE |
| Catálogos | INVENTARIAR | caché, parse, fallback | INVENTARIAR | INVENTARIAR | PENDIENTE |
| Scope | INVENTARIAR | ownership, fail closed | INVENTARIAR | INVENTARIAR | PENDIENTE |

La auditoría previa reportó 41 archivos de test eliminados, 52 añadidos y 54 modificados, incluyendo auxiliares: no son 41 suites equivalentes por definición. Registrar path exacto y comportamiento recuperado, no contar cantidad de tests como prueba de paridad.

## Recalcular por commit
SHA + fecha + run Actions; Maven clean verify, reportes Surefire/JaCoCo XML frescos; LINE covered/missed y BRANCH covered/missed por clase; Sonar new_lines_to_cover, uncovered, new_coverage y ratings sobre mismo SHA. Verificar importación XML y reporte de excludes sin cambiarlos. Separar Failsafe -Pintegration y sus skips. Publicar diff coverage y evidencia de issues, no optimizar contadores cosméticos.

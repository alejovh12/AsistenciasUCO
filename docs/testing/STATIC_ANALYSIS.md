---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# Análisis estático oficial del backend (EST-14)

Declaración de las herramientas de análisis estático que cuentan como compuerta del backend, a raíz de EST-14 de la lista de
chequeo de validación ("declarar Sonar, CodeQL y ArchUnit como oficiales, o agregar SpotBugs"). Se eligió declarar las
existentes; ver la decisión al final.

## Herramientas oficiales

| Herramienta | Qué cubre | Dónde corre | Cuándo bloquea |
| :--- | :--- | :--- | :--- |
| SonarQube Cloud | Bugs, vulnerabilidades, *security hotspots*, código duplicado, mantenibilidad y cobertura de código nuevo | `backend-ci.yml`, job `Backend Quality Gate` (`-Dsonar.qualitygate.wait=true`) | Quality Gate de la organización en rojo |
| CodeQL (`java-kotlin`) | Flujo de datos de seguridad: inyecciones, deserialización, rutas, SSRF | `security.yml`, job `CodeQL Java Analysis` | Alertas de Code Scanning en el PR |
| ArchUnit | Clean Architecture y hexagonal: capas, puertos, aislamiento de proveedores, JPA, mensajería y realtime | `src/test/java/.../architecture/` (corre en `mvnw verify`) y job `ArchUnit Architectural Guardrails` de `security-deep-scan.yml` (exige al menos 20 clases de reglas) | Cualquier regla violada |
| JaCoCo | Cobertura del bundle | `mvnw verify` (`pom.xml`) | Líneas < 80 % o ramas < 70 % |
| Trivy | Vulnerabilidades de dependencias e imagen, configuración y secretos | `security-deep-scan.yml` (repositorio e imagen) | HIGH o CRITICAL con corrección disponible, salvo `.trivyignore` justificado y con vencimiento |
| Gitleaks | Secretos en los commits del cambio | `security.yml`, job `Gitleaks Secret Scan` | Cualquier hallazgo |
| Dependency Review | Dependencias nuevas con vulnerabilidades conocidas | `security.yml`, job `Dependency Review` | Severidad configurada en el workflow |

Las pruebas de fuente que actúan como reglas estáticas (por ejemplo `ProduccionSoloInvocaProcedimientosPublicosTest`,
`AccessCatalogConsistencyTest` y `OperationalReadinessConfigurationTest`) corren con las unitarias y bloquean igual.

## Cómo se tratan los hallazgos

- Un hallazgo se corrige en el código. Si es falso positivo, se marca en la herramienta con el motivo y se cita en el work item
  (regla, archivo, línea y SHA). Nunca se desactiva la regla para todo el proyecto.
- El Quality Gate de Sonar y la cobertura global de JaCoCo miden cosas distintas: una no sustituye a la otra.
- Una herramienta que no corrió no cuenta como aprobada (`NOT_RUN` no es `PASS`).

## Decisión: no se agrega SpotBugs

- **Motivo:** las reglas `java:S*` y `javabugs:S*` de Sonar ya cubren los patrones de SpotBugs que aplican a este código
  (nulos, recursos sin cerrar, igualdad, concurrencia básica), y CodeQL cubre el flujo de datos de seguridad. SpotBugs
  agregaría un cuarto reporte con hallazgos repetidos y otro tiempo de build.
- **Cuándo revisarla:** si Sonar deja de analizar el repositorio, o si un bug de producción encaja en un detector de SpotBugs que
  Sonar no tiene.
- **Estado:** propuesta de VAL-oleada8 (2026-11-15). Necesita el visto bueno del dueño del backend antes de darla por cerrada.

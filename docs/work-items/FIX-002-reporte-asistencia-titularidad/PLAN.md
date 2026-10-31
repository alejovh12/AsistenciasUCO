---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-30
---

# PLAN — FIX-002: titularidad y acceso seguro al reporte Excel de asistencia

## Identidad y objetivo

- Fecha / base Git: 2026-10-30 / rama develop; rama histórica solo se consulta, con merge-base 72312d7c60449cd0118b8d32b8da02ad1eb0074d.
- Objetivo: autorizar la lectura antes de consultar filas o generar el Excel y preservar el acceso institucional previsto.
- Criterios: DOCENTE titular 200 y XLSX válido; DOCENTE ajeno 403; COORDINADOR con scope institucional aplicable; ADMINISTRADOR activo; ESTUDIANTE 403; anónimo y JWT inválido 401; ninguna solicitud rechazada ejecuta ReporteAsistenciaQueryPort.
- La identidad de actor sale del principal autenticado mediante AuthenticatedUserResolver. No se acepta un ID de usuario del request.
- Skills: uco-arquitectura, uco-baseline, uco-seguridad, uco-testing.
- Fuentes: AGENTS.md, SOURCE_OF_TRUTH.md, API_DESIGN_RULES.md, runtime-security-provider-architecture.md, LINEA_BASE.md, DEFINITION_OF_READY.md, TESTING_STANDARD.md y VALIDATION_RUNBOOK.md.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| GET de grupo requiere Bearer y admite DOCENTE, COORDINADOR, ADMINISTRADOR por RBAC | SecurityConfig, matcher GET /api/v1/grupos/** | No cambia el matcher ni amplía roles. |
| El controller recibe grupoId y ejecuta el InputPort sin aportar usuario | ReporteAsistenciaController.generarReporteExcel | El caso de uso no puede validar actor. |
| Application consulta inmediatamente los datos del reporte | GenerarReporteAsistenciaUseCaseImpl.execute | No hay autorización previa en AS-IS. |
| Hay un resolver autenticado y neutral | AuthenticatedUserResolver / SecurityContextAuthenticatedUserResolver | El principal normalizado contiene el UUID institucional; no lee un ID del cliente. |
| Hay consultas institucionales existentes para roles activos, identidad docente, scope docente/coordinador y administrador activo | InstitutionalScopePort | No se cambia el contrato del puerto. |
| El adaptador implementa estas lecturas con JPA; la pertenencia docente y el scope de coordinador se comprueban en consultas SQL actuales | InstitutionalScopeJpaRepository | Solo auditoría; no se modifica DB, SQL ni adaptador. |
| Los sub-recursos de estudiantes y asistencias aplican el patrón histórico de scope docente | ConsultarEstudiantesGrupoUseCaseImpl / ConsultarAsistenciasPorGrupoUseCaseImpl | Ese patrón, aislado, permite omitir el check cuando falta una identidad docente; FIX-002 lo cierra usando roles institucionales activos. |
| El endpoint no aparece en OpenAPI canónico | openapi-golden-path.yaml; fila AS-IS en HTTP_AS_IS_MATRIX.md | No se crea ni cambia contrato canónico. La matriz describe el estado implementado. |

## Comparación histórica FIX-002

La rama local histórica tiene exactamente seis commits exclusivos desde el merge-base indicado. Se inspeccionan esos commits y su diff relativo al merge-base; no se compara el árbol histórico completo con develop. La corrección se reaprovecha conceptualmente, nunca mediante merge directo.

La implementación histórica pasa el grupo y el usuario autenticado por DTO/domain, pero solo exige titularidad si findDocenteIdByUsuario devuelve una identidad. En consecuencia, ausencia de relación docente puede saltarse la autorización. Además, el coordinador histórico recibía acceso amplio por no resolver como docente. La adaptación actual exige rol institucional activo y autorización de scope para cada camino, y falla cerrado ante una identidad docente faltante.

## TARGET

- Mantener SecurityConfig y sus respuestas de autenticación/RBAC vigentes.
- Controller obtiene el usuario con AuthenticatedUserResolver y lo envía junto con grupoId por DTO/domain neutral.
- Application usa InstitutionalScopePort antes de llamar ReporteAsistenciaQueryPort:
  - ADMINISTRADOR solo con rol institucional activo y administrador activo.
  - COORDINADOR solo si el rol institucional está activo y puede acceder al grupo según scope vigente del programa.
  - DOCENTE solo si el rol está activo, la identidad docente se resuelve y tiene titularidad del grupo.
  - En usuarios con varios roles, basta una vía permitida que tenga rol activo y scope válido. Un fallo o ausencia en la vía DOCENTE no otorga acceso; otra vía solo puede autorizar por sus propias comprobaciones.
  - Cualquier otro caso, incluido ESTUDIANTE, se rechaza antes de consultar el reporte.
- Conservar el workbook, sus columnas, contenido y nombre. No exponer filas en respuestas rechazadas.

## Clase de cambio y alcance de rutas

- Change class: BEHAVIOR_CHANGE.
- PRIMARY_VARIABLE: autorización institucional previa a la lectura del reporte Excel.
- ALLOWED_PATHS:
  - docs/baseline/LINEA_BASE.md
  - docs/work-items/README.md
  - docs/contracts/HTTP_AS_IS_MATRIX.md
  - docs/work-items/FIX-002-reporte-asistencia-titularidad/**
  - src/main/java/co/edu/uco/asistenciasuco/application/features/reporte/generarreporteasistencia/**
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/reporte/ReporteAsistenciaController.java
  - src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/wiring/ReporteWiringConfiguration.java
  - src/test/java/co/edu/uco/asistenciasuco/application/features/reporte/generarreporteasistencia/**
  - src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/reporte/**
- FORBIDDEN_PATHS: .codex/**; docs/work-items/FIX-003-role-registration-idempotency/evidence/**; SQL/DB; SecurityConfig y runtime security configuration; InstitutionalScopePort y sus adaptadores; OpenAPI y su SHA; pom.xml; frontend; CI/workflows; ramas y remotos.
- CONTRACTS: ruta y forma XLSX no cambian; no hay contrato OpenAPI propio para este endpoint. Se aplica el acceso observable solicitado y se actualiza solo la matriz AS-IS.
- PROVIDERS: ninguno nuevo; reutiliza el resolver y InstitutionalScopePort actuales.
- EXTERNAL_ENVIRONMENT: no requerido para tests focalizados ni verify.
- SECURITY_IMPACT: cerrar acceso horizontal a filas del grupo con rol permitido pero scope ajeno o identidad docente irresuelta.
- OBSERVABILITY_IMPACT: se reutiliza ForbiddenException y el handler HTTP existente.
- TEST_LEVEL_REQUIRED: unitario de Application, HTTP slice con SecurityConfig/JWT/resolver/wiring/advice reales y regresión de formato XLSX; verify con ArchUnit y JaCoCo.
- ROLLBACK: revertir el cambio FIX-002 completo; la ruta vuelve al AS-IS sin autorización de titularidad.
- CONSUMERS: clientes que descargan el reporte desde grupos.
- STOP_CONDITIONS: evidencia contradictoria sobre los permisos vigentes; test que contradiga el contrato de este plan; DoR NOT_READY; requerimiento de cambio OpenAPI/DB fuera del alcance.

## Alcance

Auditar las siete piezas solicitadas; adaptar el paso de identidad por puertos/DTO/domain/interactor/wiring; validar autorización por rol activo y scope; recuperar/adaptar las cuatro suites señaladas; registrar validación real.

## No alcance

Cambio de endpoint, formato XLSX, consulta SQL, reglas RBAC globales, contrato OpenAPI, schema DB, secretos, providers, integración con frontend, push, merge, archive o eliminación de la rama histórica.

## Archivos afectados

EXISTENTES: input port, interactor, mapper, use case, implementación del use case, controller, wiring, ReporteAsistenciaControllerTest, HTTP_AS_IS_MATRIX.md, índice de work items y LINEA_BASE.md.

NUEVOS: DTO/domain de generación, GenerarReporteAsistenciaUseCaseImplTest, ReporteAsistenciaTitularidadHttpTest, ReporteAsistenciaAdministradorAccessHttpTest, TEST_PLAN.md y VALIDATION.md.

## Contratos y consumidores afectados

DOMAIN/Application: se añade usuarioEjecutor al objeto de entrada neutral.
HTTP: status/error de acceso no autorizado conforme a los criterios del usuario; URL, éxito XLSX y esquema canónico intactos.
PERSISTENCE: consulta de scope existente, sin cambios.
SECURITY: JWT Bearer y RBAC existentes; identidad tomada del principal autenticado.
REALTIME: no aplica.

## Riesgos y dependencias

- El endpoint no está documentado en OpenAPI; el plan no atribuye al contrato canónico detalles que no existen.
- Los mocks prueban el orden y la decisión de Application, no certifican SQL Server/JPA. La query de scope no se modifica.
- El gate base debe completar en Java 25 antes de implementación según DoR.

## Test plan

Ver TEST_PLAN.md. Congelar tests antes del cambio productivo, ejecutar RED causal, GREEN focal y después verify completo.

## Rollback y stop conditions

No modificar refs. Ante fallo del gate base, no empezar producción sin excepción aprobada y versionada. Ante conflicto de contrato, detener solo la conducta afectada y documentar CONTRACT_CONFLICT.

## Deuda conocida y validación manual

No se introduce deuda técnica ni validación manual obligatoria nueva. Sonar/CodeQL y CI remoto no están disponibles en esta validación local; se reportan como NO EJECUTADOS, no PASS.

## Definition of Ready

READY (2026-10-30). El usuario autorizó expresamente FIX-002. Base develop e231088217e44b43633e64ba45bd078a7b98b53e: mvnw -B -ntp verify, JDK 25, exit 0; 2682 tests, 0 failures, 0 errors, 0 skipped; ArchUnit pasó; JaCoCo line 92.05 %, branch 82.67 %, ambos gates cumplidos. El primer intento dentro del sandbox dio 121 errores de loopback y no llegó a JaCoCo; se repitió el mismo verify con permiso local de loopback y terminó BUILD SUCCESS. No hay excepción técnica pendiente.

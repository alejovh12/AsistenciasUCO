---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# Golden Path de asistencia

## Decisión y motivo

**Registrar asistencias de una sesión en lote + consultar estado + actualización realtime.** [ADR-001](../adr/ADR-001-golden-path-asistencia.md) lo selecciona porque atraviesa HTTP, identidad institucional, Input Port/Interactor/UseCase, ownership, SQL Server, eventos y observabilidad sin requerir provisioning IdP en la misma operación.

## AS-IS comprobado por inspección

```text
POST /api/v1/asistencias/lote
 → AsistenciaController → RegistrarAsistenciasSesionInputPort → Interactor → UseCase
 → SesionRepositoryPort (resuelve grupo) → InstitutionalScopePort (titularidad)
 → AsistenciaRepositoryPort → Composition Root (LB-008: JPA-only, sin selectores)
   → AsistenciaJpaRepository (@Repository, EntityManager; implementa AsistenciaRepositoryPort)
       ├─ registrarAsistenciasSesion → EntityManager.createNativeQuery("EXEC dbo.usp_registrar_asistencias_sesion …")
       │    → mismo SP dbo.usp_registrar_asistencias_sesion
       └─ registrarAsistenciaAutonoma / solicitarRevision / resolverSolicitud → la misma vía JPA
            (createNativeQuery "EXEC" → ProcedureResultMapper → ProcedureResultValidator)
 → RealtimePublisherPort → ReactorRealtimeAdapter (local-sse)
 → RealtimeStreamGateway → GET /api/v1/realtime/stream?grupoId={UUID}

GET /api/v1/grupos/{grupoId}/asistencias?sesionId={UUID}
 → AsistenciaQueryController → InputPort/UseCase → scope → AsistenciaRepositoryPort
   → AsistenciaJpaRepository (JPQL sobre entidades de vista, JPA-only)
 → uv_detalle_asistencia + uv_asistencia + uv_estudiante_grupo
```

El request por lote contiene `sesionId` y `registros[{estudianteId, estado}]`; retorna 201 con `ApiMessageResponse`. El usuario ejecutor viene del principal autenticado. El dominio Java admite `AN`, `SJC`, `EX`; esto no certifica el catálogo desplegado en DB. El adapter serializa `idEstudiante`/`estado` para el SP, con `idSesion`, `asistenciaJSON`, `idCorrelacion`, `idUsuarioEjecutor`; el mismo contrato de parámetros y el mismo SP se preservan sea JDBC o JPA el provider activo.

Desde LB-008 (JPA-01 COMMANDS) `AsistenciaRepositoryPort` se resuelve **exclusivamente** a
`AsistenciaJpaRepository`: commands y queries usan JPA/Hibernate, sin selectores, sin
adapter híbrido y sin fallback JDBC. El puerto, el contrato HTTP, los SP y la DB no cambian. Resumen de
evidencia: [LB-002.2C](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/CLOSURE.md)
(implementación + quality gates), [LB-002.2D](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2D-CLOSURE.md)
(paridad JDBC↔JPA en SQL Server real) y [LB-002.2E](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2E-CLOSURE.md)
(runtime/manual E2E). Realtime: `PASS` (dos clientes SSE). Reconexión: `PASS` (reconvergencia
observada de aproximadamente 25 s; no instantánea). HTTP y DB siguen siendo la fuente de verdad
tras la reconexión.

La consulta retorna 200 y `ApiListResponse<AsistenciaConsultadaDTO>`; `sesionId` es opcional, sin paginación/orden públicos en esta vertical. Hay POST legacy de consulta. La autorización HTTP permite docente/coordinador/administrador; si el usuario resuelve a docente, el use case exige titularidad.

SSE requiere `grupoId` y Bearer. La capa HTTP exige autenticación; el gateway exige titularidad docente al suscribirse y filtra por `payload.grupo`. El lote publica `ASISTENCIAS_SESION_ACTUALIZADAS` después del retorno exitoso de persistencia. El [estándar de eventos](../contracts/REALTIME_EVENT_STANDARD.md) conserva payload y garantías reales.

Esto prueba existencia estática, no ejecución E2E ni commit real en el ambiente actual. El IT disponible cubre estados, identidad del ejecutor, docente ajeno y atomicidad; requiere fixtures y puede abortar por assumptions. No afirmar PASS sin ejecutarlo y revisar skips.

## Evidencia reproducible

| Componente | Fuente |
|---|---|
| Command HTTP | [Command HTTP](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaController.java) |
| Request lote | [Request lote](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/request/RegistrarAsistenciasSesionRequest.java) |
| Registro | [Registro](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/request/RegistroAsistenciaRequest.java) |
| Use case lote | [Use case lote](../../src/main/java/co/edu/uco/asistenciasuco/application/features/asistencia/registrarasistenciassesion/usecase/impl/RegistrarAsistenciasSesionUseCaseImpl.java) |
| Query HTTP | [Query HTTP](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/asistencia/AsistenciaQueryController.java) |
| Use case query | [Use case query](../../src/main/java/co/edu/uco/asistenciasuco/application/features/asistencia/consultarasistenciasporgrupo/usecase/impl/ConsultarAsistenciasPorGrupoUseCaseImpl.java) |
| Port | [Port](../../src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/repository/AsistenciaRepositoryPort.java) |
| Repository JPA-only (commands y query) | [Repository JPA](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/AsistenciaJpaRepository.java) |
| Stream | [Stream](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java) |
| Gateway | [Gateway](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/localsse/LocalSseRealtimeStreamGateway.java) |
| RBAC | [RBAC](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/security/SecurityConfig.java) |
| IT SQL Server | [IT SQL Server](../../src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/core/AsistenciaRepositorySqlServerIT.java) |

## Cuándo se convierte en patrón replicable

LB-001 congela comportamiento y consumidores en contrato; LB-002 demuestra paridad de persistencia según [ADR-002](../adr/ADR-002-jpa-incremental.md); LB-003 certifica la [DoD](DEFINITION_OF_DONE.md), incluyendo seguridad negativa, errores seguros, correlación/auditoría, consulta coherente tras escritura, entrega local y recuperación mediante consulta. La evidencia manual externa se registra en [MV-001](MANUAL_VALIDATION_LEDGER.md).

No se cambian reglas de asistencia, schema, provider SSE ni toda la persistencia en este trabajo. Frontend, firmas DB liberadas y política temporal siguen pendientes de evidencia; ver [hallazgos](../work-items/LB-000-gobernanza-documentacion/FINDINGS.md).

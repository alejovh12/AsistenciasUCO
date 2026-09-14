# Backend Baseline Contract

## Endpoint Matrix

| Endpoint | Rol | UseCase | SecondaryPort | Vista/SP DB | Estado | Bloqueo si aplica | Contrato HTTP principal |
|---|---|---|---|---|---|---|---|
| `GET /api/v1/admin/decanos` | ADMINISTRADOR | Consultar decanos | `DecanoQueryPort` | `uv_decano` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<DecanoDTO>` |
| `POST /api/v1/admin/decanos` | ADMINISTRADOR | Crear decano | `DecanoCommandPort` | `usp_crear_decano` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Validar firma DB en E2E controlado | `CrearDecanoRequest` |
| `GET /api/v1/admin/parametros` | ADMINISTRADOR | Consultar parametros | `ParametroQueryPort` | `uv_parametro` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<ParametroDTO>` |
| `GET /api/v1/admin/instituciones` | ADMINISTRADOR | Consultar instituciones | `InstitucionQueryPort` | `uv_institucion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<InstitucionDTO>` |
| `GET /api/v1/admin/facultades` | ADMINISTRADOR | Consultar facultades | `FacultadQueryPort` | `uv_facultad` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<FacultadDTO>` |
| `GET /api/v1/admin/areas` | ADMINISTRADOR | Consultar areas | `AreaQueryPort` | `uv_area` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<AreaDTO>` |
| `POST /api/v1/admin/cierre-masivo` | ADMINISTRADOR | Ejecutar cierre masivo | `CierrePeriodoCommandPort` | `usp_ejecutar_cierre_masivo_periodo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Validar firma DB en E2E controlado | `CierreMasivoRequest` |
| `POST/PUT/PATCH /api/v1/admin/instituciones/**` | ADMINISTRADOR | Mutar institucion |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `POST/PUT/PATCH /api/v1/admin/facultades/**` | ADMINISTRADOR | Mutar facultad |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `POST/PUT/PATCH /api/v1/admin/areas/**` | ADMINISTRADOR | Mutar area |  |  | BLOCKED_BY_DB | No hay command publico confirmado | `FeatureUnavailableException` |
| `/api/v1/admin/sedes/**` | ADMINISTRADOR | Sedes |  |  | BLOCKED_BY_DB | Sede no esta modelada actualmente | `FeatureUnavailableException` |
| `/api/v1/admin/espacios-fisicos/**` | ADMINISTRADOR | Espacios fisicos |  |  | BLOCKED_BY_DB | EspacioFisico no esta modelado actualmente | `FeatureUnavailableException` |
| `GET /api/v1/decano/coordinadores` | DECANO | Consultar coordinadores por facultad | `CoordinadorQueryPort` | `uv_coordinador` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idFacultad` autenticada | `ApiListResponse<CoordinadorDTO>` |
| `POST /api/v1/decano/coordinadores` | DECANO | Crear coordinador | `CoordinadorCommandPort` | `usp_crear_coordinador` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idFacultad` autenticada | `CrearCoordinadorRequest` |
| `PATCH /api/v1/decano/coordinadores/{id}/estado` | DECANO | Toggle coordinador |  |  | BLOCKED_BY_DB | No hay command publico especifico confirmado | `FeatureUnavailableException` |
| `GET /api/v1/coordinador/planes-estudio` | COORDINADOR | Consultar planes | `PlanEstudioQueryPort` | `uv_plan_estudio` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<PlanEstudioDTO>` |
| `GET /api/v1/coordinador/asignaturas` | COORDINADOR | Consultar asignaturas | `AsignaturaQueryPort` | `uv_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<AsignaturaDTO>` |
| `GET /api/v1/coordinador/periodos-academicos` | COORDINADOR | Consultar periodos | `PeriodoAcademicoQueryPort` | `uv_periodo_academico` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiListResponse<PeriodoAcademicoDTO>` |
| `GET /api/v1/coordinador/estudiantes` | COORDINADOR | Consultar estudiantes programa | `EstudianteProgramaQueryPort` | `uv_estudiante_programa`, `uv_estudiante_identidad`, `uv_usuario` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `ApiListResponse<EstudianteProgramaDTO>` |
| `POST/PUT /api/v1/coordinador/planes-estudio/**` | COORDINADOR | Mutar planes | `PlanEstudioCommandPort` | `usp_registrar_o_actualizar_plan_estudio` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por `idPrograma` autenticado | `GuardarPlanEstudioRequest` |
| `POST/PUT/PATCH /api/v1/coordinador/**/asignaturas/**` | COORDINADOR | Mutar asignaturas | `AsignaturaCommandPort` | `usp_crear_asignatura`, `usp_actualizar_asignatura`, `usp_toggle_estado_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | DELETE fisico sigue bloqueado por DB | `GuardarAsignaturaRequest` |
| `GET /api/v1/docente/horarios` | DOCENTE | Consultar horario docente | `HorarioDocenteQueryPort` | `uv_horario_docente` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por docente autenticado | `ApiListResponse<HorarioDocenteDTO>` |
| `GET /api/v1/docente/asignaturas` | DOCENTE | Consultar asignaturas docente | `AsignaturaDocenteQueryPort` | `uv_docente` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por docente autenticado | `ApiListResponse<AsignaturaDocenteDTO>` |
| `GET /api/v1/estudiante/horarios` | ESTUDIANTE | Consultar horario estudiante | `HorarioEstudianteQueryPort` | `uv_horario_estudiante` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<HorarioEstudianteDTO>` |
| `GET /api/v1/estudiante/materias` | ESTUDIANTE | Consultar materias | `MateriaEstudianteQueryPort` | `uv_estudiante_grupo`, `uv_grupo`, `uv_asignatura` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<MateriaEstudianteDTO>` |
| `GET /api/v1/estudiante/materias/{materiaId}/sesiones` | ESTUDIANTE | Consultar sesiones materia | `SesionMateriaEstudianteQueryPort` | `uv_sesion`, `uv_estudiante_grupo`, `uv_grupo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope por estudiante autenticado | `ApiListResponse<SesionMateriaEstudianteDTO>` |
| `GET /api/v1/estudiante/materias/{materiaId}/prerrequisitos` | ESTUDIANTE | Prerrequisitos |  |  | BLOCKED_BY_DB | PrerrequisitoAsignatura no esta modelado | `FeatureUnavailableException` |
| `POST /api/v1/usuarios` | AUTENTICADO | Provisionar usuario | `UsuarioRepositoryPort`, `IdentityProviderPort` | `usp_sincronizar_usuario`, Keycloak Admin API | IMPLEMENTATION_AVAILABLE_PENDING_E2E | No crea perfil estudiante directo | `CrearUsuarioRequest` |
| `GET /api/v1/usuarios/perfil` | AUTENTICADO | Consultar perfil |  | `uv_usuario` | BLOCKED_BY_DB | Pendiente crear vertical query typed | `FeatureUnavailableException` |
| `PUT /api/v1/usuarios/perfil` | AUTENTICADO | Actualizar perfil |  |  | BLOCKED_BY_DB | No hay command publico confirmado para update parcial seguro | `FeatureUnavailableException` |
| `POST /api/v1/sesiones` | DOCENTE | Crear sesion | `SesionRepositoryPort` | `usp_crear_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `CrearSesionRequest` |
| `GET /api/v1/sesiones/{id}` | AUTENTICADO | Consultar sesion | `SesionRepositoryPort` | `uv_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E |  | `ApiDataResponse<SesionConsultadaDTO>` |
| `PUT /api/v1/sesiones/{id}` | DOCENTE | Actualizar sesion | `SesionRepositoryPort` | `usp_actualizar_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `ActualizarSesionRequest` |
| `POST /api/v1/sesiones/cierres` | DOCENTE | Cerrar sesion | `SesionRepositoryPort` | `usp_cerrar_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Scope docente resuelto desde JWT | `CerrarSesionRequest` |
| `POST /api/v1/sesiones/grupo/{grupoId}/generacion` | DOCENTE | Generar sesiones grupo | `SesionRepositoryPort` | `usp_generar_sesiones_grupo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | No hay loops/INSERTs en Java | Path `grupoId` |
| `GET /api/v1/sesiones/grupo/{grupoId}` | DOCENTE | Consultar sesiones por grupo |  | `uv_sesion` | READY_WITH_DB_LIMITATION | Vista soporta sesion por id; falta contrato de respuesta agregado por grupo en esta vertical | `FeatureUnavailableException` |
| `GET /api/v1/grupos/{id}/reportes/asistencia-excel` | AUTENTICADO | Reporte asistencia | `ReporteAsistenciaQueryPort` | `uv_sesion`, `uv_estudiante_grupo`, `uv_estudiante_identidad`, `uv_usuario`, `uv_asistencia`, `uv_detalle_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | Sin reglas inventadas de aprobacion | XLSX |
| `POST /api/v1/asistencias/lote` | DOCENTE | Registro masivo | `AsistenciaRepositoryPort` | `usp_registrar_asistencias_sesion` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | JSON serializado con ObjectMapper | `RegistrarAsistenciasSesionRequest` |
| `POST /api/v1/estudiante/asistencia-qr` | ESTUDIANTE | Registro autonomo | `AsistenciaRepositoryPort` | `usp_registrar_asistencia_estudiante_autonomo` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idEstudiante` resuelto desde JWT | `RegistrarAsistenciaQrRequest` |
| `POST /api/v1/asistencias/revisiones` | ESTUDIANTE | Radicar revision | `AsistenciaRepositoryPort` | `usp_radicar_solicitud_revision_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idEstudiante` resuelto desde JWT | `SolicitarRevisionAsistenciaRequest` |
| `PATCH /api/v1/docente/reclamos/{id}` | DOCENTE | Resolver revision | `AsistenciaRepositoryPort` | `usp_resolver_solicitud_revision_asistencia` | IMPLEMENTATION_AVAILABLE_PENDING_E2E | `idDocente` resuelto desde JWT | `ResolverReclamoRequest` |
| `GET /api/v1/sesiones/{id}/qr-token` | DOCENTE | Exponer codigo QR/PIN |  | `uv_sesion.codigo` | READY_WITH_DB_LIMITATION | Pendiente definir respuesta publica para codigo administrado por DB | `FeatureUnavailableException` |
| `GET /api/v1/realtime/**` | AUTENTICADO | Realtime |  |  | DEPRECATED_COMPATIBILITY | Ya no es anonimo; revisar politica por evento | SSE/JSON |

## DB ISSUES REQUIRING TEAM ACTION

1. No hay command publico para crear/modificar Horario.
2. `usp_crear_grupo` recibe `@aula` pero no la persiste.
3. `usp_actualizar_grupo` recibe `@cupoMaximo`/`@aula` pero no los persiste.
4. `cantidadEstudiantes` se usa como `capacidadMaximaPermitida` en `uv_grupo`, pero crear grupo la inicializa en 0.
5. Definir contrato oficial para `EstadoAsistencia` si se requiere registro individual.
6. Definir que ocurre con `SolicitudMatricula`.
7. Prerrequisitos no estan modelados actualmente.
8. Sede/EspacioFisico no estan modelados actualmente.
9. Confirmar firmas publicas en E2E controlado antes de release.
10. DELETE fisico de Asignatura sigue bloqueado por DB.
11. Consulta de reclamos requiere cerrar DTO/alcance sobre `uv_solicitud_revision_asistencia`.

## Security Baseline

- JWT debe validar issuer exacto y audience `asistencias-api`.
- Roles institucionales admitidos: `ADMINISTRADOR`, `DECANO`, `COORDINADOR`, `DOCENTE`, `ESTUDIANTE`.
- Codigos DB centralizados: `AD`, `DE`, `CD`, `DO`, `ES`.
- `idUsuario` debe viajar como atributo Keycloak y claim JWT.
- Controllers no deben depender de JDBC ni Keycloak.

package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteGrupoQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class GrupoJpaRepository implements GrupoRepositoryPort {

    static final String SQL_CREAR_GRUPO = """
            EXEC dbo.usp_crear_grupo
                 @idGrupo = :idGrupo,
                 @idAsignatura = :idAsignatura,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_GRUPO = """
            EXEC dbo.usp_actualizar_grupo
                 @idGrupo = :idGrupo,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @cupoMaximo = :cupoMaximo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_REGISTRAR_ESTUDIANTE = """
            EXEC dbo.usp_registrar_estudiante_en_grupo
                 @idGrupo = :idGrupo,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    static final String HQL_GRUPOS = """
            select g
            from UvGrupoEntity g
            order by g.nombreAsignatura, g.codigo, g.nombre, g.id
            """;
    static final String HQL_ESTUDIANTES = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteGrupoQueryRow(
                eg.id, eg.idEstudiante, ei.numeroIdentificacion, ei.nombreCompleto, u.correo,
                eg.codigoEstadoEstudiante, eg.nombreEstadoEstudiante)
            from UvEstudianteGrupoEntity eg
            join UvEstudianteIdentidadEntity ei on ei.id = eg.idEstudiante
            join UvUsuarioEntity u on u.id = ei.idUsuario
            where eg.idGrupo = :idGrupo
            order by ei.nombreCompleto, eg.id
            """;

    static final String OP_CREAR_GRUPO = "crearGrupo";
    static final String OP_ACTUALIZAR_GRUPO = "actualizarGrupo";
    static final String OP_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";

    private final EntityManager entityManager;
    private final TransactionOperations transactionOperations;
    private final JpaProcedureExecutor procedureExecutor;

    public GrupoJpaRepository(
            final EntityManager entityManager,
            final TransactionOperations transactionOperations,
            final JpaProcedureExecutor procedureExecutor
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.transactionOperations = Objects.requireNonNull(transactionOperations, "Las operaciones transaccionales son obligatorias.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public GrupoCommandRepositoryProjection crearGrupo(final CrearGrupoRepositoryDTO dto) {
        require(dto, "El DTO para crear grupo es obligatorio.");
        return transactionOperations.execute(status -> new GrupoCommandRepositoryProjection(
                dto.idGrupo(), ejecutarCrearGrupo(dto).getMensajeUsuarioResultado()));
    }

    @Override
    public GrupoCommandRepositoryProjection actualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        require(dto, "El DTO para actualizar grupo es obligatorio.");
        return transactionOperations.execute(status -> new GrupoCommandRepositoryProjection(
                dto.idGrupo(), ejecutarActualizarGrupo(dto).getMensajeUsuarioResultado()));
    }

    @Override
    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        require(dto, "El DTO para registrar estudiante en grupo es obligatorio.");
        return transactionOperations.execute(status -> new RegistrarEstudianteRepositoryProjection(
                ejecutarRegistrarEstudianteEnGrupo(dto).getMensajeUsuarioResultado()));
    }

    // QUERIES

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos() {
        return JpaQueryExecutor.execute(
                "consultarGrupos",
                "No fue posible consultar los grupos.",
                () -> entityManager.createQuery(HQL_GRUPOS, UvGrupoEntity.class).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toGrupo).toList()
        );
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
        return JpaQueryExecutor.execute(
                "consultarEstudiantesGrupo",
                "No fue posible consultar los estudiantes del grupo.",
                () -> entityManager.createQuery(HQL_ESTUDIANTES, EstudianteGrupoQueryRow.class)
                        .setParameter("idGrupo", grupoId).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toEstudianteGrupo).toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarCrearGrupo(final CrearGrupoRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.idGrupo());
        parametros.put("idAsignatura", dto.idAsignatura());
        parametros.put("idPeriodoAcademico", dto.idPeriodoAcademico());
        parametros.put("codigo", dto.codigo());
        parametros.put("nombre", dto.nombre());
        parametros.put("idDocente", dto.idDocente());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        return procedureExecutor.execute(OP_CREAR_GRUPO, SQL_CREAR_GRUPO, parametros, correlationId);
    }

    private ProcedureResult ejecutarActualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.idGrupo());
        parametros.put("codigo", dto.codigo());
        parametros.put("nombre", dto.nombre());
        parametros.put("idDocente", dto.idDocente());
        parametros.put("cupoMaximo", dto.cupoMaximo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        return procedureExecutor.execute(OP_ACTUALIZAR_GRUPO, SQL_ACTUALIZAR_GRUPO, parametros, correlationId);
    }

    private ProcedureResult ejecutarRegistrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.getGrupoId());
        parametros.put("numeroIdentificacion", dto.getNumeroIdentificacion());
        parametros.put("primerNombre", dto.getPrimerNombre());
        parametros.put("segundoNombre", dto.getSegundoNombre());
        parametros.put("primerApellido", dto.getPrimerApellido());
        parametros.put("segundoApellido", dto.getSegundoApellido());
        parametros.put("correo", dto.getCorreo());
        parametros.put("password", dto.getPassword());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());
        parametros.put("idTipoIdIdentificacion", dto.getTipoIdentificacionId());

        return procedureExecutor.execute(OP_REGISTRAR_ESTUDIANTE, SQL_REGISTRAR_ESTUDIANTE, parametros, correlationId);
    }

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }
}

package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SesionJpaRepository implements SesionRepositoryPort {

    static final String SQL_CREAR_SESION = """
            EXEC dbo.usp_crear_sesion
                 @idGrupo = :idGrupo,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_SESION = """
            EXEC dbo.usp_actualizar_sesion
                 @idSesion = :idSesion,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CERRAR_SESION = """
            EXEC dbo.usp_cerrar_sesion
                 @idSesion = :idSesion,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_GENERAR_SESIONES_GRUPO = """
            EXEC dbo.usp_generar_sesiones_grupo
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String SELECT_ROW = """
            select s
            from UvSesionEntity s
            """;
    static final String HQL_POR_ID = SELECT_ROW + " where s.id = :idSesion order by s.id";
    static final String HQL_POR_GRUPO = SELECT_ROW
            + " where s.idGrupo = :idGrupo order by s.fechaHoraInicio, s.numero, s.id";

    static final String OP_CREAR_SESION = "crearSesion";
    static final String OP_ACTUALIZAR_SESION = "actualizarSesion";
    static final String OP_CERRAR_SESION = "cerrarSesion";
    static final String OP_GENERAR_SESIONES_GRUPO = "generarSesionesGrupo";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SesionJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        require(dto, "El dominio para crear sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.getGrupo());
        parametros.put("nombre", dto.getNombre());
        parametros.put("fechaHoraInicio", dto.getFechaHoraInicio());
        parametros.put("fechaHoraFin", dto.getFechaHoraFin());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());

        procedureExecutor.execute(OP_CREAR_SESION, SQL_CREAR_SESION, parametros, correlationId);
    }

    @Override
    public void actualizarSesion(final ActualizarSesionRepositoryDTO dto) {
        require(dto, "El dominio para actualizar sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", dto.sesion());
        parametros.put("nombre", dto.nombre());
        parametros.put("fechaHoraInicio", dto.fechaHoraInicio());
        parametros.put("fechaHoraFin", dto.fechaHoraFin());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_ACTUALIZAR_SESION, SQL_ACTUALIZAR_SESION, parametros, correlationId);
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        require(dto, "El dominio para cerrar sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", dto.getSesion());
        parametros.put("idDocente", dto.getDocente());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());

        procedureExecutor.execute(OP_CERRAR_SESION, SQL_CERRAR_SESION, parametros, correlationId);
    }

    @Override
    public void generarSesionesGrupo(final GenerarSesionesGrupoRepositoryDTO dto) {
        require(dto, "El dominio para generar sesiones de grupo es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.grupo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_GENERAR_SESIONES_GRUPO, SQL_GENERAR_SESIONES_GRUPO, parametros, correlationId);
    }

    // QUERIES

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        require(dto, "El dominio para consultar sesion es obligatorio.");
        return consultarSesionPorId(dto.getSesion());
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        require(grupoId, "El identificador del grupo para consultar sesiones es obligatorio.");
        return JpaQueryExecutor.execute(
                "consultarSesionesPorGrupo",
                "No fue posible consultar las sesiones del grupo desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_GRUPO, UvSesionEntity.class)
                        .setParameter("idGrupo", grupoId)
                        .getResultList().stream().map(CoreViewJpaProjectionMapper::toSesion).toList()
        );
    }

    private SesionRepositoryProjection consultarSesionPorId(final UUID sesionId) {
        return JpaQueryExecutor.execute(
                "consultarSesion",
                "No fue posible consultar la sesion desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvSesionEntity.class)
                        .setParameter("idSesion", sesionId)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(CoreViewJpaProjectionMapper::toSesion)
                        .orElse(null)
        );
    }

    // PRIVATE HELPERS

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }
}

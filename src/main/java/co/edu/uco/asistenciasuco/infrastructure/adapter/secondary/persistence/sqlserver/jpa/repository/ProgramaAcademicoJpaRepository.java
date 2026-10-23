package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpProgramaAcademicoPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Programa academico por JPA: comandos por {@code usp_*_programa_academico} y consulta paginada con ambito en la DB. */
@Repository
public class ProgramaAcademicoJpaRepository implements ProgramaAcademicoCommandPort, ProgramaAcademicoQueryPort {

    static final String SQL_CREAR = """
            EXEC dbo.usp_crear_programa_academico
                 @idPrograma = :idPrograma,
                 @nombre = :nombre,
                 @idTipoPrograma = :idTipoPrograma,
                 @idCoordinador = :idCoordinador,
                 @idModalidad = :idModalidad,
                 @idJornada = :idJornada,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CREAR = "crearProgramaAcademico";

    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_programa_academico
                 @idPrograma = :idPrograma,
                 @nombre = :nombre,
                 @idTipoPrograma = :idTipoPrograma,
                 @idCoordinador = :idCoordinador,
                 @idModalidad = :idModalidad,
                 @idJornada = :idJornada,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_ACTUALIZAR = "actualizarProgramaAcademico";

    static final String SQL_CAMBIAR_ESTADO = """
            EXEC dbo.usp_cambiar_estado_programa_academico
                 @idPrograma = :idPrograma,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CAMBIAR_ESTADO = "cambiarEstadoProgramaAcademico";

    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_programas_academicos
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @nombre = :nombre,
                 @estaActivo = :estaActivo,
                 @idTipoPrograma = :idTipoPrograma
            """;
    static final String OP_CONSULTAR = "consultarProgramasAcademicos";
    static final String MENSAJE_PAGINACION_INVALIDA = "Los parametros de paginacion no son validos.";

    static final String HQL_POR_ID = """
            select p
            from UvProgramaEntity p
            where p.id = :idPrograma and p.idFacultad = :idFacultad
            """;
    static final String OP_POR_ID = "consultarProgramaAcademicoPorId";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public ProgramaAcademicoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de programas es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public void crear(final CrearProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPrograma", command.id());
        parametros.put("nombre", command.nombre());
        parametros.put("idTipoPrograma", command.idTipoPrograma());
        parametros.put("idCoordinador", command.idCoordinador());
        parametros.put("idModalidad", command.idModalidad());
        parametros.put("idJornada", command.idJornada());
        parametros.put("idFacultad", command.idFacultad());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_CREAR, SQL_CREAR, parametros, correlationId);
    }

    @Override
    public void actualizar(final ActualizarProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPrograma", command.id());
        parametros.put("nombre", command.nombre());
        parametros.put("idTipoPrograma", command.idTipoPrograma());
        parametros.put("idCoordinador", command.idCoordinador());
        parametros.put("idModalidad", command.idModalidad());
        parametros.put("idJornada", command.idJornada());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, correlationId);
    }

    @Override
    public void cambiarEstado(final CambiarEstadoProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPrograma", command.id());
        parametros.put("estaActivo", command.estaActivo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_CAMBIAR_ESTADO, SQL_CAMBIAR_ESTADO, parametros, correlationId);
    }

    @Override
    public ProgramaAcademicoPaginaProjection consultar(final ConsultaProgramas consulta) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR,
                "No fue posible consultar los programas academicos desde base de datos.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_CONSULTAR, SpProgramaAcademicoPaginadoEntity.class)
                            .setParameter("idUsuarioEjecutor", consulta.actorUsuarioId())
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("numeroPagina", consulta.pagina())
                            .setParameter("tamanoPagina", consulta.tamano())
                            .setParameter("nombre", consulta.nombre())
                            .setParameter("estaActivo", consulta.estaActivo())
                            .setParameter("idTipoPrograma", consulta.idTipoPrograma())
                            .getResultList();
                    } catch (final PersistenceException exception) {
                        if (esPaginacionInvalida(exception)) {
                            throw new ValidationException(MENSAJE_PAGINACION_INVALIDA);
                        }
                        throw exception;
                    }
                    final List<SpProgramaAcademicoPaginadoEntity> entidades = filas.stream()
                            .map(SpProgramaAcademicoPaginadoEntity.class::cast).toList();
                    final int total = entidades.isEmpty() || entidades.getFirst().totalRegistros() == null
                            ? 0 : entidades.getFirst().totalRegistros();
                    return new ProgramaAcademicoPaginaProjection(entidades.stream().map(ProgramaAcademicoJpaRepository::toProjection).toList(), total);
                }
        );
    }

    @Override
    public Optional<ProgramaAcademicoProjection> consultarPorId(final UUID idPrograma, final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                OP_POR_ID,
                "No fue posible consultar el programa academico desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvProgramaEntity.class)
                        .setParameter("idPrograma", idPrograma)
                        .setParameter("idFacultad", idFacultad)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(ProgramaAcademicoJpaRepository::toProjection)
        );
    }

    /** El SP rechaza la paginacion fuera de rango con DBCODE=PAG_001: es una entrada invalida (400), no un fallo tecnico. */
    private static boolean esPaginacionInvalida(final Throwable exception) {
        for (Throwable actual = exception; actual != null; actual = actual.getCause()) {
            if (actual.getMessage() != null && actual.getMessage().contains("DBCODE=PAG_001|")) {
                return true;
            }
        }
        return false;
    }

    private static ProgramaAcademicoProjection toProjection(final SpProgramaAcademicoPaginadoEntity e) {
        return new ProgramaAcademicoProjection(e.id(), e.nombrePrograma(), e.idTipoPrograma(), e.nombreTipoPrograma(),
                e.idModalidad(), e.nombreModalidad(), e.idJornada(), e.nombreJornada(), e.idCoordinador(),
                e.nombreCoordinador(), e.idFacultad(), e.nombreFacultad(), Boolean.TRUE.equals(e.estaActivoPrograma()));
    }

    private static ProgramaAcademicoProjection toProjection(final UvProgramaEntity e) {
        return new ProgramaAcademicoProjection(e.id(), e.nombrePrograma(), e.idTipoPrograma(), e.nombreTipoPrograma(),
                e.idModalidad(), e.nombreModalidad(), e.idJornada(), e.nombreJornada(), e.idCoordinador(),
                e.nombreCoordinador(), e.idFacultad(), e.nombreFacultad(), Boolean.TRUE.equals(e.estaActivoPrograma()));
    }
}

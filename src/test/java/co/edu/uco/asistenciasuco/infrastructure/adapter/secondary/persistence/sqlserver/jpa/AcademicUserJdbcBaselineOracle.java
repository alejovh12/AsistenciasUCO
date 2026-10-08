package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalJdbcBaselineExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Objects;
import java.util.UUID;

/**
 * ORACULO JDBC CONGELADO DE COMMANDS ACADEMICOS Y DE USUARIO (LB-008 JPA-03). Replica, sin logica funcional nueva,
 * los calls JDBC historicos de {@code AsignaturaJpaRepository}, {@code CierrePeriodoJpaRepository},
 * {@code CoordinadorJpaRepository} y {@code DecanoJpaRepository} (HEAD antes de JPA-03): mismo SP, mismos
 * parametros y misma correlacion ({@link CorrelationIdContext}), ejecutados por el mismo
 * {@link CanonicalJdbcBaselineExecutor} historico.
 *
 * <p>NO es codigo de produccion: ningun Composition Root lo referencia y no puede volver a runtime. Existe solo en
 * {@code src/test} para certificar la paridad de efectos exitosos JDBC (BEFORE) contra JPA (AFTER). Se retira junto
 * con la infraestructura de oraculo de TD-055 / JPA-07.</p>
 */
public final class AcademicUserJdbcBaselineOracle {

    static final String SQL_CREAR_ASIGNATURA = """
            EXEC dbo.usp_crear_asignatura
                 @idAsignatura = :idAsignatura,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @creditos = :creditos,
                 @idPlanEstudio = :idPlanEstudio,
                 @semestreNumero = :semestreNumero,
                 @nombreArea = :nombreArea,
                 @nombreComponente = :nombreComponente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_ASIGNATURA = """
            EXEC dbo.usp_actualizar_asignatura
                 @idAsignatura = :idAsignatura,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @creditos = :creditos,
                 @idPlanEstudio = :idPlanEstudio,
                 @semestreNumero = :semestreNumero,
                 @nombreArea = :nombreArea,
                 @nombreComponente = :nombreComponente,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_TOGGLE_ESTADO_ASIGNATURA = """
            EXEC dbo.usp_toggle_estado_asignatura
                 @idAsignatura = :idAsignatura,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String SQL_EJECUTAR_CIERRE_MASIVO = """
            EXEC dbo.usp_ejecutar_cierre_masivo_periodo
                 @codigoPeriodo = :codigoPeriodo,
                 @idActor = :idActor,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CREAR_COORDINADOR = """
            EXEC dbo.usp_crear_coordinador
                 @idCoordinador = :idCoordinador,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idPrograma = :idPrograma,
                 @idFacultad = :idFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CREAR_DECANO = """
            EXEC dbo.usp_crear_decano
                 @idDecano = :idDecano,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idFacultad = :idFacultad,
                 @nombreFacultad = :nombreFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String ID_CORRELACION = "idCorrelacion";
    private static final String ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";

    private final CanonicalJdbcBaselineExecutor procedureExecutor;

    public AcademicUserJdbcBaselineOracle(final CanonicalJdbcBaselineExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(
                procedureExecutor,
                "El ejecutor canonico historico es obligatorio para el oraculo JDBC."
        );
    }

    public CanonicalProcedureResult crearAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente,
            final UUID usuarioEjecutor
    ) {
        return procedureExecutor.execute("crearAsignatura", SQL_CREAR_ASIGNATURA, new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue("codigo", codigo)
                .addValue("nombre", nombre)
                .addValue("creditos", creditos)
                .addValue("idPlanEstudio", idPlanEstudio)
                .addValue("semestreNumero", semestreNumero)
                .addValue("nombreArea", nombreArea)
                .addValue("nombreComponente", nombreComponente)
                .addValue(ID_CORRELACION, CorrelationIdContext.require())
                .addValue(ID_USUARIO_EJECUTOR, usuarioEjecutor));
    }

    public CanonicalProcedureResult actualizarAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente
    ) {
        return procedureExecutor.execute("actualizarAsignatura", SQL_ACTUALIZAR_ASIGNATURA, new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue("codigo", codigo)
                .addValue("nombre", nombre)
                .addValue("creditos", creditos)
                .addValue("idPlanEstudio", idPlanEstudio)
                .addValue("semestreNumero", semestreNumero)
                .addValue("nombreArea", nombreArea)
                .addValue("nombreComponente", nombreComponente)
                .addValue(ID_CORRELACION, CorrelationIdContext.require()));
    }

    public CanonicalProcedureResult toggleEstadoAsignatura(final UUID idAsignatura) {
        return procedureExecutor.execute("toggleEstadoAsignatura", SQL_TOGGLE_ESTADO_ASIGNATURA, new MapSqlParameterSource()
                .addValue("idAsignatura", idAsignatura)
                .addValue(ID_CORRELACION, CorrelationIdContext.require()));
    }

    public CanonicalProcedureResult ejecutarCierreMasivoPeriodo(
            final String codigoPeriodo,
            final String idActor,
            final UUID idUsuarioEjecutor
    ) {
        return procedureExecutor.execute("ejecutarCierreMasivoPeriodo", SQL_EJECUTAR_CIERRE_MASIVO, new MapSqlParameterSource()
                .addValue("codigoPeriodo", codigoPeriodo)
                .addValue("idActor", idActor)
                .addValue(ID_CORRELACION, CorrelationIdContext.require())
                .addValue(ID_USUARIO_EJECUTOR, idUsuarioEjecutor));
    }

    public CanonicalProcedureResult crearCoordinador(
            final UUID idCoordinador,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final UUID idFacultad,
            final String password,
            final UUID usuarioEjecutor
    ) {
        return procedureExecutor.execute("crearCoordinador", SQL_CREAR_COORDINADOR, new MapSqlParameterSource()
                .addValue("idCoordinador", idCoordinador)
                .addValue("numeroIdentificacion", numeroIdentificacion)
                .addValue("primerNombre", primerNombre)
                .addValue("segundoNombre", segundoNombre)
                .addValue("primerApellido", primerApellido)
                .addValue("segundoApellido", segundoApellido)
                .addValue("correo", correo)
                .addValue("idPrograma", idPrograma)
                .addValue("idFacultad", idFacultad)
                .addValue("password", password)
                .addValue(ID_CORRELACION, CorrelationIdContext.require())
                .addValue(ID_USUARIO_EJECUTOR, usuarioEjecutor));
    }

    public CanonicalProcedureResult crearDecano(
            final UUID idDecano,
            final Integer numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idFacultad,
            final String nombreFacultad,
            final String password,
            final UUID usuarioEjecutor
    ) {
        return procedureExecutor.execute("crearDecano", SQL_CREAR_DECANO, new MapSqlParameterSource()
                .addValue("idDecano", idDecano)
                .addValue("numeroIdentificacion", numeroIdentificacion)
                .addValue("primerNombre", primerNombre)
                .addValue("segundoNombre", segundoNombre)
                .addValue("primerApellido", primerApellido)
                .addValue("segundoApellido", segundoApellido)
                .addValue("correo", correo)
                .addValue("idFacultad", idFacultad)
                .addValue("nombreFacultad", nombreFacultad)
                .addValue("password", password)
                .addValue(ID_CORRELACION, CorrelationIdContext.require())
                .addValue(ID_USUARIO_EJECUTOR, usuarioEjecutor));
    }
}





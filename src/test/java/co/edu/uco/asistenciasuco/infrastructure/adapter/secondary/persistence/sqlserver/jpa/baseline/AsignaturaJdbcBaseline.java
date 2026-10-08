package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AsignaturaJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server de asignaturas.
 *
 * <p>COMMANDS (crear, actualizar, toggle estado) delegan en {@link AsignaturaJpaRepository} sobre
 * {@code EntityManager} (LB-008 JPA-03, {@code CODE_MIGRATION_STATUS = JPA}). QUERIES siguen en JDBC temporal
 * hasta JPA-05 (CURRENT_OUTER_TX = NO y sin transaccion exterior en commands: sin cambio).</p>
 */
public final class AsignaturaJdbcBaseline implements AsignaturaQueryPort, AsignaturaCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final AsignaturaJpaRepository commands;

    public AsignaturaJdbcBaseline(
            final NamedParameterJdbcOperations jdbcOperations,
            final AsignaturaJpaRepository commands
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.commands = Objects.requireNonNull(commands, "El command JPA de asignaturas es obligatorio.");
    }

    @Override
    public void crearAsignatura(
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
        commands.crearAsignatura(idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero,
                nombreArea, nombreComponente, usuarioEjecutor);
    }

    @Override
    public void actualizarAsignatura(
            final UUID idAsignatura,
            final String codigo,
            final String nombre,
            final Integer creditos,
            final UUID idPlanEstudio,
            final Integer semestreNumero,
            final String nombreArea,
            final String nombreComponente
    ) {
        commands.actualizarAsignatura(idAsignatura, codigo, nombre, creditos, idPlanEstudio, semestreNumero,
                nombreArea, nombreComponente);
    }

    @Override
    public void toggleEstadoAsignatura(final UUID idAsignatura) {
        commands.toggleEstadoAsignatura(idAsignatura);
    }

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPrograma(final UUID idPrograma) {
        return jdbcOperations.query("""
                SELECT a.id, a.codigo, a.nombre, a.credito, a.idArea, a.nombreArea,
                       a.idComponente, a.nombreComponente, a.idSemestrePlanEstudio,
                       sp.idPlanEstudio, sp.idPrograma, a.nombrePrograma, a.codigoSemestre,
                       a.estaActivaAsignatura, a.estaActivaTextoAsignatura
                FROM dbo.uv_asignatura a
                INNER JOIN dbo.uv_semestre_plan_estudio sp ON sp.id = a.idSemestrePlanEstudio
                WHERE sp.idPrograma = :idPrograma
                ORDER BY nombre, codigo, id
                """, new MapSqlParameterSource("idPrograma", idPrograma), (rs, rowNum) -> mapAsignatura(rs));
    }

    @Override
    public List<AsignaturaProjection> consultarAsignaturasPorPlan(final UUID idPlanEstudio) {
        return jdbcOperations.query("""
                SELECT a.id, a.codigo, a.nombre, a.credito, a.idArea, a.nombreArea,
                       a.idComponente, a.nombreComponente, a.idSemestrePlanEstudio,
                       sp.idPlanEstudio, sp.idPrograma, a.nombrePrograma, a.codigoSemestre,
                       a.estaActivaAsignatura, a.estaActivaTextoAsignatura
                FROM dbo.uv_asignatura a
                INNER JOIN dbo.uv_semestre_plan_estudio sp ON sp.id = a.idSemestrePlanEstudio
                WHERE sp.idPlanEstudio = :idPlanEstudio
                ORDER BY a.codigoSemestre, a.nombre, a.codigo, a.id
                """, new MapSqlParameterSource("idPlanEstudio", idPlanEstudio), (rs, rowNum) -> mapAsignatura(rs));
    }

    private static AsignaturaProjection mapAsignatura(final java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AsignaturaProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigo")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombre")),
                JdbcBaselineValueMapper.toInteger(rs.getObject("credito")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idArea")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreArea")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idComponente")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreComponente")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idSemestrePlanEstudio")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPlanEstudio")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigoSemestre")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivaAsignatura")),
                JdbcBaselineValueMapper.toString(rs.getObject("estaActivaTextoAsignatura"))
        );
    }
}




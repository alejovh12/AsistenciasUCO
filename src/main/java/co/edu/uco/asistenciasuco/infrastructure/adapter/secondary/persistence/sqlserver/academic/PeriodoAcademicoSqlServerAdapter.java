package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PeriodoAcademicoSqlServerAdapter implements PeriodoAcademicoQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public PeriodoAcademicoSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<PeriodoAcademicoProjection> consultarPeriodosAcademicos() {
        return jdbcOperations.query("""
                SELECT id, idInstitucion, nombreInstitucion, nombre, codigo, fechaInicio, fechaFin, anio
                FROM dbo.uv_periodo_academico
                ORDER BY nombre, id
                """, (rs, rowNum) -> mapPeriodo(rs));
    }

    @Override
    public Optional<PeriodoAcademicoProjection> consultarPeriodoAcademicoPorId(final UUID idPeriodoAcademico) {
        final List<PeriodoAcademicoProjection> result = jdbcOperations.query("""
                SELECT id, idInstitucion, nombreInstitucion, nombre, codigo, fechaInicio, fechaFin, anio
                FROM dbo.uv_periodo_academico
                WHERE id = :idPeriodoAcademico
                """, new MapSqlParameterSource("idPeriodoAcademico", idPeriodoAcademico), (rs, rowNum) -> mapPeriodo(rs));
        return result.stream().findFirst();
    }

    private static PeriodoAcademicoProjection mapPeriodo(final java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PeriodoAcademicoProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toUuid(rs.getObject("idInstitucion")),
                JdbcValueMapper.toString(rs.getObject("nombreInstitucion")),
                JdbcValueMapper.toString(rs.getObject("nombre")),
                JdbcValueMapper.toString(rs.getObject("codigo")),
                JdbcValueMapper.toLocalDate(rs.getObject("fechaInicio")),
                JdbcValueMapper.toLocalDate(rs.getObject("fechaFin")),
                JdbcValueMapper.toInteger(rs.getObject("anio"))
        );
    }
}

package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class InstitucionJdbcBaseline implements InstitucionQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public InstitucionJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<InstitucionProjection> consultarInstituciones() {
        return jdbcOperations.query("""
                SELECT id, nombre, estaActivaInstitucion, estaActivaTextoInstitucion
                FROM dbo.uv_institucion
                ORDER BY nombre, id
                """, (rs, rowNum) -> new InstitucionProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombre")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivaInstitucion")),
                JdbcBaselineValueMapper.toString(rs.getObject("estaActivaTextoInstitucion"))
        ));
    }
}



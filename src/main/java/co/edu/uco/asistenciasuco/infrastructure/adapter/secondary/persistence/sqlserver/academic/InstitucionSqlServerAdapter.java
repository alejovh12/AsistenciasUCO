package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class InstitucionSqlServerAdapter implements InstitucionQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public InstitucionSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<InstitucionProjection> consultarInstituciones() {
        return jdbcOperations.query("""
                SELECT id, nombre, estaActivaInstitucion, estaActivaTextoInstitucion
                FROM dbo.uv_institucion
                ORDER BY nombre, id
                """, (rs, rowNum) -> new InstitucionProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toString(rs.getObject("nombre")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivaInstitucion")),
                JdbcValueMapper.toString(rs.getObject("estaActivaTextoInstitucion"))
        ));
    }
}

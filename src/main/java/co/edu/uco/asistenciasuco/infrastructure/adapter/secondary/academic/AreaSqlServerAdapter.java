package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class AreaSqlServerAdapter implements AreaQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public AreaSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<AreaProjection> consultarAreas() {
        return jdbcOperations.query("""
                SELECT id, nombre
                FROM dbo.uv_area
                ORDER BY nombre, id
                """, (rs, rowNum) -> new AreaProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toString(rs.getObject("nombre"))
        ));
    }
}

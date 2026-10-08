package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class ParametroJdbcBaseline implements ParametroQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public ParametroJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<ParametroProjection> consultarParametros() {
        return jdbcOperations.query("""
                SELECT id, grupo, clave, valor, tipoDato, valorDefecto, estaActivo
                FROM dbo.uv_parametro
                ORDER BY grupo, clave, id
                """, (rs, rowNum) -> new ParametroProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toString(rs.getObject("grupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("clave")),
                JdbcBaselineValueMapper.toString(rs.getObject("valor")),
                JdbcBaselineValueMapper.toString(rs.getObject("tipoDato")),
                JdbcBaselineValueMapper.toString(rs.getObject("valorDefecto")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivo"))
        ));
    }
}



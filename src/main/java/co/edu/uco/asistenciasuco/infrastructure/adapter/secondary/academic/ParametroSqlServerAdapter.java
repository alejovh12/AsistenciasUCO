package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class ParametroSqlServerAdapter implements ParametroQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public ParametroSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<ParametroProjection> consultarParametros() {
        return jdbcOperations.query("""
                SELECT id, grupo, clave, valor, tipoDato, valorDefecto, estaActivo
                FROM dbo.uv_parametro
                ORDER BY grupo, clave, id
                """, (rs, rowNum) -> new ParametroProjection(
                JdbcValueMapper.toUuid(rs.getObject("id")),
                JdbcValueMapper.toString(rs.getObject("grupo")),
                JdbcValueMapper.toString(rs.getObject("clave")),
                JdbcValueMapper.toString(rs.getObject("valor")),
                JdbcValueMapper.toString(rs.getObject("tipoDato")),
                JdbcValueMapper.toString(rs.getObject("valorDefecto")),
                JdbcValueMapper.toBoolean(rs.getObject("estaActivo"))
        ));
    }
}

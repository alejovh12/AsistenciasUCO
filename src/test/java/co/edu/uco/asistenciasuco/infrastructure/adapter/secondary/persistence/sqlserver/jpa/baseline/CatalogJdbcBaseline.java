package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Objects;
import java.util.Optional;

/**
 * Oraculo JDBC BEFORE de catalogos (LB-008 JPA-06). Solo src/test.
 * Replica el SQL AS-IS de los adapters previos: tablas base con NOLOCK, {@code TOP 1} y {@code estaActivo = 1}.
 */
public final class CatalogJdbcBaseline {

    private static final String SQL_MENSAJE_USUARIO = """
            SELECT TOP 1 contenido
            FROM dbo.CatalogoMensajeUsuario WITH (NOLOCK)
            WHERE codigo = :codigo
              AND estaActivo = 1
            """;

    private static final String SQL_MENSAJE_TECNICO = """
            SELECT TOP 1 contenido
            FROM dbo.CatalogoMensajeTecnico WITH (NOLOCK)
            WHERE codigo = :codigo
              AND estaActivo = 1
            """;

    private static final String SQL_PARAMETRO = """
            SELECT TOP 1 valor
            FROM dbo.CatalogoParametro WITH (NOLOCK)
            WHERE grupo = :grupo
              AND clave = :clave
              AND estaActivo = 1
            """;

    private final NamedParameterJdbcOperations jdbcOperations;

    public CatalogJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    public Optional<String> buscarMensajeUsuario(final String codigo) {
        return consultar(SQL_MENSAJE_USUARIO, new MapSqlParameterSource("codigo", codigo));
    }

    public Optional<String> buscarMensajeTecnico(final String codigo) {
        return consultar(SQL_MENSAJE_TECNICO, new MapSqlParameterSource("codigo", codigo));
    }

    public Optional<String> buscarParametro(final String grupo, final String clave) {
        return consultar(SQL_PARAMETRO, new MapSqlParameterSource()
                .addValue("grupo", grupo)
                .addValue("clave", clave));
    }

    private Optional<String> consultar(final String sql, final MapSqlParameterSource params) {
        try {
            return Optional.ofNullable(jdbcOperations.queryForObject(sql, params, String.class));
        } catch (final EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}



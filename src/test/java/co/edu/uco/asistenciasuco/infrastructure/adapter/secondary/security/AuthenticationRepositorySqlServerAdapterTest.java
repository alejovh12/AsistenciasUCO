package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioAutenticacionData;
import co.edu.uco.asistenciasuco.application.secondaryports.security.UsuarioPerfilData;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationRepositorySqlServerAdapterTest {

    private static final UUID ID_USUARIO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ID_PERFIL_DOCENTE = UUID.fromString("21111111-2222-3333-4444-555555555555");
    private static final UUID ID_PERFIL_ESTUDIANTE = UUID.fromString("31111111-2222-3333-4444-555555555555");

    @Test
    void consultarPorCorreo_usa_vista_autenticacion_parametrizada_y_mapea_password_como_hash() throws SQLException {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> parametrosCapturados = new AtomicReference<>();
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sqlCapturado.set(invocation.getArgument(0));
                    parametrosCapturados.set(invocation.getArgument(1));
                    final RowMapper<?> rowMapper = invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(autenticacionResultSet(), 0));
                });
        final AuthenticationRepositorySqlServerAdapter adapter =
                new AuthenticationRepositorySqlServerAdapter(jdbcOperations);

        final Optional<UsuarioAutenticacionData> resultado = adapter.consultarPorCorreo("ana.perez@uco.edu.co");

        assertTrue(resultado.isPresent());
        assertEquals(ID_USUARIO, resultado.get().getIdUsuario());
        assertEquals("ana.perez@uco.edu.co", resultado.get().getCorreo());
        assertEquals("{bcrypt}hash-controlado", resultado.get().getPasswordHash());
        assertTrue(resultado.get().isCorreoConfirmado());
        assertTrue(resultado.get().isActivo());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario_autenticacion"));
        assertTrue(sqlCapturado.get().contains("LOWER(correo) = LOWER(:correo)"));
        assertFalse(sqlCapturado.get().contains("dbo.Usuario"));
        assertEquals("ana.perez@uco.edu.co", parametrosCapturados.get()
                .getValue(AuthenticationRepositorySqlServerAdapter.PARAM_CORREO));
        assertFalse(resultado.get().toString().contains("{bcrypt}hash-controlado"));
    }

    @Test
    void consultarPorCorreo_inexistente_retorna_optional_vacio() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        final AuthenticationRepositorySqlServerAdapter adapter =
                new AuthenticationRepositorySqlServerAdapter(jdbcOperations);

        final Optional<UsuarioAutenticacionData> resultado = adapter.consultarPorCorreo("nadie@uco.edu.co");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void consultarPerfilesPorUsuario_conserva_todos_los_perfiles_de_la_vista() throws SQLException {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> parametrosCapturados = new AtomicReference<>();
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sqlCapturado.set(invocation.getArgument(0));
                    parametrosCapturados.set(invocation.getArgument(1));
                    final RowMapper<?> rowMapper = invocation.getArgument(2);
                    return List.of(
                            rowMapper.mapRow(perfilResultSet(ID_PERFIL_DOCENTE, "DO", "Docente", "Activo"), 0),
                            rowMapper.mapRow(perfilResultSet(ID_PERFIL_ESTUDIANTE, "ES", "Estudiante", "Activo"), 1)
                    );
                });
        final AuthenticationRepositorySqlServerAdapter adapter =
                new AuthenticationRepositorySqlServerAdapter(jdbcOperations);

        final List<UsuarioPerfilData> perfiles = adapter.consultarPerfilesPorUsuario(ID_USUARIO);

        assertEquals(2, perfiles.size());
        assertPerfil(perfiles.get(0), ID_PERFIL_DOCENTE, "DO", "Docente", "Activo");
        assertPerfil(perfiles.get(1), ID_PERFIL_ESTUDIANTE, "ES", "Estudiante", "Activo");
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario_perfil"));
        assertTrue(sqlCapturado.get().contains("WHERE idUsuario = :idUsuario"));
        assertFalse(sqlCapturado.get().contains("dbo.Perfil"));
        assertEquals(ID_USUARIO.toString(), parametrosCapturados.get()
                .getValue(AuthenticationRepositorySqlServerAdapter.PARAM_ID_USUARIO));
    }

    @Test
    void consultarPerfilesPorUsuario_sin_perfiles_retorna_lista_vacia() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        final AuthenticationRepositorySqlServerAdapter adapter =
                new AuthenticationRepositorySqlServerAdapter(jdbcOperations);

        final List<UsuarioPerfilData> perfiles = adapter.consultarPerfilesPorUsuario(ID_USUARIO);

        assertNotNull(perfiles);
        assertTrue(perfiles.isEmpty());
    }

    private void assertPerfil(
            final UsuarioPerfilData perfil,
            final UUID idPerfil,
            final String codigoPerfil,
            final String nombrePerfil,
            final String estado
    ) {
        assertEquals(ID_USUARIO, perfil.getIdUsuario());
        assertEquals(idPerfil, perfil.getIdPerfil());
        assertEquals(codigoPerfil, perfil.getCodigoPerfil());
        assertEquals(nombrePerfil, perfil.getNombrePerfil());
        assertEquals(estado, perfil.getEstado());
    }

    private ResultSet autenticacionResultSet() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("idUsuario")).thenReturn(ID_USUARIO.toString());
        when(resultSet.getString("correo")).thenReturn("ana.perez@uco.edu.co");
        when(resultSet.getString("password")).thenReturn("{bcrypt}hash-controlado");
        when(resultSet.getObject("correoConfirmado")).thenReturn(1);
        when(resultSet.getObject("estaActivoUsuario")).thenReturn(Boolean.TRUE);
        return resultSet;
    }

    private ResultSet perfilResultSet(
            final UUID idPerfil,
            final String codigoPerfil,
            final String nombrePerfil,
            final String estado
    ) throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("idUsuario")).thenReturn(ID_USUARIO);
        when(resultSet.getObject("idPerfil")).thenReturn(idPerfil.toString());
        when(resultSet.getString("codigoPerfil")).thenReturn(codigoPerfil);
        when(resultSet.getString("nombrePerfil")).thenReturn(nombrePerfil);
        when(resultSet.getString("estado")).thenReturn(estado);
        return resultSet;
    }
}

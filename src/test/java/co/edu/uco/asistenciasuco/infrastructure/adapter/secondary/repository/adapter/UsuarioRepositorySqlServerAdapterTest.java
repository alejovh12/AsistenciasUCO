package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;


import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioRepositorySqlServerAdapterTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ID_CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ID_CORRELACION_CONTEXTO = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void crearUsuario_envia_parametros_con_nombres_correctos_y_lee_resultado_canonico() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(
                        parametrosCapturados,
                        sqlCapturado,
                        new CanonicalProcedureResult(ID_CORRELACION, "Usuario registrado exitosamente.", "detalle interno", true)
                ),
                null
        );

        CorrelationIdContext.set(ID_CORRELACION);

        final CrearUsuarioRepositoryProjection resultado = adapter.crearUsuario(dtoValido());
        final Map<String, Object> values = parametrosCapturados.get().getValues();

        assertTrue(sqlCapturado.get().contains("EXEC dbo.usp_sincronizar_usuario"));
        assertTrue(!sqlCapturado.get().contains("usp_sincronizar_usuario_interno"));
        assertEquals(TIPO_IDENTIFICACION, values.get(UsuarioRepositorySqlServerAdapter.PARAM_ID_TIPO_ID_IDENTIFICACION));
        assertEquals(123456789, values.get(UsuarioRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
        assertEquals("PEREZ", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PRIMER_APELLIDO));
        assertEquals("GOMEZ", values.get(UsuarioRepositorySqlServerAdapter.PARAM_SEGUNDO_APELLIDO));
        assertEquals("ANA", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PRIMER_NOMBRE));
        assertEquals("MARIA", values.get(UsuarioRepositorySqlServerAdapter.PARAM_SEGUNDO_NOMBRE));
        assertEquals("ana.perez@uco.edu.co", values.get(UsuarioRepositorySqlServerAdapter.PARAM_CORREO));
        assertEquals("Clave123!", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PASSWORD));
        assertEquals(ID_CORRELACION, values.get(UsuarioRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(9, values.size());
        assertEquals("Usuario registrado exitosamente.", resultado.getMensajeUsuario());
    }

    @Test
    void crearUsuario_usa_correlationId_de_contexto_para_parametro_sql() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(parametrosCapturados, null, new CanonicalProcedureResult(ID_CORRELACION_CONTEXTO, "ok", "", true)),
                null
        );
        CorrelationIdContext.set(ID_CORRELACION_CONTEXTO);

        adapter.crearUsuario(dtoValido());

        assertEquals(
                ID_CORRELACION_CONTEXTO,
                parametrosCapturados.get().getValues().get(UsuarioRepositorySqlServerAdapter.PARAM_ID_CORRELACION)
        );
    }

    @Test
    void crearUsuario_convierte_estado_resultado_exitoso() {
        final UsuarioRepositorySqlServerAdapter adapterNumero = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(null, null, new CanonicalProcedureResult(ID_CORRELACION, "ok", "", true)),
                null
        );
        CorrelationIdContext.set(ID_CORRELACION);

        assertEquals("ok", adapterNumero.crearUsuario(dtoValido()).getMensajeUsuario());
    }

    @Test
    void crearUsuario_traduce_mensaje_resultado_a_excepcion_de_aplicacion() {
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(null, null, new CanonicalProcedureResult(ID_CORRELACION, "El correo ya existe.", "", false)),
                null
        );
        CorrelationIdContext.set(ID_CORRELACION);

        final ConflictException exception = assertThrows(ConflictException.class, () -> adapter.crearUsuario(dtoValido()));
        assertEquals("ERR_UNICIDAD_CORREO", exception.getCode());
        assertEquals(UsuarioErrorCode.ERR_UNICIDAD_CORREO.defaultMessage(), exception.getMessage());
    }

    @Test
    void crearUsuario_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(parametrosCapturados, null, new CanonicalProcedureResult(ID_CORRELACION, "ok", "", true)),
                null
        );

        assertThrows(CrosscuttingException.class, () -> adapter.crearUsuario(dtoValido()));
        assertEquals(null, parametrosCapturados.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void consultarUsuarioPorCorreo_usa_trim_lower_y_mapea_perfil_canonico() throws SQLException {
        final NamedParameterJdbcOperations queryOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> parametrosCapturados = new AtomicReference<>();
        when(queryOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sqlCapturado.set(invocation.getArgument(0));
                    parametrosCapturados.set(invocation.getArgument(1));
                    final RowMapper<?> rowMapper = invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(usuarioResultSet(ID_CORRELACION), 0));
                });
        final UsuarioRepositorySqlServerAdapter adapter =
                new UsuarioRepositorySqlServerAdapter(
                        fakeExecutor(null, null, new CanonicalProcedureResult(ID_CORRELACION, "ok", "", true)),
                        queryOperations
                );

        final var usuario = adapter.consultarUsuarioPorCorreo("  Usuario@Test.com  ");

        assertTrue(usuario.isPresent());
        assertEquals(ID_CORRELACION, usuario.get().id());
        assertEquals(TIPO_IDENTIFICACION, usuario.get().tipoIdentificacionId());
        assertEquals(123456789, usuario.get().numeroIdentificacion());
        assertEquals("ANA", usuario.get().primerNombre());
        assertEquals("PEREZ", usuario.get().primerApellido());
        assertEquals("ana.perez@uco.edu.co", usuario.get().correo());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario"));
        assertTrue(sqlCapturado.get().matches("(?s).*id,\\s+idTipoIdentificacion,\\s+numeroIdentificacion,\\s+primerNombre,\\s+primerApellido,\\s+correo.*"));
        assertTrue(!sqlCapturado.get().contains("dbo.Usuario"));
        assertTrue(sqlCapturado.get().contains("LOWER(TRIM(correo)) = LOWER(TRIM(:correo))"));
        assertEquals("  Usuario@Test.com  ", parametrosCapturados.get().getValue(UsuarioRepositorySqlServerAdapter.PARAM_CORREO));
    }

    @Test
    void consultarUsuarioPorIdentificacion_usa_idTipoIdentificacion_y_numero_identificacion() throws SQLException {
        final NamedParameterJdbcOperations queryOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> sqlCapturado = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> parametrosCapturados = new AtomicReference<>();
        when(queryOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sqlCapturado.set(invocation.getArgument(0));
                    parametrosCapturados.set(invocation.getArgument(1));
                    final RowMapper<?> rowMapper = invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(usuarioResultSet(ID_CORRELACION), 0));
                });
        final UsuarioRepositorySqlServerAdapter adapter =
                new UsuarioRepositorySqlServerAdapter(
                        fakeExecutor(null, null, new CanonicalProcedureResult(ID_CORRELACION, "ok", "", true)),
                        queryOperations
                );

        final var usuario = adapter.consultarUsuarioPorIdentificacion(TIPO_IDENTIFICACION, 123456789);

        assertTrue(usuario.isPresent());
        assertEquals(ID_CORRELACION, usuario.get().id());
        assertEquals(123456789, usuario.get().numeroIdentificacion());
        assertEquals("ana.perez@uco.edu.co", usuario.get().correo());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario"));
        assertTrue(sqlCapturado.get().matches("(?s).*id,\\s+idTipoIdentificacion,\\s+numeroIdentificacion,\\s+primerNombre,\\s+primerApellido,\\s+correo.*"));
        assertTrue(!sqlCapturado.get().contains("dbo.Usuario"));
        assertTrue(sqlCapturado.get().contains("idTipoIdentificacion = :tipoIdentificacionId"));
        assertEquals(TIPO_IDENTIFICACION, parametrosCapturados.get().getValue("tipoIdentificacionId"));
        assertEquals(123456789, parametrosCapturados.get().getValue(UsuarioRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
    }

    @Test
    void consultarUsuarioPorId_lee_proyeccion_canonica_desde_vista() throws SQLException {
        final NamedParameterJdbcOperations queryOperations = mock(NamedParameterJdbcOperations.class);
        final AtomicReference<String> sql = new AtomicReference<>();
        final AtomicReference<SqlParameterSource> params = new AtomicReference<>();
        when(queryOperations.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sql.set(invocation.getArgument(0));
                    params.set(invocation.getArgument(1));
                    return List.of(((RowMapper<?>) invocation.getArgument(2)).mapRow(usuarioResultSet(ID_CORRELACION), 0));
                });
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(
                fakeExecutor(null, null, new CanonicalProcedureResult(ID_CORRELACION, "ok", "", true)), queryOperations
        );

        final var usuario = adapter.consultarUsuarioPorId(ID_CORRELACION).orElseThrow();

        assertEquals(ID_CORRELACION, usuario.id());
        assertEquals(TIPO_IDENTIFICACION, usuario.tipoIdentificacionId());
        assertEquals("ANA", usuario.primerNombre());
        assertEquals("PEREZ", usuario.primerApellido());
        assertEquals(123456789, usuario.numeroIdentificacion());
        assertEquals("ana.perez@uco.edu.co", usuario.correo());
        assertTrue(sql.get().contains("FROM dbo.uv_usuario"));
        assertTrue(sql.get().contains("WHERE id = :idUsuario"));
        assertTrue(!sql.get().contains("dbo.Usuario"));
        assertEquals(ID_CORRELACION, params.get().getValue("idUsuario"));
    }

    private CanonicalStoredProcedureExecutor fakeExecutor(
            final AtomicReference<MapSqlParameterSource> parametersReference,
            final AtomicReference<String> sqlReference,
            final CanonicalProcedureResult result
    ) {
        return new CanonicalStoredProcedureExecutor(mock(NamedParameterJdbcOperations.class)) {
            @Override
            public CanonicalProcedureResult execute(
                    final String operation,
                    final String sql,
                    final MapSqlParameterSource parameters
            ) {
                if (parametersReference != null) {
                    parametersReference.set(parameters);
                }
                if (sqlReference != null) {
                    sqlReference.set(sql);
                }
                DbExceptionTranslator.throwIfFailed(
                        result.isEstadoResultado(),
                        result.getMensajeUsuarioResultado(),
                        result.getMensajeTecnicoResultado(),
                        result.getIdCorrelacion() == null ? null : result.getIdCorrelacion().toString(),
                        operation
                );
                return result;
            }
        };
    }

    private CrearUsuarioRepositoryDTO dtoValido() {
        return new CrearUsuarioRepositoryDTO(
                TIPO_IDENTIFICACION,
                123456789,
                "PEREZ",
                "GOMEZ",
                "ANA",
                "MARIA",
                "ana.perez@uco.edu.co",
                "Clave123!"
        );
    }

    private ResultSet usuarioResultSet(final UUID id) throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(id.toString());
        when(resultSet.getObject("idTipoIdentificacion")).thenReturn(TIPO_IDENTIFICACION.toString());
        when(resultSet.getObject("numeroIdentificacion")).thenReturn(123456789);
        when(resultSet.getString("primerNombre")).thenReturn("ANA");
        when(resultSet.getString("primerApellido")).thenReturn("PEREZ");
        when(resultSet.getString("correo")).thenReturn("ana.perez@uco.edu.co");
        return resultSet;
    }
}

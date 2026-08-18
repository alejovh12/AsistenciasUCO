package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
    void crearUsuario_envia_parametros_con_nombres_correctos_y_lee_outputs() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(parameters -> {
            parametrosCapturados.set(parameters);
            final Map<String, Object> output = new HashMap<>();
            output.put(UsuarioRepositorySqlServerAdapter.OUT_ESTADO, Boolean.TRUE);
            output.put(UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "Usuario registrado exitosamente.");
            output.put(UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, "detalle interno");
            return output;
        });

        CorrelationIdContext.set(ID_CORRELACION);

        final CrearUsuarioRepositoryEntity resultado = adapter.crearUsuario(dtoValido());
        final Map<String, Object> values = parametrosCapturados.get().getValues();

        assertEquals(TIPO_IDENTIFICACION.toString(), values.get(UsuarioRepositorySqlServerAdapter.PARAM_ID_TIPO_ID_IDENTIFICACION));
        assertEquals(123456789, values.get(UsuarioRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
        assertEquals("PEREZ", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PRIMER_APELLIDO));
        assertEquals("GOMEZ", values.get(UsuarioRepositorySqlServerAdapter.PARAM_SEGUNDO_APELLIDO));
        assertEquals("ANA", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PRIMER_NOMBRE));
        assertEquals("MARIA", values.get(UsuarioRepositorySqlServerAdapter.PARAM_SEGUNDO_NOMBRE));
        assertEquals("ana.perez@uco.edu.co", values.get(UsuarioRepositorySqlServerAdapter.PARAM_CORREO));
        assertEquals("Clave123!", values.get(UsuarioRepositorySqlServerAdapter.PARAM_PASSWORD));
        assertEquals(ID_CORRELACION.toString(), values.get(UsuarioRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(9, values.size());
        assertEquals("Usuario registrado exitosamente.", resultado.getMensajeUsuario());
    }

    @Test
    void crearUsuario_usa_correlationId_de_contexto_para_parametro_sql() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(parameters -> {
            parametrosCapturados.set(parameters);
            return Map.of(
                    UsuarioRepositorySqlServerAdapter.OUT_ESTADO, 1,
                    UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "ok",
                    UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, ""
            );
        });
        CorrelationIdContext.set(ID_CORRELACION_CONTEXTO);

        adapter.crearUsuario(dtoValido());

        assertEquals(
                ID_CORRELACION_CONTEXTO.toString(),
                parametrosCapturados.get().getValues().get(UsuarioRepositorySqlServerAdapter.PARAM_ID_CORRELACION)
        );
    }

    @Test
    void crearUsuario_convierte_estado_resultado_exitoso() {
        final UsuarioRepositorySqlServerAdapter adapterNumero = new UsuarioRepositorySqlServerAdapter(parameters -> Map.of(
                UsuarioRepositorySqlServerAdapter.OUT_ESTADO, 1,
                UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "ok",
                UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, ""
        ));
        CorrelationIdContext.set(ID_CORRELACION);

        assertEquals("ok", adapterNumero.crearUsuario(dtoValido()).getMensajeUsuario());
    }

    @Test
    void crearUsuario_traduce_mensaje_resultado_a_excepcion_de_aplicacion() {
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(parameters -> Map.of(
                UsuarioRepositorySqlServerAdapter.OUT_ESTADO, "0",
                UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_USUARIO, "El correo ya existe.",
                UsuarioRepositorySqlServerAdapter.OUT_MENSAJE_TECNICO, ""
        ));
        CorrelationIdContext.set(ID_CORRELACION);

        final ConflictException exception = assertThrows(ConflictException.class, () -> adapter.crearUsuario(dtoValido()));
        assertEquals("ERR_UNICIDAD_CORREO", exception.getCode());
        assertEquals("El correo ya existe.", exception.getMessage());
    }

    @Test
    void crearUsuario_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicReference<MapSqlParameterSource> parametrosCapturados = new AtomicReference<>();
        final UsuarioRepositorySqlServerAdapter adapter = new UsuarioRepositorySqlServerAdapter(parameters -> {
            parametrosCapturados.set(parameters);
            return Map.of();
        });

        assertThrows(CrosscuttingException.class, () -> adapter.crearUsuario(dtoValido()));
        assertEquals(null, parametrosCapturados.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void consultarUsuarioPorCorreo_usa_sql_parametrizado_y_mapea_id() throws SQLException {
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
                new UsuarioRepositorySqlServerAdapter(parameters -> Map.of(), queryOperations);

        final var usuario = adapter.consultarUsuarioPorCorreo("ana.perez@uco.edu.co");

        assertTrue(usuario.isPresent());
        assertEquals(ID_CORRELACION, usuario.get().id());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario"));
        assertTrue(sqlCapturado.get().contains("LOWER(correo) = LOWER(:correo)"));
        assertEquals("ana.perez@uco.edu.co", parametrosCapturados.get().getValue(UsuarioRepositorySqlServerAdapter.PARAM_CORREO));
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
                new UsuarioRepositorySqlServerAdapter(parameters -> Map.of(), queryOperations);

        final var usuario = adapter.consultarUsuarioPorIdentificacion(TIPO_IDENTIFICACION, 123456789);

        assertTrue(usuario.isPresent());
        assertEquals(ID_CORRELACION, usuario.get().id());
        assertTrue(sqlCapturado.get().contains("FROM dbo.uv_usuario"));
        assertTrue(sqlCapturado.get().contains("idTipoIdentificacion = :tipoIdentificacionId"));
        assertEquals(TIPO_IDENTIFICACION.toString(), parametrosCapturados.get().getValue("tipoIdentificacionId"));
        assertEquals(123456789, parametrosCapturados.get().getValue(UsuarioRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
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
        return resultSet;
    }
}

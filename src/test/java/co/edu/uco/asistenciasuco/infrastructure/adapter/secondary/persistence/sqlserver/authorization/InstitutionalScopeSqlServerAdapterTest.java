package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.authorization;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstitutionalScopeSqlServerAdapterTest {

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final InstitutionalScopeSqlServerAdapter adapter = new InstitutionalScopeSqlServerAdapter(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void findUsuarioIdById_retorna_presente_cuando_existe() throws Exception {
        final UUID usuarioId = UUID.randomUUID();
        final UUID found = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(found);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> ((ResultSetExtractor<Optional<UUID>>) invocation.getArgument(2)).extractData(rs));

        final Optional<UUID> result = adapter.findUsuarioIdById(usuarioId);

        assertTrue(result.isPresent());
        assertEquals(found, result.get());
        final ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(anyString(), params.capture(), any(ResultSetExtractor.class));
        assertEquals(usuarioId, params.getValue().getValue("usuarioId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findUsuarioIdByEmail_retorna_vacio_cuando_no_hay_filas() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> ((ResultSetExtractor<Optional<UUID>>) invocation.getArgument(2)).extractData(rs));

        final Optional<UUID> result = adapter.findUsuarioIdByEmail("ana@uco.edu.co");

        assertFalse(result.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void metodos_find_por_usuario_delegan_en_querySingleUuid() throws Exception {
        final UUID usuarioId = UUID.randomUUID();
        final UUID found = UUID.randomUUID();
        final ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(found);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> ((ResultSetExtractor<Optional<UUID>>) invocation.getArgument(2)).extractData(rs));

        assertEquals(Optional.of(found), adapter.findDocenteIdByUsuario(usuarioId));
        assertEquals(Optional.of(found), adapter.findEstudianteIdByUsuario(usuarioId));
        assertEquals(Optional.of(found), adapter.findProgramaIdByCoordinadorUsuario(usuarioId));
        assertEquals(Optional.of(found), adapter.findCoordinadorIdByUsuario(usuarioId));
        assertEquals(Optional.of(found), adapter.findFacultadIdByDecanoUsuario(usuarioId));
        assertEquals(Optional.of(found), adapter.findDecanoIdByUsuario(usuarioId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void querySingleUuid_traduce_error_jdbc_a_databaseOperationException() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexion"));

        assertThrows(DatabaseOperationException.class, () -> adapter.findUsuarioIdById(UUID.randomUUID()));
    }

    @Test
    void canDocenteAccessGrupo_evalua_conteo_positivo_y_cero() {
        final UUID usuarioId = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(1, 0);

        assertTrue(adapter.canDocenteAccessGrupo(usuarioId, grupoId));
        assertFalse(adapter.canDocenteAccessGrupo(usuarioId, grupoId));
    }

    @Test
    void canEstudianteAccessGrupo_evalua_conteo() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(2);

        assertTrue(adapter.canEstudianteAccessGrupo(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void canCoordinadorAccessPrograma_evalua_conteo() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(1);

        assertTrue(adapter.canCoordinadorAccessPrograma(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void canDecanoAccessFacultad_con_conteo_nulo_retorna_falso() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(null);

        assertFalse(adapter.canDecanoAccessFacultad(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void queryCount_traduce_error_jdbc_a_databaseOperationException() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexion"));

        assertThrows(DatabaseOperationException.class,
                () -> adapter.canDocenteAccessGrupo(UUID.randomUUID(), UUID.randomUUID()));
    }
}
